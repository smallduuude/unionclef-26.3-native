package adris.altoclef.util.projectiles;

import adris.altoclef.util.time.TimerGame;
import java.lang.reflect.Type;
import net.minecraft.world.phys.Vec3;

public class CachedProjectile {
    private final TimerGame lastCache = new TimerGame(2);
    public Vec3 velocity;
    public Vec3 position;
    public double gravity;
    public Type projectileType;
    /**
     * The entity id, so a projectile can be told apart from itself a tick later.
     *
     * <p>Added for the release-range instrument: the tracker rebuilds this list every tick by
     * scanning the world, so without an identity every arrow is counted once per tick of flight.
     * The FIRST tick an arrow appears is within a tick of the shot, which is what makes the gap
     * at that moment the range it was fired from.
     */
    public int entityId = -1;
    private Vec3 cachedHit;
    private boolean cacheHeld = false;

    public Vec3 getCachedHit() {
        return cachedHit;
    }

    public void setCacheHit(Vec3 cache) {
        cachedHit = cache;
        cacheHeld = true;
        lastCache.reset();
    }

    public boolean needsToRecache() {
        return !cacheHeld || lastCache.elapsed();
    }
}
