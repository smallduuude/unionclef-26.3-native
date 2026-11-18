package kaptainwutax.tungsten.api;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * The single place tungsten asks "is somebody else driving?".
 *
 * <p>Every hook is written as {@code if (MovementDelegates.<something>(...)) return;} at the TOP of
 * the primitive, so with no engine installed (the default) each one is a null check and nothing
 * else. With one installed and selected, tungsten's own search, executor, walker and queue are not
 * started at all — the external engine owns the body, and tungsten's state queries answer for it so
 * that altoclef's own gates (is a route executing, is it safe to interrupt) stay correct.
 */
public final class MovementDelegates {

    private MovementDelegates() {
    }

    private static volatile ExternalMovement external;

    /** Installed once, by the mod that provides the alternative engine. */
    public static void set(ExternalMovement engine) {
        external = engine;
    }

    public static ExternalMovement external() {
        return external;
    }

    /** An engine is installed and chosen by the player. */
    public static boolean externalSelected() {
        ExternalMovement e = external;
        return e != null && e.selected();
    }

    /** An engine is chosen AND driving the body right now. */
    public static boolean externalActive() {
        ExternalMovement e = external;
        return e != null && e.selected() && e.active();
    }

    /** @return true when the external engine took the goal (tungsten must not also start) */
    public static boolean pathTo(Vec3 target) {
        if (target == null) return false;
        ExternalMovement e = external;
        return e != null && e.selected() && e.pathTo(target.x, target.y, target.z);
    }

    public static boolean pathExact(BlockPos cell) {
        if (cell == null) return false;
        ExternalMovement e = external;
        return e != null && e.selected() && e.pathExact(cell.getX(), cell.getY(), cell.getZ());
    }

    public static boolean pathNearest(Predicate<BlockPos> reached) {
        if (reached == null) return false;
        ExternalMovement e = external;
        return e != null && e.selected() && e.pathNearest(reached);
    }

    public static boolean pillarTo(int y, int x, int z) {
        ExternalMovement e = external;
        return e != null && e.selected() && e.pillarTo(y, x, z);
    }

    /** Stop the external engine, if it is the chosen one. */
    public static void stop() {
        ExternalMovement e = external;
        if (e != null && e.selected()) e.stop();
    }
}
