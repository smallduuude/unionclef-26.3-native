package kaptainwutax.tungsten.combat;

import kaptainwutax.tungsten.task.BowShooter;
import kaptainwutax.tungsten.task.ShieldBlocker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * The combat-primitives facade (docs/features/TUNGSTEN_ALTOCLEF_API.md):
 * tungsten's execution surface for the altoclef battle brain. Everything
 * here is stateless or delegates to a ticked primitive; strategy (weapon
 * choice, WHEN to strike, consumables, HP logic) lives on the altoclef side.
 */
public final class CombatPrimitives {

    public static final double MELEE_REACH = 3.0;

    private CombatPrimitives() {}

    /** Reach + COLLIDER line of sight + look angle — the melee gate without
     *  the cooldown part (the brain may check cooldown separately). */
    public static boolean canHit(LocalPlayer player, Entity target, double maxLookAngleDeg) {
        Vec3 eye = player.getEyePosition();
        AABB box = target.getBoundingBox();
        Vec3 closest = new Vec3(
                Mth.clamp(eye.x, box.minX, box.maxX),
                Mth.clamp(eye.y, box.minY, box.maxY),
                Mth.clamp(eye.z, box.minZ, box.maxZ));
        if (eye.distanceToSqr(closest) > MELEE_REACH * MELEE_REACH) return false;

        Vec3 toTarget = box.getCenter().subtract(eye).normalize();
        double angle = Math.toDegrees(Math.acos(Mth.clamp(
                player.getViewVector(1.0f).dot(toTarget), -1.0, 1.0)));
        if (angle > maxLookAngleDeg) return false;

        HitResult hit = player.level().clip(new ClipContext(
                eye, box.getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** Deliver a melee attack right now (no gate — combine with canHit). */
    public static void attack(LocalPlayer player, Entity target) {
        // The vanilla crosshair is the gate: this surface must not be a killaura bypass.
        if (target == null || !target.isAlive()) return;
        if (Minecraft.getInstance().crosshairPickEntity != target) return;
        Minecraft.getInstance().gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND, false);
    }

    /** Raise the shield for N ticks (shield in hand is the brain's job). */
    public static void shieldHold(int ticks) {
        ShieldBlocker.hold(ticks);
    }

    public static void shieldRelease() {
        ShieldBlocker.release();
    }

    public static boolean isShieldBlocking() {
        return ShieldBlocker.isBlocking();
    }

    /** Ballistic solution for a full-draw arrow at a moving entity. */
    public static TrajectorySolver.Solution solveArrow(LocalPlayer player, Entity target) {
        return TrajectorySolver.solve(player, target, 1.0);
    }

    /** Fire an aimed, lead-predicted arrow (bow already in hand). */
    public static boolean shootArrow(Entity target) {
        return BowShooter.shootAt(target);
    }
}
