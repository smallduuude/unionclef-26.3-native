package kaptainwutax.tungsten.mixin;

import kaptainwutax.tungsten.combat.CombatController;
import kaptainwutax.tungsten.util.WindMouseRotation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into InGameHud.render() — called every rendered frame (~60 FPS).
 * Drives render-frequency systems: WindMouse rotation + SafetySystem visualization.
 *
 * When window is unfocused, Mouse.updateMouse() doesn't run, so we
 * apply pending rotation deltas directly via MixinMouse.applyPendingUnfocused().
 */
@Mixin(Gui.class)
public class MixinInGameHud {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRenderFrame(DeltaTracker tickCounter, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            WindMouseRotation.INSTANCE.applyRenderStep(mc.player);

            // when unfocused, updateMouse() never runs — apply deltas ourselves
            if (!mc.isWindowActive()) {
                kaptainwutax.tungsten.util.UnfocusedMouseHelper.applyPendingDeltas();
            }

            CombatController.safety.renderUpdate(tickCounter.getGameTimeDeltaPartialTick(true));
        }
    }
}
