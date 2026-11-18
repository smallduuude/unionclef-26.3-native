package kaptainwutax.tungsten.util;

import net.minecraft.client.Minecraft;

/**
 * Applies pending WindMouse rotation deltas when MC window is unfocused.
 * Mouse.updateMouse() doesn't run without focus, so we replicate the
 * sensitivity formula + changeLookDirection here.
 */
public final class UnfocusedMouseHelper {

    private UnfocusedMouseHelper() {}

    public static void applyPendingDeltas() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;

        double[] deltas = WindMouseRotation.INSTANCE.consumeRawPixelDeltas();
        if (deltas[0] == 0 && deltas[1] == 0) return;

        double sens = mc.options.sensitivity().get();
        double f = sens * 0.6 + 0.2;
        double scale = f * f * f * 8.0;

        mc.player.turn(deltas[0] * scale, deltas[1] * scale);
    }
}
