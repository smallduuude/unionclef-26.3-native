package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Brushes armadillos until {@code armadillo_scute} is collected.
 *
 * <p>26.3's {@code Armadillo.mobInteract} checks for a brush in the interacting hand and calls
 * {@code brushOffScute}, which drops the {@code gameplay/armadillo_brush} loot (a scute) as an item
 * entity — babies cannot be brushed, so they are filtered out. A single right-click per scute is
 * enough, but the scute lands on the ground, so the parent {@link ResourceTask}'s normal pickup gate
 * is what actually collects it. The armadillo is skittish and rolls away, so the pursuit is the
 * shared {@link AbstractDoToEntityTask} machinery rather than a straight-line walk.
 */
public class BrushArmadilloTask extends ResourceTask {

    private static final long SEARCH_BUDGET_MS = 6 * 60_000L;
    private static final long TASK_BUDGET_MS = 10 * 60_000L;
    private static final int MAX_BRUSH_ATTEMPTS = 40;
    private static final long BRUSH_COOLDOWN_MS = 400L;

    private final Item scute = Items.ARMADILLO_SCUTE;

    private long startedMs = 0L;
    private int attempts = 0;
    private boolean gaveUp = false;

    public BrushArmadilloTask(int count) {
        super(new ItemTarget(Items.ARMADILLO_SCUTE, count));
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        startedMs = System.currentTimeMillis();
        Debug.logMessage("Brush: looking for armadillos to brush for " + itemId(scute));
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
            giveUp("spent " + (TASK_BUDGET_MS / 60_000L) + " min without collecting " + itemId(scute));
            return null;
        }
        if (attempts >= MAX_BRUSH_ATTEMPTS) {
            giveUp("brushed " + attempts + " times but no " + itemId(scute) + " was collected");
            return null;
        }

        // Armadillos spawn in the overworld (savanna / badlands).
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Going to the overworld for armadillos");
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        if (!mod.getItemStorage().hasItem(Items.BRUSH)) {
            setDebugState("Obtaining a brush");
            return TaskCatalogue.getItemTask(Items.BRUSH, 1);
        }

        if (!mod.getEntityTracker().entityFound(entity -> !((Armadillo) entity).isBaby(),
                Armadillo.class)) {
            if (now - startedMs > SEARCH_BUDGET_MS) {
                giveUp("no armadillo found nearby in " + (SEARCH_BUDGET_MS / 60_000L) + " min");
                return null;
            }
            setDebugState("Searching for an armadillo");
            return new TimeoutWanderTask();
        }

        setDebugState("Approaching an armadillo with a brush");
        return new BrushArmadilloSubTask();
    }

    private static String itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? item.toString() : id.toString();
    }

    private void giveUp(String reason) {
        if (gaveUp) return;
        gaveUp = true;
        setDebugState("Gave up: " + reason);
        Debug.logWarning("Brush: gave up on " + itemId(scute) + " — " + reason);
    }

    @Override
    public boolean isFinished() {
        return gaveUp || super.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof BrushArmadilloTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Brushing armadillos for " + itemId(scute);
    }

    private class BrushArmadilloSubTask extends AbstractDoToEntityTask {

        private long lastBrushMs = 0L;

        BrushArmadilloSubTask() {
            super(0, -1, -1);
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof BrushArmadilloSubTask;
        }

        @Override
        protected Optional<Entity> getEntityTarget(AltoClef mod) {
            return mod.getEntityTracker().getClosestEntity(mod.getPlayer().position(),
                    entity -> !((Armadillo) entity).isBaby(), Armadillo.class);
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (mod.getPlayer().getMainHandItem().getItem() != Items.BRUSH) {
                if (!mod.getItemStorage().hasItem(Items.BRUSH)) {
                    setDebugState("Waiting for a brush");
                    return null;
                }
                setDebugState("Equipping the brush");
                mod.getSlotHandler().forceEquipItem(Items.BRUSH);
                return null;
            }
            long now = System.currentTimeMillis();
            if (now - lastBrushMs < BRUSH_COOLDOWN_MS) {
                return null;
            }
            lastBrushMs = now;
            attempts++;
            setDebugState("Brushing an armadillo for a scute");
            mod.getController().interact(mod.getPlayer(), entity,
                    new EntityHitResult(entity), InteractionHand.MAIN_HAND);
            return null;
        }

        @Override
        protected String toDebugString() {
            return "Brushing an armadillo";
        }
    }
}
