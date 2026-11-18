package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.GetAdjacentToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Collects {@code powder_snow_bucket}: use an empty bucket on a powder-snow block.
 *
 * <p>26.3 fills a bucket through {@code BucketItem.use}, which raycasts and then asks any
 * {@code BucketPickup} block for its pickup stack; {@code PowderSnowBlock} implements
 * {@code BucketPickup}, so a plain held right-click on the block while holding an empty bucket
 * returns the {@code powder_snow_bucket} in hand. {@link InteractWithBlockTask} already does exactly
 * that (equip, aim, click, approach if needed), which is why this task is a thin finder: pick the
 * nearest powder-snow block the block scanner knows about and keep interacting until the parent
 * {@link ResourceTask} sees the filled bucket.
 *
 * <p>Powder snow only generates in cold biomes (snowy slopes / peaks and similar), so the block
 * scanner may know none; the task wanders for a budget and then gives up honestly rather than
 * claiming coverage it could not produce.
 */
public class CollectPowderSnowBucketTask extends ResourceTask {

    private static final long SEARCH_BUDGET_MS = 8 * 60_000L;
    /** Per-block budget before moving on to another known powder-snow block. */
    private static final long TARGET_BUDGET_MS = 60_000L;
    private static final int MAX_TARGETS_TRIED = 8;

    private long startedMs = 0L;
    private BlockPos target;
    private long targetSinceMs = 0L;
    private int targetsTried = 0;
    private boolean gaveUp = false;

    public CollectPowderSnowBucketTask(int count) {
        super(new ItemTarget(Items.POWDER_SNOW_BUCKET, count));
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        startedMs = System.currentTimeMillis();
        Debug.logMessage("Bucket: filling an empty bucket with powder snow");
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (gaveUp) return null;
        if (startedMs == 0L) startedMs = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        if (now - startedMs > SEARCH_BUDGET_MS) {
            giveUp("no powder snow could be found in " + (SEARCH_BUDGET_MS / 60_000L) + " min");
            return null;
        }
        if (targetsTried >= MAX_TARGETS_TRIED) {
            giveUp("tried " + targetsTried + " powder-snow blocks without filling the bucket");
            return null;
        }

        if (!mod.getItemStorage().hasItem(Items.BUCKET)) {
            setDebugState("Obtaining an empty bucket");
            return TaskCatalogue.getItemTask(Items.BUCKET, 1);
        }

        if (target != null && now - targetSinceMs > TARGET_BUDGET_MS) {
            mod.getBlockScanner().requestBlockUnreachable(target);
            target = null;
        }

        if (target == null) {
            Optional<BlockPos> nearest =
                    mod.getBlockScanner().getNearestBlock(Blocks.POWDER_SNOW);
            if (nearest.isEmpty()) {
                setDebugState("Searching for powder snow in cold biomes");
                return new TimeoutWanderTask();
            }
            target = nearest.get();
            targetSinceMs = now;
            targetsTried++;
            setDebugState("Found powder snow at " + target.toShortString());
            return null;
        }

        if (LookHelper.getReach(target).isEmpty()) {
            setDebugState("Approaching powder snow at " + target.toShortString());
            return new GetAdjacentToBlockTask(target);
        }

        setDebugState("Filling the bucket with powder snow");
        return new InteractWithBlockTask(new ItemTarget(Items.BUCKET, 1), target);
    }

    private static String itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? item.toString() : id.toString();
    }

    private void giveUp(String reason) {
        if (gaveUp) return;
        gaveUp = true;
        setDebugState("Gave up: " + reason);
        Debug.logWarning("Bucket: gave up on " + itemId(Items.POWDER_SNOW_BUCKET) + " — " + reason);
    }

    @Override
    public boolean isFinished() {
        return gaveUp || super.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectPowderSnowBucketTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + itemId(Items.POWDER_SNOW_BUCKET);
    }
}
