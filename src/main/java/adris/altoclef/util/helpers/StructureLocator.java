package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Cheat-free structure locator.
 *
 * <p>Vanilla's {@code /locate structure} is a server command: on a multiplayer server it needs op or
 * cheats, so a client bot cannot use it. But when the player is in a <b>singleplayer</b> world the
 * client owns the integrated server, and the server-side world generator is reachable from the
 * client process without any command and without operator rights. This class asks that generator
 * the same question the command asks:
 *
 * <pre>
 *   ChunkGenerator.findNearestMapStructure(ServerLevel, HolderSet&lt;Structure&gt;, origin, radiusChunks, skipExistingChunks)
 * </pre>
 *
 * <p>It is therefore cheat-free (no command, no op, no seed extraction) and exact (it uses the real
 * structure placement), but it only works in singleplayer. On a multiplayer server
 * {@link #isAvailable()} is false and callers must fall back to exploration or report the limit —
 * never silently pretend a location was found.
 *
 * <p><b>Threading.</b> {@code findNearestMapStructure} reads server-side data and is normally called
 * on the server thread. This class never calls it from the render thread: it enqueues the lookup with
 * {@link net.minecraft.server.MinecraftServer#executeIfPossible(Runnable)} and completes a
 * {@link CompletableFuture} with the answer. The returned future is completed with {@code null} for
 * "not found" (no structure in range, no integrated server, bad id) — callers poll
 * {@link CompletableFuture#isDone()} and must handle null.
 *
 * <p>Note: a structure <i>position</i> is the generator's locate position (the chunk's locate anchor),
 * not the structure's bounding box. For a generic loot search that is enough: the task travels there,
 * then searches the loaded chunks around it for containers.
 */
public final class StructureLocator {

    /** A structure found by the locator. */
    public record Located(String structureId, BlockPos pos, ResourceKey<Level> dimension) {
        @Override
        public String toString() {
            return structureId + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
    }

    private StructureLocator() {
    }

    /** True when the client owns an integrated server, i.e. the player is in a singleplayer world. */
    public static boolean isAvailable() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null;
    }

    /**
     * Locate the nearest generated structure among {@code structureIds} around the player's current
     * position, in the player's current dimension.
     *
     * @param mod                 the mod instance (player/world access for the origin)
     * @param structureIds        vanilla structure ids, e.g. {@code "minecraft:buried_treasure"} or
     *                            {@code "minecraft:abandoned_camp_forest"}. Unresolvable ids are
     *                            logged and skipped; an empty resolvable set completes with null.
     * @param radiusChunks        search radius in chunks (vanilla {@code /locate} uses 100)
     * @param skipExistingChunks  forwarded to the generator (false is what vanilla's command uses)
     * @return a future that completes on the server thread with the result, or null if none
     */
    public static CompletableFuture<Located> locate(AltoClef mod, List<String> structureIds, int radiusChunks,
                                                    boolean skipExistingChunks) {
        CompletableFuture<Located> result = new CompletableFuture<>();
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer()) {
            result.complete(null);
            return result;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mod.getWorld() == null || mod.getPlayer() == null) {
            result.complete(null);
            return result;
        }
        ResourceKey<Level> dimension = mod.getWorld().dimension();
        BlockPos origin = mod.getPlayer().blockPosition();

        server.executeIfPossible(() -> {
            Located located = null;
            try {
                ServerLevel level = server.getLevel(dimension);
                if (level != null) {
                    Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                    List<Holder<Structure>> holders = new ArrayList<>();
                    for (String id : structureIds) {
                        try {
                            Optional<Holder.Reference<Structure>> holder = registry.get(Identifier.parse(id));
                            if (holder.isPresent()) {
                                holders.add(holder.get());
                            } else {
                                Debug.logWarning("StructureLocator: unknown structure id '" + id + "'");
                            }
                        } catch (Exception e) {
                            Debug.logWarning("StructureLocator: bad structure id '" + id + "': " + e.getMessage());
                        }
                    }
                    if (!holders.isEmpty()) {
                        ChunkGenerator generator = level.getChunkSource().getGenerator();
                        Pair<BlockPos, Holder<Structure>> found = generator.findNearestMapStructure(
                                level, HolderSet.direct(holders), origin, radiusChunks, skipExistingChunks);
                        if (found != null) {
                            Identifier key = registry.getKey(found.getSecond().value());
                            located = new Located(key == null ? "unknown" : key.toString(),
                                    found.getFirst(), dimension);
                        }
                    }
                }
            } catch (Throwable t) {
                // A locate failure is never a crash: the caller falls back to whatever else it can do.
                Debug.logWarning("StructureLocator: locate failed: " + t);
            }
            result.complete(located);
        });
        return result;
    }

    /**
     * Locate a single structure id around the player, retrying outward through {@code radiusChunks}
     * in increasing steps (100, 200, 400 chunks) because one generator search can return null for a
     * rare structure even when the next ring has one.
     *
     * <p>Asynchronous: returns the future for the <b>first</b> attempt; use {@link #locateAndWait}
     * when a synchronous answer is acceptable.
     */
    public static CompletableFuture<Located> locateWithRings(AltoClef mod, List<String> structureIds,
                                                            int[] radiusChunksRings) {
        CompletableFuture<Located> result = new CompletableFuture<>();
        int[] rings = radiusChunksRings == null || radiusChunksRings.length == 0
                ? new int[]{100, 200, 400} : radiusChunksRings;
        locateRing(mod, structureIds, rings, 0, result);
        return result;
    }

    private static void locateRing(AltoClef mod, List<String> structureIds, int[] rings, int index,
                                   CompletableFuture<Located> result) {
        if (index >= rings.length) {
            result.complete(null);
            return;
        }
        locate(mod, structureIds, rings[index], false).whenComplete((found, error) -> {
            if (error != null || found != null) {
                result.complete(found);
            } else {
                locateRing(mod, structureIds, rings, index + 1, result);
            }
        });
    }

    /** Synchronous convenience for commands/diagnostics. Never call from the server thread. */
    public static Located locateAndWait(AltoClef mod, List<String> structureIds, int radiusChunks, long timeoutMs) {
        try {
            return locate(mod, structureIds, radiusChunks, false).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }
}
