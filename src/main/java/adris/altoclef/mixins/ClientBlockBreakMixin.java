package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockBreakingCancelEvent;
import adris.altoclef.eventbus.events.BlockBreakingEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public final class ClientBlockBreakMixin {

    // for SOME REASON baritone triggers a block cancel breaking every other frame, so we have a 2 frame requirement for that?
    @Unique
    private static int _breakCancelFrames;

    /**
     * A door or a fence gate. Refused at the ONE choke point every break path uses — the planner,
     * the movement executor, altoclef mining tasks and an agent swing all end up in
     * {@code MultiPlayerGameMode} — so no matter which code decided to mine, a door/gate cannot be
     * broken while this mod is loaded. They are opened, not mined.
     */
    @Unique
    private static boolean altoclef$breakRefused(BlockPos pos) {
        if (pos == null) return false;
        var world = Minecraft.getInstance().level;
        if (world == null) return false;
        var block = world.getBlockState(pos).getBlock();
        return block instanceof net.minecraft.world.level.block.DoorBlock
                || block instanceof net.minecraft.world.level.block.FenceGateBlock;
    }

    @Shadow private float destroyProgress;
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private boolean isDestroying;
    @Shadow private int destroyDelay;

    @Inject(
            method = "continueDestroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onBreakUpdate(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if (altoclef$breakRefused(pos)) {
            ci.setReturnValue(false);
            return;
        }
        // ⛔ NEVER CONTINUE A BREAK THE SERVER NEVER STARTED -- baritone BlockBreakHelper.tick: when the
        // controller is not hitting a block (hasBrokenBlock), it calls clickBlock (a fresh
        // START_DESTROY_BLOCK) instead of onPlayerDamageBlock.
        //
        // Vanilla 1.21.11 continues a break whenever destroyBlockPos equals the target, WITHOUT
        // checking isDestroying. After a break the server rejected, isDestroying is false but
        // destroyBlockPos still names the block the server put back; holding attack then mines it
        // again with no START, every STOP reaches a server whose miningPos is unset, and the server
        // restores the block every time. Measured on nav_cliff (2026-09-24): one START, sixteen
        // client-side completions in a row, the stone never gone on the server -- the bot stood
        // mining the same block for the rest of the course. So: not hitting -> start over properly.
        if (!isDestroying && destroyDelay <= 0 && destroyBlockPos != null
                && destroyBlockPos.equals(pos)
                && Minecraft.getInstance().player != null
                && !Minecraft.getInstance().player.getAbilities().instabuild) {
            adris.altoclef.control.BreakResetTrace.freshStarts++;
            ci.setReturnValue(((MultiPlayerGameMode) (Object) this).startDestroyBlock(pos, direction));
            return;
        }
        // A new target while a break has progress throws that progress away (vanilla restarts).
        if (isDestroying && destroyBlockPos != null && !destroyBlockPos.equals(pos)) {
            adris.altoclef.control.BreakResetTrace.noteDiscard("retarget", destroyProgress);
        }
        ClientBlockBreakAccessor breakAccessor = (ClientBlockBreakAccessor) (Minecraft.getInstance().gameMode);
        if (breakAccessor != null) {
            _breakCancelFrames = 2;
            EventBus.publish(new BlockBreakingEvent(pos, breakAccessor.getCurrentBreakingProgress()));
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if (altoclef$breakRefused(pos)) {
            ci.setReturnValue(false);
            return;
        }
        adris.altoclef.control.BreakResetTrace.event("ATTACK", pos == null ? "-" : pos.toShortString(),
                isDestroying, destroyProgress, destroyDelay);
        adris.altoclef.control.BreakResetTrace.starts++;
        adris.altoclef.control.BreakResetTrace.lastStarted = pos == null ? "-" : pos.toShortString()
                + "/" + direction + (isDestroying ? " (was " + destroyBlockPos.toShortString() + ")" : "");
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        adris.altoclef.control.BreakResetTrace.event("BREAK", pos == null ? "-" : pos.toShortString(),
                isDestroying, destroyProgress, destroyDelay);
        adris.altoclef.control.BreakResetTrace.completions++;
        adris.altoclef.control.BreakResetTrace.lastCompleted = pos == null ? "-" : pos.toShortString();
    }

    @Inject(
            method = "stopDestroyBlock",
            at = @At("HEAD")
    )
    private void cancelBlockBreaking(CallbackInfo ci) {
        if (isDestroying) {
            adris.altoclef.control.BreakResetTrace.event("CANCEL", destroyBlockPos == null ? "-" : destroyBlockPos.toShortString(),
                    isDestroying, destroyProgress, destroyDelay);
            adris.altoclef.control.BreakResetTrace.noteDiscard("cancel", destroyProgress);
        }
        if (_breakCancelFrames-- == 0) {
            EventBus.publish(new BlockBreakingCancelEvent());
        }
    }
}
