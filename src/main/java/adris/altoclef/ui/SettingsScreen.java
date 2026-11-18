package adris.altoclef.ui;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.Settings;
import adris.altoclef.util.helpers.ConfigHelper;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game settings screen, built with Cloth Config.
 *
 * <p>Opened by the "UnionClef Settings" keybind (default: O, rebindable in Options &gt; Controls)
 * or programmatically. Saving writes {@code altoclef_settings.json} through the same
 * {@link ConfigHelper} path the rest of the mod uses, so the file format is unchanged.
 *
 * <p>Every entry reads its current value from a {@link Settings} getter and writes back through a
 * setter, so the GUI and the JSON stay one source of truth.
 */
public final class SettingsScreen {

    private SettingsScreen() {
    }

    public static Screen create(Screen parent) {
        AltoClef mod = AltoClef.getInstance();
        Settings s = mod == null ? null : mod.getModSettings();
        if (s == null) {
            // Settings are loaded on the title screen; nothing to show yet.
            Debug.logWarning("Settings not loaded yet; cannot open the settings screen.");
            return parent;
        }

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("UnionClef Settings"))
                .setSavingRunnable(() -> {
                    ConfigHelper.saveConfig(Settings.SETTINGS_PATH, s);
                    Debug.logMessage("Settings saved to " + Settings.SETTINGS_PATH);
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ── General / HUD ───────────────────────────────────────────────────
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        toggle(eb, general, "Task chain overlay",
                "Show the current task chain in the top-left corner.", s.shouldShowTaskChain(), s::setShowTaskChains);
        toggle(eb, general, "Show timer",
                "Show a running timer in the overlay.", s.shouldShowTimer(), s::setShowTimer);
        toggle(eb, general, "Epic camera",
                "Use the smooth third-person orbit camera (F5).", s.isEpicCameraEnabled(), s::setEpicCameraEnabled);
        toggle(eb, general, "Debug tick ms",
                "Show how many milliseconds the mod spends per tick.", s.shouldShowDebugTickMs(), s::setShowDebugTickMs);
        toggle(eb, general, "Hide warning logs",
                "Silence warning logs. NOT recommended: makes debugging harder.", s.shouldHideAllWarningLogs(), s::setHideAllWarningLogs);
        general.addEntry(eb.startStringDropdownMenu(Component.literal("Chat messages"), s.getChatVerbosity())
                .setSelections(java.util.List.of("All", "Important only", "Off"))
                .setDefaultValue("Important only")
                .setTooltip(Component.literal("How much the mod prints in chat. Details still go to the log file."))
                .setSaveConsumer(s::setChatVerbosity)
                .build());
        number(eb, general, "Overlay text size",
                "Size of the on-screen text (1.0 = normal).", s.getOverlayTextScale(), 1.0f, 0.25f, 3.0f, s::setOverlayTextScale);
        toggle(eb, general, "Smooth aim (anti-cheat)",
                "Limit non-combat turning speed instead of snapping. Combat already uses the smooth aim.",
                s.isSmoothAim(), s::setSmoothAim);
        number(eb, general, "Max turn speed (deg/tick)",
                "Turn-rate cap used when Smooth aim is on.", s.getMaxTurnSpeedDegPerTick(), 20.0f, 1.0f, 180.0f,
                s::setMaxTurnSpeedDegPerTick);
        general.addEntry(eb.startStrField(Component.literal("Command prefix"), s.getCommandPrefix())
                .setDefaultValue("@")
                .setTooltip(Component.literal("Prefix for commands, e.g. @gamer."))
                .setSaveConsumer(s::setCommandPrefix)
                .build());
        general.addEntry(eb.startStringDropdownMenu(Component.literal("Log level"), s.getLogLevel())
                .setSelections(java.util.List.of("ALL", "NORMAL", "WARN", "ERROR", "NONE"))
                .setDefaultValue("NORMAL")
                .setTooltip(Component.literal("How much the mod logs to chat."))
                .setSaveConsumer(s::setLogLevel)
                .build());
        general.addEntry(eb.startStrField(Component.literal("Chat log prefix"), s.getChatLogPrefix())
                .setDefaultValue("[Alto Clef] ")
                .setTooltip(Component.literal("Prepended to every chat log line."))
                .setSaveConsumer(s::setChatLogPrefix)
                .build());

        // ── Movement & Combat ───────────────────────────────────────────────
        ConfigCategory combat = builder.getOrCreateCategory(Component.literal("Movement & Combat"));
        toggle(eb, combat, "Mob defense",
                "Fight back against hostile mobs.", s.isMobDefense(), s::setMobDefense);
        toggle(eb, combat, "Dodge projectiles",
                "Sidestep incoming arrows and projectiles.", s.isDodgeProjectiles(), s::setDodgeProjectiles);
        toggle(eb, combat, "Deal with annoying hostiles",
                "Kill or avoid mobs that keep interrupting.", s.shouldDealWithAnnoyingHostiles(), s::setKillOrAvoidAnnoyingHostiles);
        toggle(eb, combat, "Super parkour mode",
                "Allow riskier parkour moves when pathing.", s.isSuperParkourMode(), s::setSuperParkourMode);
        toggle(eb, combat, "Auto MLG bucket",
                "Place water to break a fall.", s.shouldAutoMLGBucket(), s::setAutoMLGBucket);
        number(eb, combat, "Min pearl fall distance",
                "Minimum fall height before an ender pearl is thrown to survive.",
                s.getMinPearlFallDistance(), 4.0, 1.0, 100.0, s::setMinPearlFallDistance);
        number(eb, combat, "Entity reach range",
                "How far the bot will reach to interact with entities.",
                s.getEntityReachRange(), 4.0f, 1.0f, 6.0f, s::setEntityReachRange);

        // ── Resource Gathering ──────────────────────────────────────────────
        ConfigCategory res = builder.getOrCreateCategory(Component.literal("Resource Gathering"));
        number(eb, res, "Resource pickup range",
                "How far away dropped items are picked up (-1 = no limit).",
                s.getResourcePickupRange(), -1.0f, -1.0f, 64.0f, s::setResourcePickupDropRange);
        number(eb, res, "Chest locate range",
                "How far away storage containers are searched.",
                s.getResourceChestLocateRange(), 500.0f, 0.0f, 500.0f, s::setResourceChestLocateRange);
        number(eb, res, "Mine range",
                "How far away blocks are mined for a resource.", s.getResourceMineRange(), 100.0f, 1.0f, 256.0f, s::setResourceMineRange);
        toggle(eb, res, "Collect pickaxe first",
                "Gather a pickaxe before other resources.", s.shouldCollectPickaxeFirst(), s::setCollectPickaxeFirst);
        toggle(eb, res, "Replant crops",
                "Replant harvested crops.", s.shouldReplantCrops(), s::setReplantCrops);
        toggle(eb, res, "Avoid dungeon chests",
                "Do not path to dungeon chests (dangerous).", s.shouldAvoidSearchingForDungeonChests(), s::setAvoidSearchingDungeonChests);
        toggle(eb, res, "Avoid ocean blocks",
                "Prefer not to traverse ocean for resources.", s.shouldAvoidOcean(), s::setAvoidOceanBlocks);

        // ── Survival ────────────────────────────────────────────────────────
        ConfigCategory survival = builder.getOrCreateCategory(Component.literal("Survival"));
        toggle(eb, survival, "Auto eat",
                "Eat when hungry.", s.isAutoEat(), s::setAutoEat);
        toggle(eb, survival, "Avoid drowning",
                "Surface when about to drown.", s.shouldAvoidDrowning(), s::setAvoidDrowning);
        toggle(eb, survival, "Extinguish with water",
                "Place water when on fire.", s.shouldExtinguishSelfWithWater(), s::setExtinguishSelfWithWater);
        toggle(eb, survival, "Close screen when acting",
                "Close open containers when looking or mining.", s.shouldCloseScreenWhenLookingOrMining(), s::setAutoCloseScreenWhenLookingOrMining);
        number(eb, survival, "Minimum food allowed",
                "Keep at least this much food before spending it.", s.getMinimumFoodAllowed(), 40, 0, 1000, s::setMinimumFoodAllowed);
        number(eb, survival, "Food units to collect",
                "Food target the bot gathers.", s.getFoodUnitsToCollect(), 220, 0, 5000, s::setFoodUnitsToCollect);

        // ── Inventory & Crafting ────────────────────────────────────────────
        ConfigCategory inv = builder.getOrCreateCategory(Component.literal("Inventory & Crafting"));
        number(eb, inv, "Container move delay",
                "Delay between inventory moves (seconds).", s.getContainerItemMoveDelay(), 0.2f, 0.0f, 5.0f, s::setContainerItemMoveDelay);
        toggle(eb, inv, "Use crafting recipe book",
                "Use the recipe book to fill the crafting grid (faster).", s.shouldUseCraftingBookToCraft(), s::setUseCraftingBookToCraft);
        number(eb, inv, "Reserved building blocks",
                "Building blocks kept back from being thrown away.", s.getReservedBuildingBlockCount(), 64, 0, 4096, s::setReservedBuildingBlockCount);
        toggle(eb, inv, "Throw away unused items",
                "Throw away items that are not needed.", s.shouldThrowawayUnusedItems(), s::setThrowAwayUnusedItems);
        toggle(eb, inv, "Keep custom-named items",
                "Never throw away renamed items.", s.getDontThrowAwayCustomNameItems(), s::setDontThrowAwayCustomNameItems);
        toggle(eb, inv, "Keep enchanted items",
                "Never throw away enchanted items.", s.getDontThrowAwayEnchantedItems(), s::setDontThrowAwayEnchantedItems);
        toggle(eb, inv, "Limit to supported fuels",
                "Only use fuels from the supported list.", s.shouldLimitFuelsToSupportedFuels(), s::setLimitFuelsToSupportedFuels);

        // ── Session & Automation ────────────────────────────────────────────
        ConfigCategory session = builder.getOrCreateCategory(Component.literal("Session & Automation"));
        toggle(eb, session, "Auto reconnect",
                "Reconnect automatically after a disconnect.", s.isAutoReconnect(), s::setAutoReconnect);
        number(eb, session, "Reconnect delay (s)",
                "Seconds to wait before reconnecting.", s.getReconnectDelaySec(), 7.0, 0.0, 300.0, s::setReconnectDelaySec);
        toggle(eb, session, "Auto respawn",
                "Respawn automatically after death.", s.isAutoRespawn(), s::setAutoRespawn);
        session.addEntry(eb.startStrField(Component.literal("Auto connect server"), s.getAutoConnectServer() == null ? "" : s.getAutoConnectServer())
                .setDefaultValue("")
                .setTooltip(Component.literal("Server address to connect to on launch (blank = off)."))
                .setSaveConsumer(s::setAutoConnectServer)
                .build());
        session.addEntry(eb.startStrField(Component.literal("Idle command"), s.getIdleCommand() == null ? "" : s.getIdleCommand())
                .setDefaultValue("")
                .setTooltip(Component.literal("Command run when no task is active (blank = off)."))
                .setSaveConsumer(s::setIdleCommand)
                .build());
        session.addEntry(eb.startStrField(Component.literal("Death command"), s.getDeathCommand() == null ? "" : s.getDeathCommand())
                .setDefaultValue("")
                .setTooltip(Component.literal("Command run after dying (blank = off)."))
                .setSaveConsumer(s::setDeathCommand)
                .build());
        number(eb, session, "Nether travel walk range",
                "Threshold for switching to nether fast-travel on foot.", s.getNetherFastTravelWalkingRange(), 600, 0, 100000, s::setNetherFastTravelWalkingRange);

        // ── Agent Bridge (Python / MCP) ─────────────────────────────────────
        ConfigCategory bridge = builder.getOrCreateCategory(Component.literal("Agent Bridge"));
        toggle(eb, bridge, "MCP server enabled",
                "Host the Model Context Protocol server so an agent can drive the bot over the LAN.", s.isMcpEnabled(), s::setMcpEnabled);
        number(eb, bridge, "Py4j gateway port",
                "Port for the Python (py4j) bridge.", s.getPythonGatewayPort(), 25333, 1024, 65535, s::setPythonGatewayPort);
        number(eb, bridge, "MCP port",
                "Port for the MCP HTTP server.", s.getMcpPort(), 25350, 1024, 65535, s::setMcpPort);
        bridge.addEntry(eb.startStrField(Component.literal("MCP auth token"), s.getMcpAuthToken() == null ? "" : s.getMcpAuthToken())
                .setDefaultValue("")
                .setTooltip(Component.literal("Bearer token required by the MCP server. Clear to regenerate."))
                .setSaveConsumer(s::setMcpAuthToken)
                .build());

        return builder.build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void toggle(ConfigEntryBuilder eb, ConfigCategory cat, String label, String tooltip,
                               boolean value, Consumer<Boolean> save) {
        cat.addEntry(eb.startBooleanToggle(Component.literal(label), value)
                .setDefaultValue(value)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(save)
                .build());
    }

    private static void number(ConfigEntryBuilder eb, ConfigCategory cat, String label, String tooltip,
                               int value, int def, int min, int max, Consumer<Integer> save) {
        cat.addEntry(eb.startIntField(Component.literal(label), value)
                .setDefaultValue(def)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(save)
                .build());
    }

    private static void number(ConfigEntryBuilder eb, ConfigCategory cat, String label, String tooltip,
                               float value, float def, float min, float max, Consumer<Float> save) {
        cat.addEntry(eb.startFloatField(Component.literal(label), value)
                .setDefaultValue(def)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(save)
                .build());
    }

    private static void number(ConfigEntryBuilder eb, ConfigCategory cat, String label, String tooltip,
                               double value, double def, double min, double max, Consumer<Double> save) {
        cat.addEntry(eb.startDoubleField(Component.literal(label), value)
                .setDefaultValue(def)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(save)
                .build());
    }
}
