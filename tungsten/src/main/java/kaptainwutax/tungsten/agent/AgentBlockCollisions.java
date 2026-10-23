package kaptainwutax.tungsten.agent;

import com.google.common.collect.AbstractIterator;

import kaptainwutax.tungsten.TungstenConfig;
import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.render.Cube;
import kaptainwutax.tungsten.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AgentBlockCollisions extends AbstractIterator<VoxelShape> {

    private final AABB box;
    private final CollisionContext context;
    private final Cursor3D blockIterator;
    private final BlockPos.MutableBlockPos pos;
    private final VoxelShape boxShape;
    private final CollisionGetter world;
    private final boolean forEntity;
    private BlockGetter chunk;
    private long chunkPos;
    public int scannedBlocks;

    /** Intersection of both anvil rotations — narrow center, no edges.
     *  0.5 wide is enough for 0.6-wide player to stand at center only. */
    private static final VoxelShape ANVIL_SAFE_SHAPE =
            Shapes.box(0.25, 0, 0.25, 0.75, 1.0, 0.75);

    public AgentBlockCollisions(CollisionGetter world, Agent agent, AABB box) {
        this(world, agent, box, false);
    }

    public AgentBlockCollisions(CollisionGetter world, Agent agent, AABB box, boolean forEntity) {
        this.context = new AgentShapeContext(agent);
        this.pos = new BlockPos.MutableBlockPos();
        this.boxShape = Shapes.create(box);
        this.world = world;
        this.box = box;
        this.forEntity = forEntity;
        int i = Mth.floor(box.minX - 1.0E-7D) - 1;
        int j = Mth.floor(box.maxX + 1.0E-7D) + 1;
        int k = Mth.floor(box.minY - 1.0E-7D) - 1;
        int l = Mth.floor(box.maxY + 1.0E-7D) + 1;
        int m = Mth.floor(box.minZ - 1.0E-7D) - 1;
        int n = Mth.floor(box.maxZ + 1.0E-7D) + 1;
        this.blockIterator = new Cursor3D(i, k, m, j, l, n);
    }

    @Nullable
    private BlockGetter getChunk(int x, int z) {
        int i = SectionPos.blockToSectionCoord(x);
        int j = SectionPos.blockToSectionCoord(z);
        long l = ChunkPos.pack(i, j);

        if(this.chunk != null && this.chunkPos == l) {
            return this.chunk;
        } else {
            BlockGetter blockView = this.world.getChunkForCollisions(i, j);
            this.chunk = blockView;
            this.chunkPos = l;
            return blockView;
        }
    }

    protected VoxelShape computeNext() {
//        while(true) {
//            if (this.blockIterator.step()) {
//                int i = this.blockIterator.getX();
//                int j = this.blockIterator.getY();
//                int k = this.blockIterator.getZ();
//                int l = this.blockIterator.getEdgeCoordinatesCount();
//
//                if(l == 3) {
//                    continue;
//                }
//
//                /*
//                BlockView blockView = this.getChunk(i, k);
//
//                if(blockView == null) {
//                    continue;
//                }*/
//
//                BlockView blockView = this.world;
//
//                this.pos.set(i, j, k);
//                BlockState blockState = blockView.getBlockState(this.pos);
//                this.scannedBlocks++;
//
//                if(this.forEntity && !blockState.shouldSuffocate(blockView, this.pos) || l == 1 && !blockState.exceedsCube()
//                    || l == 2 && !blockState.isOf(Blocks.MOVING_PISTON)) {
//                    continue;
//                }
//
//                VoxelShape voxelShape = blockState.getCollisionShape(this.world, this.pos, this.context);
//
//                if(voxelShape == VoxelShapes.fullCube()) {
//                    if(!this.box.intersects((double)i, (double)j, (double)k, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D)) {
//                        continue;
//                    }
//
//                    return voxelShape.offset((double)i, (double)j, (double)k);
//                }
//
//                VoxelShape voxelShape2 = voxelShape.offset((double)i, (double)j, (double)k);
//
//                if(!VoxelShapes.matchesAnywhere(voxelShape2, this.boxShape, BooleanBiFunction.AND)) {
//                    continue;
//                }
//
//                return voxelShape2;
//            }
//
//            return this.endOfData();
//        }
    	while (this.blockIterator.advance()) {
			int i = this.blockIterator.nextX();
			int j = this.blockIterator.nextY();
			int k = this.blockIterator.nextZ();
			int l = this.blockIterator.getNextType();
			if (l != 3) {
				BlockGetter blockView = this.getChunk(i, k);
				if (blockView != null) {
					this.pos.set(i, j, k);
					BlockState blockState = blockView.getBlockState(this.pos);
					if ((!this.forEntity || blockState.isSuffocating(blockView, this.pos))
						&& (l != 1 || blockState.hasLargeCollisionShape())
						&& (l != 2 || blockState.is(Blocks.MOVING_PISTON))) {
						VoxelShape voxelShape = blockState.getCollisionShape(this.world, this.pos, this.context);

						// ViaVersion: anvil rotation may differ from client —
						// use intersection of both rotations (center square)
						if (TungstenConfig.get().avoidStuckAnvil
								&& blockState.getBlock() instanceof AnvilBlock) {
							voxelShape = ANVIL_SAFE_SHAPE;
						}

						if (voxelShape == Shapes.block()) {
							if (this.box.intersects((double)i, (double)j, (double)k, (double)i + 1.0, (double)j + 1.0, (double)k + 1.0)) {
								return voxelShape.move((double)i, (double)j, (double)k);
							}
						} else {
							VoxelShape voxelShape2 = voxelShape.move((double)i, (double)j, (double)k);
							if (!voxelShape2.isEmpty() && Shapes.joinIsNotEmpty(voxelShape2, this.boxShape, BooleanOp.AND)) {
								return voxelShape2;
							}
						}
					}
				}
			}
		}

		return this.endOfData();
    }

}

