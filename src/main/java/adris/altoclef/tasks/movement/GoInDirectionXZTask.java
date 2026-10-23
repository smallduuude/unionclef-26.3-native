package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.goals.AltoGoal;
import net.minecraft.world.phys.Vec3;

public class GoInDirectionXZTask extends CustomBaritoneGoalTask {

    private final Vec3 _origin;
    private final Vec3 _delta;

    /**
     * @param sidePenalty ACCEPTED AND DELIBERATELY UNUSED. It was a ranking term for baritone's A*,
     *                    which weighed candidate nodes against each other; the tungsten drive
     *                    steers at a point instead of ranking, so staying on the line falls out of
     *                    the steering rather than out of a cost. The parameter stays so the five
     *                    call sites need not change, and so this note is where anyone looking for
     *                    the penalty will find it.
     */
    public GoInDirectionXZTask(Vec3 origin, Vec3 delta, double sidePenalty) {
        _origin = origin;
        _delta = delta;
    }

    private static boolean closeEnough(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b) < 0.001;
    }

    @Override
    protected AltoGoal newAltoGoal(AltoClef mod) {
        // G-0: GoalDirectionXZ was a Goal implementation whose isInGoal was `return false` -- you
        // never arrive at a direction -- wrapped around one heuristic. AltoGoal.Direction says the
        // same thing in the drive's own terms. The zero-length guard stays: normalising a zero
        // offset yields NaN, and a NaN target steers the bot nowhere at all.
        if (_delta.multiply(1, 0, 1).lengthSqr() < 1.0E-6) {
            Debug.logMessage("Invalid goal direction XZ (probably zero distance)");
            return null;
        }
        return AltoGoal.direction(_origin, _delta);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GoInDirectionXZTask) {
            GoInDirectionXZTask task = (GoInDirectionXZTask) other;
            return (closeEnough(task._origin, _origin) && closeEnough(task._delta, _delta));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going in direction: <" + _origin.x + "," + _origin.z + "> direction: <" + _delta.x + "," + _delta.z + ">";
    }
}
