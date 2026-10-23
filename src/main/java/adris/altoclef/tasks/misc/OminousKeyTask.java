package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.movement.GetAdjacentToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StructureLocator;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import kaptainwutax.tungsten.path.movements.Input;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Obtains an {@code ominous_trial_key} from an <b>ominous trial spawner</b>, which is the only
 * survival source of the ominous key and therefore of the ominous vaults ({@code heavy_core} → the
 * mace, {@code flow_banner_pattern}, {@code music_disc_creator}).
 *
 * <p>Mechanic (vanilla, 1.21+): an {@link Items#OMINOUS_BOTTLE} applies Bad Omen; being detected by a
 * trial spawner converts that into Trial Omen and turns the spawner <b>ominous</b>
 * ({@code TrialSpawnerBlock.OMINOUS} blockstate). The spawner then runs a wave of mobs; when the wave
 * is cleared it ejects its reward loot table, which contains the ominous trial key. The key and the
 * omen effect are both readable client-side (inventory / {@code LivingEntity.hasEffect}), and the
 * spawner's state is client-synced as blockstate ({@code TrialSpawnerBlock.STATE}).
 *
 * <p>Task shape: acquire an ominous bottle through the normal item chain (itself trial-chamber vault
 * loot) → locate a trial chamber → travel → find a trial spawner (block scanner knows every loaded
 * block, including the deep ones) and walk/dig to it → drink the bottle next to it → wait for the
 * spawner to turn ominous and become ACTIVE → kill the wave mobs (each one via the entity tracker,
 * using its exact runtime class as the tracker requires) → wait for the reward ejection and pick up
 * the key. If a spawner never turns ominous (e.g. the effect was already spent) the task tries the
 * next spawner and then another trial chamber, and gives up honestly with a log warning.
 *
 * <p>This task deliberately does not try to fight the wave with a broad "kill all hostiles" scan:
 * {@code EntityTracker} keys by exact runtime class, so the wave mobs are picked by their actual
 * entity instance and handed to {@link KillEntityTask}.
 */
public class OminousKeyTask extends ResourceTask {

    private static final int[] LOCATE_RINGS = {100, 200, 400};
    private static final int SPAWNER_SCAN_RADIUS = 96;
    private static final double ARRIVE_RADIUS = 18.0;
    private static final int MAX_SITES = 2;
    private static final int MAX_SPAWNERS_PER_SITE = 6;
    private static final int RETREAT_DISTANCE = 512;

    private enum Phase {BOTTLE, LOCATE, TRAVEL, APPROACH, DRINK, WAIT_SPAWNER, PICKUP, NEXT_SITE, DONE}

    private Phase phase = Phase.BOTTLE;
    private CompletableFuture<StructureLocator.Located> locating;
    private StructureLocator.Located site;
    private int sitesTried = 0;
    private boolean gaveUp = false;

    private final Set<BlockPos> triedSpawners = new HashSet<>();
    private final List<BlockPos> spawnerQueue = new ArrayList<>();
    private BlockPos spawner;
    private int spawnersTriedHere = 0;
    private int drinksTried = 0;
    private int bottleAttempts = 0;
    private boolean waveActive = false;
    private BlockPos retreatTarget = null;

    private Task activeSubtask;
    /** True while the child is only an approach to the spawner (arrival is distance, not isFinished). */
    private boolean approachingSpawner = false;
    private final TimerGame approachTimer = new TimerGame(90);
    private final TimerGame drinkTimer = new TimerGame(4);
    private final TimerGame activateTimer = new TimerGame(15);
    private final TimerGame waveTimer = new TimerGame(150);
    private final TimerGame pickupTimer = new TimerGame(25);
    private final TimerGame scanTimer = new TimerGame(2);

    /** True between equipping the bottle and releasing the right mouse button. */
    private boolean drinkInputHeld = false;

    public OminousKeyTask(int count) {
        super(new ItemTarget(Items.OMINOUS_TRIAL_KEY, count));
    }

    private boolean hasOmen(AltoClef mod) {
        return mod.getPlayer().hasEffect(MobEffects.BAD_OMEN) || mod.getPlayer().hasEffect(MobEffects.TRIAL_OMEN);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        Debug.logMessage("OminousKey: looking for an ominous trial spawner");
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        releaseDrink(mod);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (gaveUp) return null;

        // Book-keeping for the child task we handed out last tick.
        if (activeSubtask != null) {
            if (approachingSpawner) {
                // Same warning as LootSearchTask: GetAdjacentToBlockTask's isFinished uses
                // LookHelper.getReach(), which the playtest showed as a false positive far from the
                // block. Arrival is the distance test; a real failure is the timeout.
                if (spawner != null && closeEnough(mod, spawner, 3.5)) {
                    approachingSpawner = false;
                    activeSubtask = null;
                    return null;
                }
                if (approachTimer.elapsed()) {
                    if (spawner != null) triedSpawners.add(spawner);
                    spawner = null;
                    approachingSpawner = false;
                    activeSubtask = null;
                    phase = Phase.APPROACH;
                    return null;
                }
                return activeSubtask;
            }
            if (!activeSubtask.isFinished()) return activeSubtask;
            activeSubtask = null;
            return null;
        }

        switch (phase) {
            case BOTTLE -> {
                return tickBottle(mod);
            }
            case LOCATE -> {
                return tickLocate(mod);
            }
            case TRAVEL -> {
                if (site == null) {
                    phase = Phase.LOCATE;
                    return null;
                }
                if (horizontalDistance(mod, site.pos()) <= ARRIVE_RADIUS) {
                    setDebugState("Arrived at " + site);
                    spawnerQueue.clear();
                    spawner = null;
                    scanTimer.forceElapse();
                    phase = Phase.APPROACH;
                    return null;
                }
                setDebugState("Travelling to " + site);
                return track(new GetToXZTask(site.pos().getX(), site.pos().getZ(), Dimension.OVERWORLD));
            }
            case APPROACH -> {
                return tickApproach(mod);
            }
            case DRINK -> {
                return tickDrink(mod);
            }
            case WAIT_SPAWNER -> {
                return tickSpawner(mod);
            }
            case PICKUP -> {
                return tickPickup(mod);
            }
            case NEXT_SITE -> {
                return tickNextSite(mod);
            }
            case DONE -> {
                return null;
            }
        }
        return null;
    }

    private Task tickBottle(AltoClef mod) {
        if (hasOmen(mod) || mod.getItemStorage().getItemCountInventoryOnly(Items.OMINOUS_BOTTLE) > 0) {
            phase = Phase.LOCATE;
            return null;
        }
        bottleAttempts++;
        if (bottleAttempts > 3) {
            giveUp("could not obtain an ominous bottle");
            return null;
        }
        Task bottle = TaskCatalogue.getItemTask(Items.OMINOUS_BOTTLE, 1);
        if (bottle == null) {
            giveUp("no chain can obtain an ominous bottle");
            return null;
        }
        setDebugState("Getting an ominous bottle (attempt " + bottleAttempts + ")");
        return track(bottle);
    }

    private Task tickLocate(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Travelling to the overworld");
            return track(new adris.altoclef.tasks.movement.DefaultGoToDimensionTask(Dimension.OVERWORLD));
        }
        if (locating == null) {
            if (sitesTried >= MAX_SITES) {
                giveUp("tried " + sitesTried + " trial chambers without a key");
                return null;
            }
            sitesTried++;
            spawnersTriedHere = 0;
            triedSpawners.clear();
            setDebugState("Locating trial chambers (attempt " + sitesTried + "/" + MAX_SITES + ")");
            locating = StructureLocator.locateWithRings(mod, List.of("minecraft:trial_chambers"), LOCATE_RINGS);
            return null;
        }
        if (!locating.isDone()) {
            setDebugState("Locating trial chambers ...");
            return null;
        }
        StructureLocator.Located found;
        try {
            found = locating.getNow(null);
        } catch (Throwable t) {
            found = null;
        }
        locating = null;
        if (found == null) {
            giveUp("no trial chambers found within the locate rings");
            return null;
        }
        site = found;
        scanTimer.forceElapse();
        setDebugState("Found " + found);
        Debug.logMessage("OminousKey: " + found);
        phase = Phase.TRAVEL;
        return null;
    }

    private Task tickApproach(AltoClef mod) {
        if (spawner == null) {
            if (!scanTimer.elapsed()) return null;
            scanTimer.reset();
            spawnerQueue.removeIf(triedSpawners::contains);
            if (spawnerQueue.isEmpty()) {
                for (BlockPos pos : mod.getBlockScanner().getKnownLocations(Blocks.TRIAL_SPAWNER)) {
                    if (triedSpawners.contains(pos)) continue;
                    if (mod.getPlayer().position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                            > (double) SPAWNER_SCAN_RADIUS * SPAWNER_SCAN_RADIUS) continue;
                    spawnerQueue.add(pos);
                }
                spawnerQueue.sort(Comparator.comparingDouble(pos ->
                        mod.getPlayer().position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
            }
            if (spawnerQueue.isEmpty()) {
                if (spawnersTriedHere >= MAX_SPAWNERS_PER_SITE) {
                    phase = Phase.NEXT_SITE;
                } else {
                    setDebugState("No trial spawner in range; exploring the chamber");
                    return track(new TimeoutWanderTask(6, true));
                }
                return null;
            }
            spawner = spawnerQueue.remove(0);
            spawnersTriedHere++;
            Debug.logMessage("OminousKey: trying trial spawner at " + spawner.toShortString());
        }
        if (closeEnough(mod, spawner, 4.0)) {
            phase = Phase.DRINK;
            drinksTried = 0;
            drinkTimer.reset();
            return null;
        }
        setDebugState("Getting to the trial spawner (digging if needed)");
        approachTimer.reset();
        approachingSpawner = true;
        return track(new GetAdjacentToBlockTask(spawner));
    }

    private Task tickDrink(AltoClef mod) {
        if (hasOmen(mod)) {
            releaseDrink(mod);
            phase = Phase.WAIT_SPAWNER;
            activateTimer.reset();
            setDebugState("Omen active; waiting for the spawner to turn ominous");
            return null;
        }
        if (mod.getItemStorage().getItemCountInventoryOnly(Items.OMINOUS_BOTTLE) < 1) {
            releaseDrink(mod);
            if (drinksTried >= 1) {
                // The effect is gone and we are out of bottles: this spawner cannot turn ominous.
                triedSpawners.add(spawner);
                spawner = null;
                phase = Phase.APPROACH;
                return null;
            }
            phase = Phase.BOTTLE;
            return null;
        }
        if (mod.getSlotHandler().forceEquipItem(Items.OMINOUS_BOTTLE)) {
            mod.getInputControls().hold(Input.CLICK_RIGHT);
            drinkInputHeld = true;
        }
        if (drinkInputHeld && (drinkTimer.elapsed()
                || mod.getItemStorage().getItemCountInventoryOnly(Items.OMINOUS_BOTTLE) < 1)) {
            releaseDrink(mod);
            drinksTried++;
            drinkTimer.reset();
        }
        setDebugState("Drinking the ominous bottle");
        return null;
    }

    private Task tickSpawner(AltoClef mod) {
        if (spawner == null) {
            phase = Phase.APPROACH;
            return null;
        }
        if (!mod.getChunkTracker().isChunkLoaded(spawner)) {
            setDebugState("Waiting for the spawner chunk");
            return null;
        }
        BlockState state = mod.getWorld().getBlockState(spawner);
        if (!state.is(Blocks.TRIAL_SPAWNER)) {
            triedSpawners.add(spawner);
            spawner = null;
            phase = Phase.APPROACH;
            return null;
        }
        boolean ominous = state.getValue(TrialSpawnerBlock.OMINOUS);
        TrialSpawnerState spawnerState = state.getValue(TrialSpawnerBlock.STATE);

        if (spawnerState == TrialSpawnerState.EJECTING_REWARD || spawnerState == TrialSpawnerState.COOLDOWN) {
            setDebugState("Trial spawner finished; collecting the key");
            triedSpawners.add(spawner);
            waveActive = false;
            pickupTimer.reset();
            phase = Phase.PICKUP;
            return null;
        }

        if (spawnerState == TrialSpawnerState.ACTIVE
                || spawnerState == TrialSpawnerState.WAITING_FOR_REWARD_EJECTION) {
            if (!waveActive) {
                waveActive = true;
                waveTimer.reset();
            }
            if (waveTimer.elapsed()) {
                Debug.logMessage("OminousKey: wave at " + spawner.toShortString() + " timed out");
                triedSpawners.add(spawner);
                spawner = null;
                waveActive = false;
                phase = Phase.APPROACH;
                return null;
            }
            // Kill the wave mobs ourselves: the entity tracker keys by exact runtime class, so hand it
            // the actual entity instance instead of a superclass filter that would never match.
            Entity nearest = nearestWaveMob(mod);
            if (nearest != null) {
                setDebugState("Clearing the trial spawner wave");
                return track(new KillEntityTask(nearest));
            }
            setDebugState("Waiting for wave mobs ...");
            return null;
        }

        // WAITING_FOR_PLAYERS / INACTIVE: it has not turned ominous yet.
        if (ominous) {
            setDebugState("Ominous spawner is waiting for players");
            return null;
        }
        if (activateTimer.elapsed()) {
            Debug.logMessage("OminousKey: spawner at " + spawner.toShortString() + " did not turn ominous");
            if (hasOmen(mod) && mod.getItemStorage().getItemCountInventoryOnly(Items.OMINOUS_BOTTLE) > 0) {
                phase = Phase.DRINK;
                drinkTimer.reset();
            } else {
                triedSpawners.add(spawner);
                spawner = null;
                phase = Phase.APPROACH;
            }
            return null;
        }
        setDebugState("Waiting for the spawner to detect the omen");
        return null;
    }

    private Entity nearestWaveMob(AltoClef mod) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : mod.getEntityTracker().getAllTrackedEntities()) {
            // The tracker can briefly (or on a bad tick, for longer) retain an entity the client has
            // already discarded: targeting that produces the endless "Costing <mob>: removed=DISCARDED"
            // loop measured in the playtest. Only live, present entities are wave mobs.
            if (!(entity instanceof Monster)) continue;
            if (entity.isRemoved() || !entity.isAlive()) continue;
            double d = entity.position().distanceToSqr(spawner.getX() + 0.5, spawner.getY() + 0.5, spawner.getZ() + 0.5);
            if (d < 72 * 72 && d < bestDistance) {
                bestDistance = d;
                best = entity;
            }
        }
        return best;
    }

    private Task tickPickup(AltoClef mod) {
        if (mod.getItemStorage().getItemCountInventoryOnly(Items.OMINOUS_TRIAL_KEY) > 0) {
            setDebugState("Got the ominous trial key");
            return null;
        }
        if (pickupTimer.elapsed()) {
            triedSpawners.add(spawner);
            spawner = null;
            phase = Phase.APPROACH;
            return null;
        }
        if (mod.getEntityTracker().itemDropped(new ItemTarget(Items.OMINOUS_TRIAL_KEY, 1))) {
            setDebugState("Picking up the ominous trial key");
            return track(new PickupDroppedItemTask(Items.OMINOUS_TRIAL_KEY, 1));
        }
        setDebugState("Waiting for the trial spawner reward");
        return null;
    }

    private Task tickNextSite(AltoClef mod) {
        locating = null;
        spawner = null;
        spawnerQueue.clear();
        triedSpawners.clear();
        waveActive = false;
        if (site == null) {
            phase = Phase.LOCATE;
            return null;
        }
        if (retreatTarget == null) {
            double angle = Math.toRadians(45 + sitesTried * 137);
            retreatTarget = site.pos().offset(
                    (int) Math.round(Math.cos(angle) * RETREAT_DISTANCE), 0,
                    (int) Math.round(Math.sin(angle) * RETREAT_DISTANCE));
            setDebugState("Moving away to find another trial chamber");
        }
        if (horizontalDistance(mod, retreatTarget) <= 32) {
            retreatTarget = null;
            site = null;
            phase = Phase.LOCATE;
            return null;
        }
        return track(new GetToXZTask(retreatTarget.getX(), retreatTarget.getZ(), Dimension.OVERWORLD));
    }

    private void releaseDrink(AltoClef mod) {
        if (drinkInputHeld) {
            mod.getInputControls().release(Input.CLICK_RIGHT);
            drinkInputHeld = false;
        }
    }

    private Task track(Task task) {
        activeSubtask = task;
        return task;
    }

    private void giveUp(String reason) {
        if (gaveUp) return;
        gaveUp = true;
        phase = Phase.DONE;
        setDebugState("Gave up: " + reason);
        Debug.logWarning("OminousKey: gave up — " + reason);
    }

    /** True when the player's feet are within {@code range} blocks of the block centre. */
    private static boolean closeEnough(AltoClef mod, BlockPos pos, double range) {
        return mod.getPlayer().position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                <= range * range;
    }

    private static double horizontalDistance(AltoClef mod, BlockPos pos) {
        BlockPos player = mod.getPlayer().blockPosition();
        double dx = player.getX() - pos.getX();
        double dz = player.getZ() - pos.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public boolean isFinished() {
        return gaveUp || super.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof OminousKeyTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Getting an ominous trial key";
    }
}
