package adris.altoclef.chains.coverage;

import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.TradeWithVillagerTask;
import adris.altoclef.util.helpers.StructureLocator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Villager-trade coverage: the items whose only survival source is a villager trade, above all the
 * 26.3 cartographer structure maps (and the cartographer's globe banner pattern) plus the fisherman's
 * cod bucket.
 *
 * <p><b>Data, not memory.</b> Every entry below is derived from the 26.3 client jar's own trade data:
 * {@code data/minecraft/village_trade/<profession>/<level>/*.json} (level, cost, result) and
 * {@code data/minecraft/trade_set/<profession>/level_<n>.json} ({@code amount: 2} random offers per
 * level). The cartographer map trades carry a {@code merchant_predicate} on the merchant's villager
 * variant, so a map can only be sold by the villager types listed in {@link TradeTarget#villagerTypes}.
 * Only plains/desert/savanna/snow/taiga villages generate, so village types are mapped to the matching
 * {@code minecraft:village_*} structure ids for the locator; types that never occur in a generated
 * village (jungle, swamp) are dropped -- the trade is still attempted with the remaining types when
 * there are any, and skipped when the wanted map is only sold by a type a generated village cannot
 * provide.
 *
 * <p><b>Trade-only items covered here</b> (12 ids):
 * <ul>
 *   <li>cartographer, level 2: desert_village_map (savanna villagers), plains_village_map
 *       (taiga/snow/savanna/desert), savanna_village_map (plains/desert), snowy_village_map (taiga),
 *       taiga_village_map (snow/plains), swamp_hut_map (taiga/snow), jungle_pyramid_map (savanna)</li>
 *   <li>cartographer, level 3: buried_trial_chambers_map, ocean_monument_map (any type)</li>
 *   <li>cartographer, level 5: woodland_mansion_map, globe_banner_pattern (any type)</li>
 *   <li>fisherman, level 1: cod_bucket (any type)</li>
 * </ul>
 *
 * <p><b>Deliberately not covered</b>: {@code pufferfish_bucket} and {@code tropical_fish_bucket} are
 * wandering-trader trades only ({@code data/minecraft/village_trade/wandering_trader/
 * emerald_{pufferfish,tropical_fish}_bucket}); the wandering trader spawns at random and cannot be
 * located without cheats, so this chain leaves them to the general bucket coverage
 * ({@link BucketChains}) instead of pretending a locate-based task could reliably get them.
 *
 * <p><b>Availability.</b> The whole chain requires {@link StructureLocator#isAvailable()} -- it locates
 * villages through the integrated server, which exists only on a singleplayer world the client owns.
 * In multiplayer this method returns null and the catalogue falls through to its honest "no source"
 * warning; it never silently claims coverage.
 *
 * <p>Note the random level roll: each level offers only 2 of the eligible trades, so even a correct
 * villager type may not offer a level-2/3 map. {@link TradeWithVillagerTask} levels the villager, checks
 * the real offer list, and moves on to another village (bounded) when the offer is not there.
 */
public final class TradeChains {

    /** Where one item comes from: profession, level the trade lives at, and villager-type constraint. */
    private record TradeTarget(ResourceKey<VillagerProfession> profession,
                               int requiredLevel,
                               Set<ResourceKey<VillagerType>> villagerTypes,
                               Item jobSiteItem,
                               Block jobSiteBlock) {
    }

    private static final Set<ResourceKey<VillagerType>> ANY_TYPE = Set.of();

    private static final Item CARTOGRAPHY_TABLE_ITEM = Items.CARTOGRAPHY_TABLE;
    private static final Block CARTOGRAPHY_TABLE_BLOCK = Blocks.CARTOGRAPHY_TABLE;
    private static final Item BARREL_ITEM = Items.BARREL;
    private static final Block BARREL_BLOCK = Blocks.BARREL;

    /** Item id -> trade. Keyed by id so a version without the 26.3 items simply never matches. */
    private static final Map<String, TradeTarget> TARGETS = new HashMap<>();

    static {
        // ── Cartographer, level 2: village maps (26.3 data: data/minecraft/village_trade/cartographer/2)
        // The villager-type sets are the trade's own merchant_predicate, reduced to the types that
        // actually occur in generated villages.
        cartographerMap("desert_village_map", type(VillagerType.SAVANNA));
        cartographerMap("plains_village_map",
                type(VillagerType.TAIGA, VillagerType.SNOW, VillagerType.SAVANNA, VillagerType.DESERT));
        cartographerMap("savanna_village_map",
                type(VillagerType.PLAINS, VillagerType.JUNGLE, VillagerType.DESERT));
        cartographerMap("snowy_village_map", type(VillagerType.TAIGA, VillagerType.SWAMP));
        cartographerMap("taiga_village_map",
                type(VillagerType.SWAMP, VillagerType.SNOW, VillagerType.PLAINS));
        cartographerMap("swamp_hut_map",
                type(VillagerType.TAIGA, VillagerType.SNOW, VillagerType.JUNGLE));
        cartographerMap("jungle_pyramid_map",
                type(VillagerType.SWAMP, VillagerType.SAVANNA, VillagerType.DESERT));

        // ── Cartographer, level 3: trial chambers and ocean monument maps (no merchant predicate).
        cartographerMap("buried_trial_chambers_map", ANY_TYPE);
        cartographerMap("ocean_monument_map", ANY_TYPE);

        // ── Cartographer, level 5: the woodland mansion map and the globe banner pattern. The level-5
        // trade set has exactly two entries and asks for both, so a level-5 cartographer always sells
        // these two.
        cartographerMap("woodland_mansion_map", ANY_TYPE);
        cartographerMap("globe_banner_pattern", ANY_TYPE);

        // ── Fisherman, level 1: cod bucket costs 3 emeralds.
        put("cod_bucket", new TradeTarget(VillagerProfession.FISHERMAN, 1, ANY_TYPE,
                BARREL_ITEM, BARREL_BLOCK));
    }

    private TradeChains() {
    }

    private static void put(String path, TradeTarget target) {
        TARGETS.put("minecraft:" + path, target);
    }

    private static void cartographerMap(String path, Set<ResourceKey<VillagerType>> villagerTypes) {
        int level = mapRequiredLevel(path);
        put(path, new TradeTarget(VillagerProfession.CARTOGRAPHER, level, villagerTypes,
                CARTOGRAPHY_TABLE_ITEM, CARTOGRAPHY_TABLE_BLOCK));
    }

    /** The cartographer level each mapped trade lives at, straight from the trade folder name. */
    private static int mapRequiredLevel(String path) {
        return switch (path) {
            case "desert_village_map", "plains_village_map", "savanna_village_map",
                 "snowy_village_map", "taiga_village_map", "swamp_hut_map", "jungle_pyramid_map" -> 2;
            case "buried_trial_chambers_map", "ocean_monument_map" -> 3;
            case "woodland_mansion_map", "globe_banner_pattern" -> 5;
            default -> 2;
        };
    }

    @SafeVarargs
    private static Set<ResourceKey<VillagerType>> type(ResourceKey<VillagerType>... types) {
        return Set.of(types);
    }

    public static ResourceTask getTask(Item item, int count) {
        if (item == null || TARGETS.isEmpty()) return null;
        // Villages are located through the integrated server; there is none on a multiplayer server,
        // so no honest task can be offered there.
        if (!StructureLocator.isAvailable()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
        TradeTarget target = TARGETS.get(id.toString());
        if (target == null) return null;
        List<String> structures = villageStructuresFor(target.villagerTypes());
        return new TradeWithVillagerTask(item, count, target.profession(), target.requiredLevel(),
                target.villagerTypes(), structures, target.jobSiteItem(), target.jobSiteBlock());
    }

    /**
     * The village structure ids whose generated villagers can carry the wanted villager type. An empty
     * constraint (any type) means "any of the five village structures".
     */
    private static List<String> villageStructuresFor(Set<ResourceKey<VillagerType>> villagerTypes) {
        List<String> structures = new ArrayList<>();
        Set<ResourceKey<VillagerType>> types = new HashSet<>(villagerTypes);
        if (types.isEmpty()) {
            types.add(VillagerType.PLAINS);
            types.add(VillagerType.DESERT);
            types.add(VillagerType.SAVANNA);
            types.add(VillagerType.SNOW);
            types.add(VillagerType.TAIGA);
        }
        if (types.contains(VillagerType.PLAINS)) structures.add("minecraft:village_plains");
        if (types.contains(VillagerType.DESERT)) structures.add("minecraft:village_desert");
        if (types.contains(VillagerType.SAVANNA)) structures.add("minecraft:village_savanna");
        if (types.contains(VillagerType.SNOW)) structures.add("minecraft:village_snowy");
        if (types.contains(VillagerType.TAIGA)) structures.add("minecraft:village_taiga");
        // Jungle/swamp villagers cannot be found in generated villages; if the wanted type set reduces
        // to nothing, fall back to all village structures so the task at least checks real villages
        // (the villager-type filter inside the task will reject ineligible cartographers).
        if (structures.isEmpty()) {
            structures.add("minecraft:village_plains");
            structures.add("minecraft:village_desert");
            structures.add("minecraft:village_savanna");
            structures.add("minecraft:village_snowy");
            structures.add("minecraft:village_taiga");
        }
        return List.copyOf(structures);
    }
}
