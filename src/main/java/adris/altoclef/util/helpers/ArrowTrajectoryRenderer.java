package adris.altoclef.util.helpers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ArrowTrajectoryRenderer {
    private static final double GRAVITY = 0.05D;
    private static final float ARROW_VELOCITY = 3.0F;

    public void renderTrajectory(PoseStack matrixStack, float partialTicks, Player player) {
        if (!player.isUsingItem() || player.getUseItem().getItem() != Items.BOW) {
            return;
        }

        // Calculate bow charge
        int useDuration = player.getTicksUsingItem();
        float power = (float) (useDuration) / 20.0F;
        power = (power * power + power * 2.0F) / 3.0F;
        power = Math.min(1.0F, power);

        if (power < 0.1F) {
            return;
        }
        matrixStack.pushPose();

        // Get camera position for proper transformation
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        matrixStack.translate(
                -camera.position().x,
                -camera.position().y,
                -camera.position().z
        );

        // Start position
        Vec3 startPos = player.getEyePosition().subtract(0, 0.1, 0)
                .add(player.getLookAngle().scale(0.5));
        Vec3 lookVec = player.getLookAngle();

        // Initial velocity
        double velocityX = lookVec.x * ARROW_VELOCITY * power;
        double velocityY = lookVec.y * ARROW_VELOCITY * power;
        double velocityZ = lookVec.z * ARROW_VELOCITY * power;

        // Trajectory rendering currently disabled (rendering API changes between MC versions).
        // Uncomment and adapt for your Minecraft version if needed.

        matrixStack.popPose();
    }
}
