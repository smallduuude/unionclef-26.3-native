package kaptainwutax.tungsten.api;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/**
 * A movement engine that can take over the body from tungsten.
 *
 * <h2>Why this interface lives in tungsten</h2>
 *
 * altoclef drives movement through tungsten's primitives, and so does tungsten itself: the build
 * queue positions itself with a {@code FastNavigator} walk, {@code PillarTask} raises towers, and
 * {@code BlockPathWalker} / {@code MovementQueue} carry routes. A caller-side switch (in altoclef)
 * therefore cannot cover the mod: the builder's own walks and every pillar start INSIDE tungsten,
 * where altoclef's classes are not on the classpath.
 *
 * <p>So the switch sits at the primitives, behind this interface. When no engine is installed — the
 * ordinary case — every hook is a no-op and tungsten behaves exactly as it did before.
 *
 * <p>The selected engine is <b>the only thing that presses keys</b> while it is selected: tungsten
 * does not run its search, executor, walker or queue underneath it.
 */
public interface ExternalMovement {

    /** Is this engine installed AND the one the player chose? */
    boolean selected();

    /** Is it driving the body right now? */
    boolean active();

    /** Send the body to a world point. @return true when the goal was accepted */
    boolean pathTo(double x, double y, double z);

    /** Send the body INTO one exact cell (the builder standing where it will place). */
    boolean pathExact(int x, int y, int z);

    /** Send the body to the nearest cell satisfying {@code reached}. */
    boolean pathNearest(Predicate<BlockPos> reached);

    /** Raise the body to {@code y} on column {@code x,z} — a pillar. */
    boolean pillarTo(int y, int x, int z);

    /** Stop whatever this engine is driving. */
    void stop();
}
