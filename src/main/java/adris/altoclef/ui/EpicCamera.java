package adris.altoclef.ui;

import adris.altoclef.util.time.TimerReal;
import kaptainwutax.tungsten.path.movements.Rotation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/*
 * EpicCamera approach:
 * - Camera should be positioned behind the player's head (NEED FIX)
 * - Camera should move EXACTLY RELATIVE to the player's head with straight orbit (NEED FIX)
 * - Camera should be side offesed (shoulder) to the right (WORKS GOOD)
 *
 * Pitch and absolute positions explanation:
 * where x is forward axis relative to player's forward, y is just height
 * pitch is current player's head pitch
 *
 * How it needs to be:
 *
 * For the pitch = -90 (look up):
 * x: 0.5 (most forward), y: -1 (min height)
 * For the pitch = 0 (is looking forward):
 * x: -1 (most backward), y: 0 (height as player's head)
 * For the pitch = 90 (look down):
 * x: -0.5 (most half-back), y: 1 (max height)
 *
 * (do not erase explanation upper)
 *
 * It works like this now, but it's not perfect. TODO FAR FUTURE fix it.
 * Now we need another TODO for the proper smoothing system:
 * - smoother should store a list of last positions and last pitches and yaws, like keyframes
 * - smoother should update position smoothly following this keyframes
 * (it should follow exactly player move path, but with some delay that is specified by delay time)
 * We should not remove previous realisation, but just append new keyframe smoothing to it.
 *
 *
 */
public class EpicCamera {
    private static final EpicCamera INSTANCE = new EpicCamera();

    // Base camera configuration
    private static final float BASE_RIGHT = 0.7f;    // Right shoulder offset
    private static final float BASE_BACK = 4.0f;     // Default orbit radius
    private static final float BASE_UP = 0.5f;       // Base height offset from eye level

    // Zoom configuration
    private static final float MIN_BACK = 2.0f;
    private static final float MAX_BACK = 8.0f;
    private static final float ZOOM_STEP = 0.5f;

    // Camera behavior
    private static final float SMOOTH_FACTOR = 0.12f;
    private static final float PITCH_INFLUENCE = 0.8f; // How much pitch affects position

    // Current state
    private Vec3 lastPos = Vec3.ZERO;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private float orbitRadius = BASE_BACK;
    private float targetRadius = BASE_BACK;
    public TimerReal _lastPosTimer = new TimerReal(2);
    private long lastUpdateTime = System.currentTimeMillis();

    private EpicCamera() {}

    public static EpicCamera getInstance() {
        return INSTANCE;
    }

    public void freezeCam() {
        _lastPosTimer.reset();
    }

    public void freezeCam(double interval) {
        _lastPosTimer.setInterval(interval);
        _lastPosTimer.reset();
    }

    public void forceStopFreezing() {
        _lastPosTimer.forceElapse();
    }

    public void adjustZoom(float scrollAmount) {
        targetRadius = Mth.clamp(
            targetRadius - scrollAmount * ZOOM_STEP,
            MIN_BACK,
            MAX_BACK
        );
    }

    public CameraUpdate getUpdate(Entity focusedEntity, float tickDelta, Rotation modifierRotation, Vec3 modifierPos) {
        if (focusedEntity == null) {
            reset();
            return null;
        }

        // Calculate time delta for smooth interpolation
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min(0.1f, (currentTime - lastUpdateTime) / 1000.0f);
        float smoothFactor = SMOOTH_FACTOR * deltaTime * 60;
        lastUpdateTime = currentTime;

        // Get head position
        Vec3 headPos = new Vec3(
            interpolate(focusedEntity.xo, focusedEntity.getX(), tickDelta),
            interpolate(focusedEntity.yo, focusedEntity.getY(), tickDelta) + focusedEntity.getEyeHeight(),
            interpolate(focusedEntity.zo, focusedEntity.getZ(), tickDelta)
        );

        // Get rotation angles
        float yaw = focusedEntity.getYRot();
        float pitch = focusedEntity.getXRot();
        if (modifierRotation != null) {
            if (modifierRotation.getYaw() != -500) yaw = modifierRotation.getYaw();
            if (modifierRotation.getPitch() != -500) pitch = modifierRotation.getPitch();
        }
        if (modifierPos != null) {
            // Sanity check: don't teleport camera more than 50 blocks from player
            if (modifierPos.distanceToSqr(headPos) < 50 * 50) {
                headPos = modifierPos;
            }
        }
        // Smooth orbit radius
        orbitRadius = Mth.lerp(smoothFactor, orbitRadius, targetRadius);

        // Calculate orbital position
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw + 180);

