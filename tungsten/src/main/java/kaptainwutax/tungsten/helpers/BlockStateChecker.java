package kaptainwutax.tungsten.helpers;

import static kaptainwutax.tungsten.path.blockSpaceSearchAssist.Ternary.NO;
import static kaptainwutax.tungsten.path.blockSpaceSearchAssist.Ternary.YES;

import kaptainwutax.tungsten.TungstenConfig;
import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.path.blockSpaceSearchAssist.Ternary;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.Fluids;

/**
 * Helper class to easily check block state.
 */
public class BlockStateChecker {
	
	public static Ternary fullyPassableBlockState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof AirBlock) { // early return for most common case
            return YES;
        }
        // exceptions - blocks that are isPassable true, but we can't actually jump through
        if (block instanceof FireBlock
                || block == Blocks.TRIPWIRE
                || block == Blocks.COBWEB
                || block == Blocks.VINE
                || block == Blocks.LADDER
                || block == Blocks.COCOA
                || block instanceof AzaleaBlock
                || block instanceof DoorBlock
                || block instanceof FenceGateBlock
                || !state.getFluidState().isEmpty()
                || block instanceof TrapDoorBlock
                || block instanceof EndPortalBlock
                || block instanceof SkullBlock
                || block instanceof ShulkerBoxBlock) {
            return NO;
        }
        return YES;
    }
	
	/**
     * Checks if a block is connected to another of its type.
     * 
     * @param pos Position of the block
     * @return true if a block is connected to another of its type. Example: fence to fence
     */
	public static boolean isConnected(BlockPos pos, LevelReader world) {
	    BlockState state = world.getBlockState(pos);
	    // Off-thread planning reads a live world that can return null mid chunk-swap; a block that
	    // is not there is not connected to anything. See Agent.isClimbing for the full note.
	    if (state == null) return false;
	    Block block = state.getBlock();

	    // ViaVersion: any fence-like block with an adjacent fence-like neighbor
	    // is considered connected regardless of client-side connection state.
	    // The server may have connections the 1.21 client doesn't render.
	    if (TungstenConfig.get().avoidStuckFence && isFenceLikeBlock(state)
	            && hasFenceLikeNeighbor(world, pos)) {
	        return true;
	    }

	    // Check for Fence connections
	    if (block instanceof FenceBlock) {
	        return isFenceConnected(state, world, pos);
	    }

	    // Check for Wall connections
	    if (block instanceof WallBlock) {
	        return isWallConnected(state);
	    }

	    // Check for Glass Pane connections
	    if (block instanceof IronBarsBlock) {
	        return isGlassPaneConnected(state, world, pos);
	    }

	    return false;
	}

	private static boolean isFenceLikeBlock(BlockState state) {
	    if (state == null) return false;   // off-thread null read (see Agent.isClimbing)
	    return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)
	            || state.getBlock() instanceof IronBarsBlock;
	}

	private static boolean hasFenceLikeNeighbor(LevelReader world, BlockPos pos) {
	    return isFenceLikeBlock(world.getBlockState(pos.north()))
	        || isFenceLikeBlock(world.getBlockState(pos.south()))
	        || isFenceLikeBlock(world.getBlockState(pos.east()))
	        || isFenceLikeBlock(world.getBlockState(pos.west()));
	}

	/**
     * Checks if a block is connected to another of its type.
     * 
     * @param state BlockState for this block
     * @param world
     * @param pos Position of the block
     * @return true if a block is connected to another of its type. Example: fence to fence
     */
	public static boolean isFenceConnected(BlockState state, LevelReader world, BlockPos pos) {
	    return state.getValue(BlockStateProperties.NORTH) && isFence(world, pos.north())
	        || state.getValue(BlockStateProperties.SOUTH) && isFence(world, pos.south())
	        || state.getValue(BlockStateProperties.EAST) && isFence(world, pos.east())
	        || state.getValue(BlockStateProperties.WEST) && isFence(world, pos.west());
	}

	public static boolean isFence(LevelReader world, BlockPos pos) {
	    Block block = world.getBlockState(pos).getBlock();
	    return block instanceof FenceBlock;
	}

	/** Fence, wall, iron bars, glass pane — any block that forms
	 *  thin connection bars to adjacent blocks of the same type. */
	public static boolean isFenceOrWall(LevelReader world, BlockPos pos) {
	    BlockState state = world.getBlockState(pos);
	    if (state == null) return false;   // off-thread null read (see Agent.isClimbing)
	    return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)
	            || state.getBlock() instanceof IronBarsBlock;
	}

	public static boolean isAnvil(LevelReader world, BlockPos pos) {
	    return world.getBlockState(pos).getBlock() instanceof AnvilBlock;
	}

	/**
     * Checks if a wall block is connected to anything.
     * 
     * @param state BlockState for this block
     * @return true if wall block is connected to anything.
     */
	public static boolean isWallConnected(BlockState state) {
	    return state.getValue(BlockStateProperties.NORTH_WALL) != WallSide.NONE
	        || state.getValue(BlockStateProperties.SOUTH_WALL) != WallSide.NONE
	        || state.getValue(BlockStateProperties.EAST_WALL) != WallSide.NONE
	        || state.getValue(BlockStateProperties.WEST_WALL) != WallSide.NONE;
	}

	public static boolean isGlassPaneConnected(BlockState state, LevelReader world, BlockPos pos) {
	    return state.getValue(BlockStateProperties.NORTH)
	        || state.getValue(BlockStateProperties.SOUTH)
	        || state.getValue(BlockStateProperties.EAST)
	        || state.getValue(BlockStateProperties.WEST);
	}

	public static boolean isPane(LevelReader world, BlockPos pos) {
	    Block block = world.getBlockState(pos).getBlock();
	    return block instanceof IronBarsBlock;
	}
	
	public static boolean isSlab(LevelReader world, BlockPos pos) {
	    return world.getBlockState(pos).getBlock() instanceof SlabBlock;
	}

	/**
     * Checks if a block is a double slab.
     * 
     * @param world
     * @param pos Position of the block
     * @return true if a block is a double slab.
     */
	public static boolean isDoubleSlab(LevelReader world, BlockPos pos) {
	    BlockState state = world.getBlockState(pos);
	    Block block = state.getBlock();
	    return block instanceof SlabBlock && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE;
	}
	
	public static boolean isTrapdoor(BlockState state) {
        return state.getBlock() instanceof TrapDoorBlock;
    }
	
	public static boolean isTrapdoor(Block block) {
        return block instanceof TrapDoorBlock;
    }

    // Helper method to check if the block is an open trapdoor
	public static boolean isOpenTrapdoor(BlockState state) {
        return isTrapdoor(state) && state.getValue(BlockStateProperties.OPEN);
    }
	
	public static boolean isClosedBottomTrapdoor(BlockState state) {
        return isTrapdoor(state) && state.getValue(BlockStateProperties.HALF) == Half.BOTTOM && !state.getValue(BlockStateProperties.OPEN);
    }

    // Helper method to check if the block is a bottom slab
	public static boolean isBottomSlab(BlockState state) {
        return state.getBlock() instanceof SlabBlock && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM;
    }
	
    // Helper method to check if the block is a top slab
	public static boolean isTopSlab(BlockState state) {
        return state.getBlock() instanceof SlabBlock && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP;
    }
	
    // Helper method to check if the block contains water
	public static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER) || state.getFluidState().is(Fluids.WATER);
    }
	
    // Helper method to check if the block is flowing water
	public static boolean isFlowingWater(BlockState state) {
        return state.getFluidState().is(Fluids.FLOWING_WATER);
    }
	
	// Helper method to check if the block is water. Either flowing or source
	public static boolean isAnyWater(BlockState state) {
        return isWater(state) || isFlowingWater(state);
    }
	

    // Helper method to check if the block is water logged
	public static boolean isWaterLogged(BlockState state) {
        return state.getProperties().contains(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED) == true;
    }

    // Helper method to check if the block is lava
	static boolean isLava(BlockState state) {
        return state.is(Blocks.LAVA) || state.getFluidState().is(Fluids.LAVA);
    }

    // Helper method to check if the block is flowing lava
	static boolean isFlowingLava(BlockState state) {
        return state.getFluidState().is(Fluids.FLOWING_LAVA);
    }

	// Helper method to check if the block is lava. Either flowing or source
	public static boolean isAnyLava(BlockState state) {
        return isLava(state) || isFlowingLava(state);
    }


}
