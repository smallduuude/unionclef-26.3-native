package adris.altoclef.multiversion;

import net.minecraft.world.level.block.state.BlockState;

public class BlockStateVer {


    @Pattern
    private static boolean isSolid(BlockState state) {
        return state.isSolid();
    }

    @Pattern
    private static boolean isReplaceable(BlockState state) {
        return state.canBeReplaced();
    }

    @Pattern
    private static float getHardness(BlockState state) {
        return state.getBlock().defaultDestroyTime();
    }

}
