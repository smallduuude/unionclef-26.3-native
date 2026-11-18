package adris.altoclef.tasks.examples;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.misc.PositionWrapper;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasks.misc.ChooseStrategyTask;
import adris.altoclef.util.helpers.WorldHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ExampleStrategyTask extends Task {

    public ExampleStrategyTask() {
    }

    // Define your strategy enum
    enum Strategy {
        LOOT_CHEST,
        KILL_PLAYER,
        DIAMONDS,
        SOME_STRATEGY
    }


    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();
        mod.getBehaviour().push();
        // BlockScanner doesn't need trackBlock - scanning is automatic
        mod.getBehaviour().addProtectedItems(Items.COBBLESTONE);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        // Create strategy map
        Map<Strategy, PositionWrapper> strategyMap = new HashMap<>();
        Vec3 pos = mod.getPlayer().position();
        Optional<Entity> ply = mod.getEntityTracker().getClosestEntity(
                mod.getPlayer().position(),
                toPunk -> toPunk.distanceTo(mod.getPlayer()) < 50,
                Player.class, Zombie.class, Pig.class

        );

        Optional<BlockPos> cont = mod.getBlockScanner().getNearestBlock(
                blockPos -> WorldHelper.isUnopenedChest(blockPos) &&
                        mod.getPlayer().blockPosition().closerThan(blockPos, 50), Blocks.CHEST);
        Optional<BlockPos> ore = mod.getBlockScanner().getNearestBlock(
                blockPos -> mod.getPlayer().blockPosition().closerThan(blockPos, 50) &&
                        WorldHelper.canReach(blockPos), Blocks.DIAMOND_ORE);
        // Debug.logMessage("cont "+ cont.orElse(null));
        strategyMap.put(Strategy.LOOT_CHEST, PositionWrapper.ofBlockPos(cont.orElse(null),
            (mod_next, p) -> WorldHelper.isUnopenedChest(p)));
        strategyMap.put(Strategy.DIAMONDS, PositionWrapper.ofBlockPos(ore.orElse(null),
            (mod_next, p) -> !WorldHelper.isAir(p)));
        strategyMap.put(Strategy.KILL_PLAYER, PositionWrapper.ofEntity(ply.orElse(null),
            (mod_next, e) -> e != null && e.isAlive() && e.closerThan(mod_next.getPlayer(), 50)));
// Can also put empty positions:
        //strategyMap.put(Strategy.SOME_STRATEGY, PositionWrapper.empty());

        // Create the task
        return new ChooseStrategyTask<>(
                strategy -> {
                    // Your strategy handler
                    switch (strategy) {
                        case LOOT_CHEST:
                            setDebugState("LOOT_CHEST");
                            return new InteractWithBlockTask(cont.orElse(null));
                        case KILL_PLAYER:
                            setDebugState("KILL_PLAYER");
                            return new KillEntityTask(ply.orElse(null));
                        case DIAMONDS:
                            setDebugState("Diamonds");
                            return new DestroyBlockTask(ore.orElse(null));
                        default: {
                            setDebugState("No strategy");
                            return null;
                        }
                    }
                },
                strategyMap
        );

        //return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof ExampleStrategyTask;
    }

    @Override
    protected String toDebugString() {
        return "ExampleStrategyTask";
    }

}
