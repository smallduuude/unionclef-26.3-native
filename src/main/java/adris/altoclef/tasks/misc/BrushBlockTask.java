package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetAdjacentToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.movement.SearchWithinBiomeTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import kaptainwutax.tungsten.path.movements.Input;
import kaptainwutax.tungsten.path.movements.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Brushes suspicious sand/gravel until the wanted pottery sherd (or {@code music_disc_relic}) drops.
 *
 * <p>This is the archaeology counterpart of {@link LootSearchTask}: where that task locates a
 * structure and loots its containers, this one locates a structure and brushes the suspicious blocks
 * its generator placed. Which loot table a suspicious block rolls is decided by the structure that
 * generated it, so a sherd whose only source is one structure's archaeology table must be farmed at
 * that structure — {@link adris.altoclef.chains.coverage.ArchaeologyChains} owns that mapping.
 *
 * <p><b>How brushing works in 26.3.</b> A brush is a held right-click: the first right-click calls
 * {@code BrushItem.useOn}, which starts the use animation, and the server ticks
 * {@code BrushItem.onUseTick} while the use key is held. Every ten ticks the block's dusted level
 * advances; after four steps the brushable block turns into its sand/gravel host and pops its loot.
 * So the bot must equip the brush, keep the crosshair on the block, and keep {@code CLICK_RIGHT}
 * down — a single click is not enough. The drop itself is picked up by
 * {@link ResourceTask}'s normal pickup gate, which is why this task only has to make the block
 * change.
 *
 * <p><b>Finding the blocks.</b> {@code BlockScanner} already indexes every non-air block of every
 * loaded chunk (including buried ones, which is exactly what trail-ruins suspicious gravel is), so
 * "the suspicious blocks within 110 blocks of this structure anchor" is a question the client can
 * answer without a per-structure coordinate table. The anchor itself comes from
 * {@link StructureLocator}, which is cheat-free but singleplayer-only.
 *
 * <p><b>Desert wells.</b> Desert wells are worldgen features, not locatable structures, yet their
 * water bottom is the only source of {@code arms_up_pottery_sherd} and
 * {@code brewer_pottery_sherd}. Those two items are therefore handled by exploring desert biome
 * ({@link SearchWithinBiomeTask}) and waiting for the block scanner to see a well's suspicious sand.
 * That is slower and not guaranteed within the explore budget; when the budget runs out the task
 * gives up and says so instead of pretending the item is unobtainable.
 *
 * <p><b>Giving up.</b> Loot rolls are random, so "all blocks at one site were brushed and the item
 * did not drop" is normal. The task retreats ~512 blocks, locates another site, and repeats, up to
 * {@link #MAX_SITES} sites / {@link #MAX_BRUSHES} blocks. After that it gives up honestly
 * ({@link #isFinished()} becomes true, with a warning in the log) rather than roaming forever.
 */
public class BrushBlockTask extends ResourceTask {

    /** Suspicious blocks are searched within this block radius of the structure anchor. */
    private static final int BRUSH_RADIUS = 110;
    /** Desert-well exploration ignores known suspicious blocks beyond this distance. */
    private static final int DESERT_KNOWN_RADIUS = 256;
    /** Locator search rings, in chunks. {@code /locate structure} uses 100; rare structures need more. */
    private static final int[] LOCATE_RINGS = {100, 200, 400};
    /** Enough sites to survive bad loot rolls; few enough that the task can give up. */
    private static final int MAX_SITES = 8;
    /** Total suspicious blocks one task may brush before admitting the rolls are not coming. */
    private static final int MAX_BRUSHES = 120;
    /** Horizontal distance that counts as "arrived at the site". */
    private static final double ARRIVE_RADIUS = 18.0;
    /** How far to walk away between two sites, so the locator returns a different one. */
    private static final int RETREAT_DISTANCE = 512;
    /** Empty scans in a row before declaring "no suspicious blocks left here". */
    private static final int MAX_EMPTY_SCANS = 3;
    /** Wall-clock budget to reach one known suspicious block before skipping it. */
    private static final long APPROACH_BUDGET_MS = 120_000L;
    /** Wall-clock budget for one block's brush hold before skipping it. */
    private static final long BRUSH_BUDGET_MS = 25_000L;
    /** Wall-clock budget for the desert-well exploration path. */
    private static final long DESERT_SEARCH_BUDGET_MS = 8 * 60_000L;

    private static final Block[] SUSPICIOUS_BLOCKS = {
            Blocks.SUSPICIOUS_SAND, Blocks.SUSPICIOUS_GRAVEL
    };

    private enum Phase {OBTAIN_BRUSH, LOCATE, TRAVEL, BRUSH, NEXT, EXPLORE, DONE}

    private final Item item;
    private final String siteName;
    private final List<String> structureIds;
    private final Dimension dimension;
    private final boolean desertWell;

    private Phase phase;
    private CompletableFuture<StructureLocator.Located> locating;
    private StructureLocator.Located site;
    private int sitesTried = 0;
    private int brushes = 0;
    private int emptyScans = 0;
    private boolean gaveUp = false;
    private long desertSearchStartMs = 0L;

    /** Blocks already brushed or skipped, so one brushable block is never attempted twice. */
    private final Set<BlockPos> visited = new HashSet<>();
    private BlockPos brushTarget;
    private long brushTargetSinceMs = 0L;
    private boolean holdingUse = false;
    private long holdingSinceMs = 0L;
    /** True on ticks where {@link ResourceTask#onResourceTick} actually ran (see onTick below). */
    private boolean resourceTickRan = false;

    private final TimerGame scanTimer = new TimerGame(3);
    private BlockPos retreatTarget;

    public BrushBlockTask(Item item, int count, String siteName, List<String> structureIds,
                          Dimension dimension, boolean desertWell) {
        super(new ItemTarget(item, count));
        this.item = item;
        this.siteName = siteName;
        this.structureIds = List.copyOf(structureIds);
        this.dimension = dimension;
        this.desertWell = desertWell;
        // Both paths start by making sure we own a brush; the desert-well path then explores
        // instead of running the structure locator.
        this.phase = Phase.OBTAIN_BRUSH;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // The loot pops out of the brushed block, so walking toward a target drop is the point.
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        Debug.logMessage("Brush: looking for " + itemId() + " at " + siteName
                + (desertWell ? " (desert-well exploration)" : " (" + structureIds.size() + " structure ids)"));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        releaseUse(mod);
        brushTarget = null;
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        resourceTickRan = false;
        Task result = super.onTick();
        if (!resourceTickRan) {
            // The parent ResourceTask claimed this tick (picking up the drop we just made, moving a
            // cursor item, ...). Our brush hold is not being refreshed, and holding a use key
            // through someone else's movement can click whatever the crosshair lands on. Let go;
            // the next brushing tick presses again.
            releaseUse(mod);
        }
        return result;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        resourceTickRan = true;
        if (gaveUp) return null;

        // Every archaeology site in the game is in the overworld.
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            releaseUse(mod);
            setDebugState("Going to the overworld for " + siteName);
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        if (phase == Phase.BRUSH) {
            return tickBrush(mod);
        }
        releaseUse(mod);

        switch (phase) {
            case OBTAIN_BRUSH -> {
                if (!mod.getItemStorage().hasItem(Items.BRUSH)) {
                    setDebugState("Obtaining a brush");
                    return TaskCatalogue.getItemTask(Items.BRUSH, 1);
                }
                if (desertWell) {
                    phase = Phase.EXPLORE;
                } else {
                    phase = Phase.LOCATE;
                }
                return null;
            }
            case LOCATE -> {
                return tickLocate(mod);
            }
            case TRAVEL -> {
                return tickTravel(mod);
            }
            case NEXT -> {
                return tickNext(mod);
            }
            case EXPLORE -> {
                return tickExplore(mod);
            }
            case DONE -> {
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    // ------------------------------------------------------------------ locating

    private Task tickLocate(AltoClef mod) {
        if (locating == null) {
            if (sitesTried >= MAX_SITES) {
                giveUp("tried " + sitesTried + " " + siteName + " sites without finding " + itemId());
                return null;
            }
            if (brushes >= MAX_BRUSHES) {
                giveUp("brushed " + brushes + " suspicious blocks without finding " + itemId());
                return null;
            }
            sitesTried++;
            setDebugState("Locating archaeology site (" + sitesTried + "/" + MAX_SITES + ")");
            locating = StructureLocator.locateWithRings(mod, structureIds, LOCATE_RINGS);
            return null;
        }
        if (!locating.isDone()) {
            setDebugState("Locating archaeology site...");
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
        Debug.logMessage("Brush: located " + site + " for " + itemId());
        phase = Phase.TRAVEL;
        return null;
    }

    private Task tickTravel(AltoClef mod) {
        if (site == null) {
            phase = desertWell ? Phase.EXPLORE : Phase.LOCATE;
            return null;
        }
        if (horizontalDistance(mod, site.pos()) <= ARRIVE_RADIUS) {
            setDebugState("Arrived at " + site);
            scanTimer.forceElapse();
            emptyScans = 0;
            phase = Phase.BRUSH;
            return null;
        }
        setDebugState("Travelling to archaeology site " + siteName);
        return new GetToXZTask(site.pos().getX(), site.pos().getZ(), dimension);
    }

    private Task tickNext(AltoClef mod) {
        if (site == null) {
            phase = Phase.LOCATE;
            return null;
        }
        if (retreatTarget == null) {
            double angle = Math.toRadians(45 + sitesTried * 137);
            retreatTarget = site.pos().offset(
                    (int) Math.round(Math.cos(angle) * RETREAT_DISTANCE), 0,
                    (int) Math.round(Math.sin(angle) * RETREAT_DISTANCE));
            setDebugState("Moving away to find another " + siteName + " site");
            Debug.logMessage("Brush: site " + site + " gave no " + itemId() + ", moving on");
        }
        if (horizontalDistance(mod, retreatTarget) <= 32) {
            retreatTarget = null;
            site = null;
            phase = Phase.LOCATE;
            return null;
        }
        return new GetToXZTask(retreatTarget.getX(), retreatTarget.getZ(), dimension);
    }

    // ------------------------------------------------------------------ desert-well exploration

    private Task tickExplore(AltoClef mod) {
        if (desertSearchStartMs == 0L) {
            desertSearchStartMs = System.currentTimeMillis();
        }
        List<BlockPos> known = findSuspicious(mod);
        if (!known.isEmpty()) {
            setDebugState("Found suspicious sand while exploring the desert");
            Debug.logMessage("Brush: found " + known.size() + " known suspicious block(s) near "
                    + mod.getPlayer().blockPosition().toShortString() + " while exploring desert biome");
            scanTimer.forceElapse();
            emptyScans = 0;
            phase = Phase.BRUSH;
            return null;
        }
        if (System.currentTimeMillis() - desertSearchStartMs > DESERT_SEARCH_BUDGET_MS) {
            giveUp("explored desert biome for " + (DESERT_SEARCH_BUDGET_MS / 60_000L)
                    + " min and never saw a desert well's suspicious sand");
            return null;
        }
        if (brushes >= MAX_BRUSHES) {
            giveUp("brushed " + brushes + " suspicious blocks without finding " + itemId());
            return null;
        }
        setDebugState("Exploring desert biome for a desert well");
        return new SearchWithinBiomeTask(Biomes.DESERT);
    }

    // ------------------------------------------------------------------ brushing

    private Task tickBrush(AltoClef mod) {
        if (brushTarget == null) {
            return tickPickNextBlock(mod);
        }

        BlockPos target = brushTarget;
        boolean loaded = mod.getChunkTracker().isChunkLoaded(target);
        Block block = loaded ? mod.getWorld().getBlockState(target).getBlock() : Blocks.AIR;
        boolean stillSuspicious = loaded && isSuspicious(block);

        if (!stillSuspicious) {
            releaseUse(mod);
            if (loaded) {
                // The brushable block turned into its sand/gravel host: it was brushed (or broken).
                brushes++;
                visited.add(target);
                emptyScans = 0;
                scanTimer.forceElapse();
                setDebugState("Brushed a suspicious block (" + brushes + " so far)");
                Debug.logMessage("Brush: finished a suspicious block at " + target.toShortString()
                        + " (" + brushes + " blocks brushed)");
            } else {
                // Chunk unloaded mid-approach; not a success, but don't keep the stale target.
                setDebugState("Lost sight of suspicious block at " + target.toShortString());
            }
            brushTarget = null;
            if (brushes >= MAX_BRUSHES) {
                giveUp("brushed " + brushes + " suspicious blocks without finding " + itemId());
            }
            return null;
        }

        if (System.currentTimeMillis() - brushTargetSinceMs > APPROACH_BUDGET_MS) {
            releaseUse(mod);
            visited.add(target);
            brushTarget = null;
            Debug.logMessage("Brush: could not get to " + target.toShortString() + " in time, skipping");
            return null;
        }

        Optional<Rotation> reach = LookHelper.getReach(target);
        if (reach.isEmpty()) {
            releaseUse(mod);
            setDebugState("Approaching suspicious block at " + target.toShortString());
            return new GetAdjacentToBlockTask(target);
        }

        if (!LookHelper.isLookingAt(mod, target)) {
            releaseUse(mod);
            LookHelper.lookAt(mod, reach.get(), false);
            setDebugState("Aiming at suspicious block at " + target.toShortString());
            return null;
        }

        // Equip the brush. Changing slots mid-use cancels the use, so this happens before the hold.
        if (mod.getPlayer().getMainHandItem().getItem() != Items.BRUSH) {
            releaseUse(mod);
            if (!mod.getItemStorage().hasItem(Items.BRUSH)) {
                brushTarget = null;
                phase = Phase.OBTAIN_BRUSH;
                setDebugState("Out of brushes, getting another");
                return null;
            }
            mod.getSlotHandler().forceEquipItem(Items.BRUSH);
            return null;
        }

        if (!holdingUse) {
            holdingUse = true;
            holdingSinceMs = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - holdingSinceMs > BRUSH_BUDGET_MS) {
            releaseUse(mod);
            visited.add(target);
            brushTarget = null;
            Debug.logMessage("Brush: brushing at " + target.toShortString() + " did not take, skipping");
            return null;
        }

        setDebugState("Brushing " + blockName(block) + " at " + target.toShortString());
        // HELD right click: one click only starts the animation, the server advances the dust while
        // the key stays down. Repeating hold() every tick keeps it down without re-clicking.
        mod.getInputControls().hold(Input.CLICK_RIGHT);
        return null;
    }

    private Task tickPickNextBlock(AltoClef mod) {
        if (!scanTimer.elapsed()) {
            // The block scanner rescans about once a second; give it a beat after arriving.
            setDebugState("Looking for suspicious blocks near " + siteName);
            return null;
        }
        scanTimer.reset();
        List<BlockPos> candidates = findSuspicious(mod);
        if (candidates.isEmpty()) {
            emptyScans++;
            if (emptyScans <= MAX_EMPTY_SCANS) {
                setDebugState("No suspicious blocks seen yet; exploring the site");
                return new TimeoutWanderTask(6, true);
            }
            emptyScans = 0;
            Debug.logMessage("Brush: no more suspicious blocks at " + (site == null ? "here" : site.toString()));
            if (desertWell) {
                phase = Phase.EXPLORE;
                desertSearchStartMs = System.currentTimeMillis();
            } else {
                phase = Phase.NEXT;
            }
            return null;
        }
        emptyScans = 0;
        brushTarget = candidates.get(0);
        brushTargetSinceMs = System.currentTimeMillis();
        holdingUse = false;
        setDebugState("Found suspicious block at " + brushTarget.toShortString());
        return null;
    }

    /**
     * Known suspicious blocks that have not been visited yet, nearest first. Structure sites are
     * limited to the structure's neighbourhood; the desert-well path accepts anything known nearby
     * because the well's position is discovered by exploring, not by locating.
     */
    private List<BlockPos> findSuspicious(AltoClef mod) {
        List<BlockPos> known = mod.getBlockScanner().getKnownLocations(SUSPICIOUS_BLOCKS);
        List<BlockPos> result = new ArrayList<>();
        Vec3 player = mod.getPlayer().position();
        if (desertWell || site == null) {
            for (BlockPos pos : known) {
                if (visited.contains(pos)) continue;
                if (!pos.closerToCenterThan(player, DESERT_KNOWN_RADIUS)) continue;
                if (!isStillSuspiciousHere(mod, pos)) continue;
                result.add(pos);
            }
        } else {
            Vec3 center = Vec3.atCenterOf(site.pos());
            for (BlockPos pos : known) {
                if (visited.contains(pos)) continue;
                if (!pos.closerToCenterThan(center, BRUSH_RADIUS)) continue;
                if (!isStillSuspiciousHere(mod, pos)) continue;
                result.add(pos);
            }
        }
        result.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(player.x, player.y, player.z)));
        return result;
    }

    private boolean isStillSuspiciousHere(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos)) return false;
        return isSuspicious(mod.getWorld().getBlockState(pos).getBlock());
    }

    // ------------------------------------------------------------------ helpers

    private void releaseUse(AltoClef mod) {
        mod.getInputControls().release(Input.CLICK_RIGHT);
        holdingUse = false;
    }

    private static boolean isSuspicious(Block block) {
        return block == Blocks.SUSPICIOUS_SAND || block == Blocks.SUSPICIOUS_GRAVEL;
    }

    private static String blockName(Block block) {
        return block == Blocks.SUSPICIOUS_GRAVEL ? "suspicious gravel" : "suspicious sand";
    }

    private String itemId() {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? item.toString() : id.toString();
    }

    private void giveUp(String reason) {
        if (gaveUp) return;
        gaveUp = true;
        phase = Phase.DONE;
        setDebugState("Gave up: " + reason);
        Debug.logWarning("Brush: gave up on " + itemId() + " — " + reason);
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
        return other instanceof BrushBlockTask task
                && task.siteName.equals(siteName)
                && task.structureIds.equals(structureIds)
                && task.desertWell == desertWell;
    }

    @Override
    protected String toDebugStringName() {
        return "Brushing " + siteName + " for " + item;
    }
}
