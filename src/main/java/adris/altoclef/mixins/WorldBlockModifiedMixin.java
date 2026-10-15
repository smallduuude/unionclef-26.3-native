package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Publishes {@link BlockPlaceEvent} when a block becomes a solid/redstone-conductor at a position.
 *
 * <p>26.3 port: the old hook {@code Level.onBlockStateChanged} is gone; {@code sendBlockUpdated}
 * is now abstract (no body to inject into). We hook {@code Level.setBlock(BlockPos, BlockState,
 * int, int)} at HEAD and compare the state currently at the position with the incoming one.
 */
@Mixin(Level.class)
public class WorldBlockModifiedMixin {

    @Unique
    private static boolean hasBlock(BlockState state, BlockPos pos) {
        return !state.isAir() && state.isRedstoneConductor(Minecraft.getInstance().level, pos);
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD")
    )
    public void onBlockWasChanged(BlockPos pos, BlockState newBlock, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        BlockState oldBlock = ((Level) (Object) this).getBlockState(pos);
        if (!hasBlock(oldBlock, pos) && hasBlock(newBlock, pos)) {
            EventBus.publish(new BlockPlaceEvent(pos, newBlock));
        }
    }
}
