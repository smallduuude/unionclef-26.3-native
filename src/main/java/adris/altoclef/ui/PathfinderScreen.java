package adris.altoclef.ui;

import adris.altoclef.AltoClef;
import adris.altoclef.Settings;
import adris.altoclef.control.BaritoneBridge;
import adris.altoclef.util.helpers.ConfigHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/**
 * Pathfinder screen: pick the movement engine and drive it without memorising commands.
 *
 * <p>Two engines are available. <b>Tungsten</b> is this mod's built-in pathfinder (labelled
 * <i>buggy</i> honestly). <b>Baritone</b> is the separate baritone mod, driven through its own
 * {@code #} chat commands ({@code #goto}, {@code #set}). This screen routes the same actions to
 * whichever engine is selected, so the two can be compared on the same route.
 *
 * <p>Everything is dispatched the way a human would type it into chat, so the engines see ordinary
 * commands: altoclef commands go through the command executor, baritone ones as chat.
 */
public final class PathfinderScreen extends Screen {

    private static final Component TITLE = Component.literal("Pathfinder");
    private static final Component SUBTITLE =
            Component.literal("Choose an engine. Baritone (the separate mod) is the default; Tungsten is this mod's port of it.");
    private static final Component GOTO_HINT = Component.literal("Go to (coordinates)");
    private static final Component SET_HINT = Component.literal("Baritone settings  (#set <name> <value>) \u2014 fields show the value in force");

    private final Screen parent;
    private Component status = Component.empty();

    private EditBox xField, yField, zField;
    /** Baritone setting name -> value field. The field's placeholder IS the setting name. */
    private final String[] baritoneSettings = {
            "freeLook", "smoothLook",
            "blockBreakAdditionalPenalty", "blockPlacementPenalty",
            "allowBreak", "allowPlace",
            "blockReachDistance", "allowParkour",
    };
    private EditBox[] settingFields;
    private Button tungstenButton, baritoneButton;

