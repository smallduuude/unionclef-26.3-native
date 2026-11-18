package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.BlockTagVer;
import adris.altoclef.trackers.threats.WeaponThreat;
import adris.altoclef.util.slots.Slot;
import kaptainwutax.tungsten.path.movements.Input;
import adris.altoclef.multiversion.entity.PlayerVer;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.multiversion.versionedfields.Items;
import adris.altoclef.util.WoodType;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.material.MapColor;
import java.util.*;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Helper functions and definitions for useful groupings of items
 */
public class ItemHelper {
    public static final Item[] SAPLINGS = new Item[]{Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.MANGROVE_PROPAGULE,
            Items.CHERRY_SAPLING};
    public static final Block[] SAPLING_SOURCES = new Block[]{Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES,
            Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.MANGROVE_PROPAGULE, Blocks.CHERRY_LEAVES};
    public static final Item[] HOSTILE_MOB_DROPS = new Item[]{Items.BLAZE_ROD, Items.FEATHER, Items.CHICKEN,
            Items.COOKED_CHICKEN, Items.ROTTEN_FLESH, Items.ZOMBIE_HEAD, Items.GUNPOWDER, Items.CREEPER_HEAD,
            Items.TOTEM_OF_UNDYING, Items.EMERALD, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.LEATHER,
            Items.MAGMA_CREAM, Items.PHANTOM_MEMBRANE, Items.ARROW, Items.SADDLE, Items.SHULKER_SHELL, Items.BONE,
            Items.SKELETON_SKULL, Items.SLIME_BALL, Items.STRING, Items.SPIDER_EYE, Items.SCULK_CATALYST,
            Items.GLASS_BOTTLE, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.STICK, Items.SUGAR, Items.POTION,
            Items.NETHER_STAR, Items.COAL, Items.WITHER_SKELETON_SKULL, Items.GHAST_TEAR, Items.IRON_INGOT,
            Items.CARROT, Items.POTATO, Items.BAKED_POTATO, Items.COPPER_INGOT};
    public static final Item[] DIRTS = new Item[]{Items.DIRT, Items.DIRT_PATH, Items.COARSE_DIRT, Items.ROOTED_DIRT};
    public static final Item[] COPPER_BLOCKS = new Item[]{Items.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.UNAFFECTED), Items.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.EXPOSED),
            Items.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.WEATHERED), Items.COPPER_BLOCK.weathering().pick(WeatheringCopper.WeatherState.OXIDIZED), Items.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.UNAFFECTED), Items.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.EXPOSED),
            Items.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.WEATHERED), Items.CUT_COPPER.weathering().pick(WeatheringCopper.WeatherState.OXIDIZED), Items.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.UNAFFECTED),
            Items.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.EXPOSED), Items.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.WEATHERED), Items.COPPER_BLOCK.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED),
            Items.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.UNAFFECTED), Items.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.EXPOSED), Items.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.WEATHERED),
            Items.CUT_COPPER.waxed().pick(WeatheringCopper.WeatherState.OXIDIZED)};
    public static final Item[] PLANKS = new Item[]{Items.ACACIA_PLANKS, Items.BIRCH_PLANKS, Items.CRIMSON_PLANKS,
            Items.DARK_OAK_PLANKS, Items.OAK_PLANKS, Items.JUNGLE_PLANKS, Items.SPRUCE_PLANKS, Items.WARPED_PLANKS,
            Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS, Items.BAMBOO_PLANKS, Items.PALE_OAK_PLANKS};
    public static final Item[] LEAVES = new Item[]{Items.ACACIA_LEAVES, Items.BIRCH_LEAVES, Items.DARK_OAK_LEAVES,
            Items.OAK_LEAVES, Items.JUNGLE_LEAVES, Items.SPRUCE_LEAVES, Items.MANGROVE_LEAVES, Items.CHERRY_LEAVES,
            Items.PALE_OAK_LEAVES};
    public static final Item[] WOOD = new Item[]{Items.ACACIA_WOOD, Items.BIRCH_WOOD, Items.CRIMSON_HYPHAE,
            Items.DARK_OAK_WOOD, Items.OAK_WOOD, Items.JUNGLE_WOOD, Items.SPRUCE_WOOD, Items.WARPED_HYPHAE,
            Items.MANGROVE_WOOD, Items.PALE_OAK_WOOD};
    public static final Item[] WOOD_BUTTON = new Item[]{Items.ACACIA_BUTTON, Items.BIRCH_BUTTON, Items.CRIMSON_BUTTON,
            Items.DARK_OAK_BUTTON, Items.OAK_BUTTON, Items.JUNGLE_BUTTON, Items.SPRUCE_BUTTON, Items.WARPED_BUTTON,
            Items.MANGROVE_BUTTON, Items.BAMBOO_BUTTON, Items.CHERRY_BUTTON, Items.PALE_OAK_BUTTON};
    public static final Item[] WOOD_SIGN = new Item[]{Items.ACACIA_SIGN, Items.BIRCH_SIGN, Items.CRIMSON_SIGN,
            Items.DARK_OAK_SIGN, Items.OAK_SIGN, Items.JUNGLE_SIGN, Items.SPRUCE_SIGN, Items.WARPED_SIGN,
            Items.MANGROVE_SIGN, Items.BAMBOO_SIGN, Items.CHERRY_SIGN, Items.PALE_OAK_SIGN};
    public static final Item[] WOOD_HANGING_SIGN = new Item[]{Items.ACACIA_HANGING_SIGN, Items.BIRCH_HANGING_SIGN,
            Items.CRIMSON_HANGING_SIGN, Items.DARK_OAK_HANGING_SIGN, Items.OAK_HANGING_SIGN, Items.JUNGLE_HANGING_SIGN,
            Items.SPRUCE_HANGING_SIGN, Items.WARPED_HANGING_SIGN, Items.MANGROVE_HANGING_SIGN, Items.BAMBOO_HANGING_SIGN,
            Items.CHERRY_HANGING_SIGN, Items.PALE_OAK_HANGING_SIGN};
    public static final Item[] WOOD_PRESSURE_PLATE = new Item[]{Items.ACACIA_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE,
            Items.CRIMSON_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE, Items.OAK_PRESSURE_PLATE,
            Items.JUNGLE_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE,
            Items.MANGROVE_PRESSURE_PLATE, Items.BAMBOO_PRESSURE_PLATE, Items.CHERRY_PRESSURE_PLATE,
            Items.PALE_OAK_PRESSURE_PLATE};
    public static final Item[] WOOD_FENCE = new Item[]{Items.ACACIA_FENCE, Items.BIRCH_FENCE, Items.DARK_OAK_FENCE,
            Items.OAK_FENCE, Items.JUNGLE_FENCE, Items.SPRUCE_FENCE, Items.CRIMSON_FENCE, Items.WARPED_FENCE,
            Items.MANGROVE_FENCE, Items.BAMBOO_FENCE, Items.CHERRY_FENCE, Items.PALE_OAK_FENCE};
    public static final Item[] WOOD_FENCE_GATE = new Item[]{Items.ACACIA_FENCE_GATE, Items.BIRCH_FENCE_GATE,
            Items.DARK_OAK_FENCE_GATE, Items.OAK_FENCE_GATE, Items.JUNGLE_FENCE_GATE, Items.SPRUCE_FENCE_GATE,
            Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE, Items.MANGROVE_FENCE_GATE, Items.BAMBOO_FENCE_GATE,
            Items.CHERRY_FENCE_GATE, Items.PALE_OAK_FENCE_GATE};
    public static final Item[] WOOD_BOAT = new Item[]{Items.ACACIA_BOAT, Items.BIRCH_BOAT, Items.DARK_OAK_BOAT,
            Items.OAK_BOAT, Items.JUNGLE_BOAT, Items.SPRUCE_BOAT, Items.MANGROVE_BOAT, Items.CHERRY_BOAT,
            Items.PALE_OAK_BOAT};
    public static final Item[] WOOD_DOOR = new Item[]{Items.ACACIA_DOOR, Items.BIRCH_DOOR, Items.CRIMSON_DOOR,
            Items.DARK_OAK_DOOR, Items.OAK_DOOR, Items.JUNGLE_DOOR, Items.SPRUCE_DOOR, Items.WARPED_DOOR,
            Items.MANGROVE_DOOR, Items.BAMBOO_DOOR, Items.CHERRY_DOOR, Items.PALE_OAK_DOOR};
    public static final Item[] WOOD_SLAB = new Item[]{Items.ACACIA_SLAB, Items.BIRCH_SLAB, Items.CRIMSON_SLAB,
            Items.DARK_OAK_SLAB, Items.OAK_SLAB, Items.JUNGLE_SLAB, Items.SPRUCE_SLAB, Items.WARPED_SLAB,
            Items.MANGROVE_SLAB, Items.BAMBOO_SLAB, Items.CHERRY_SLAB, Items.PALE_OAK_SLAB};
    public static final Item[] WOOD_STAIRS = new Item[]{Items.ACACIA_STAIRS, Items.BIRCH_STAIRS, Items.CRIMSON_STAIRS,
            Items.DARK_OAK_STAIRS, Items.OAK_STAIRS, Items.JUNGLE_STAIRS, Items.SPRUCE_STAIRS, Items.WARPED_STAIRS,
            Items.MANGROVE_STAIRS, Items.BAMBOO_STAIRS, Items.CHERRY_STAIRS, Items.PALE_OAK_STAIRS};
    public static final Item[] WOOD_TRAPDOOR = new Item[]{Items.ACACIA_TRAPDOOR, Items.BIRCH_TRAPDOOR,
            Items.CRIMSON_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.OAK_TRAPDOOR, Items.JUNGLE_TRAPDOOR,
            Items.SPRUCE_TRAPDOOR, Items.WARPED_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.BAMBOO_TRAPDOOR,
            Items.CHERRY_TRAPDOOR, Items.PALE_OAK_TRAPDOOR};
    public static final Item[] LOG = new Item[]{Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG, Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG,
            Items.ACACIA_WOOD, Items.BIRCH_WOOD, Items.DARK_OAK_WOOD, Items.OAK_WOOD, Items.JUNGLE_WOOD, Items.SPRUCE_WOOD,
            Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_DARK_OAK_WOOD, Items.STRIPPED_OAK_WOOD, Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_SPRUCE_WOOD,
            Items.CRIMSON_STEM, Items.WARPED_STEM, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
            Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_CRIMSON_HYPHAE,
            Items.STRIPPED_WARPED_HYPHAE, Items.MANGROVE_LOG, Items.MANGROVE_WOOD, Items.STRIPPED_MANGROVE_LOG,
            Items.STRIPPED_MANGROVE_WOOD, Items.CHERRY_LOG, Items.CHERRY_WOOD, Items.STRIPPED_CHERRY_LOG,
            Items.STRIPPED_CHERRY_WOOD, Items.PALE_OAK_LOG, Items.PALE_OAK_WOOD, Items.STRIPPED_PALE_OAK_LOG,
            Items.STRIPPED_PALE_OAK_WOOD};
    public static final Item[] STRIPPED_LOGS = new Item[]{Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_BIRCH_LOG,
            Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_MANGROVE_LOG,
            Items.STRIPPED_CHERRY_LOG, Items.STRIPPED_PALE_OAK_LOG};
    public static final Item[] STRIPPABLE_LOGS = new Item[]{Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG,
            Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG, Items.CRIMSON_STEM, Items.WARPED_STEM, Items.MANGROVE_LOG,
            Items.CHERRY_LOG, Items.PALE_OAK_LOG};
    public static final Item[] DYE = new Item[]{Items.DYE.pick(DyeColor.WHITE), Items.DYE.pick(DyeColor.BLACK), Items.DYE.pick(DyeColor.BLUE), Items.DYE.pick(DyeColor.BROWN), Items.DYE.pick(DyeColor.CYAN), Items.DYE.pick(DyeColor.GRAY), Items.DYE.pick(DyeColor.GREEN), Items.DYE.pick(DyeColor.LIGHT_BLUE), Items.DYE.pick(DyeColor.LIGHT_GRAY), Items.DYE.pick(DyeColor.LIME), Items.DYE.pick(DyeColor.MAGENTA), Items.DYE.pick(DyeColor.ORANGE), Items.DYE.pick(DyeColor.PINK), Items.DYE.pick(DyeColor.PURPLE), Items.DYE.pick(DyeColor.RED), Items.DYE.pick(DyeColor.YELLOW)};
    public static final Item[] WOOL = new Item[]{Items.WOOL.pick(DyeColor.WHITE), Items.WOOL.pick(DyeColor.BLACK), Items.WOOL.pick(DyeColor.BLUE), Items.WOOL.pick(DyeColor.BROWN), Items.WOOL.pick(DyeColor.CYAN), Items.WOOL.pick(DyeColor.GRAY), Items.WOOL.pick(DyeColor.GREEN), Items.WOOL.pick(DyeColor.LIGHT_BLUE), Items.WOOL.pick(DyeColor.LIGHT_GRAY), Items.WOOL.pick(DyeColor.LIME), Items.WOOL.pick(DyeColor.MAGENTA), Items.WOOL.pick(DyeColor.ORANGE), Items.WOOL.pick(DyeColor.PINK), Items.WOOL.pick(DyeColor.PURPLE), Items.WOOL.pick(DyeColor.RED), Items.WOOL.pick(DyeColor.YELLOW)};
    public static final Item[] BED = new Item[]{Items.BED.pick(DyeColor.WHITE), Items.BED.pick(DyeColor.BLACK), Items.BED.pick(DyeColor.BLUE), Items.BED.pick(DyeColor.BROWN), Items.BED.pick(DyeColor.CYAN), Items.BED.pick(DyeColor.GRAY), Items.BED.pick(DyeColor.GREEN), Items.BED.pick(DyeColor.LIGHT_BLUE), Items.BED.pick(DyeColor.LIGHT_GRAY), Items.BED.pick(DyeColor.LIME), Items.BED.pick(DyeColor.MAGENTA), Items.BED.pick(DyeColor.ORANGE), Items.BED.pick(DyeColor.PINK), Items.BED.pick(DyeColor.PURPLE), Items.BED.pick(DyeColor.RED), Items.BED.pick(DyeColor.YELLOW)};
    public static final Item[] ARROWS = new Item[]{Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW};
    public static final Item[] ShootWeapons = new Item[]{Items.BOW, Items.CROSSBOW};
    public static final Item[] FunnyShiftItems = new Item[]{Items.BLAZE_ROD, Items.STICK};
    public static final Item[] CARPET = new Item[]{Items.CARPET.pick(DyeColor.WHITE), Items.CARPET.pick(DyeColor.BLACK), Items.CARPET.pick(DyeColor.BLUE), Items.CARPET.pick(DyeColor.BROWN), Items.CARPET.pick(DyeColor.CYAN), Items.CARPET.pick(DyeColor.GRAY), Items.CARPET.pick(DyeColor.GREEN), Items.CARPET.pick(DyeColor.LIGHT_BLUE), Items.CARPET.pick(DyeColor.LIGHT_GRAY), Items.CARPET.pick(DyeColor.LIME), Items.CARPET.pick(DyeColor.MAGENTA), Items.CARPET.pick(DyeColor.ORANGE), Items.CARPET.pick(DyeColor.PINK), Items.CARPET.pick(DyeColor.PURPLE), Items.CARPET.pick(DyeColor.RED), Items.CARPET.pick(DyeColor.YELLOW)};
    public static final Item[] SHULKER_BOXES = new Item[]{Items.DYED_SHULKER_BOX.pick(DyeColor.WHITE), Items.DYED_SHULKER_BOX.pick(DyeColor.BLACK), Items.DYED_SHULKER_BOX.pick(DyeColor.BLUE), Items.DYED_SHULKER_BOX.pick(DyeColor.BROWN), Items.DYED_SHULKER_BOX.pick(DyeColor.CYAN), Items.DYED_SHULKER_BOX.pick(DyeColor.GRAY), Items.DYED_SHULKER_BOX.pick(DyeColor.GREEN), Items.DYED_SHULKER_BOX.pick(DyeColor.LIGHT_BLUE), Items.DYED_SHULKER_BOX.pick(DyeColor.LIGHT_GRAY), Items.DYED_SHULKER_BOX.pick(DyeColor.LIME), Items.DYED_SHULKER_BOX.pick(DyeColor.MAGENTA), Items.DYED_SHULKER_BOX.pick(DyeColor.ORANGE), Items.DYED_SHULKER_BOX.pick(DyeColor.PINK), Items.DYED_SHULKER_BOX.pick(DyeColor.PURPLE), Items.DYED_SHULKER_BOX.pick(DyeColor.RED), Items.DYED_SHULKER_BOX.pick(DyeColor.YELLOW)};
    public static final Item[] FLOWER = new Item[]{Items.ALLIUM, Items.AZURE_BLUET, Items.BLUE_ORCHID, Items.CORNFLOWER, Items.DANDELION, Items.LILAC, Items.LILY_OF_THE_VALLEY, Items.ORANGE_TULIP, Items.OXEYE_DAISY, Items.PINK_TULIP, Items.POPPY, Items.PEONY, Items.RED_TULIP, Items.ROSE_BUSH, Items.SUNFLOWER, Items.WHITE_TULIP};
    public static final Item[] LEATHER_ARMORS = new Item[]{Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_HELMET, Items.LEATHER_BOOTS};
    public static final Item[] GOLDEN_ARMORS = new Item[]{Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_HELMET, Items.GOLDEN_BOOTS};
    public static final Item[] IRON_ARMORS = new Item[]{Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_HELMET, Items.IRON_BOOTS};
    public static final Item[] DIAMOND_ARMORS = new Item[]{Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET, Items.DIAMOND_BOOTS};
    public static final Item[] NETHERITE_ARMORS = new Item[]{Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_HELMET, Items.NETHERITE_BOOTS};
    public static final Item[] WOODEN_TOOLS = new Item[]{Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_HOE};
    public static final Item[] STONE_TOOLS = new Item[]{Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_HOE};
    public static final Item[] IRON_TOOLS = new Item[]{Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_HOE};
    public static final Item[] GOLDEN_TOOLS = new Item[]{Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_HOE};
    public static final Item[] DIAMOND_TOOLS = new Item[]{Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_HOE};
    public static final Item[] NETHERITE_TOOLS = new Item[]{Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_HOE};
    public static final Block[] WOOD_SIGNS_ALL = new Block[]{Blocks.ACACIA_SIGN, Blocks.BIRCH_SIGN, Blocks.DARK_OAK_SIGN,
            Blocks.OAK_SIGN, Blocks.JUNGLE_SIGN, Blocks.SPRUCE_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_WALL_SIGN,
            Blocks.DARK_OAK_WALL_SIGN, Blocks.OAK_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN,
            Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.BAMBOO_SIGN, Blocks.BAMBOO_WALL_SIGN,
            Blocks.CHERRY_SIGN, Blocks.CHERRY_WALL_SIGN};

    private static final Map<Item, Item> logToPlanks = new HashMap<>() {
        {
            put(Items.CHERRY_LOG, Items.CHERRY_PLANKS);
            put(Items.CHERRY_WOOD, Items.CHERRY_PLANKS);
            put(Items.STRIPPED_CHERRY_LOG, Items.CHERRY_PLANKS);
            put(Items.STRIPPED_CHERRY_WOOD, Items.CHERRY_PLANKS);
            put(Items.MANGROVE_LOG, Items.MANGROVE_PLANKS);
            put(Items.MANGROVE_WOOD, Items.MANGROVE_PLANKS);
            put(Items.STRIPPED_MANGROVE_LOG, Items.MANGROVE_PLANKS);
            put(Items.STRIPPED_MANGROVE_WOOD, Items.MANGROVE_PLANKS);
            put(Items.ACACIA_LOG, Items.ACACIA_PLANKS);
            put(Items.BIRCH_LOG, Items.BIRCH_PLANKS);
            put(Items.CRIMSON_STEM, Items.CRIMSON_PLANKS);
            put(Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS);
            put(Items.OAK_LOG, Items.OAK_PLANKS);
            put(Items.JUNGLE_LOG, Items.JUNGLE_PLANKS);
            put(Items.SPRUCE_LOG, Items.SPRUCE_PLANKS);
            put(Items.WARPED_STEM, Items.WARPED_PLANKS);
            put(Items.STRIPPED_ACACIA_LOG, Items.ACACIA_PLANKS);
            put(Items.STRIPPED_BIRCH_LOG, Items.BIRCH_PLANKS);
            put(Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_PLANKS);
            put(Items.STRIPPED_DARK_OAK_LOG, Items.DARK_OAK_PLANKS);
            put(Items.STRIPPED_OAK_LOG, Items.OAK_PLANKS);
            put(Items.STRIPPED_JUNGLE_LOG, Items.JUNGLE_PLANKS);
            put(Items.STRIPPED_SPRUCE_LOG, Items.SPRUCE_PLANKS);
            put(Items.STRIPPED_WARPED_STEM, Items.WARPED_PLANKS);
            put(Items.ACACIA_WOOD, Items.ACACIA_PLANKS);
            put(Items.BIRCH_WOOD, Items.BIRCH_PLANKS);
            put(Items.CRIMSON_HYPHAE, Items.CRIMSON_PLANKS);
            put(Items.DARK_OAK_WOOD, Items.DARK_OAK_PLANKS);
            put(Items.OAK_WOOD, Items.OAK_PLANKS);
            put(Items.JUNGLE_WOOD, Items.JUNGLE_PLANKS);
            put(Items.SPRUCE_WOOD, Items.SPRUCE_PLANKS);
            put(Items.WARPED_HYPHAE, Items.WARPED_PLANKS);
            put(Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_PLANKS);
            put(Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_PLANKS);
            put(Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_PLANKS);
            put(Items.STRIPPED_DARK_OAK_WOOD, Items.DARK_OAK_PLANKS);
            put(Items.STRIPPED_OAK_WOOD, Items.OAK_PLANKS);
            put(Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_PLANKS);
            put(Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_PLANKS);
            put(Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_PLANKS);
        }
    };
    private static final Map<Item, Item> planksToLogs = new HashMap<>() {
        {
            put(Items.CHERRY_PLANKS, Items.CHERRY_LOG);
            put(Items.MANGROVE_PLANKS, Items.MANGROVE_LOG);
            put(Items.ACACIA_PLANKS, Items.ACACIA_LOG);
            put(Items.BIRCH_PLANKS, Items.BIRCH_LOG);
            put(Items.CRIMSON_PLANKS, Items.CRIMSON_STEM);
            put(Items.DARK_OAK_PLANKS, Items.DARK_OAK_LOG);
            put(Items.OAK_PLANKS, Items.OAK_LOG);
            put(Items.JUNGLE_PLANKS, Items.JUNGLE_LOG);
            put(Items.SPRUCE_PLANKS, Items.SPRUCE_LOG);
            put(Items.WARPED_PLANKS, Items.WARPED_STEM);
        }
    };
    private static final Map<Item, Item> strippedToLogs = new HashMap<>() {
        {
            put(Items.STRIPPED_CHERRY_LOG, Items.CHERRY_LOG);
            put(Items.STRIPPED_MANGROVE_LOG, Items.MANGROVE_LOG);
            put(Items.STRIPPED_ACACIA_LOG, Items.ACACIA_LOG);
            put(Items.STRIPPED_BIRCH_LOG, Items.BIRCH_LOG);
            put(Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_STEM);
            put(Items.STRIPPED_DARK_OAK_LOG, Items.DARK_OAK_LOG);
            put(Items.STRIPPED_OAK_LOG, Items.OAK_LOG);
            put(Items.STRIPPED_JUNGLE_LOG, Items.JUNGLE_LOG);
            put(Items.STRIPPED_SPRUCE_LOG, Items.SPRUCE_LOG);
            put(Items.STRIPPED_WARPED_STEM, Items.WARPED_STEM);
            put(Items.STRIPPED_PALE_OAK_LOG, Items.PALE_OAK_LOG);
        }
    };
    // This is kinda jank ngl
    private static final Map<MapColor, ColorfulItems> colorMap = new HashMap<MapColor, ColorfulItems>() {
        {
            p(DyeColor.RED, "red", Items.DYE.pick(DyeColor.RED), Items.WOOL.pick(DyeColor.RED), Items.BED.pick(DyeColor.RED), Items.CARPET.pick(DyeColor.RED), Items.STAINED_GLASS.pick(DyeColor.RED), Items.STAINED_GLASS_PANE.pick(DyeColor.RED), Items.DYED_TERRACOTTA.pick(DyeColor.RED), Items.GLAZED_TERRACOTTA.pick(DyeColor.RED), Items.CONCRETE.pick(DyeColor.RED), Items.CONCRETE_POWDER.pick(DyeColor.RED), Items.BANNER.pick(DyeColor.RED), Items.DYED_SHULKER_BOX.pick(DyeColor.RED), Blocks.WALL_BANNER.pick(DyeColor.RED));
            p(DyeColor.WHITE, "white", Items.DYE.pick(DyeColor.WHITE), Items.WOOL.pick(DyeColor.WHITE), Items.BED.pick(DyeColor.WHITE), Items.CARPET.pick(DyeColor.WHITE), Items.STAINED_GLASS.pick(DyeColor.WHITE), Items.STAINED_GLASS_PANE.pick(DyeColor.WHITE), Items.DYED_TERRACOTTA.pick(DyeColor.WHITE), Items.GLAZED_TERRACOTTA.pick(DyeColor.WHITE), Items.CONCRETE.pick(DyeColor.WHITE), Items.CONCRETE_POWDER.pick(DyeColor.WHITE), Items.BANNER.pick(DyeColor.WHITE), Items.DYED_SHULKER_BOX.pick(DyeColor.WHITE), Blocks.WALL_BANNER.pick(DyeColor.WHITE));
            p(DyeColor.BLACK, "black", Items.DYE.pick(DyeColor.BLACK), Items.WOOL.pick(DyeColor.BLACK), Items.BED.pick(DyeColor.BLACK), Items.CARPET.pick(DyeColor.BLACK), Items.STAINED_GLASS.pick(DyeColor.BLACK), Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK), Items.DYED_TERRACOTTA.pick(DyeColor.BLACK), Items.GLAZED_TERRACOTTA.pick(DyeColor.BLACK), Items.CONCRETE.pick(DyeColor.BLACK), Items.CONCRETE_POWDER.pick(DyeColor.BLACK), Items.BANNER.pick(DyeColor.BLACK), Items.DYED_SHULKER_BOX.pick(DyeColor.BLACK), Blocks.WALL_BANNER.pick(DyeColor.BLACK));
            p(DyeColor.BLUE, "blue", Items.DYE.pick(DyeColor.BLUE), Items.WOOL.pick(DyeColor.BLUE), Items.BED.pick(DyeColor.BLUE), Items.CARPET.pick(DyeColor.BLUE), Items.STAINED_GLASS.pick(DyeColor.BLUE), Items.STAINED_GLASS_PANE.pick(DyeColor.BLUE), Items.DYED_TERRACOTTA.pick(DyeColor.BLUE), Items.GLAZED_TERRACOTTA.pick(DyeColor.BLUE), Items.CONCRETE.pick(DyeColor.BLUE), Items.CONCRETE_POWDER.pick(DyeColor.BLUE), Items.BANNER.pick(DyeColor.BLUE), Items.DYED_SHULKER_BOX.pick(DyeColor.BLUE), Blocks.WALL_BANNER.pick(DyeColor.BLUE));
            p(DyeColor.BROWN, "brown", Items.DYE.pick(DyeColor.BROWN), Items.WOOL.pick(DyeColor.BROWN), Items.BED.pick(DyeColor.BROWN), Items.CARPET.pick(DyeColor.BROWN), Items.STAINED_GLASS.pick(DyeColor.BROWN), Items.STAINED_GLASS_PANE.pick(DyeColor.BROWN), Items.DYED_TERRACOTTA.pick(DyeColor.BROWN), Items.GLAZED_TERRACOTTA.pick(DyeColor.BROWN), Items.CONCRETE.pick(DyeColor.BROWN), Items.CONCRETE_POWDER.pick(DyeColor.BROWN), Items.BANNER.pick(DyeColor.BROWN), Items.DYED_SHULKER_BOX.pick(DyeColor.BROWN), Blocks.WALL_BANNER.pick(DyeColor.BROWN));
            p(DyeColor.CYAN, "cyan", Items.DYE.pick(DyeColor.CYAN), Items.WOOL.pick(DyeColor.CYAN), Items.BED.pick(DyeColor.CYAN), Items.CARPET.pick(DyeColor.CYAN), Items.STAINED_GLASS.pick(DyeColor.CYAN), Items.STAINED_GLASS_PANE.pick(DyeColor.CYAN), Items.DYED_TERRACOTTA.pick(DyeColor.CYAN), Items.GLAZED_TERRACOTTA.pick(DyeColor.CYAN), Items.CONCRETE.pick(DyeColor.CYAN), Items.CONCRETE_POWDER.pick(DyeColor.CYAN), Items.BANNER.pick(DyeColor.CYAN), Items.DYED_SHULKER_BOX.pick(DyeColor.CYAN), Blocks.WALL_BANNER.pick(DyeColor.CYAN));
            p(DyeColor.GRAY, "gray", Items.DYE.pick(DyeColor.GRAY), Items.WOOL.pick(DyeColor.GRAY), Items.BED.pick(DyeColor.GRAY), Items.CARPET.pick(DyeColor.GRAY), Items.STAINED_GLASS.pick(DyeColor.GRAY), Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY), Items.DYED_TERRACOTTA.pick(DyeColor.GRAY), Items.GLAZED_TERRACOTTA.pick(DyeColor.GRAY), Items.CONCRETE.pick(DyeColor.GRAY), Items.CONCRETE_POWDER.pick(DyeColor.GRAY), Items.BANNER.pick(DyeColor.GRAY), Items.DYED_SHULKER_BOX.pick(DyeColor.GRAY), Blocks.WALL_BANNER.pick(DyeColor.GRAY));
            p(DyeColor.GREEN, "green", Items.DYE.pick(DyeColor.GREEN), Items.WOOL.pick(DyeColor.GREEN), Items.BED.pick(DyeColor.GREEN), Items.CARPET.pick(DyeColor.GREEN), Items.STAINED_GLASS.pick(DyeColor.GREEN), Items.STAINED_GLASS_PANE.pick(DyeColor.GREEN), Items.DYED_TERRACOTTA.pick(DyeColor.GREEN), Items.GLAZED_TERRACOTTA.pick(DyeColor.GREEN), Items.CONCRETE.pick(DyeColor.GREEN), Items.CONCRETE_POWDER.pick(DyeColor.GREEN), Items.BANNER.pick(DyeColor.GREEN), Items.DYED_SHULKER_BOX.pick(DyeColor.GREEN), Blocks.WALL_BANNER.pick(DyeColor.GREEN));
            p(DyeColor.LIGHT_BLUE, "light_blue", Items.DYE.pick(DyeColor.LIGHT_BLUE), Items.WOOL.pick(DyeColor.LIGHT_BLUE), Items.BED.pick(DyeColor.LIGHT_BLUE), Items.CARPET.pick(DyeColor.LIGHT_BLUE), Items.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE), Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_BLUE), Items.DYED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE), Items.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE), Items.CONCRETE.pick(DyeColor.LIGHT_BLUE), Items.CONCRETE_POWDER.pick(DyeColor.LIGHT_BLUE), Items.BANNER.pick(DyeColor.LIGHT_BLUE), Items.DYED_SHULKER_BOX.pick(DyeColor.LIGHT_BLUE), Blocks.WALL_BANNER.pick(DyeColor.LIGHT_BLUE));
            p(DyeColor.LIGHT_GRAY, "light_gray", Items.DYE.pick(DyeColor.LIGHT_GRAY), Items.WOOL.pick(DyeColor.LIGHT_GRAY), Items.BED.pick(DyeColor.LIGHT_GRAY), Items.CARPET.pick(DyeColor.LIGHT_GRAY), Items.STAINED_GLASS.pick(DyeColor.LIGHT_GRAY), Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_GRAY), Items.DYED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY), Items.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY), Items.CONCRETE.pick(DyeColor.LIGHT_GRAY), Items.CONCRETE_POWDER.pick(DyeColor.LIGHT_GRAY), Items.BANNER.pick(DyeColor.LIGHT_GRAY), Items.DYED_SHULKER_BOX.pick(DyeColor.LIGHT_GRAY), Blocks.WALL_BANNER.pick(DyeColor.LIGHT_GRAY));
            p(DyeColor.LIME, "lime", Items.DYE.pick(DyeColor.LIME), Items.WOOL.pick(DyeColor.LIME), Items.BED.pick(DyeColor.LIME), Items.CARPET.pick(DyeColor.LIME), Items.STAINED_GLASS.pick(DyeColor.LIME), Items.STAINED_GLASS_PANE.pick(DyeColor.LIME), Items.DYED_TERRACOTTA.pick(DyeColor.LIME), Items.GLAZED_TERRACOTTA.pick(DyeColor.LIME), Items.CONCRETE.pick(DyeColor.LIME), Items.CONCRETE_POWDER.pick(DyeColor.LIME), Items.BANNER.pick(DyeColor.LIME), Items.DYED_SHULKER_BOX.pick(DyeColor.LIME), Blocks.WALL_BANNER.pick(DyeColor.LIME));
            p(DyeColor.MAGENTA, "magenta", Items.DYE.pick(DyeColor.MAGENTA), Items.WOOL.pick(DyeColor.MAGENTA), Items.BED.pick(DyeColor.MAGENTA), Items.CARPET.pick(DyeColor.MAGENTA), Items.STAINED_GLASS.pick(DyeColor.MAGENTA), Items.STAINED_GLASS_PANE.pick(DyeColor.MAGENTA), Items.DYED_TERRACOTTA.pick(DyeColor.MAGENTA), Items.GLAZED_TERRACOTTA.pick(DyeColor.MAGENTA), Items.CONCRETE.pick(DyeColor.MAGENTA), Items.CONCRETE_POWDER.pick(DyeColor.MAGENTA), Items.BANNER.pick(DyeColor.MAGENTA), Items.DYED_SHULKER_BOX.pick(DyeColor.MAGENTA), Blocks.WALL_BANNER.pick(DyeColor.MAGENTA));
            p(DyeColor.ORANGE, "orange", Items.DYE.pick(DyeColor.ORANGE), Items.WOOL.pick(DyeColor.ORANGE), Items.BED.pick(DyeColor.ORANGE), Items.CARPET.pick(DyeColor.ORANGE), Items.STAINED_GLASS.pick(DyeColor.ORANGE), Items.STAINED_GLASS_PANE.pick(DyeColor.ORANGE), Items.DYED_TERRACOTTA.pick(DyeColor.ORANGE), Items.GLAZED_TERRACOTTA.pick(DyeColor.ORANGE), Items.CONCRETE.pick(DyeColor.ORANGE), Items.CONCRETE_POWDER.pick(DyeColor.ORANGE), Items.BANNER.pick(DyeColor.ORANGE), Items.DYED_SHULKER_BOX.pick(DyeColor.ORANGE), Blocks.WALL_BANNER.pick(DyeColor.ORANGE));
            p(DyeColor.PINK, "pink", Items.DYE.pick(DyeColor.PINK), Items.WOOL.pick(DyeColor.PINK), Items.BED.pick(DyeColor.PINK), Items.CARPET.pick(DyeColor.PINK), Items.STAINED_GLASS.pick(DyeColor.PINK), Items.STAINED_GLASS_PANE.pick(DyeColor.PINK), Items.DYED_TERRACOTTA.pick(DyeColor.PINK), Items.GLAZED_TERRACOTTA.pick(DyeColor.PINK), Items.CONCRETE.pick(DyeColor.PINK), Items.CONCRETE_POWDER.pick(DyeColor.PINK), Items.BANNER.pick(DyeColor.PINK), Items.DYED_SHULKER_BOX.pick(DyeColor.PINK), Blocks.WALL_BANNER.pick(DyeColor.PINK));
            p(DyeColor.PURPLE, "purple", Items.DYE.pick(DyeColor.PURPLE), Items.WOOL.pick(DyeColor.PURPLE), Items.BED.pick(DyeColor.PURPLE), Items.CARPET.pick(DyeColor.PURPLE), Items.STAINED_GLASS.pick(DyeColor.PURPLE), Items.STAINED_GLASS_PANE.pick(DyeColor.PURPLE), Items.DYED_TERRACOTTA.pick(DyeColor.PURPLE), Items.GLAZED_TERRACOTTA.pick(DyeColor.PURPLE), Items.CONCRETE.pick(DyeColor.PURPLE), Items.CONCRETE_POWDER.pick(DyeColor.PURPLE), Items.BANNER.pick(DyeColor.PURPLE), Items.DYED_SHULKER_BOX.pick(DyeColor.PURPLE), Blocks.WALL_BANNER.pick(DyeColor.PURPLE));
            p(DyeColor.RED, "red", Items.DYE.pick(DyeColor.RED), Items.WOOL.pick(DyeColor.RED), Items.BED.pick(DyeColor.RED), Items.CARPET.pick(DyeColor.RED), Items.STAINED_GLASS.pick(DyeColor.RED), Items.STAINED_GLASS_PANE.pick(DyeColor.RED), Items.DYED_TERRACOTTA.pick(DyeColor.RED), Items.GLAZED_TERRACOTTA.pick(DyeColor.RED), Items.CONCRETE.pick(DyeColor.RED), Items.CONCRETE_POWDER.pick(DyeColor.RED), Items.BANNER.pick(DyeColor.RED), Items.DYED_SHULKER_BOX.pick(DyeColor.RED), Blocks.WALL_BANNER.pick(DyeColor.RED));
            p(DyeColor.YELLOW, "yellow", Items.DYE.pick(DyeColor.YELLOW), Items.WOOL.pick(DyeColor.YELLOW), Items.BED.pick(DyeColor.YELLOW), Items.CARPET.pick(DyeColor.YELLOW), Items.STAINED_GLASS.pick(DyeColor.YELLOW), Items.STAINED_GLASS_PANE.pick(DyeColor.YELLOW), Items.DYED_TERRACOTTA.pick(DyeColor.YELLOW), Items.GLAZED_TERRACOTTA.pick(DyeColor.YELLOW), Items.CONCRETE.pick(DyeColor.YELLOW), Items.CONCRETE_POWDER.pick(DyeColor.YELLOW), Items.BANNER.pick(DyeColor.YELLOW), Items.DYED_SHULKER_BOX.pick(DyeColor.YELLOW), Blocks.WALL_BANNER.pick(DyeColor.YELLOW));
        }

        void p(DyeColor color, String colorName, Item dye, Item wool, Item bed, Item carpet, Item stainedGlass, Item stainedGlassPane, Item terracotta, Item glazedTerracotta, Item concrete, Item concretePowder, Item banner, Item shulker, Block wallBanner) {
            put(color.getMapColor(), new ColorfulItems(color, colorName, dye, wool, bed, carpet, stainedGlass, stainedGlassPane, terracotta, glazedTerracotta, concrete, concretePowder, banner, shulker, wallBanner));
        }
    };
    private static final Map<WoodType, WoodItems> woodMap = new HashMap<WoodType, WoodItems>() {
        {
            p(WoodType.CHERRY, "cherry", Items.CHERRY_PLANKS, Items.CHERRY_LOG, Items.STRIPPED_CHERRY_LOG, Items.STRIPPED_CHERRY_WOOD, Items.CHERRY_WOOD, Items.CHERRY_SIGN, Items.CHERRY_HANGING_SIGN, Items.CHERRY_DOOR, Items.CHERRY_BUTTON, Items.CHERRY_STAIRS, Items.CHERRY_SLAB, Items.CHERRY_FENCE, Items.CHERRY_FENCE_GATE, Items.CHERRY_BOAT, Items.CHERRY_SAPLING, Items.CHERRY_LEAVES, Items.CHERRY_PRESSURE_PLATE, Items.CHERRY_TRAPDOOR);
            // 26.3's new wood. Pale Oak (the pale garden / Creaking wood) was added in 26.3 and was
            // missing here, so none of its ~18 wood products were obtainable.
            p(WoodType.PALE_OAK, "pale_oak", Items.PALE_OAK_PLANKS, Items.PALE_OAK_LOG, Items.STRIPPED_PALE_OAK_LOG, Items.STRIPPED_PALE_OAK_WOOD, Items.PALE_OAK_WOOD, Items.PALE_OAK_SIGN, Items.PALE_OAK_HANGING_SIGN, Items.PALE_OAK_DOOR, Items.PALE_OAK_BUTTON, Items.PALE_OAK_STAIRS, Items.PALE_OAK_SLAB, Items.PALE_OAK_FENCE, Items.PALE_OAK_FENCE_GATE, Items.PALE_OAK_BOAT, Items.PALE_OAK_SAPLING, Items.PALE_OAK_LEAVES, Items.PALE_OAK_PRESSURE_PLATE, Items.PALE_OAK_TRAPDOOR);
            p(WoodType.BAMBOO, "bamboo", null, null, Items.STRIPPED_BAMBOO_BLOCK, null, null, Items.BAMBOO_SIGN, Items.BAMBOO_HANGING_SIGN, Items.BAMBOO_DOOR, Items.BAMBOO_BUTTON, Items.BAMBOO_STAIRS, Items.BAMBOO_SLAB, Items.BAMBOO_FENCE, Items.BAMBOO_FENCE_GATE, Items.BAMBOO_RAFT, Items.BAMBOO, null, Items.BAMBOO_PRESSURE_PLATE, Items.BAMBOO_TRAPDOOR);
            p(WoodType.MANGROVE, "mangrove", Items.MANGROVE_PLANKS, Items.MANGROVE_LOG, Items.STRIPPED_MANGROVE_LOG, Items.STRIPPED_MANGROVE_WOOD, Items.MANGROVE_WOOD, Items.MANGROVE_SIGN, Items.MANGROVE_HANGING_SIGN, Items.MANGROVE_DOOR, Items.MANGROVE_BUTTON, Items.MANGROVE_STAIRS, Items.MANGROVE_SLAB, Items.MANGROVE_FENCE, Items.MANGROVE_FENCE_GATE, Items.MANGROVE_BOAT, Items.MANGROVE_PROPAGULE, Items.MANGROVE_LEAVES, Items.MANGROVE_PRESSURE_PLATE, Items.MANGROVE_TRAPDOOR);
            p(WoodType.ACACIA, "acacia", Items.ACACIA_PLANKS, Items.ACACIA_LOG, Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_WOOD, Items.ACACIA_SIGN, Items.ACACIA_HANGING_SIGN, Items.ACACIA_DOOR, Items.ACACIA_BUTTON, Items.ACACIA_STAIRS, Items.ACACIA_SLAB, Items.ACACIA_FENCE, Items.ACACIA_FENCE_GATE, Items.ACACIA_BOAT, Items.ACACIA_SAPLING, Items.ACACIA_LEAVES, Items.ACACIA_PRESSURE_PLATE, Items.ACACIA_TRAPDOOR);
            p(WoodType.BIRCH, "birch", Items.BIRCH_PLANKS, Items.BIRCH_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_WOOD, Items.BIRCH_SIGN, Items.BIRCH_HANGING_SIGN, Items.BIRCH_DOOR, Items.BIRCH_BUTTON, Items.BIRCH_STAIRS, Items.BIRCH_SLAB, Items.BIRCH_FENCE, Items.BIRCH_FENCE_GATE, Items.BIRCH_BOAT, Items.BIRCH_SAPLING, Items.BIRCH_LEAVES, Items.BIRCH_PRESSURE_PLATE, Items.BIRCH_TRAPDOOR);
            p(WoodType.CRIMSON, "crimson", Items.CRIMSON_PLANKS, Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_HYPHAE, Items.CRIMSON_SIGN, Items.CRIMSON_HANGING_SIGN, Items.CRIMSON_DOOR, Items.CRIMSON_BUTTON, Items.CRIMSON_STAIRS, Items.CRIMSON_SLAB, Items.CRIMSON_FENCE, Items.CRIMSON_FENCE_GATE, null, Items.CRIMSON_FUNGUS, null, Items.CRIMSON_PRESSURE_PLATE, Items.CRIMSON_TRAPDOOR);
            p(WoodType.DARK_OAK, "dark_oak", Items.DARK_OAK_PLANKS, Items.DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_WOOD, Items.DARK_OAK_WOOD, Items.DARK_OAK_SIGN, Items.DARK_OAK_HANGING_SIGN, Items.DARK_OAK_DOOR, Items.DARK_OAK_BUTTON, Items.DARK_OAK_STAIRS, Items.DARK_OAK_SLAB, Items.DARK_OAK_FENCE, Items.DARK_OAK_FENCE_GATE, Items.DARK_OAK_BOAT, Items.DARK_OAK_SAPLING, Items.DARK_OAK_LEAVES, Items.DARK_OAK_PRESSURE_PLATE, Items.DARK_OAK_TRAPDOOR);
            p(WoodType.OAK, "oak", Items.OAK_PLANKS, Items.OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_OAK_WOOD, Items.OAK_WOOD, Items.OAK_SIGN, Items.OAK_HANGING_SIGN, Items.OAK_DOOR, Items.OAK_BUTTON, Items.OAK_STAIRS, Items.OAK_SLAB, Items.OAK_FENCE, Items.OAK_FENCE_GATE, Items.OAK_BOAT, Items.OAK_SAPLING, Items.OAK_LEAVES, Items.OAK_PRESSURE_PLATE, Items.OAK_TRAPDOOR);
            p(WoodType.JUNGLE, "jungle", Items.JUNGLE_PLANKS, Items.JUNGLE_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_WOOD, Items.JUNGLE_SIGN, Items.JUNGLE_HANGING_SIGN, Items.JUNGLE_DOOR, Items.JUNGLE_BUTTON, Items.JUNGLE_STAIRS, Items.JUNGLE_SLAB, Items.JUNGLE_FENCE, Items.JUNGLE_FENCE_GATE, Items.JUNGLE_BOAT, Items.JUNGLE_SAPLING, Items.JUNGLE_LEAVES, Items.JUNGLE_PRESSURE_PLATE, Items.JUNGLE_TRAPDOOR);
            p(WoodType.SPRUCE, "spruce", Items.SPRUCE_PLANKS, Items.SPRUCE_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_WOOD, Items.SPRUCE_SIGN, Items.SPRUCE_HANGING_SIGN, Items.SPRUCE_DOOR, Items.SPRUCE_BUTTON, Items.SPRUCE_STAIRS, Items.SPRUCE_SLAB, Items.SPRUCE_FENCE, Items.SPRUCE_FENCE_GATE, Items.SPRUCE_BOAT, Items.SPRUCE_SAPLING, Items.SPRUCE_LEAVES, Items.SPRUCE_PRESSURE_PLATE, Items.SPRUCE_TRAPDOOR);
            p(WoodType.WARPED, "warped", Items.WARPED_PLANKS, Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_HYPHAE, Items.WARPED_SIGN, Items.WARPED_HANGING_SIGN, Items.WARPED_DOOR, Items.WARPED_BUTTON, Items.WARPED_STAIRS, Items.WARPED_SLAB, Items.WARPED_FENCE, Items.WARPED_FENCE_GATE, null, Items.WARPED_FUNGUS, null, Items.WARPED_PRESSURE_PLATE, Items.WARPED_TRAPDOOR);
        }

        void p(WoodType type, String prefix, Item planks, Item log, Item strippedLog, Item strippedWood, Item wood, Item sign, Item hangingSign, Item door, Item button, Item stairs, Item slab, Item fence, Item fenceGate, Item boat, Item sapling, Item leaves, Item pressurePlate, Item trapdoor) {
            put(type, new WoodItems(prefix, planks, log, strippedLog, strippedWood, wood, sign, hangingSign, door, button, stairs, slab, fence, fenceGate, boat, sapling, leaves, pressurePlate, trapdoor));
        }
    };
    public static final HashMap<Item, Item> cookableFoodMap = new HashMap<>() {
        {
            put(Items.PORKCHOP, Items.COOKED_PORKCHOP);
            put(Items.BEEF, Items.COOKED_BEEF);
            put(Items.CHICKEN, Items.COOKED_CHICKEN); // chicken is best meat, fight me
            put(Items.MUTTON, Items.COOKED_MUTTON);
            put(Items.RABBIT, Items.COOKED_RABBIT);
            put(Items.SALMON, Items.COOKED_SALMON);
            put(Items.COD, Items.COOKED_COD);
            put(Items.POTATO, Items.BAKED_POTATO);
        }
    };
    public static final Item[] RAW_FOODS = cookableFoodMap.keySet().toArray(Item[]::new);
    public static final Item[] COOKED_FOODS = cookableFoodMap.values().toArray(Item[]::new);
    private static Map<Item, Integer> fuelTimeMap = null;

    public static String stripItemName(Item item) {
        String[] possibilities = new String[]{"item.minecraft.", "block.minecraft."};
        for (String possible : possibilities) {
            if (item.getDescriptionId().startsWith(possible)) {
                return item.getDescriptionId().substring(possible.length());
            }
        }
        return item.getDescriptionId();
    }

    public static Item[] blocksToItems(Block[] blocks) {
        Item[] result = new Item[blocks.length];
        for (int i = 0; i < blocks.length; ++i) {
            result[i] = blocks[i].asItem();
        }
        return result;
    }

    /* Logs:
        ACACIA
        BIRCH
        CHERRY
        CRIMSON
        DARK_OAK
        OAK
        JUNGLE
        SPRUCE
        WARPED
     */

    /* Colors:
        WHITE
        BLACK
        BLUE
        BROWN
        CYAN
        GRAY
        GREEN
        LIGHT_BLUE
        LIGHT_GRAY
        LIME
        MAGENTA
        ORANGE
        PINK
        PURPLE
        RED
        YELLOW
     */

    public static Block[] itemsToBlocks(Item[] items) {
        ArrayList<Block> result = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof BlockItem) {
                Block b = Block.byItem(item);
                if (b != null && b != Blocks.AIR) {
                    result.add(b);
                }
            }
        }
        return result.toArray(Block[]::new);
    }

    public static Item logToPlanks(Item logItem) {
        return logToPlanks.getOrDefault(logItem, null);
    }

    public static Item planksToLog(Item plankItem) {
        return planksToLogs.getOrDefault(plankItem, null);
    }

    public static Item strippedToLogs(Item logItem) {
        return strippedToLogs.getOrDefault(logItem, null);
    }

    public static ColorfulItems getColorfulItems(MapColor color) {
        return colorMap.get(color);
    }

    public static ColorfulItems getColorfulItems(DyeColor color) {
        return getColorfulItems(color.getMapColor());
    }

    public static Collection<ColorfulItems> getColorfulItems() {
        return colorMap.values();
    }

    public static WoodItems getWoodItems(WoodType type) {
        return woodMap.get(type);
    }

    public static Collection<WoodItems> getWoodItems() {
        return woodMap.values();
    }

    public static Optional<Item> getCookedFood(Item rawFood) {
        return Optional.ofNullable(cookableFoodMap.getOrDefault(rawFood, null));
    }

    public static String trimItemName(String name) {
        if (name.startsWith("block.minecraft.")) {
            name = name.substring("block.minecraft.".length());
        } else if (name.startsWith("item.minecraft.")) {
            name = name.substring("item.minecraft.".length());
        }
        return name;
    }

    public static boolean areShearsEffective(Block b) {
        return
                //b.getRegistryEntry().streamTags().anyMatch(t -> t ==
                // BlockTags.LEAVES); should also work... but is slower
                b instanceof LeavesBlock
                        || b == Blocks.COBWEB
                        || b == Blocks.SHORT_GRASS
                        || b == Blocks.TALL_GRASS
                        || b == Blocks.LILY_PAD
                        || b == Blocks.FERN
                        || b == Blocks.DEAD_BUSH
                        || b == Blocks.VINE
                        || b == Blocks.TRIPWIRE
                        || BlockTagVer.isWool(b)
                        || b == Blocks.NETHER_SPROUTS;
    }

    private static boolean isStackProtected(AltoClef mod, ItemStack stack) {
        if (stack.isEnchanted() && mod.getModSettings().getDontThrowAwayEnchantedItems())
            return true;
        if (ItemVer.hasCustomName(stack) && mod.getModSettings().getDontThrowAwayCustomNameItems())
            return true;
        return mod.getBehaviour().isProtected(stack.getItem()) || mod.getModSettings().isImportant(stack.getItem());
    }

    /** Eating consumes a supply; it does not require permission to discard unused items. */
    public static boolean canConsumeFood(AltoClef mod, ItemStack stack) {
        return !stack.isEmpty() && ItemVer.isFood(stack) && !isStackProtected(mod, stack);
    }

    public static boolean canThrowAwayStack(AltoClef mod, ItemStack stack) {
        // Can't throw away empty stacks!
        if (stack.isEmpty())
            return false;
        if (isStackProtected(mod, stack) || mod.getBehaviour().isDiscardProtected(stack.getItem()))
            return false;
        return mod.getModSettings().isThrowaway(stack.getItem()) || mod.getModSettings().shouldThrowawayUnusedItems();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canStackTogether(ItemStack from, ItemStack to) {
        if (to.isEmpty() && from.getCount() <= from.getMaxStackSize())
            return true;
        return to.getItem().equals(from.getItem()) && (from.getCount() + to.getCount() < to.getMaxStackSize());
    }

    /**
     * What burns, and for how long.
     *
     * <h2>On 1.21.11 this was an empty map, and that made smelting impossible</h2>
     *
     * The port left {@code new HashMap<>()} with a TODO because {@code createFuelTimeMap()} was
     * removed from AbstractFurnaceBlockEntity. An empty map does not mean "we do not know yet" to
     * any of the three callers below — it means NOTHING IN THE GAME IS FUEL. {@link #isFuel} is
     * false for coal, {@link #getFuelAmount} is 0 for coal, and so
     * {@code StorageHelper.calculateInventoryFuelCount} is 0 no matter what the bot is carrying.
     *
     * <p>Measured on the smelt course, with eight coal in the pack and a furnace already placed and
     * open: SmeltInFurnaceTask asks for one unit of fuel, reads zero, and returns CollectFuelTask,
     * which walks off to MINE COAL. It can never stop, because no amount of coal ever raises the
     * count above zero. The chain said it plainly —
     * {@code <Doing stuff in [furnace] container> Getting Fuel -> <Collect Fuel: x2.0> -> <Mine And
     * Collect: [[coal]]> -> Wander for Infinity blocks} — while the pack held {@code coal 8}.
     *
     * <p>1.21.11 did not delete the data, it MOVED it: fuels are a registry hanging off the world
     * ({@code World.getFuelRegistry()}), which is strictly better than the old static map because a
     * datapack can change what burns. So ask the game rather than hardcoding a table — the values
     * are then right by construction, including modded and datapack fuels.
     *
     * <p>The re-read on an empty map matters: this is called from tracker code that can run before
     * the client has a world, and caching the empty answer once would reproduce the very bug this
     * replaces.
     */
    private static Map<Item, Integer> getFuelTimeMap() {
        if (fuelTimeMap == null || fuelTimeMap.isEmpty()) {
            java.util.Map<Item, Integer> built = new java.util.HashMap<>();
            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                int burn = resolveBurnTicks(new ItemStack(item));
                if (burn >= 0) {
                    built.put(item, burn);
                }
            }
            fuelTimeMap = built;
        }
        return fuelTimeMap;
    }

    /**
     * Furnace burn time (ticks) for one item, or {@code -1} if it is not a fuel.
     *
     * <p>26.3 moved burn time onto the {@code minecraft:cooking_fuel} item component as a
     * {@code ResolvableInt}. Inline constants resolve directly. The datapack values are
     * {@code COOKING_TIME_*} providers whose final value is {@code base / factor}, where the
     * factor is 1 for a furnace and 2 for a smoker/blast furnace; the choice is made by a
     * {@code LootItemCondition} on the block state, which needs a server-side {@code LootContext}
     * the client does not have. So referenced providers resolve to their <b>furnace</b> base time
     * (what the mod plans against), and a live {@code ConstantValue} is preferred when present.
     */
    private static int resolveBurnTicks(ItemStack stack) {
        net.minecraft.world.item.component.CookingFuel fuel =
                stack.get(net.minecraft.core.component.DataComponents.COOKING_FUEL);
        if (fuel == null) {
            return -1;
        }
        var resolvable = fuel.burnTime();
        if (resolvable instanceof net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant c) {
            return c.value();
        }
        if (resolvable instanceof net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Reference ref) {
            Integer base = COOKING_BASE_TICKS.get(ref.key());
            if (base != null) {
                return base;
            }
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                try {
                    var lookup = level.registryAccess()
                            .lookupOrThrow(net.minecraft.core.registries.Registries.CONTEXT_INT_PROVIDER);
                    var holder = lookup.get(ref.key());
                    if (holder.isPresent()
                            && holder.get().value()
                            instanceof net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue cv) {
                        return cv.value();
                    }
                } catch (Exception ignored) {
                    // fall through to 0
                }
            }
            return 0;
        }
        return 0;
    }

    /**
     * Vanilla furnace base burn times (ticks) per {@code cooking/*} provider, mirroring
     * {@code ContextIntProviders.bootstrap}. Used because the client cannot evaluate the
     * {@code ResolvableInt} references (see {@link #resolveBurnTicks}).
     */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<
            net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider>, Integer>
            COOKING_BASE_TICKS = buildCookingBaseTicks();

    private static java.util.Map<net.minecraft.resources.ResourceKey<
            net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider>, Integer>
            buildCookingBaseTicks() {
        java.util.Map<net.minecraft.resources.ResourceKey<
                net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider>, Integer> m =
                new java.util.HashMap<>();
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_BAMBOO, 50);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOL_SLABS, 50);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOL_CARPETS, 67);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_DRY_PLANTS, 100);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL, 100);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOL, 100);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_SLABS, 150);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE, 200);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_ROOTS, 300);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_BLOCKS, 300);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_WOOD_ITEMS_SMALL, 300);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_HANGING_SIGNS, 800);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_BOATS, 1200);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_COAL, 1600);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_BLAZE_ROD, 2400);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_DRIED_KELP_BLOCK, 4001);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_COAL_BLOCK, 16000);
        m.put(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_LAVA_BUCKET, 20000);
        return m;
    }

    public static double getFuelAmount(Item... items) {
        double total = 0;
        for (Item item : items) {
            if (getFuelTimeMap().containsKey(item)) {
                int timeTicks = getFuelTimeMap().get(item);
                // 300 ticks of wood -> 1.5 operations
                // 200 ticks -> 1 operation
                total += (double) timeTicks / 200.0;
            }
        }
        return total;
    }

    public static double getFuelAmount(ItemStack stack) {
        return getFuelAmount(stack.getItem()) * stack.getCount();
    }

    public static boolean isFuel(Item item) {
        return getFuelTimeMap().containsKey(item);
    }

    public boolean isRawFood(Item item) {
        return cookableFoodMap.containsKey(item);
    }

    public static class ColorfulItems {
        public DyeColor color;
        public String colorName;
        public Item dye;
        public Item wool;
        public Item bed;
        public Item carpet;
        public Item stainedGlass;
        public Item stainedGlassPane;
        public Item terracotta;
        public Item glazedTerracotta;
        public Item concrete;
        public Item concretePowder;
        public Item banner;
        public Item shulker;
        public Block wallBanner;

        public ColorfulItems(DyeColor color, String colorName, Item dye, Item wool, Item bed, Item carpet, Item stainedGlass, Item stainedGlassPane, Item terracotta, Item glazedTerracotta, Item concrete, Item concretePowder, Item banner, Item shulker, Block wallBanner) {
            this.color = color;
            this.colorName = colorName;
            this.dye = dye;
            this.wool = wool;
            this.bed = bed;
            this.carpet = carpet;
            this.stainedGlass = stainedGlass;
            this.stainedGlassPane = stainedGlassPane;
            this.terracotta = terracotta;
            this.glazedTerracotta = glazedTerracotta;
            this.concrete = concrete;
            this.concretePowder = concretePowder;
            this.banner = banner;
            this.shulker = shulker;
            this.wallBanner = wallBanner;
        }
    }

    public static class WoodItems {
        public String prefix;
        public Item planks;
        public Item log;
        public Item strippedLog;
        public Item strippedWood;
        public Item wood;
        public Item sign;
        public Item hangingSign;
        public Item door;
        public Item button;
        public Item stairs;
        public Item slab;
        public Item fence;
        public Item fenceGate;
        public Item boat;
        public Item sapling;
        public Item leaves;
        public Item pressurePlate;
        public Item trapdoor;

        public WoodItems(String prefix, Item planks, Item log, Item strippedLog, Item strippedWood, Item wood, Item sign, Item hangingSign, Item door, Item button, Item stairs, Item slab, Item fence, Item fenceGate, Item boat, Item sapling, Item leaves, Item pressurePlate, Item trapdoor) {
            this.prefix = prefix;
            this.planks = planks;
            this.log = log;
            this.strippedLog = strippedLog;
            this.strippedWood = strippedWood;
            this.wood = wood;
            this.sign = sign;
            this.hangingSign = hangingSign;
            this.door = door;
            this.button = button;
            this.stairs = stairs;
            this.slab = slab;
            this.fence = fence;
            this.fenceGate = fenceGate;
            this.boat = boat;
            this.sapling = sapling;
            this.leaves = leaves;
            this.pressurePlate = pressurePlate;
            this.trapdoor = trapdoor;
        }

        public boolean isNetherWood() {
            return planks == Items.CRIMSON_PLANKS || planks == Items.WARPED_PLANKS;
        }
    }

    // --- Multiplayer helpers (ported from autoclef) ---

    // Throttle for server-menu join clicks: autojoin task loops call clickCustomItem EVERY tick,
    // and the musteryworld cluster rate-limits rapid clicks (server text: "wait before clicking again").
    // Pace the actual click to ~1 per 1.2s so autojoin does not spam-trip the server anti-double-click.
    private static final adris.altoclef.util.time.TimerGame _customItemClickTimer =
            new adris.altoclef.util.time.TimerGame(1.2);

    /**
     * Clicks a custom-named item in the current screen by name match.
     */
    /**
     * Finds an item with matching custom name in player inventory, equips it, and right-clicks.
     * Only works when no container screen is open (to avoid corrupting open chest handlers).
     */
    public static boolean clickCustomItem(AltoClef mod, String... joinItems) {
        // Don't equip/click while a container screen is open — forceEquipSlot would
        // do a SWAP through the chest handler, corrupting the GUI and closing it.
        if (!(mod.getPlayer().containerMenu instanceof net.minecraft.world.inventory.InventoryMenu)) {
            return false;
        }
        Slot newGameSlot = getCustomItemSlot(mod, joinItems);
        if (newGameSlot != null) {
            // Rate-limit: called every tick by autojoin; the server rejects rapid clicks
            // ("wait before clicking again"). Act at most ~once per 1.2s.
            if (!_customItemClickTimer.elapsed()) return false;
            _customItemClickTimer.reset();
            // If the item is in the hotbar, just switch selected slot instead of SWAP.
            // Server menu items are often immovable, so SWAP would fail.
            int invSlot = newGameSlot.getInventorySlot();
            if (invSlot >= 0 && invSlot <= 8) {
                // Hotbar slot — just switch selected slot, don't SWAP (server menu items are immovable)
                PlayerVer.setSelectedSlot(mod.getPlayer().getInventory(), invSlot);
            } else {
                mod.getSlotHandler().forceEquipSlot(newGameSlot);
            }
            LookHelper.tryAvoidingInteractable(mod);
            mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            return true;
        }
        return false;
    }

    /**
     * Finds a slot in the current screen containing an item with a matching custom name.
     */
    public static Slot getCustomItemSlot(AltoClef mod, String... checkItemName) {
        Iterable<Slot> slots = Slot.getCurrentScreenSlots();
        if (!AltoClef.inGame() || mod.getPlayer() == null || slots == null) return null;
        for (Slot slot : slots) {
            net.minecraft.world.item.ItemStack item = StorageHelper.getItemStackInSlot(slot);
            if (item != null && !item.isEmpty()) {
                boolean hasCustomName = item.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME) != null;
                if (hasCustomName) {
                    String itemCustomName = removeMCFormatCodes(item.getHoverName().getString().toLowerCase());
                    for (String check : checkItemName) {
                        String checkLower = check.toLowerCase();
                        if (checkLower.equals(itemCustomName)
                                || checkLower.contains(itemCustomName)
                                || itemCustomName.contains(checkLower)) {
                            return slot;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String removeMCFormatCodes(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    // --- Combat item arrays and weapon threat helpers (ported from autoclef) ---

    public static final net.minecraft.world.item.Item[] MMKillerWeapons = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.SHEARS, net.minecraft.world.item.Items.IRON_SWORD, net.minecraft.world.item.Items.IRON_AXE
    };
    public static final net.minecraft.world.item.Item[] MMDetectiveWeapons = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.BOW, net.minecraft.world.item.Items.CROSSBOW
    };

    public static final net.minecraft.world.item.Item[] RangedTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.BOW, net.minecraft.world.item.Items.CROSSBOW,
        net.minecraft.world.item.Items.SPLASH_POTION, net.minecraft.world.item.Items.LINGERING_POTION,
        net.minecraft.world.item.Items.SNOWBALL
    };

    public static boolean holdWeapon(Player entity, net.minecraft.world.item.Item... items) {
        for (net.minecraft.world.item.Item weapon : items) {
            if (entity.getMainHandItem().is(weapon)) return true;
        }
        return false;
    }

    /**
     * Attack damage this item adds, read from its own attribute modifiers.
     *
     * <p>THE ONE ANSWER TO "HOW HARD DOES THIS HIT", FOR EVERY VERSION. It used to be asked as
     * {@code item instanceof SwordItem} followed by {@code getMaterial().getAttackDamage()}, and
     * 1.21.11 deleted both the class and the material accessor. Every site that asked it was left
     * with a preprocessor branch whose 1.21.11 half was an empty stub, so on the live version the
     * answer was silently ZERO -- see {@link adris.altoclef.control.KillAura#equipWeapon}, whose
     * whole body sat in the dead half.
     *
     * <p>Matched by the attribute's registry PATH rather than an Attributes constant: the
     * constant is GENERIC_ATTACK_DAMAGE on one version and ATTACK_DAMAGE on the other, and the
     * point of this method is to stop maintaining two spellings of one question. The registry id
     * is "attack_damage" on both.
     *
     * <p>Returns 0 for anything that is not a weapon, which every caller reads as "no weapon".
     */
    public static float meleeDamageOf(net.minecraft.world.item.Item item) {
        return attributeSum(item, "attack_damage");
    }

    /**
     * Damage per second, which is what "best weapon" actually means.
     *
     * <p>Ranking by raw damage picks a netherite AXE (10) over a netherite SWORD (8), and the axe
     * swings at 1.0/s against the sword's 1.6 -- 10 dps against 12.8. The old code dodged this by
     * only ever considering swords, which it could do because it had a sword CLASS to test. With
     * the class gone, asking the item how fast it swings is both the smaller change and the more
     * correct one: it needs no class, no tag and no item list, and it prefers swords for the
     * reason a player does rather than by fiat.
     *
     * <p>Base values are the vanilla player's: 1 damage, 4 swings a second, before modifiers.
     */
    public static float meleeDps(net.minecraft.world.item.Item item) {
        float damage = 1.0f + attributeSum(item, "attack_damage");
        float speed = 4.0f + attributeSum(item, "attack_speed");
        if (speed <= 0) {
            return 0;
        }
        return damage * speed;
    }

    /**
     * How fast this stack mines that block — ported out of shredder's {@code ToolSet}.
     *
     * <p>StorageHelper's "which tool is best here" used {@code ToolSet.calculateSpeedVsBlock}, and
     * that was altoclef's ONLY reason to import anything from {@code baritone.utils}. The function
     * is a pure one of (stack, state): mining-speed multiplier, the efficiency enchantment, and
     * hardness. No engine, no pathfinder, no context. So it moves here rather than keeping a
     * module dependency alive for one call — the direction of travel is that shredder goes and
     * altoclef stays.
     *
     * <p>Copied faithfully, including the two quirks worth not silently "improving":
     * <ul>
     *   <li>a hardness read that throws is treated as UNBREAKABLE (-1), not as free
     *   <li>the /30 vs /100 split is upstream's, and encodes "the right tool or none required"
     *       against "wrong tool, mining anyway"
     * </ul>
     *
     * <p>THE FORCED-TOOL OVERRIDE IS KEPT, and the first draft of this comment said it was dropped
     * because "nothing in altoclef sets it — checked before dropping it". That sentence was FALSE
     * and was written before the check: ShearAndCollectBlockTask calls
     * {@code botBehaviour.forceUseTool(...)} and BotBehaviour pushes those predicates into
     * AltoClefSettings. Dropping it would have stopped the bot forcing shears onto leaves, quietly,
     * on a path no course covers. Exactly the RULE FOUR failure — a checkable claim about the
     * running system, asserted in a comment, unchecked.
     *
     * <p>Keeping it costs an {@code AltoClefSettings} reference. ⛔ CORRECTED 2026-09-02: this used
     * to say the class "lives in shredder's tree" and "dies with shredder rather than before it" —
     * that was true when this comment was written, but {@code AltoClefSettings} has since moved to
     * {@code adris.altoclef.settings}, its own altoclef package (part of G-0's tail; see
     * {@code TODOS.md}'s "G-0 connectivity" item). It is now a fully native altoclef reference with no
     * connection to shredder at all — an even cleaner remove of the old {@code baritone.utils}
     * dependency than originally described, not a debt that dies alongside shredder.
     *
     * @return blocks per tick, or -1 when the block cannot be broken
     */
    public static double miningSpeedVsBlock(ItemStack item, net.minecraft.world.level.block.state.BlockState state) {
        float hardness;
        try {
            hardness = state.getDestroySpeed(null, null);
        } catch (NullPointerException npe) {
            return -1;
        }
        if (hardness < 0) {
            return -1;
        }
        float speed = item.getDestroySpeed(state);
        if (speed > 1) {
            var enchants = item.getEnchantments();
            outer:
            for (var enchant : enchants.keySet()) {
                var effects = enchant.value().getEffects(
                        net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES);
                for (var e : effects) {
                    // BY REGISTRY PATH, NOT BY CONSTANT. Attributes.MINING_EFFICIENCY does
                    // not exist on every version altoclef builds for (shredder compiles against
                    // one; this module compiles against three), and the build says so. Same
                    // technique as meleeDamageOf above, for the same reason.
                    String attrPath = e.attribute().unwrapKey()
                            .map(k -> k.identifier().getPath()).orElse("");
                    if ("mining_efficiency".equals(attrPath)) {
                        speed += e.amount().calculate(enchants.getLevel(enchant));
                        break outer;
                    }
                }
            }
        }
        if (adris.altoclef.settings.AltoClefSettings.getInstance().shouldForceUseTool(state, item)) {
            return Double.POSITIVE_INFINITY;
        }
        speed /= hardness;
        if (!state.requiresCorrectToolForDrops() || (!item.isEmpty() && item.isCorrectToolForDrops(state))) {
            return speed / 30;
        }
        return speed / 100;
    }

    /**
     * Is this a tool — a pickaxe, axe, shovel, hoe or sword?
     *
     * <p>Replaces {@code item instanceof ToolItem}, which 1.21.11 deleted. The TOOL component is
     * what the game itself uses to decide mining behaviour, and swords carry it there exactly as
     * they used to extend ToolItem, so the set this matches is the same one on both versions.
     *
     * <p>Two callers were silently inverted without it. {@code @deposit} filtered tools OUT of the
     * chest with {@code !(item instanceof ToolItem)} and its 1.21.11 stub was {@code return true} —
     * so on the live version the command posted the bot's own pickaxe into the chest. And
     * {@code forceDeequipHitTool} stubbed to {@code stack -> false}, which never puts anything away.
     */
    public static boolean isTool(net.minecraft.world.item.Item item) {
        return item != null && item.components().has(net.minecraft.core.component.DataComponents.TOOL);
    }

    /**
     * Which {@link net.minecraft.world.entity.EquipmentSlot} this item wears in, or {@code null} if it
     * is not equippable at all.
     *
     * <p>Replaces {@code ((ArmorItem) item).getSlotType()}, which 1.21.11 deleted along with the
     * whole {@code ArmorItem} class. The EQUIPPABLE component is what the game itself now uses to
     * decide which slot an item goes in on right-click/dispenser-equip, so this matches the same
     * slot vanilla would pick.
     */
    public static net.minecraft.world.entity.EquipmentSlot getArmorSlot(net.minecraft.world.item.Item item) {
        if (item == null) {
            return null;
        }
        var equippable = item.components().get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable == null ? null : equippable.slot();
    }

    /**
     * Which "kind" of tool this is, for grouping items that should be compared against each other
     * (two pickaxes, never a pickaxe against an axe) — {@code null} for anything that is not a
     * recognized tool.
     *
     * <p>Replaces {@code tool.getClass()} (a {@code PickaxeItem}/{@code AxeItem}/... subclass),
     * which 1.21.11 deleted along with the whole tool class hierarchy. These vanilla item tags are
     * data-driven and stable across versions — the same grouping the game itself uses to decide,
     * for instance, which items a lodestone compass or a piglin trade will accept.
     */
    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> toolFamily(net.minecraft.world.item.Item item) {
        if (item == null) {
            return null;
        }
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        if (stack.is(net.minecraft.tags.ItemTags.PICKAXES)) return net.minecraft.tags.ItemTags.PICKAXES;
        if (stack.is(net.minecraft.tags.ItemTags.AXES)) return net.minecraft.tags.ItemTags.AXES;
        if (stack.is(net.minecraft.tags.ItemTags.SHOVELS)) return net.minecraft.tags.ItemTags.SHOVELS;
        if (stack.is(net.minecraft.tags.ItemTags.HOES)) return net.minecraft.tags.ItemTags.HOES;
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) return net.minecraft.tags.ItemTags.SWORDS;
        return null;
    }

    /**
     * A material-agnostic replacement for the deleted {@code ToolMaterial} "mining level"
     * (0 = wood/gold ... 4 = netherite) — meaningful only to compare two tools of the SAME
     * {@link #toolFamily}, never across families.
     *
     * <p>A mining-speed multiplier is a flat per-material constant on any block a tool is
     * efficient against (2.0/4.0/6.0/8.0/9.0 for wood/stone/iron/diamond/netherite pickaxes, for
     * instance — see {@link #miningSpeedVsBlock}), so the reference block only needs to be one the
     * whole family is efficient on, not the specific block actually being mined. For swords,
     * {@link #meleeDps} plays the same role: sword damage strictly increases with material tier at
     * a fixed attack speed, so DPS preserves the same ordering material tier used to give directly.
     */
    public static double toolQuality(net.minecraft.world.item.Item item) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) {
            return meleeDps(item);
        }
        net.minecraft.world.level.block.state.BlockState reference;
        if (stack.is(net.minecraft.tags.ItemTags.PICKAXES)) {
            reference = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        } else if (stack.is(net.minecraft.tags.ItemTags.AXES)) {
            reference = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
        } else if (stack.is(net.minecraft.tags.ItemTags.SHOVELS)) {
            reference = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        } else {
            // Hoes (and anything else with no natural "how fast does it mine" reference): fall
            // back to the pickaxe-vs-stone reading. Only ever compared within its own family, so
            // this only needs to preserve ORDER between two hoes, not match a real-world speed.
            reference = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        }
        return stack.getDestroySpeed(reference);
    }

    private static float attributeSum(net.minecraft.world.item.Item item, String path) {
        if (item == null) {
            return 0;
        }
        try {
            ItemStack stack = new ItemStack(item);
            var comp = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            if (comp == null) {
                return 0;
            }
            float sum = 0;
            for (var entry : comp.modifiers()) {
                String id = entry.attribute().unwrapKey().map(k -> k.identifier().getPath()).orElse("");
                if (path.equals(id)) {
                    sum += (float) entry.modifier().amount();
                }
            }
            return sum;
        } catch (Throwable t) {
            // A reading that throws must not decide a fight. Say "no weapon" -- the safe half.
            return 0;
        }
    }

    public static WeaponThreat getWeaponThreat(AltoClef mod, Player entity) {
        ItemStack stack = entity.getMainHandItem();
        if (stack != null) {
            if (holdWeapon(entity, RangedTopPriority)) return WeaponThreat.Ranged;
            net.minecraft.world.item.Item handItem = stack.getItem();
            float damage = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            // ASK THE ITEM, DO NOT ASK ITS CLASS. The version split that used to live here had an
            // EMPTY 1.21.11 half, so an enemy holding a diamond sword was read as Harmless unless
            // their base attack attribute already cleared 4 -- on the version this mod ships for.
            // The +3 the old branch added on top of the material damage is kept: it is the melee
            // threat bonus this scale was tuned with, not part of the item's own numbers.
            float weapon = meleeDamageOf(handItem);
            if (weapon > 0) {
                damage += weapon + 3;
            }
            if (handItem instanceof net.minecraft.world.item.TridentItem || handItem instanceof net.minecraft.world.item.MaceItem) {
                damage += 7f;
            }
            if (damage >= 4f) return WeaponThreat.Melee;
        }
        return WeaponThreat.Harmless;
    }

    public static boolean hasItems(AltoClef mod, Item... items) {
        for (Item item : items) {
            if (mod.getItemStorage().hasItem(item)) return true;
        }
        return false;
    }

    // --- Priority arrays for KitPVP/SkyWars armor and tool tiers (highest priority = index 0) ---

    public static final net.minecraft.world.item.Item[] HelmetsTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_HELMET,
        net.minecraft.world.item.Items.DIAMOND_HELMET,
        net.minecraft.world.item.Items.IRON_HELMET,
        net.minecraft.world.item.Items.CHAINMAIL_HELMET,
        net.minecraft.world.item.Items.GOLDEN_HELMET,
        net.minecraft.world.item.Items.LEATHER_HELMET
    };
    public static final net.minecraft.world.item.Item[] ChestplatesTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_CHESTPLATE,
        net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,
        net.minecraft.world.item.Items.IRON_CHESTPLATE,
        net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE,
        net.minecraft.world.item.Items.GOLDEN_CHESTPLATE,
        net.minecraft.world.item.Items.LEATHER_CHESTPLATE
    };
    public static final net.minecraft.world.item.Item[] LeggingsTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_LEGGINGS,
        net.minecraft.world.item.Items.DIAMOND_LEGGINGS,
        net.minecraft.world.item.Items.IRON_LEGGINGS,
        net.minecraft.world.item.Items.CHAINMAIL_LEGGINGS,
        net.minecraft.world.item.Items.GOLDEN_LEGGINGS,
        net.minecraft.world.item.Items.LEATHER_LEGGINGS
    };
    public static final net.minecraft.world.item.Item[] BootsTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_BOOTS,
        net.minecraft.world.item.Items.DIAMOND_BOOTS,
        net.minecraft.world.item.Items.IRON_BOOTS,
        net.minecraft.world.item.Items.CHAINMAIL_BOOTS,
        net.minecraft.world.item.Items.GOLDEN_BOOTS,
        net.minecraft.world.item.Items.LEATHER_BOOTS
    };
    public static final net.minecraft.world.item.Item[] SwordsTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_SWORD,
        net.minecraft.world.item.Items.DIAMOND_SWORD,
        net.minecraft.world.item.Items.IRON_SWORD,
        net.minecraft.world.item.Items.GOLDEN_SWORD,
        net.minecraft.world.item.Items.STONE_SWORD,
        net.minecraft.world.item.Items.WOODEN_SWORD
    };
    public static final net.minecraft.world.item.Item[] AxesTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_AXE,
        net.minecraft.world.item.Items.DIAMOND_AXE,
        net.minecraft.world.item.Items.IRON_AXE,
        net.minecraft.world.item.Items.GOLDEN_AXE,
        net.minecraft.world.item.Items.STONE_AXE,
        net.minecraft.world.item.Items.WOODEN_AXE
    };
    public static final net.minecraft.world.item.Item[] PickaxesTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_PICKAXE,
        net.minecraft.world.item.Items.DIAMOND_PICKAXE,
        net.minecraft.world.item.Items.IRON_PICKAXE,
        net.minecraft.world.item.Items.GOLDEN_PICKAXE,
        net.minecraft.world.item.Items.STONE_PICKAXE,
        net.minecraft.world.item.Items.WOODEN_PICKAXE
    };
    public static final net.minecraft.world.item.Item[] ShovelsTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_SHOVEL,
        net.minecraft.world.item.Items.DIAMOND_SHOVEL,
        net.minecraft.world.item.Items.IRON_SHOVEL,
        net.minecraft.world.item.Items.GOLDEN_SHOVEL,
        net.minecraft.world.item.Items.STONE_SHOVEL,
        net.minecraft.world.item.Items.WOODEN_SHOVEL
    };
    public static final net.minecraft.world.item.Item[] HoesTopPriority = new net.minecraft.world.item.Item[]{
        net.minecraft.world.item.Items.NETHERITE_HOE,
        net.minecraft.world.item.Items.DIAMOND_HOE,
        net.minecraft.world.item.Items.IRON_HOE,
        net.minecraft.world.item.Items.GOLDEN_HOE,
        net.minecraft.world.item.Items.STONE_HOE,
        net.minecraft.world.item.Items.WOODEN_HOE
    };

}
