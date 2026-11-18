package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.entity.AbstractKillEntityTask;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import java.util.HashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Attacks closest player, but stands still when anyone has direct line of sight on the bot.
 */
public class SCP173Task extends Task {

    private static final double MAX_RANGE = 300;
    private static final double LOOK_CLOSENESS_THRESHOLD = 0.2;
    private static final double HIT_RANGE = 2.5;
    private static final double WALK_THRESHOLD = 0.1;
    private final HashMap<Player, Double> _lastLookCloseness = new HashMap<>();
    private Player _lastTarget = null;
    private Vec3 _lastWalkVelocity = Vec3.ZERO;

    @Override
    protected void onStart() {
        _lastLookCloseness.clear();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        boolean seen = isSeenByPlayer(mod);

        Vec3 currentVelocity = mod.getPlayer().getDeltaMovement();
        if (currentVelocity.lengthSqr() > WALK_THRESHOLD * WALK_THRESHOLD) {
            _lastWalkVelocity = currentVelocity;
        }

        if (seen) {
            setDebugState("Standing still and being menacing");
        } else {
            setDebugState("Scrape Scrape Scrape");
        }

        if (seen) {
            if (_lastTarget != null) {
                LookHelper.lookAt(mod, LookHelper.getCameraPos(_lastTarget));
            }
            return null;
        }

        if (_lastTarget != null && mod.getPlayer().closerThan(_lastTarget, HIT_RANGE)) {
            if (LookHelper.seesPlayer(mod.getPlayer(), _lastTarget, HIT_RANGE)) {
                AbstractKillEntityTask.equipWeapon(mod);
                if (mod.getPlayer().getAttackStrengthScale(0) >= 0.99) {
                    mod.getControllerExtras().attack(_lastTarget);
                }
            }
        }

        return new DoToClosestEntityTask(
                target -> {
                    _lastTarget = (Player) target;
                    return new GetToEntityTask(target);
                },
                Player.class
        );
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SCP173Task;
    }

    @Override
    protected String toDebugString() {
        return "Acting like SCP 173";
    }

    private boolean isSeenByPlayer(AltoClef mod) {
        if (mod.getEntityTracker().entityFound(Player.class)) {
            for (Player player : mod.getEntityTracker().getTrackedEntities(Player.class)) {
                if (entityIsLookingInOurGeneralDirection(mod, player) && entityHasLineOfSightToUs(mod, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean entityIsLookingInOurGeneralDirection(AltoClef mod, Player other) {
        double lookCloseness = LookHelper.getLookCloseness(other, mod.getPlayer().position());
        double last = _lastLookCloseness.getOrDefault(other, lookCloseness);
        double delta = lookCloseness - last;
        double predicted = lookCloseness + delta * 6;
        _lastLookCloseness.put(other, lookCloseness);
        return lookCloseness > LOOK_CLOSENESS_THRESHOLD || predicted > LOOK_CLOSENESS_THRESHOLD;
    }

    private boolean entityHasLineOfSightToUs(AltoClef mod, Player other) {
        if (LookHelper.seesPlayer(mod.getPlayer(), other, MAX_RANGE)) {
            return true;
        }
        double playerVelMul = 5;
        double entityVelMul = 10;
        Vec3 lastVelocityOffs = _lastWalkVelocity.scale(playerVelMul);
        if (!_lastWalkVelocity.equals(Vec3.ZERO)) {
            double minLength = 1.3;
            if (lastVelocityOffs.lengthSqr() < minLength * minLength) {
                lastVelocityOffs = lastVelocityOffs.normalize().scale(minLength);
            }
        }
        return LookHelper.seesPlayer(mod.getPlayer(), other, MAX_RANGE, mod.getPlayer().getDeltaMovement().scale(playerVelMul), other.getDeltaMovement().scale(entityVelMul))
                || LookHelper.seesPlayer(mod.getPlayer(), other, MAX_RANGE, lastVelocityOffs, Vec3.ZERO);
    }
}