    public PathfinderScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    private static boolean baritoneLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded("baritone");
        } catch (Throwable t) {
            return false;
        }
    }

    private static Settings settings() {
        AltoClef mod = AltoClef.getInstance();
        return mod == null ? null : mod.getModSettings();
    }

    @Override
    protected void init() {
        final int panelW = 408;
        final int left = this.width / 2 - panelW / 2;
        final int gap = 6;
        final int rows = 10;
        final int top = 4;
        final int rowH = Math.max(16, Math.min(24, (this.height - 2 * top) / rows));
        final int btnH = Math.max(14, rowH - 2);
        int y = top;

        this.addRenderableWidget(new StringWidget(left, y, panelW, rowH, TITLE, this.font));
        y += rowH;
        this.addRenderableWidget(new StringWidget(left, y, panelW, rowH, SUBTITLE, this.font));
        y += rowH;

        // ── Engine selector (2 across) ──────────────────────────────────────
        final int half = (panelW - gap) / 2;
        this.tungstenButton = Button.builder(tungstenLabel(), b -> selectEngine("Tungsten"))
                .bounds(left, y, half, btnH).build();
        this.baritoneButton = Button.builder(baritoneLabel(), b -> selectEngine("Baritone"))
                .bounds(left + half + gap, y, half, btnH).build();
        this.addRenderableWidget(this.tungstenButton);
        this.addRenderableWidget(this.baritoneButton);
        y += rowH;

        // ── Go to ───────────────────────────────────────────────────────────
        this.addRenderableWidget(new StringWidget(left, y, panelW, Math.max(10, rowH - 4), GOTO_HINT, this.font));
        y += rowH;
        int xw = 110, yw = 80, zw = 110, gw = panelW - xw - yw - zw - gap * 3;
        this.xField = new EditBox(this.font, left, y, xw, btnH, Component.literal("X"));
        this.yField = new EditBox(this.font, left + xw + gap, y, yw, btnH, Component.literal("Y"));
        this.zField = new EditBox(this.font, left + xw + gap + yw + gap, y, zw, btnH, Component.literal("Z"));
        this.xField.setMaxLength(9); this.yField.setMaxLength(9); this.zField.setMaxLength(9);
        this.addRenderableWidget(this.xField);
        this.addRenderableWidget(this.yField);
        this.addRenderableWidget(this.zField);
        this.addRenderableWidget(Button.builder(Component.literal("Go"), b -> goTo())
                .bounds(left + panelW - gw, y, gw, btnH).build());
        y += rowH;

        // ── Baritone settings ───────────────────────────────────────────────
        this.addRenderableWidget(new StringWidget(left, y, panelW, Math.max(10, rowH - 4), SET_HINT, this.font));
        y += rowH;
        this.settingFields = new EditBox[baritoneSettings.length];
        for (int i = 0; i < baritoneSettings.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = left + col * (half + gap);
            int fy = y + row * rowH;
            EditBox box = new EditBox(this.font, fx, fy, half, btnH, Component.literal(baritoneSettings[i]));
            box.setMaxLength(32);
            // START FROM THE VALUE IN FORCE, so the number you are editing is the number Baritone is
            // actually using. Empty when the setting is unknown to this Baritone build.
            box.setValue(BaritoneBridge.settingValue(baritoneSettings[i]));
            this.settingFields[i] = box;
            this.addRenderableWidget(box);
        }
        y += 4 * rowH;

        this.addRenderableWidget(Button.builder(Component.literal("Apply Baritone settings"), b -> applyBaritoneSettings())
                .bounds(left, y, panelW, btnH).build());
        y += rowH;

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> Minecraft.getInstance().setScreenAndShow(this.parent))
                .bounds(left, y, panelW, btnH).build());

        updateEngineLabels();
    }

    private Component tungstenLabel() {
        return Component.literal(("Tungsten".equals(currentEngine()) ? "\u25B6 " : "") + "Tungsten (buggy)");
    }

    private Component baritoneLabel() {
        String suffix = baritoneLoaded() ? "" : " [not installed]";
        return Component.literal(("Baritone".equals(currentEngine()) ? "\u25B6 " : "") + "Baritone" + suffix);
    }

    /** The engine actually driving: the chosen one, unless it is not installed. */
    private String effectiveEngine() {
        String chosen = currentEngine();
        if (BaritoneBridge.ENGINE.equals(chosen) && !baritoneLoaded()) return "Tungsten";
        return chosen;
    }

    private String currentEngine() {
        Settings s = settings();
        return s == null ? "Tungsten" : s.getPathfinder();
    }

    private void updateEngineLabels() {
        if (this.tungstenButton != null) this.tungstenButton.setMessage(tungstenLabel());
        if (this.baritoneButton != null) this.baritoneButton.setMessage(baritoneLabel());
    }

    private void selectEngine(String engine) {
        Settings s = settings();
        if (s == null) {
            this.status = Component.literal("Mod not ready.");
            return;
        }
        s.setPathfinder(engine);
        ConfigHelper.saveConfig(Settings.SETTINGS_PATH, s);
        updateEngineLabels();
        if ("Baritone".equals(engine) && !baritoneLoaded()) {
            this.status = Component.literal("Baritone is selected, but the baritone mod is not installed.");
        } else {
            this.status = Component.literal("Engine: " + effectiveEngine()
                    + ("Baritone".equals(engine) ? "" : "  (Tungsten is this mod's port of Baritone's planner)"));
        }
    }

    /** Send a baritone/altoclef command exactly as chat would, without closing the screen. */
    private void send(String raw) {
        Minecraft mc = Minecraft.getInstance();
        AltoClef mod = AltoClef.getInstance();
        try {
            if (raw.startsWith("/")) {
                ClientPacketListener conn = mc.getConnection();
                if (conn != null) conn.sendCommand(raw.substring(1));
            } else if (raw.startsWith("@")) {
                if (mod != null && mod.getCommandExecutor() != null) {
                    mod.getCommandExecutor().executeWithPrefix(raw);
                } else {
                    this.status = Component.literal("Mod not ready.");
                    return;
                }
            } else {
                ClientPacketListener conn = mc.getConnection();
                if (conn == null) {
                    this.status = Component.literal("Not in a world.");
                    return;
                }
                conn.sendChat(raw);
            }
        } catch (RuntimeException e) {
            this.status = Component.literal("Failed: " + e.getMessage());
            return;
        }
        this.status = Component.literal("Sent: " + raw);
    }

    private void goTo() {
        if (!AltoClef.inGame()) {
            this.status = Component.literal("Join a world first.");
            return;
        }
        String x = this.xField.getValue().trim();
        String yv = this.yField.getValue().trim();
        String z = this.zField.getValue().trim();
        if (x.isEmpty() || z.isEmpty()) {
            this.status = Component.literal("Enter at least X and Z.");
            return;
        }
        String coords = x + " " + (yv.isEmpty() ? "" : yv + " ") + z;
        if ("Baritone".equals(currentEngine())) {
            if (!baritoneLoaded()) {
                this.status = Component.literal("Baritone is not installed.");
                return;
            }
            send("#goto " + coords);
        } else {
            send("@goto " + coords);
        }
    }

    private void applyBaritoneSettings() {
        if (!AltoClef.inGame()) {
            this.status = Component.literal("Join a world first.");
            return;
        }
        if (!baritoneLoaded()) {
            this.status = Component.literal("Baritone is not installed.");
            return;
        }
        int sent = 0;
        for (int i = 0; i < baritoneSettings.length; i++) {
            String value = this.settingFields[i].getValue().trim();
            if (value.isEmpty()) continue;
            send("#set " + baritoneSettings[i] + " " + value);
            sent++;
        }
        this.status = Component.literal(sent == 0
                ? "Nothing to apply (fill a setting value first)."
                : ("Sent " + sent + " baritone setting(s)."));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            Minecraft.getInstance().setScreenAndShow(this.parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (!this.status.getString().isEmpty()) {
            graphics.text(this.font, this.status, this.width / 2 - 204, this.height - 12, 0xFFAAAAAA, true);
        }
    }
}
