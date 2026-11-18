package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;

public class CoverWithBlocksTask extends Task {

    private static final TimerGame timer = new TimerGame(30);
    private static final Task goToNether = new DefaultGoToDimensionTask(Dimension.NETHER);
    private static final Task goToOverworld = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
    private static Task getBlocks;
    private BlockPos lavaPos;

    @Override
    protected void onStart() {
        timer.reset();
        // No explicit tracking needed — BlockScanner handles discovery automatically
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        Item[] throwAways = mod.getModSettings().getThrowawayItems(true);
        Item[] throwAwaysToUse = Arrays.stream(throwAways)
                .filter(item -> !(Block.byItem(item) instanceof LeavesBlock)
                        && !(Block.byItem(item) instanceof FallingBlock)
                        && item instanceof BlockItem)
                .toArray(Item[]::new);
        int throwAwayCount = mod.getItemStorage().getItemCount(throwAwaysToUse);

        if (getBlocks != null && throwAwayCount < 128) {
            setDebugState("Getting blocks to cover nether lava.");
            timer.reset();
            return getBlocks;
        } else {
            getBlocks = null;
        }

        Block[] blocks = ItemHelper.itemsToBlocks(throwAwaysToUse);
        boolean hasAnyNearby = mod.getBlockScanner().getNearestBlock(pos -> true, blocks).isPresent();
        if (!mod.getItemStorage().hasItem(throwAwaysToUse) && hasAnyNearby) {
            timer.reset();
            ItemTarget throwAwaysTarget = new ItemTarget(throwAwaysToUse);
            getBlocks = new MineAndCollectTask(throwAwaysTarget, blocks, MiningRequirement.STONE);
            return getBlocks;
        }
        if (!mod.getItemStorage().hasItem(throwAwaysToUse) && !hasAnyNearby) {
            timer.reset();
            if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
                setDebugState("Trying nether to search for blocks.");
                return goToNether;
            }
            if (WorldHelper.getCurrentDimension() == Dimension.NETHER) {
                setDebugState("Trying overworld to search for blocks.");
                return goToOverworld;
            }
        }
        if (WorldHelper.getCurrentDimension() != Dimension.NETHER) {
            setDebugState("Going to nether.");
            timer.reset();
            return goToNether;
        }

        Task place = coverLavaWithBlocks(mod);
        if (place == null) {
            setDebugState("Searching valid lava.");
            timer.reset();
            return new TimeoutWanderTask();
        }
        setDebugState("Covering lava with blocks");
        return place;
    }

    private Task coverLavaWithBlocks(AltoClef mod) {
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
                || !WorldHelper.isAir(lavaPos.above())
                || !mod.getWorld().getBlockState(lavaPos).getFluidState().isSource()) {
            lavaPos = lava.get();
            timer.reset();
        }

        Item[] throwAways = mod.getModSettings().getThrowawayItems(true);
        Item[] throwAwaysToUse = Arrays.stream(throwAways)
                .filter(item -> !(Block.byItem(item) instanceof LeavesBlock)
                        && !(Block.byItem(item) instanceof FallingBlock)
                        && item instanceof BlockItem)
                .toArray(Item[]::new);
        List<Slot> presentThrowAways = mod.getItemStorage().getSlotsWithItemPlayerInventory(true, throwAwaysToUse);
        if (!presentThrowAways.isEmpty()) {
            for (Slot slot : presentThrowAways) {
                Item item = StorageHelper.getItemStackInSlot(slot).getItem();
                Block block = Block.byItem(item);
                return new PlaceBlockTask(lavaPos, block);
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        // BlockScanner handles cleanup automatically
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof CoverWithBlocksTask;
    }

    @Override
    protected String toDebugString() {
        return "Covering nether lava with blocks";
    }
}
