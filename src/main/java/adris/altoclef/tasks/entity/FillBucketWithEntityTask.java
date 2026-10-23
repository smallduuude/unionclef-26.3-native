package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.resources.CollectBucketLiquidTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Makes a mob bucket by using the right container item on the right live mob:
 * a water bucket on a cod/salmon/pufferfish/tropical fish/tadpole/axolotl, or an empty bucket on a
 * (non-baby) sulfur cube. The result replaces the held stack, so the parent {@link ResourceTask}
 * sees the bucket in the inventory and finishes.
 *
 * <p>The interaction itself is {@code MultiPlayerGameMode.interact(player, entity, hitResult, hand)}
 * — the same path a player's right-click takes, which is also what
 * {@link adris.altoclef.tasks.resources.CollectMilkTask} uses. The mob's own {@code mobInteract}
 * then runs the vanilla {@code Bucketable.bucketMobPickup} and consumes the held bucket; the server
 * only accepts the packet when the entity is within interaction range, so the approach is left to
 * {@link AbstractDoToEntityTask}, whose in-range gate is {@code canHitEntity} (line of sight and
 * well inside the server's range check).
 *
 * <p>Verification and retry: vanilla gives no client-visible "success" signal other than the held
 * water bucket turning into the filled one, so after each attempt the task re-checks the inventory
 * from the parent, re-equips a fresh container item if the bucket was consumed but the target did
 * not appear, and never spends more than {@link #MAX_INTERACT_ATTEMPTS} clicks or
 * {@link #TASK_BUDGET_MS} on one request before giving up honestly.
 */
public class FillBucketWithEntityTask extends ResourceTask {

    /** Give up if the mob cannot even be found for this long. */
    private static final long SEARCH_BUDGET_MS = 6 * 60_000L;
    /** Absolute budget for a whole request, including chasing elusive fish. */
    private static final long TASK_BUDGET_MS = 12 * 60_000L;
    /** Clicks one request may spend before admitting the interaction is not working. */
    private static final int MAX_INTERACT_ATTEMPTS = 25;
    /** Minimum gap between two interact packets against the same mob. */
    private static final long INTERACT_COOLDOWN_MS = 400L;

    private final Item targetBucket;
    private final Item requiredItem;
    private final String entityName;
    private final Predicate<Entity> entityFilter;
    private final Class[] entityClasses;

    private long startedMs = 0L;
    private int attempts = 0;
    private boolean gaveUp = false;

    public FillBucketWithEntityTask(Item targetBucket, int count, Item requiredItem,
                                    String entityName, Predicate<Entity> entityFilter,
                                    List<Class<? extends Entity>> entityClasses) {
        super(new ItemTarget(targetBucket, count));
        this.targetBucket = targetBucket;
        this.requiredItem = requiredItem;
        this.entityName = entityName;
        this.entityFilter = entityFilter;
        // Raw array on purpose: EntityTracker's lookup API is `Class...`, and the exact runtime
        // class of each mob is what it tracks.
        this.entityClasses = entityClasses.toArray(new Class[0]);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        startedMs = System.currentTimeMillis();
        Debug.logMessage("Bucket: catching a " + entityName + " with a " + itemId(requiredItem)
                + " to make " + itemId(targetBucket));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (gaveUp) return null;
        if (startedMs == 0L) startedMs = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        if (now - startedMs > TASK_BUDGET_MS) {
            giveUp("spent " + (TASK_BUDGET_MS / 60_000L) + " min and never caught a " + entityName);
            return null;
        }
        if (attempts >= MAX_INTERACT_ATTEMPTS) {
            giveUp("used the " + itemId(requiredItem) + " " + attempts
                    + " times but no " + itemId(targetBucket) + " appeared");
            return null;
        }

        // Every bucketed mob lives in the overworld.
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Going to the overworld for " + entityName);
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        // We need a fresh container item for every attempt: the mob consumes the held bucket.
        if (!mod.getItemStorage().hasItem(requiredItem)) {
            setDebugState("Obtaining a " + itemId(requiredItem));
            return requiredItem == Items.WATER_BUCKET
                    ? new CollectBucketLiquidTask.CollectWaterBucketTask(1)
                    : TaskCatalogue.getItemTask(requiredItem, 1);
        }

        if (!mod.getEntityTracker().entityFound(entityFilter, entityClasses)) {
            if (now - startedMs > SEARCH_BUDGET_MS) {
                giveUp("no " + entityName + " found nearby in " + (SEARCH_BUDGET_MS / 60_000L)
                        + " min");
                return null;
            }
            setDebugState("Searching for a " + entityName);
            return new TimeoutWanderTask();
        }

        setDebugState("Approaching a " + entityName + " with a " + itemId(requiredItem));
        return new BucketMobTask();
    }

    private String itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? item.toString() : id.toString();
    }

    private void giveUp(String reason) {
        if (gaveUp) return;
        gaveUp = true;
        setDebugState("Gave up: " + reason);
        Debug.logWarning("Bucket: gave up on " + itemId(targetBucket) + " — " + reason);
    }

    @Override
    public boolean isFinished() {
        return gaveUp || super.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof FillBucketWithEntityTask task
                && task.targetBucket == targetBucket
                && task.requiredItem == requiredItem
                && task.entityName.equals(entityName);
    }

    @Override
    protected String toDebugStringName() {
        return "Catching a " + entityName + " for " + itemId(targetBucket);
    }

    /**
     * Pursues one mob and clicks it with the bucket. {@link AbstractDoToEntityTask} owns the part
     * that is hard (target choice, approach, reachability/budget recovery, wander fallback); this
     * class only supplies "which mob" and the actual right-click.
     */
    private class BucketMobTask extends AbstractDoToEntityTask {

        private long lastInteractMs = 0L;

        BucketMobTask() {
            // Same shape as CollectMilkTask's cow interaction: no combat standoff distance.
            super(0, -1, -1);
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof BucketMobTask;
        }

        @Override
        protected Optional<Entity> getEntityTarget(AltoClef mod) {
            return mod.getEntityTracker().getClosestEntity(mod.getPlayer().position(),
                    entityFilter, entityClasses);
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (mod.getPlayer().getMainHandItem().getItem() != requiredItem) {
                if (!mod.getItemStorage().hasItem(requiredItem)) {
                    setDebugState("Waiting for a " + itemId(requiredItem));
                    return null;
                }
                setDebugState("Equipping a " + itemId(requiredItem));
                mod.getSlotHandler().forceEquipItem(requiredItem);
                return null;
            }
            long now = System.currentTimeMillis();
            if (now - lastInteractMs < INTERACT_COOLDOWN_MS) {
                return null;
            }
            lastInteractMs = now;
            attempts++;
            setDebugState("Using a " + itemId(requiredItem) + " on the " + entityName);
            mod.getController().interact(mod.getPlayer(), entity,
                    new EntityHitResult(entity), InteractionHand.MAIN_HAND);
            return null;
        }

        @Override
        protected String toDebugString() {
            return "Catching a " + entityName + " in a bucket";
        }
    }
}
