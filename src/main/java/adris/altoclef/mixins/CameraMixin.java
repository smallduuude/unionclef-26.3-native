package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import adris.altoclef.ui.EpicCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private Entity entity;

    // 26.3: Camera.update is (DeltaTracker); third-person/inverse are now read from the camera/options.
    @Inject(at = @At("TAIL"), method = "update")
    private void onUpdate(DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean thirdPerson = ((Camera) (Object) this).isDetached();
        boolean inverseView = Minecraft.getInstance().options.getCameraType().isMirrored();
        if (thirdPerson && !inverseView
                && AltoClef.getInstance() != null
                && AltoClef.getInstance().getModSettings().isEpicCameraEnabled()
                && this.entity != null) {
            var update = EpicCamera.getInstance().getUpdate(
                    this.entity,
                    deltaTracker.getGameTimeDeltaPartialTick(true),
                    AltoClef.getCameraRotationModifer(),
                    AltoClef.getCameraPositionModifer());
            if (update != null) {
                setPosition(update.getPosition().x, update.getPosition().y, update.getPosition().z);
                setRotation(update.getYaw(), update.getPitch());
            }
        } else {
            EpicCamera.getInstance().reset();
        }
    }
}
