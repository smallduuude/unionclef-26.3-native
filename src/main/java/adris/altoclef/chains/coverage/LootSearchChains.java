package adris.altoclef.chains.coverage;

import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.LootSearchTask;
import adris.altoclef.tasks.misc.OminousKeyTask;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.StructureLocator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Structure-loot coverage: the items whose only survival source is loot placed by a generated
 * structure. Each entry names the structures that can yield the item and the dimension they live in;
 * {@link LootSearchTask} locates the nearest one, travels, and loots every target around it.
 *
 * <p>The item → structure map below is derived from the 26.3/26.2 client jars' own loot tables (the
 * same data the game uses), not from memory:
 *
 * <ul>
 *   <li>nautilus armour family — {@code chests/buried_treasure}, {@code chests/shipwreck_*},
 *       {@code chests/underwater_ruin_*}</li>
 *   <li>echo_shard / disc_fragment_5 — {@code chests/ancient_city}</li>
 *   <li>goat_horn — {@code chests/pillager_outpost}</li>
 *   <li>trial_key — {@code chests/trial_chambers/entrance} and the corridor/pot/spawner tables</li>
 *   <li>music_disc_13/cat/otherside — {@code chests/ancient_city}, {@code chests/woodland_mansion}</li>
 *   <li>music_disc_bounce — {@code chests/abandoned_mineshaft}</li>
 *   <li>trident, guster_banner_pattern, music_disc_precipice — trial-chamber VAULT
 *       ({@code reward_unique}); ominous_bottle — {@code reward_common} vault loot</li>
 *   <li>music_disc_creator_music_box — decorated pots in trial-chamber corridors</li>
 *   <li>piglin_banner_pattern, music_disc_pigstep — bastion remnant chests (Nether)</li>
 *   <li>26.3 structure maps — {@code chests/abandoned_camp_common_chest} and
 *       {@code chests/abandoned_camp_secret_chest} (the 18 vanilla {@code abandoned_camp_*} structure
 *       ids are all the same camp type in different biomes); {@code buried_treasure_map} instead
 *       comes from shipwreck and ocean-ruin chests</li>
 * </ul>
 *
 * <p>The ominous-trial-spawner family is here too: {@code ominous_trial_key} is obtained by
 * {@code OminousKeyTask} (ominous bottle -> ominous trial spawner -> ejects the key), and
 * {@code flow_banner_pattern}, {@code music_disc_creator} and {@code heavy_core} come from the
 * ominous vaults opened with that key (the same {@code LootSearchTask} with its ominous-vault mode).
 *
 * <p>On a multiplayer server {@link StructureLocator} is unavailable (no integrated server), so this
 * chain offers nothing there and the catalogue falls through to its honest "no source" warning.
 */
public final class LootSearchChains {

    private record LootSite(String name, List<String> structures, Dimension dimension,
                            boolean breakPots, boolean openVaults, boolean ominousVaults) {
    }

    private static final LootSite NAUTILUS = new LootSite("nautilus_armor", List.of(
            "minecraft:buried_treasure",
            "minecraft:shipwreck", "minecraft:shipwreck_beached",
            "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm"), Dimension.OVERWORLD, false, false, false);

    private static final LootSite ANCIENT_CITY = new LootSite("ancient_city",
            List.of("minecraft:ancient_city"), Dimension.OVERWORLD, false, false, false);

    private static final LootSite PILLAGER_OUTPOST = new LootSite("pillager_outpost",
            List.of("minecraft:pillager_outpost"), Dimension.OVERWORLD, false, false, false);

    private static final LootSite TRIAL_CHAMBERS = new LootSite("trial_chambers",
            List.of("minecraft:trial_chambers"), Dimension.OVERWORLD, true, false, false);

    private static final LootSite TRIAL_VAULTS = new LootSite("trial_chamber_vaults",
            List.of("minecraft:trial_chambers"), Dimension.OVERWORLD, false, true, false);

    private static final LootSite TRIAL_POTS = new LootSite("trial_chamber_pots",
            List.of("minecraft:trial_chambers"), Dimension.OVERWORLD, true, false, false);

    private static final LootSite OMINOUS_VAULTS = new LootSite("ominous_vaults",
            List.of("minecraft:trial_chambers"), Dimension.OVERWORLD, false, true, true);

    private static final LootSite BASTION = new LootSite("bastion", List.of("minecraft:bastion_remnant"),
            Dimension.NETHER, false, false, false);

    private static final LootSite MINESHAFT = new LootSite("mineshaft",
            List.of("minecraft:mineshaft", "minecraft:mineshaft_mesa"), Dimension.OVERWORLD, false, false, false);

    private static final LootSite DUNGEON_DISCS = new LootSite("dungeon_disc",
            List.of("minecraft:ancient_city", "minecraft:mansion"), Dimension.OVERWORLD, false, false, false);

    private static final LootSite ABANDONED_CAMP = new LootSite("abandoned_camp", List.of(
            "minecraft:abandoned_camp_bamboo_jungle",
            "minecraft:abandoned_camp_birch_forest",
            "minecraft:abandoned_camp_cherry_grove",
            "minecraft:abandoned_camp_dappled_forest",
            "minecraft:abandoned_camp_flower_forest",
            "minecraft:abandoned_camp_forest",
            "minecraft:abandoned_camp_meadow",
            "minecraft:abandoned_camp_old_growth_birch_forest",
            "minecraft:abandoned_camp_old_growth_pine_taiga",
            "minecraft:abandoned_camp_old_growth_spruce_taiga",
            "minecraft:abandoned_camp_pale_garden",
            "minecraft:abandoned_camp_savanna",
            "minecraft:abandoned_camp_snowy_taiga",
            "minecraft:abandoned_camp_sparse_jungle",
            "minecraft:abandoned_camp_swamp",
            "minecraft:abandoned_camp_taiga",
            "minecraft:abandoned_camp_windswept_forest",
            "minecraft:abandoned_camp_wooded_badlands"), Dimension.OVERWORLD, false, false, false);

