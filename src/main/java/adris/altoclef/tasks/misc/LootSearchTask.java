package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetAdjacentToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StructureLocator;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Structure loot search: locate a structure (cheat-free, through the integrated server), travel
 * there, find its loot containers/blocks in the loaded world and loot them until the wanted item is
 * obtained.
 *
 * <p>This is the generic form of what {@link adris.altoclef.tasks.misc.LootDesertTempleTask} does for
 * one structure. It is a {@link ResourceTask}, so it inherits the ordinary source order for the same
 * item: the parent {@code ResourceTask.onTick} first lets the bot pick up drops (which is how vault
 * loot and archaeology drops reach the pack) and only then calls {@link #onResourceTick}.
 *
 * <p><b>Why a locator and not exploration.</b> Vanilla {@code /locate structure} needs op or
 * cheats. In singleplayer the client owns the integrated server, so {@link StructureLocator} asks
 * the world generator directly: no command, no op, no seed parsing. On a multiplayer server there is
 * no integrated server and the owning chain does not offer this task at all.
 *
 * <p><b>Why the targets are found by scanning, not by a per-structure map.</b> Chest positions
 * differ per structure and per generated variant (an abandoned camp is a template jigsaw; a shipwreck
 * has three possible chest positions). The client already knows every block of every loaded chunk
 * including underground, and {@code BlockScanner} rescans all loaded chunks about once a second, so
 * "the chests/pots/vaults within 110 blocks of this structure anchor" is a question the mod can
 * already answer.
 *
 * <p><b>Loot kinds.</b> Three kinds of lootable target are handled:
 * <ul>
 *   <li>containers (chest / trapped chest / barrel) — opened with {@link LootContainerTask};</li>
 *   <li>decorated pots (trial chambers) — broken, their loot pops out and is picked up;</li>
 *   <li>vaults (trial chambers) — the bot first obtains the matching key through the ordinary item
 *       chain (the normal {@code trial_key} for a normal vault, the {@code ominous_trial_key} — which
 *       itself comes from an ominous trial spawner — for an ominous vault), then right-clicks the
 *       vault with that key held; the vault ejects its loot and the parent {@link ResourceTask} picks
 *       it up.</li>
 * </ul>
 *
 * <p><b>Ordering.</b> The task keeps visited sets, so a container/pot/vault is never touched twice.
 * When every available target has been checked and the item is still missing, the task moves
 * ~512 blocks away (so the locator returns a different structure next time) and starts again, up to
 * {@link #MAX_STRUCTURES} structures. It then stops and says so instead of roaming forever.
 */
public class LootSearchTask extends ResourceTask {

    /** Loot targets are searched within this block radius of the structure anchor. */
    private static final int TARGET_RADIUS = 110;
    /** Enough structures to survive bad loot rolls, few enough that the task gives up rather than roaming forever. */
    private static final int MAX_STRUCTURES = 8;
    /** Locator search rings, in chunks. {@code /locate structure} uses 100; rare structures may need more. */
    private static final int[] LOCATE_RINGS = {100, 200, 400};
    /** Horizontal distance that counts as "arrived at the structure". */
    private static final double ARRIVE_RADIUS = 18.0;
    /** How far to walk away between two structure attempts, so the locator returns a different one. */
    private static final int RETREAT_DISTANCE = 512;
    private static final Block[] CONTAINER_BLOCKS = {
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL
    };
    /** Vaults are filtered by the {@code OMINOUS} blockstate; see the class comment. */
    private static final Block[] VAULT_BLOCKS = {Blocks.VAULT};
    private static final Block[] POT_BLOCKS = {Blocks.DECORATED_POT};

    private enum Phase {LOCATE, TRAVEL, LOOT, NEXT, DONE}

    private enum Kind {NONE, APPROACH, CONTAINER, VAULT, POT}

    private final Item item;
    private final String siteName;
    private final List<String> structureIds;
    private final Dimension dimension;
    private final boolean breakPots;
    private final boolean openVaults;
    /** True for the ominous trial-chamber vaults (require an ominous trial key instead of a trial key). */
    private final boolean ominousVaults;
    private final Item vaultKey;

    private Phase phase = Phase.LOCATE;
    private CompletableFuture<StructureLocator.Located> locating;
    private StructureLocator.Located site;
    private int structuresTried = 0;
    private boolean gaveUp = false;

    private final Set<BlockPos> visitedContainers = new HashSet<>();
    private final Set<BlockPos> visitedPots = new HashSet<>();
    private final Set<BlockPos> visitedVaults = new HashSet<>();
    private final List<BlockPos> vaultAttempts = new ArrayList<>();
    private final TimerGame scanTimer = new TimerGame(3);
    private final TimerGame approachTimer = new TimerGame(90);
    private final TimerGame vaultTimer = new TimerGame(8);
    private int emptyScans = 0;
    private BlockPos retreatTarget;

    /** The child task this task is currently running, what kind of loot it is, and where. */
    private Task activeSubtask;
    private Kind activeKind = Kind.NONE;
    private BlockPos activeTarget;

    /** Trial-key acquisition for the vault mode (recursive: the key itself is trial-chamber loot). */
    private Task keyTask;
    private int keyAttempts = 0;

    public LootSearchTask(Item item, int count, String siteName, List<String> structureIds,
                          Dimension dimension, boolean breakPots, boolean openVaults) {
        this(item, count, siteName, structureIds, dimension, breakPots, openVaults, false);
    }

    public LootSearchTask(Item item, int count, String siteName, List<String> structureIds,
                          Dimension dimension, boolean breakPots, boolean openVaults,
                          boolean ominousVaults) {
        super(new ItemTarget(item, count));
        this.item = item;
        this.siteName = siteName;
        this.structureIds = List.copyOf(structureIds);
        this.dimension = dimension;
        this.breakPots = breakPots;
        this.openVaults = openVaults;
        this.ominousVaults = ominousVaults;
        this.vaultKey = ominousVaults ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // Drops are the whole point (vaults eject into the world; broken pots spill), so never walk
        // past a target item lying on the ground.
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        Debug.logMessage("LootSearch: looking for " + item + " in " + siteName + " ("
                + structureIds.size() + " structure ids"
                + (openVaults ? ", vaults" : "") + (breakPots ? ", pots" : "") + ")");
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Children close their own screens; nothing else to release.
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (gaveUp) return null;

        // Finish bookkeeping for the previous child before deciding anything new.
        if (activeSubtask != null) {
            // Let every non-approach child (movement, pickup, the nested key task, container/pot/vault
            // interactions) actually RUN until it reports finished. The approach child is the exception:
            // it is measured by distance above because its own isFinished is unreliable.
            if (activeKind != Kind.APPROACH && !activeSubtask.isFinished()) {
                return activeSubtask;
            }
            if (activeKind == Kind.APPROACH) {
                // ⛔ Do NOT trust the approach child's isFinished here: GetAdjacentToBlockTask
                // completes on LookHelper.getReach(), which the playtest measured as a false positive
                // for a vault still ~30 blocks away — that made the task flip between two containers
                // for ever. Arrival is our own distance test; a genuine failure is a timeout.
                if (activeTarget != null && closeEnough(mod, activeTarget, 3.5)) {
                    activeSubtask = null;
                    activeKind = Kind.NONE;
                    activeTarget = null;
                    return null;
                }
                if (approachTimer.elapsed()) {
                    Debug.logMessage("LootSearch: could not get to " + activeTarget + ", skipping it");
                    if (activeTarget != null) visitedContainers.add(activeTarget);
                    activeSubtask = null;
                    activeKind = Kind.NONE;
                    activeTarget = null;
                    return null;
                }
                return activeSubtask;
            }
            switch (activeKind) {
                case VAULT -> {
                    boolean keySpent = mod.getItemStorage().getItemCountInventoryOnly(vaultKey) < 1;
                    boolean openable = isVaultOpenable(mod, activeTarget);
                    if ((keySpent || !openable) && !visitedVaults.contains(activeTarget)) {
                        // Key consumed -> unlocked; state leaving INACTIVE/ACTIVE -> it is animating the
                        // reward. Either way this vault has served its purpose.
                        visitedVaults.add(activeTarget);
                    } else if (!keySpent && openable && !vaultTimer.elapsed()) {
                        return activeSubtask;
                    } else if (!keySpent) {
                        long attempts = vaultAttempts.stream().filter(p -> p.equals(activeTarget)).count();
                        Debug.logMessage("LootSearch: vault attempt at " + activeTarget.toShortString()
                                + " (attempt " + (attempts + 1) + ")");
                        if (attempts >= 2) visitedVaults.add(activeTarget);
                    }
                    vaultAttempts.add(activeTarget);
                }
                case CONTAINER -> visitedContainers.add(activeTarget);
                case POT -> visitedPots.add(activeTarget);
                default -> {
                    // Movement/wait/key children have no loot target to remember.
                }
            }
            if (activeKind == Kind.NONE && keyTask != null && keyTask.isFinished()
                    && mod.getItemStorage().getItemCountInventoryOnly(vaultKey) < 1) {
                keyTask = null;
            }
            activeSubtask = null;
            activeKind = Kind.NONE;
            activeTarget = null;
            return null;
        }

        switch (phase) {
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
                    scanTimer.forceElapse();
                    emptyScans = 0;
                    phase = Phase.LOOT;
                    return null;
                }
                setDebugState("Travelling to " + site);
                return track(new GetToXZTask(site.pos().getX(), site.pos().getZ(), dimension), Kind.NONE, null);
            }
            case LOOT -> {
                return tickLoot(mod);
            }
            case NEXT -> {
                return tickNext(mod);
            }
            case DONE -> {
                return null;
            }
        }
        return null;
    }

    private Task tickLocate(AltoClef mod) {
        if (dimension != null && WorldHelper.getCurrentDimension() != dimension) {
            setDebugState("Travelling to the " + dimension + " first");
            return track(new DefaultGoToDimensionTask(dimension), Kind.NONE, null);
        }
        if (locating == null) {
            if (structuresTried >= MAX_STRUCTURES) {
                giveUp("tried " + structuresTried + " " + siteName + " without finding " + item);
                return null;
            }
            structuresTried++;
            setDebugState("Locating " + siteName + " (attempt " + structuresTried + "/" + MAX_STRUCTURES + ")");
            locating = StructureLocator.locateWithRings(mod, structureIds, LOCATE_RINGS);
            return null;
        }
        if (!locating.isDone()) {
            setDebugState("Locating " + siteName + " ...");
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
            giveUp("no " + siteName + " found within the locate rings");
            return null;
        }
        site = found;
        emptyScans = 0;
        scanTimer.forceElapse();
        setDebugState("Found " + site);
        Debug.logMessage("LootSearch: " + found);
        phase = Phase.TRAVEL;
        return null;
    }

    private Task tickLoot(AltoClef mod) {
        if (mod.getBlockScanner() == null || mod.getPlayer() == null) return null;

        // A vault needs a trial key. The key is itself trial-chamber loot, so acquiring it is a
        // nested LootSearchTask through the ordinary item chain; do that before approaching a vault.
        if (openVaults && mod.getItemStorage().getItemCountInventoryOnly(vaultKey) < 1) {
            if (keyTask == null) {
                keyAttempts++;
                if (keyAttempts > 3) {
                    giveUp("could not obtain a trial key for the vaults");
                    return null;
                }
                keyTask = TaskCatalogue.getItemTask(vaultKey, 1);
                if (keyTask == null) {
                    giveUp("no chain can obtain a trial key");
                    return null;
                }
            }
            setDebugState("Getting a trial key for a vault (attempt " + keyAttempts + ")");
            return track(keyTask, Kind.NONE, null);
        }
        keyTask = null;

        if (!scanTimer.elapsed()) return null;
        scanTimer.reset();

        // For a vault-only target the chests/pots in the chamber can never contain it, so skip them
        // (they only cost the 10 s container timeout each while the chamber is searched for a vault).
        boolean containerPhasesRelevant = !openVaults || item.equals(vaultKey);

        List<BlockPos> containers = containerPhasesRelevant
                ? findNearby(mod, CONTAINER_BLOCKS, visitedContainers) : List.of();
        if (!containers.isEmpty()) {
            BlockPos container = containers.get(0);
            if (!closeEnough(mod, container, 4.0)) {
                setDebugState("Approaching container at " + container.toShortString());
                approachTimer.reset();
                return track(new GetAdjacentToBlockTask(container), Kind.APPROACH, container);
            }
            setDebugState("Looting container at " + container.toShortString());
            return track(new LootContainerTask(container, List.of(item)), Kind.CONTAINER, container);
        }

        if (breakPots && !openVaults) {
            List<BlockPos> pots = findNearby(mod, POT_BLOCKS, visitedPots);
            if (!pots.isEmpty()) {
                BlockPos pot = pots.get(0);
                setDebugState("Breaking decorated pot at " + pot.toShortString());
                return track(new DestroyBlockTask(pot), Kind.POT, pot);
            }
        }

        if (openVaults) {
            List<BlockPos> vaults = findVaults(mod);
            if (!vaults.isEmpty()) {
                BlockPos vault = vaults.get(0);
                if (!closeEnough(mod, vault, 4.0)) {
                    setDebugState("Approaching vault at " + vault.toShortString());
                    approachTimer.reset();
                    return track(new GetAdjacentToBlockTask(vault), Kind.APPROACH, vault);
                }
                vaultTimer.reset();
                setDebugState("Opening vault at " + vault.toShortString());
                return track(new InteractWithBlockTask(new ItemTarget(vaultKey, 1), vault),
                        Kind.VAULT, vault);
            }
        }

        emptyScans++;
        if (emptyScans <= 3) {
            // The scanner may simply not have rescanned this area yet; walking loads chunks and gives
            // the background scan a reason to revisit them.
            setDebugState("No loot targets found near " + site + "; exploring");
            return track(new TimeoutWanderTask(6, true), Kind.NONE, null);
        }
        Debug.logMessage("LootSearch: no usable loot for " + siteName + " at " + site + ", moving on");
        phase = Phase.NEXT;
        return null;
    }

    private Task tickNext(AltoClef mod) {
        if (site == null) {
            phase = Phase.LOCATE;
            return null;
        }
        if (retreatTarget == null) {
            double angle = Math.toRadians(45 + structuresTried * 137);
            retreatTarget = site.pos().offset(
                    (int) Math.round(Math.cos(angle) * RETREAT_DISTANCE), 0,
                    (int) Math.round(Math.sin(angle) * RETREAT_DISTANCE));
            setDebugState("Moving away to find another " + siteName);
        }
        if (horizontalDistance(mod, retreatTarget) <= 32) {
            retreatTarget = null;
            site = null;
            phase = Phase.LOCATE;
            return null;
        }
        return track(new GetToXZTask(retreatTarget.getX(), retreatTarget.getZ(), dimension), Kind.NONE, null);
    }

    /** Positions of the given block types that the scanner knows, near the site anchor, nearest first. */
    private List<BlockPos> findNearby(AltoClef mod, Block[] types, Set<BlockPos> visited) {
        if (site == null) return List.of();
        Vec3 siteCenter = Vec3.atCenterOf(site.pos());
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : mod.getBlockScanner().getKnownLocations(types)) {
            if (visited.contains(pos)) continue;
            if (!pos.closerToCenterThan(siteCenter, TARGET_RADIUS)) continue;
            found.add(pos);
        }
        Vec3 player = mod.getPlayer().position();
        found.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(player.x, player.y, player.z)));
        return found;
    }

    /** Still-inactive vaults matching this site's ominous/non-ominous kind that we have not used yet. */
    private List<BlockPos> findVaults(AltoClef mod) {
        List<BlockPos> candidates = findNearby(mod, VAULT_BLOCKS, visitedVaults);
        candidates.removeIf(pos -> !isVaultOpenable(mod, pos));
        return candidates;
    }

    /**
     * A vault is OPENABLE while its state is INACTIVE or ACTIVE. Vanilla flips it to ACTIVE as soon as a
     * player is near — the interaction itself is what runs {@code tryInsertKey} — so treating "not
     * INACTIVE" as "already used" (the first playtest build did) abandoned the vault the moment we
     * walked up to it and the key was never consumed. Used vaults move through UNLOCKING/EJECTING to
     * COOLDOWN, and that is the real "done" signal.
     */
    private boolean isVaultOpenable(AltoClef mod, BlockPos pos) {
        if (pos == null || mod.getWorld() == null) return false;
        if (!mod.getChunkTracker().isChunkLoaded(pos)) return false;
        BlockState state = mod.getWorld().getBlockState(pos);
        if (!state.is(Blocks.VAULT)) return false;
        if (state.getValue(VaultBlock.OMINOUS) != ominousVaults) return false;
        VaultState vaultState = state.getValue(VaultBlock.STATE);
        return vaultState == VaultState.INACTIVE || vaultState == VaultState.ACTIVE;
    }


    private Task track(Task task, Kind kind, BlockPos target) {
        activeSubtask = task;
        activeKind = kind;
        activeTarget = target;
        return task;
    }

    private void giveUp(String reason) {
        if (gaveUp) return;
        gaveUp = true;
        phase = Phase.DONE;
        setDebugState("Gave up: " + reason);
        Debug.logWarning("LootSearch: gave up on " + item + " — " + reason);
    }

    /** True when the player's feet are within {@code range} blocks of the block centre. */
    private static boolean closeEnough(AltoClef mod, BlockPos pos, double range) {
        if (mod.getPlayer() == null) return false;
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
        return other instanceof LootSearchTask task
                && task.siteName.equals(siteName)
                && task.structureIds.equals(structureIds);
    }

    @Override
    protected String toDebugStringName() {
        return "Loot searching " + siteName + " for " + item;
    }
}
