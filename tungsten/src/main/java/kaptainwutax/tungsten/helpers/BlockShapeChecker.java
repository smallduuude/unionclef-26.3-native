package kaptainwutax.tungsten.helpers;

import kaptainwutax.tungsten.TungstenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Helper class to easily get blocks shape data.
 */
public class BlockShapeChecker {
	
	/**
     * Calculates the height of a block at given position.
     * 
     * @param pos Position of the block
     * @return the height of a block at given position.
     */
	public static double getBlockHeight(BlockPos pos, LevelReader world) {
		BlockState state = world.getBlockState(pos);
		
		if (state.isAir()) return 0;
		
		VoxelShape shape = state.getCollisionShape(world, pos);
		double height = shape.max(Axis.Y);
		
		return height;
	}
	
	/**
     * Calculates the volume in X and Z axis of a block at given position.
     * 
     * @param pos Position of the block
     * @return the volume of a block at given position.
     */
	public static double getShapeVolume(BlockPos pos, LevelReader world) {
		BlockState state = world.getBlockState(pos);
    	return getShapeVolume(state, pos, world);
    }
	
	/**
     * Calculates the volume in X and Z axis of a block at given position.
     * 
     * @param pos Position of the block
     * @return the volume of a block at given position.
     */
	public static double getShapeVolume(BlockState state, BlockPos pos, LevelReader world) {
		VoxelShape shape = state.getCollisionShape(world, pos);
//        if (shape.isEmpty()) shape = state.getOutlineShape(world, pos);
        
    	return getShapeVolume(shape);
    }
	
	/**
     * Calculates the volume in X and Z axis of a block at given position.
     * 
     * @param pos Position of the block
     * @return the volume of a block at given position.
     */
	public static double getShapeVolume(VoxelShape shape) {
    	
    	double maxX = shape.max(Direction.Axis.X);
    	double minX = shape.min(Direction.Axis.X);
    	double maxZ = shape.max(Direction.Axis.Z);
    	double minZ = shape.min(Direction.Axis.Z);
    	
    	double blockVolume = (maxX - minX) * (maxZ - minZ);
    	
    	if (Double.isInfinite(blockVolume))
        	blockVolume = 0;
    	
    	return blockVolume;
    }

    
    // Helper method to get blocks height
	public static double getBlockHeight(VoxelShape blockShape) {
        return blockShape.max(Axis.Y);
    }
    
	public static double getBlockHeight(LevelReader world, BlockState state, BlockPos pos) {
    	VoxelShape blockShape = state.getCollisionShape(world, pos);
    	
    	return getBlockHeight(blockShape);
    }
    
    public static boolean hasBiggerCollisionShapeThanAbove(LevelReader world, BlockPos pos) {
        // Get the block states of the block at pos and the two blocks above it
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof LadderBlock) return false;
        
        // Calculate the volume of the collision shapes
        double blockVolume = BlockShapeChecker.getShapeVolume(pos, world);
        double aboveBlockVolume1 = BlockShapeChecker.getShapeVolume(pos.above(1), world);
        double aboveBlockVolume2 = BlockShapeChecker.getShapeVolume(pos.above(2), world);
        
        // Compare the volumes
        return blockVolume > aboveBlockVolume1 && blockVolume > aboveBlockVolume2;
    }
	 
   public static boolean isBlockNormalCube(BlockState state) {
	        Block block = state.getBlock();
	        // ⛔ FIXED 2026-09-05: this is an independent duplicate of the already-audited
	        // MovementHelperB.isBlockNormalCube (both port baritone's MovementHelper.java:788-804,
	        // per that method's own citation comment) -- a NO-DUPLICATES drift, since this copy was
	        // missing two of that port's blacklist entries: BambooBlock and PistonExtensionBlock.
	        // Matched to the more complete, citation-verified version rather than picking one
	        // arbitrarily; the (null, null) below is confirmed intentional there too ("this overload
	        // must not consult the world, because it is called from the search for states that may
	        // not be at any position").
	        if (block instanceof net.minecraft.world.level.block.BambooStalkBlock
	                || block instanceof net.minecraft.world.level.block.piston.MovingPistonBlock
	                || block instanceof ScaffoldingBlock
	                || block instanceof ShulkerBoxBlock
	                || block instanceof PointedDripstoneBlock
	                || block instanceof AmethystClusterBlock) {
	            return false;
	        }
	        try {
	            return Block.isShapeFullBlock(state.getCollisionShape(null, null));
	        } catch (Exception ignored) {
	            // if we can't get the collision shape, assume it's bad and add to blocksToAvoid
	        }
	        return false;
    }
}
