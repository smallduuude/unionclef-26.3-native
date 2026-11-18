package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.MLGBucketTask;
import adris.altoclef.tasks.movement.ThrowEnderPearlSimpleProjectileTask;
import adris.altoclef.tasksystem.ITaskOverridesGrounded;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import kaptainwutax.tungsten.path.movements.Rotation;
import kaptainwutax.tungsten.util.WindMouseRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import kaptainwutax.tungsten.path.movements.Input;
import java.util.Optional;

@SuppressWarnings("UnnecessaryLocalVariable")
public class MLGBucketFallChain extends SingleTaskChain implements ITaskOverridesGrounded {

    private final TimerGame tryCollectWaterTimer = new TimerGame(4);
    private final TimerGame pickupRepeatTimer = new TimerGame(0.25);
    private MLGBucketTask lastMLG = null;
    private ThrowEnderPearlSimpleProjectileTask lastEP = null;
    private boolean wasPickingUp = false;
    private boolean doingChorusFruit = false;
    private BlockPos lastGroundBlockPos = null;
    private final TimerGame voidFallTimer = new TimerGame(0.25);
    private double _fallStartY = Double.MAX_VALUE;

    public MLGBucketFallChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        //_lastMLG = null;
    }

    @Override
    public float getPriority() {
        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;

        AltoClef mod = AltoClef.getInstance();

        // Track last safe ground position and try ender pearl save in void/hell-hole
        if (mod.getPlayer().onGround()) {
            lastGroundBlockPos = mod.getPlayer().blockPosition();
            voidFallTimer.reset();
            _fallStartY = Double.MAX_VALUE;
        } else if (isInHellHole(mod)) {
            // Track fall start for minimum distance check
            if (_fallStartY == Double.MAX_VALUE) {
                _fallStartY = mod.getPlayer().getY();
            }
            if (mod.getItemStorage().hasItem(Items.ENDER_PEARL) && lastGroundBlockPos != null) {
                if (voidFallTimer.elapsed()
                        && _fallStartY != Double.MAX_VALUE
                        && (_fallStartY - mod.getPlayer().getY()) >= mod.getModSettings().getMinPearlFallDistance()) {
                    Optional<Entity> closestPlayer = mod.getEntityTracker().getClosestEntity(
                            mod.getPlayer().position(),
                            p -> pearlAllowable(mod, (Player) p),
                            Player.class);
                    if (closestPlayer.isPresent()) {
                        voidFallTimer.reset();
                        Debug.logMessage("Pearl clutch to nearest player!");
                        setTask(new ThrowEnderPearlSimpleProjectileTask(closestPlayer.get().blockPosition()));
                        lastEP = (ThrowEnderPearlSimpleProjectileTask) mainTask;
                        return 100;
                    } else {
                        voidFallTimer.reset();
                        Debug.logMessage("Pearl clutch to last ground block! Vel: " + mod.getPlayer().getDeltaMovement().y());
                        setTask(new ThrowEnderPearlSimpleProjectileTask(lastGroundBlockPos.offset(0,
                                (int) (-0.9 - mod.getPlayer().getDeltaMovement().y()), 0)));
                        lastEP = (ThrowEnderPearlSimpleProjectileTask) mainTask;
                        return 100;
                    }
                }
            }
        }

        if (isFalling(mod)) {
            tryCollectWaterTimer.reset();
            setTask(new MLGBucketTask());
            lastMLG = (MLGBucketTask) mainTask;
            return 100;
        } else if (!tryCollectWaterTimer.elapsed()) { // Why -0.5? Cause it's slower than -0.7.
            // We just placed water, try to collect it.
            if (mod.getItemStorage().hasItem(Items.BUCKET) && !mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                if (lastMLG != null) {
                    BlockPos placed = lastMLG.getWaterPlacedPos();
                    boolean isPlacedWater;
                    try {
                        // A poured source may be inside a waterlogged slab rather than a water block.
                        var fluid = mod.getWorld().getBlockState(placed).getFluidState();
                        isPlacedWater = fluid.is(Fluids.WATER) && fluid.isSource();
                    } catch (Exception e) {
                        isPlacedWater = false;
                    }
                    //Debug.logInternal("PLACED: " + placed);
                    if (placed != null && placed.closerToCenterThan(mod.getPlayer().position(), 5.5) && isPlacedWater) {
                        BlockPos toInteract = placed;
                        // Allow looking at fluids
                        mod.getBehaviour().push();
                        mod.getBehaviour().setRayTracingFluidHandling(ClipContext.Fluid.SOURCE_ONLY);
                        Optional<Rotation> reach = LookHelper.getReach(toInteract, Direction.UP);
                        if (reach.isPresent()) {
                            // Request the aim, then click only once the crosshair actually got
                            // there. This is the shape baritone's updateTarget had here (ask now,
                            // arrive later), so it maps straight onto tungsten's camera driver:
                            // the target is re-asserted every tick while this branch holds and
                            // auto-releases 600 ms after we stop asking.
                            WindMouseRotation.INSTANCE.setTarget(reach.get().getYaw(), reach.get().getPitch());
                            if (LookHelper.isLookingAt(mod, toInteract)) {
                                if (mod.getSlotHandler().forceEquipItem(Items.BUCKET)) {
                                    if (pickupRepeatTimer.elapsed()) {
                                        // Pick up
                                        pickupRepeatTimer.reset();
                                        mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                                        wasPickingUp = true;
                                    } else if (wasPickingUp) {
                                        // Stop picking up, wait and try again.
                                        wasPickingUp = false;
                                    }
                                }
                            }
                        } else {
                            // Eh just try collecting water the regular way if all else fails.
                            setTask(TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1));
                        }
                        mod.getBehaviour().pop();
                        return 60;
                    }
                }
            }
        }
        if (wasPickingUp) {
            wasPickingUp = false;
            lastMLG = null;
        }
        if (mod.getPlayer().hasEffect(MobEffects.LEVITATION) &&
                !mod.getPlayer().getCooldowns().isOnCooldown(new net.minecraft.world.item.ItemStack(Items.CHORUS_FRUIT)) &&
                mod.getPlayer().getActiveEffectsMap().get(MobEffects.LEVITATION).getDuration() <= 70 &&
                mod.getItemStorage().hasItemInventoryOnly(Items.CHORUS_FRUIT) &&
                !mod.getItemStorage().hasItemInventoryOnly(Items.WATER_BUCKET)) {
            doingChorusFruit = true;
            mod.getSlotHandler().forceEquipItem(Items.CHORUS_FRUIT);
            mod.getInputControls().hold(Input.CLICK_RIGHT);
            mod.getExtraBaritoneSettings().setInteractionPaused(true);
        } else if (doingChorusFruit) {
            doingChorusFruit = false;
            mod.getInputControls().release(Input.CLICK_RIGHT);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
        }
        // The use packet and the water/inventory updates need not arrive in the
        // landing tick. Keep the attempted source through the existing pickup window;
        // one temporarily missing source or empty bucket must not erase it forever.
        if (tryCollectWaterTimer.elapsed()
                || (lastMLG != null && lastMLG.getWaterPlacedPos() == null)) {
            lastMLG = null;
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public String getName() {
        return "MLG Water Bucket Fall Chain";
    }

    @Override
    public boolean isActive() {
        // We're always checking for mlg.
        return true;
    }

    public boolean doneMLG() {
        return lastMLG == null;
    }

    public boolean isChorusFruiting() {
        return doingChorusFruit;
    }

    public boolean isFalling(AltoClef mod) {
        if (!mod.getModSettings().shouldAutoMLGBucket()) {
            return false;
        }
        if (mod.getPlayer().isSwimming() || mod.getPlayer().isInWater() || mod.getPlayer().onGround() || mod.getPlayer().onClimbable()) {
            // We're grounded.
            return false;
        }
        double ySpeed = mod.getPlayer().getDeltaMovement().y;
        return ySpeed < -0.7;
    }

    public boolean isInHellHole(AltoClef mod) {
        return WorldHelper.isHellHole(mod, mod.getPlayer().blockPosition());
    }

    private boolean pearlAllowable(AltoClef mod, Player player) {
        if (player.equals(mod.getPlayer())) return false;
        return LookHelper.cleanLineOfSight(player.position(), 100)
                && !WorldHelper.isHellHole(mod, player.blockPosition());
    }
}
