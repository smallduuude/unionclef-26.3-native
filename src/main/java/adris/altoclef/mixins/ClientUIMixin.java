package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ClientRenderEvent;
import adris.altoclef.multiversion.DrawContextWrapper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public final class ClientUIMixin {
    @Inject(
            method = "extractRenderState",
            at = @At("TAIL")
    )
    private void clientRender(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo ci) {
        EventBus.publish(new ClientRenderEvent(DrawContextWrapper.of(graphics), tickCounter.getGameTimeDeltaPartialTick(true)));
    }


}