    private static final LootSite SHIPWRECK = new LootSite("shipwreck",
            List.of("minecraft:shipwreck", "minecraft:shipwreck_beached",
                    "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm"), Dimension.OVERWORLD, false, false, false);

    /** Buried-treasure-only loot (the chest under the beach). */
    private static final LootSite BURIED_TREASURE = new LootSite("buried_treasure",
            List.of("minecraft:buried_treasure"), Dimension.OVERWORLD, false, false, false);

    /** Horse armour is chest loot only — there is no recipe for iron/gold/diamond/copper armour. */
    private static final LootSite HORSE_ARMOR = new LootSite("horse_armor", List.of(
            "minecraft:desert_pyramid", "minecraft:jungle_pyramid", "minecraft:stronghold",
            "minecraft:mansion", "minecraft:ancient_city"), Dimension.OVERWORLD, false, false, false);

    /** Item id → where to get it. Keyed by id so 26.2 (which lacks the 26.3 maps) simply never matches. */
    private static final Map<String, LootSite> TARGETS = new HashMap<>();

    static {
        // 26.2 nautilus family (also present in 26.3).
        put("copper_nautilus_armor", NAUTILUS);
        put("iron_nautilus_armor", NAUTILUS);
        put("golden_nautilus_armor", NAUTILUS);
        put("diamond_nautilus_armor", NAUTILUS);

        put("echo_shard", ANCIENT_CITY);
        put("disc_fragment_5", ANCIENT_CITY);
        put("goat_horn", PILLAGER_OUTPOST);
        put("trial_key", TRIAL_CHAMBERS);
        put("heart_of_the_sea", BURIED_TREASURE);

        // Horse armour: no recipe exists for the metal variants, only loot in pyramid/stronghold/
        // mansion/ancient-city chests (leather armour is the only craftable one).
        put("iron_horse_armor", HORSE_ARMOR);
        put("golden_horse_armor", HORSE_ARMOR);
        put("diamond_horse_armor", HORSE_ARMOR);
        put("copper_horse_armor", HORSE_ARMOR);

        put("music_disc_13", DUNGEON_DISCS);
        put("music_disc_cat", DUNGEON_DISCS);
        put("music_disc_otherside", DUNGEON_DISCS);
        put("music_disc_bounce", MINESHAFT);

        // Ominous-vault loot: reachable only through an ominous trial key (OminousKeyTask).
        put("heavy_core", OMINOUS_VAULTS);
        put("flow_banner_pattern", OMINOUS_VAULTS);
        put("music_disc_creator", OMINOUS_VAULTS);

        // Vault loot: the vault ejects one of these and the ResourceTask picks it up.
        put("trident", TRIAL_VAULTS);
        put("guster_banner_pattern", TRIAL_VAULTS);
        put("music_disc_precipice", TRIAL_VAULTS);
        put("ominous_bottle", TRIAL_VAULTS);
        // Decorated pots in trial-chamber corridors. The three trial-chamber sherds are not in any
        // archaeology or chest loot table: they are the pot patterns themselves, so breaking pots is
        // the only survival source.
        put("music_disc_creator_music_box", TRIAL_POTS);
        put("flow_pottery_sherd", TRIAL_POTS);
        put("guster_pottery_sherd", TRIAL_POTS);
        put("scrape_pottery_sherd", TRIAL_POTS);

        // Nether bastion chests.
        put("piglin_banner_pattern", BASTION);
        put("music_disc_pigstep", BASTION);

        // 26.3 structure maps. The camp's common chest holds the camp map, the secret chest the
        // "buried …" maps; buried_treasure_map is shipwreck/ocean-ruin loot instead.
        put("abandoned_camp_map", ABANDONED_CAMP);
        put("buried_ancient_city_map", ABANDONED_CAMP);
        put("buried_mineshaft_map", ABANDONED_CAMP);
        put("desert_pyramid_map", ABANDONED_CAMP);
        put("jungle_pyramid_map", ABANDONED_CAMP);
        put("warm_ocean_ruins_map", ABANDONED_CAMP);
        put("woodland_mansion_map", ABANDONED_CAMP);
        put("buried_trial_chambers_map", ABANDONED_CAMP);
        put("buried_treasure_map", SHIPWRECK);
    }

    private LootSearchChains() {
    }

    private static void put(String path, LootSite site) {
        TARGETS.put("minecraft:" + path, site);
    }

    public static ResourceTask getTask(Item item, int count) {
        if (item == null || TARGETS.isEmpty()) return null;
        if (!StructureLocator.isAvailable()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
        if ("minecraft:ominous_trial_key".equals(id.toString())) {
            // The ominous key is not in a container: it is ejected by an ominous trial spawner.
            return new OminousKeyTask(count);
        }
        LootSite site = TARGETS.get(id.toString());
        if (site == null) return null;
        return new LootSearchTask(item, count, site.name(), site.structures(), site.dimension(),
                site.breakPots(), site.openVaults(), site.ominousVaults());
    }
}