        // Forward/backward position correction
        float pitchFactor = pitch / 90.0f; // -1 to 1
        float xFactor = -1.0f; // Start at most backward position

        if (pitchFactor > 0) { // Looking up
            xFactor = -0.5f + (Math.abs(pitchFactor) * 0.65f);
        }
        if (pitchFactor <= 0) { // Looking down
            xFactor = -0.5f + (Math.abs(pitchFactor) * 0.35f);
        }

        // Apply the forward/back offset
        float xOffset = orbitRadius * xFactor;

        // Height stays the same as it works correctly
        float heightOffset;
        if (pitchFactor <= 0) {
            heightOffset = BASE_UP + (orbitRadius * 0.5f * pitchFactor);
        } else {
            heightOffset = BASE_UP + (orbitRadius * 0.1f * pitchFactor);
        }
        // Calculate the final position with corrected xOffset
        Vec3 orbitPos = new Vec3(
            Math.sin(yawRad) * xOffset,
            heightOffset,
            -Math.cos(yawRad) * xOffset
        );

        // Add right shoulder offset
        Vec3 rightOffset = new Vec3(
            Math.cos(yawRad) * BASE_RIGHT,
            0,
            Math.sin(yawRad) * BASE_RIGHT
        );

        // Combine all offsets
        Vec3 targetPos;
        if (_lastPosTimer.elapsed() || lastPos == null || lastPos == Vec3.ZERO) {
            targetPos = headPos.add(orbitPos).add(rightOffset);
        } else {
            yaw = lastYaw;
            pitch = lastPitch;
            targetPos = lastPos;
        }

        // Initialize or apply smooth transitions
        if (lastPos == Vec3.ZERO) {
            lastPos = targetPos;
            lastYaw = yaw;
            lastPitch = pitch;
            return new CameraUpdate(targetPos, yaw, pitch);
        }

        // Smooth position
        Vec3 smoothedPos = new Vec3(
            Mth.lerp(smoothFactor, lastPos.x, targetPos.x),
            Mth.lerp(smoothFactor, lastPos.y, targetPos.y),
            Mth.lerp(smoothFactor, lastPos.z, targetPos.z)
        );

        // Smooth rotation
        float deltaYaw = yaw - lastYaw;
        while (deltaYaw > 180.0f) deltaYaw -= 360.0f;
        while (deltaYaw < -180.0f) deltaYaw += 360.0f;

        float smoothedYaw = lastYaw + deltaYaw * smoothFactor;
        float smoothedPitch = Mth.lerp(smoothFactor, lastPitch, pitch);

        // Update state
        lastPos = smoothedPos;
        lastYaw = smoothedYaw;
        lastPitch = smoothedPitch;

        return new CameraUpdate(smoothedPos, smoothedYaw, smoothedPitch);
    }

    public static Vec3 getRelativePosition(Vec3 pos, float pitch, float yaw, Vec3 offset) {
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);

        double x = pos.x + offset.x * Math.cos(yawRad) * Math.cos(pitchRad) - offset.z * Math.sin(yawRad);
        double y = pos.y + offset.y * Math.cos(pitchRad) - offset.x * Math.sin(pitchRad);
        double z = pos.z + offset.x * Math.sin(yawRad) * Math.cos(pitchRad) + offset.z * Math.cos(yawRad);

        return new Vec3(x, y, z);
    }

    private double interpolate(double prev, double current, float tickDelta) {
        return prev + (tickDelta * (current - prev));
    }

    public void reset() {
        lastPos = Vec3.ZERO;
        lastUpdateTime = System.currentTimeMillis();
        orbitRadius = targetRadius;
    }

    public static class CameraUpdate {
        private final Vec3 position;
        private final float yaw;
        private final float pitch;

        public CameraUpdate(Vec3 position, float yaw, float pitch) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Vec3 getPosition() { return position; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }
}
