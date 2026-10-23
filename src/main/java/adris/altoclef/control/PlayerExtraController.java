package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockBreakingCancelEvent;
import adris.altoclef.eventbus.events.BlockBreakingEvent;
import adris.altoclef.util.helpers.LookHelper;
import kaptainwutax.tungsten.path.movements.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class PlayerExtraController {

    private final AltoClef mod;
    /** Wall-clock of the last block-breaking progress event; 0 until one arrives. */
    public static volatile long lastBreakProgressMs = 0L;

    private BlockPos blockBreakPos;
    private double blockBreakProgress;
    public static boolean IsPvpRotating;
    private static boolean _succesfulHit = false;

    public PlayerExtraController(AltoClef mod) {
        this.mod = mod;

        EventBus.subscribe(BlockBreakingEvent.class, evt -> onBlockBreak(evt.blockPos, evt.progress));
        EventBus.subscribe(BlockBreakingCancelEvent.class, evt -> onBlockStopBreaking());
    }

    private void onBlockBreak(BlockPos pos, double progress) {
        // WHEN DID A BREAK LAST MAKE PROGRESS? UnstuckChain needs to tell a bot DIGGING
        // from a bot STRANDED, and both stand still pressing nothing. Arming the rescue on
        // the key-press test alone fired it 127 times in one course and turned mine_diamond
        // red, because a bot digging downward looks exactly like a wedged one.
        lastBreakProgressMs = System.currentTimeMillis();
        blockBreakPos = pos;
        blockBreakProgress = progress;
        kaptainwutax.tungsten.render.RouteOverlay.noteMining(pos, progress);
    }

    private void onBlockStopBreaking() {
        blockBreakPos = null;
        blockBreakProgress = 0;
        kaptainwutax.tungsten.render.RouteOverlay.noteMining(null, 0);
    }

    public BlockPos getBreakingBlockPos() {
        return blockBreakPos;
    }

    public boolean isBreakingBlock() {
        return blockBreakPos != null;
    }

    public double getBreakingBlockProgress() {
        return blockBreakProgress;
    }

    public boolean inRange(Entity entity) {
        return LookHelper.canHitEntity(mod, entity); // checks both LOS and reach range (matching autoclef)
    }

    /**
     * Attack entity via simulated left-click (anti-cheat safe).
     * Only attacks if the game's crosshair is actually targeting an attackable entity.
     * This naturally prevents through-wall attacks without explicit LOS checks.
     */
    /** Swings refused because the vanilla crosshair was not on the target entity yet. */
    public static volatile int crosshairRefused;
    /** Swings actually sent, i.e. the crosshair was on the target entity. */
    public static volatile int crosshairHits;

    /**
     * Attack entity via simulated left-click (anti-cheat safe).
     *
     * <p>THE CROSSHAIR MUST BE ON THE TARGET ENTITY (2026-10-22). This used to accept any
     * attackable entity under the crosshair: with several mobs around, the aim could still be
     * converging onto mob A while the swing went out for mob B, which reads exactly like killaura
     * (and, because vanilla resolves the hit from the crosshair, often swung at nothing). The
     * operator's requirement is the human one: turn the camera onto the mob, then swing. A tick
     * where the crosshair is elsewhere is a tick where we simply do not click.
     */
    public boolean attack(Entity entity, boolean doRotates) {
        _succesfulHit = false;
        if (entity == null || !entity.isAlive()) return false;
        LookHelper.smoothLook(mod, entity);
        boolean onTarget = LookHelper.isCrosshairOn(entity);
        boolean attackable = onTarget && Minecraft.getInstance().crosshairPickEntity != null
                && Minecraft.getInstance().crosshairPickEntity.isAttackable();
        if (!attackable) {
            if (!onTarget) crosshairRefused++;
            return false;
        }
        {
            crosshairHits++;
            // Don't release RMB while eating — slot/key changes during eating
            // generate suspicious packets that trigger server anti-cheat.
            if (!mod.getFoodChain().isTryingToEat()) {
                mod.getInputControls().release(Input.CLICK_RIGHT);
            }
            mod.getInputControls().tryPress(Input.CLICK_LEFT);
            mod.getDamageTracker().onClientMeleeAttack(entity);
            kaptainwutax.tungsten.combat.VoidGuard.meleeAtMs = System.currentTimeMillis();
            _succesfulHit = true;
        }
        return _succesfulHit;
    }

    public boolean attack(Entity entity) {
        return this.attack(entity, false);
    }
}
