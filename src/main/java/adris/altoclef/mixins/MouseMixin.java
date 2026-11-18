package adris.altoclef.mixins;

import adris.altoclef.util.agent.AgentInputBridge;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private double xpos;
    @Shadow private double ypos;

    @Unique private double virtualX = 0;
    @Unique private double virtualY = 0;

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void onUpdateMouseHead(CallbackInfo ci) {
        if (AgentInputBridge.isAgentInputActive) {
            double[] deltas = AgentInputBridge.consumeDeltas();
            double dx = deltas[0];
            double dy = deltas[1];

            virtualX += dx;
            virtualY += dy;

            Window window = this.minecraft.getWindow();
            virtualX = Math.max(0, Math.min(window.getScreenWidth(), virtualX));
            virtualY = Math.max(0, Math.min(window.getScreenHeight(), virtualY));

            this.xpos = virtualX;
            this.ypos = virtualY;

            if (this.minecraft.player != null && this.minecraft.gui.screen() == null) {
                this.minecraft.player.turn(dx, dy);
            }

            this.accumulatedDX = 0;
            this.accumulatedDY = 0;
            AgentInputBridge.afterTick();
        }
    }
}
