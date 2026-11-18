package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CoverWithSandTask extends Task {

    private static final TimerGame timer = new TimerGame(30);
    private static final Task getSand = TaskCatalogue.getItemTask(Items.SAND, 128);
    private static final Task goToNether = new DefaultGoToDimensionTask(Dimension.NETHER);
    private static final Task goToOverworld = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
    private BlockPos lavaPos;

    @Override
    protected void onStart() {
        timer.reset();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        if (getSand != null && getSand.isActive() && !getSand.isFinished()) {
            setDebugState("Getting sand to cover nether lava.");
            timer.reset();
            return getSand;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD
                && mod.getItemStorage().getItemCount(Items.SAND) < 64) {
            timer.reset();
            return getSand;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD
                && mod.getItemStorage().getItemCount(Items.SAND) > 64) {
            setDebugState("Going to nether.");
            timer.reset();
            return goToNether;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.NETHER
                && !mod.getItemStorage().hasItem(Items.SAND)) {
            setDebugState("Going to overworld to get sand.");
            timer.reset();
            return goToOverworld;
        }

        Task place = coverLavaWithSand(mod);
        if (place == null) {
            setDebugState("Searching valid lava.");
            timer.reset();
            return new TimeoutWanderTask();
        }
        setDebugState("Covering lava with sand");
        return place;
    }

    private Task coverLavaWithSand(AltoClef mod) {
        Predicate<BlockPos> validLava = blockPos ->
                mod.getWorld().getBlockState(blockPos).getFluidState().isSource()
                        && WorldHelper.isAir(blockPos.above())
                        && (!WorldHelper.isBlock(blockPos.north(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.south(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.east(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.west(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.north().above(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.south().above(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.east().above(), Blocks.LAVA)
                        || !WorldHelper.isBlock(blockPos.west().above(), Blocks.LAVA));

        Optional<BlockPos> lava = mod.getBlockScanner().getNearestBlock(validLava, Blocks.LAVA);
        if (lava.isEmpty()) return null;

        if (lavaPos == null || timer.elapsed()) {
            lavaPos = lava.get();
            timer.reset();
        }
        if (!WorldHelper.isBlock(lavaPos, Blocks.LAVA)
                || (!WorldHelper.isAir(lavaPos.above()) && !WorldHelper.isFallingBlock(lavaPos.above()))
                || !mod.getWorld().getBlockState(lavaPos).getFluidState().isSource()) {
            lavaPos = lava.get();
            timer.reset();
        }

        return new PlaceBlockTask(lavaPos.above(), Blocks.SAND);
    }

    @Override
    protected void onStop(Task interruptTask) {
        // BlockScanner handles cleanup automatically
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof CoverWithSandTask;
    }

    @Override
    protected String toDebugString() {
        return "Covering nether lava with sand";
    }
}
