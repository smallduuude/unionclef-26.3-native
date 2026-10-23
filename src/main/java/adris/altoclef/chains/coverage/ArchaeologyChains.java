package adris.altoclef.chains.coverage;

import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.BrushArmadilloTask;
import adris.altoclef.tasks.misc.BrushBlockTask;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.StructureLocator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Archaeology coverage: the pottery sherds and the music disc whose only source is the loot table
 * of a suspicious sand/gravel block, brushed with a brush.
 *
 * <p>A suspicious block's loot table is chosen by whatever generated the block, so the item to site
 * map below is exactly the structure → archaeology loot table relation taken from the 26.3 client
 * jar's own data ({@code data/minecraft/loot_table/archaeology/*.json} in the merged jar), not from
 * memory:
 *
 * <ul>
 *   <li>{@code archaeology/desert_pyramid} — archer, miner, prize, skull sherds</li>
 *   <li>{@code archaeology/trail_ruins_rare} — burn, danger, friend, heart, heartbreak, howl,
 *       sheaf sherds and {@code music_disc_relic} (trail ruins' rare table is used by its buried
 *       suspicious gravel; the scanner still sees those blocks)</li>
 *   <li>{@code archaeology/ocean_ruin_cold} — blade, explorer, mourner, plenty sherds</li>
 *   <li>{@code archaeology/ocean_ruin_warm} — angler, shelter, snort sherds</li>
 *   <li>{@code archaeology/desert_well} — arms_up, brewer sherds</li>
 * </ul>
 *
 * <p>Desert wells are worldgen features, not locatable structures: their two sherds are farmed by
 * exploring desert biome and waiting for the block scanner to see the well's suspicious sand. That
 * path is slower and best-effort, and the task gives up honestly after its budget.
 *
 * <p>{@code flow}, {@code guster} and {@code scrape} pottery sherds are deliberately NOT here:
 * they are not archaeology loot (they come from decorated-pot patterns), and the Lead's
 * trial-chamber pot search owns them.
 *
 * <p>Structure sites need {@link StructureLocator}, which only works in singleplayer. On a
 * multiplayer server only the desert-well path can work, so the other ids fall through to the
 * next coverage chain (and ultimately to the honest "no source" warning).
 */
public final class ArchaeologyChains {

    /** One archaeology site: its name, the structures to locate it by, and whether it is a well. */
    private record Site(String name, List<String> structureIds, boolean desertWell) {
    }

    private static final Site DESERT_PYRAMID = new Site("desert_pyramid",
            List.of("minecraft:desert_pyramid"), false);

    private static final Site TRAIL_RUINS = new Site("trail_ruins",
            List.of("minecraft:trail_ruins"), false);

    private static final Site OCEAN_RUIN_COLD = new Site("ocean_ruin_cold",
            List.of("minecraft:ocean_ruin_cold"), false);

    private static final Site OCEAN_RUIN_WARM = new Site("ocean_ruin_warm",
            List.of("minecraft:ocean_ruin_warm"), false);

    /** Not a locatable structure id; found by exploring desert biome. */
    private static final Site DESERT_WELL = new Site("desert_well", List.of(), true);

    /** Item id → site. Keyed by id so version-specific ids simply never match. */
    private static final Map<String, Site> TARGETS = new HashMap<>();

    static {
        put(DESERT_PYRAMID, "archer_pottery_sherd", "miner_pottery_sherd",
                "prize_pottery_sherd", "skull_pottery_sherd");

        put(TRAIL_RUINS, "burn_pottery_sherd", "danger_pottery_sherd", "friend_pottery_sherd",
                "heart_pottery_sherd", "heartbreak_pottery_sherd", "howl_pottery_sherd",
                "sheaf_pottery_sherd", "music_disc_relic");

        put(OCEAN_RUIN_COLD, "blade_pottery_sherd", "explorer_pottery_sherd",
                "mourner_pottery_sherd", "plenty_pottery_sherd");

        put(OCEAN_RUIN_WARM, "angler_pottery_sherd", "shelter_pottery_sherd",
                "snort_pottery_sherd");

        put(DESERT_WELL, "arms_up_pottery_sherd", "brewer_pottery_sherd");
    }

    private ArchaeologyChains() {
    }

    private static void put(Site site, String... itemPaths) {
        for (String path : itemPaths) {
            TARGETS.put("minecraft:" + path, site);
        }
    }

    public static ResourceTask getTask(Item item, int count) {
        if (item == null) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
        // Armadillo scute: same brush, different medium — right-click the mob instead of a block.
        // Needs no structure locator, so it is offered on multiplayer too.
        if (id.toString().equals("minecraft:armadillo_scute")) {
            return new BrushArmadilloTask(count);
        }
        Site site = TARGETS.get(id.toString());
        if (site == null) return null;
        // Locating a structure needs the integrated server. The desert-well path explores and scans,
        // which also works on a server, so it is offered in both cases.
        if (!site.desertWell() && !StructureLocator.isAvailable()) return null;
        return new BrushBlockTask(item, count, site.name(), site.structureIds(),
                Dimension.OVERWORLD, site.desertWell());
    }
}
