package kaptainwutax.tungsten.agent;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AgentShapeContext implements CollisionContext {

    protected static final CollisionContext ABSENT = new AgentShapeContext(false, -1.7976931348623157E308, ItemStack.EMPTY, fluidState -> false) {
        @Override
        public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
            return defaultValue;
        }
    };

    private final boolean descending;
    private final double minY;
    private final ItemStack heldItem;
    private final Predicate<FluidState> walkOnFluidPredicate;

    protected AgentShapeContext(boolean descending, double minY, ItemStack heldItem, Predicate<FluidState> walkOnFluidPredicate) {
        this.descending = descending;
        this.minY = minY;
        this.heldItem = heldItem;
        this.walkOnFluidPredicate = walkOnFluidPredicate;
    }

    protected AgentShapeContext(Agent agent) {
        this(agent.input.playerInput.sneak(), agent.box.minY, ItemStack.EMPTY, fluidState -> false);
    }

    @Override
    public boolean isHoldingItem(Item item) {
        return this.heldItem.is(item);
    }

    @Override
    public boolean canStandOnFluid(FluidState state, FluidState fluidState) {
        return this.walkOnFluidPredicate.test(fluidState) && !state.getType().isSame(fluidState.getType());
    }

    @Override
    public boolean isDescending() {
        return this.descending;
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
        return this.minY > (double)pos.getY() + shape.max(Direction.Axis.Y) - (double)1.0E-5f;
    }

	// getCollisionShape not in ShapeContext interface in MC 1.21 — removed @Override
	// public VoxelShape getCollisionShape(BlockState state, CollisionView world, BlockPos pos) { return null; }

	@Override
	public VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.CollisionGetter world, BlockPos pos) {
	    return state.getCollisionShape(world, pos);
	}
	@Override
	public boolean alwaysCollideWithFluid() { return false; }

}
