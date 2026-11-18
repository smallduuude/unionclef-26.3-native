package adris.altoclef.ui;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
 * UnionClef hub screen: get any item by name, click a quick task, or run a command.
 *
 * <p>The point of this screen is that a player who has never read the command list can still drive
 * the bot. The item picker is the important part: type part of an item's natural name and a live
 * suggestion row shows matching catalogue items; pick one, set an amount, press <b>Get</b> — no
 * command syntax required. Everything else is one-click tasks, with the command box for anyone who
 * wants the full surface.
 *
 * <p>The whole layout is computed from the window height so it always fits: every element sits on
 * one of eight rows whose height is derived from the available height. A fixed layout used to run
 * off the bottom on a small window, which silently puts buttons out of reach.
 *
 * <p>Opened by the "UnionClef" keybind. Commands go through the same paths a human uses: a leading
 * {@code /} is sent as a server command, the mod's own prefix (default {@code @}) runs through
 * {@code CommandExecutor}, anything else is sent as chat — so the server sees an ordinary player
 * message, not a synthetic packet sequence.
 */
public class UnionClefScreen extends Screen {

    private static final Component TITLE = Component.literal("UnionClef");
    private static final Component SUBTITLE = Component.literal("Pick an item or a task below.");

    /** One-click tasks, as {@code {label, command-without-prefix}} pairs. */
    private static final String[][] QUICK_TASKS = {
            {"Kill Hostiles", "hero"},
            {"Self-Care", "selfcare"},
            {"Get Food", "food 220"},
            {"Beat the Game", "gamer"},
            {"Go to Nether", "goto NETHER"},
            {"Deposit Items", "deposit"},
            {"Pause", "pause"},
            {"Stop", "stop"},
    };

    /** Shown in the suggestion row before the user has typed anything. */
    private static final List<String> DEFAULT_SUGGESTIONS =
            List.of("diamond", "iron_ingot", "gold_ingot", "coal", "cobblestone");

    private static final int SUGGESTIONS = 5;

    private EditBox itemField;
    private EditBox amountField;
    private EditBox commandInput;
    private final Button[] suggestionButtons = new Button[SUGGESTIONS];
    private final String[] suggestionIds = new String[SUGGESTIONS];

    private Component status = Component.empty();

    public UnionClefScreen() {
        super(TITLE);
    }

    /** Number of vertical rows the layout is divided into. See {@link #init()}. */
    private static final int ROWS = 8;

    @Override
    protected void init() {
        final int panelW = 408;
        final int left = this.width / 2 - panelW / 2;
        final boolean inGame = AltoClef.inGame();
        final int gap = 6;

        // Fit everything into the window: eight rows, each as tall as the height allows (clamped so
        // buttons stay clickable and rows never overflow the screen).
        final int top = 4;
        final int rowH = Math.max(16, Math.min(24, (this.height - 2 * top) / ROWS));
        final int btnH = Math.max(14, rowH - 2);
        int y = top;

        this.addRenderableWidget(new StringWidget(left, y, panelW, rowH, TITLE, this.font));
        y += rowH;
        this.addRenderableWidget(new StringWidget(left, y, panelW, rowH, SUBTITLE, this.font));
        y += rowH;

        // ── Row 3: get an item — name (with live suggestions) + amount + Get ──
        this.itemField = new EditBox(this.font, left, y, 236, btnH, Component.literal("Item name"));
        this.itemField.setMaxLength(64);
        this.addRenderableWidget(this.itemField);
        this.amountField = new EditBox(this.font, left + 242, y, 64, btnH, Component.literal("Amount"));
        this.amountField.setMaxLength(4);
        this.amountField.setValue("1");
        this.addRenderableWidget(this.amountField);
        Button getButton = Button.builder(Component.literal("Get"), b -> runGet())
                .bounds(left + 312, y, 96, btnH).build();
        getButton.active = inGame;
        this.addRenderableWidget(getButton);
        y += rowH;

        // ── Row 4: item suggestions (relabelled every tick in updateSuggestions) ──
        final int sugW = (panelW - gap * (SUGGESTIONS - 1)) / SUGGESTIONS;
        for (int i = 0; i < SUGGESTIONS; i++) {
            final int idx = i;
            Button b = Button.builder(Component.empty(), bt -> selectSuggestion(idx))
                    .bounds(left + i * (sugW + gap), y, sugW, btnH).build();
            b.visible = false;
            this.suggestionButtons[i] = b;
            this.addRenderableWidget(b);
        }
        y += rowH;

        // ── Rows 5-6: quick tasks (4 columns x 2 rows) ──
        final int cols = 4;
        final int colW = (panelW - gap * (cols - 1)) / cols;
        for (int i = 0; i < QUICK_TASKS.length; i++) {
            int bx = left + (i % cols) * (colW + gap);
            int by = y + (i / cols) * rowH;
            String label = QUICK_TASKS[i][0];
            String command = QUICK_TASKS[i][1];
            Button button = Button.builder(Component.literal(label), b -> runQuickTask(command))
                    .bounds(bx, by, colW, btnH).build();
            // Tasks need a loaded world; grey them out on the title screen instead of erroring.
            button.active = inGame;
            this.addRenderableWidget(button);
        }
        y += 2 * rowH;

        // ── Row 7: manual command box ──
        this.commandInput = new EditBox(this.font, left, y, 300, btnH, Component.literal("Command"));
        this.commandInput.setMaxLength(256);
        this.addRenderableWidget(this.commandInput);
        this.addRenderableWidget(Button.builder(Component.literal("Run"), b -> runCommand())
                .bounds(left + 306, y, 102, btnH).build());
        y += rowH;

        // ── Row 8: Help / Pathfinder / Settings / Close ──
        final int bw = (panelW - gap * 3) / 4;
        this.addRenderableWidget(Button.builder(Component.literal("Help"), b -> runQuickTask("help"))
                .bounds(left, y, bw, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Pathfinder..."),
                        b -> Minecraft.getInstance().setScreenAndShow(new PathfinderScreen(this)))
                .bounds(left + (bw + gap), y, bw, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Settings..."),
                        b -> Minecraft.getInstance().setScreenAndShow(SettingsScreen.create(this)))
                .bounds(left + 2 * (bw + gap), y, bw, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(left + 3 * (bw + gap), y, bw, btnH).build());

        this.setInitialFocus(this.itemField);
        updateSuggestions();
    }

    @Override
    public void tick() {
        super.tick();
        updateSuggestions();
    }

    /**
     * Fill the suggestion row from what is typed: matching catalogue items, or a short popular list
     * when the field is empty. Runs every tick so the row tracks the text box as it is edited; the
     * widgets are created once in {@link #init()} and only relabelled here.
     */
    private void updateSuggestions() {
        if (this.itemField == null) return;
        String filter = this.itemField.getValue().trim().toLowerCase(Locale.ROOT);

        List<String> names = new ArrayList<>();
        if (filter.isEmpty()) {
            names.addAll(DEFAULT_SUGGESTIONS);
        } else {
            Collection<String> all = TaskCatalogue.resourceNames();
            if (all != null) {
                for (String name : all) {
                    if (name.toLowerCase(Locale.ROOT).contains(filter)) {
                        names.add(name);
                    }
                }
            }
            Collections.sort(names);
        }

        for (int i = 0; i < SUGGESTIONS; i++) {
            Button button = this.suggestionButtons[i];
            if (button == null) continue;
            if (i < names.size()) {
                String id = names.get(i);
                this.suggestionIds[i] = id;
                button.setMessage(Component.literal(prettyName(id)));
                button.visible = true;
            } else {
                this.suggestionIds[i] = null;
                button.visible = false;
            }
        }
    }

    /** Select a suggestion into the item field without running it, so the amount can be set. */
    private void selectSuggestion(int index) {
        if (index < 0 || index >= this.suggestionIds.length) return;
        String id = this.suggestionIds[index];
        if (id != null && this.itemField != null) {
            this.itemField.setValue(id);
            this.setFocused(this.amountField);
        }
    }

    /** Build and run a {@code get <item> <amount>} command from the picker. */
    private void runGet() {
        if (!AltoClef.inGame()) {
            this.status = Component.literal("Join a world first.");
            return;
        }
        String id = this.itemField == null ? "" : this.itemField.getValue().trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            this.status = Component.literal("Type or pick an item first.");
            return;
        }
        if (!TaskCatalogue.taskExists(id)) {
            this.status = Component.literal("Unknown item: " + id);
            return;
        }
        int amount = 1;
        String rawAmount = this.amountField == null ? "" : this.amountField.getValue().trim();
        if (!rawAmount.isEmpty()) {
            try {
                amount = Math.max(1, Integer.parseInt(rawAmount));
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }
        execute(prefix() + "get " + id + " " + amount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            if (this.getFocused() == this.commandInput) {
                runCommand();
            } else {
                runGet();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    /** Run one of the plain-English task buttons, which wrap a mod command. */
    private void runQuickTask(String command) {
        if (!AltoClef.inGame()) {
            this.status = Component.literal("Join a world first.");
            return;
        }
        execute(prefix() + command);
    }

    private void runCommand() {
        String raw = this.commandInput.getValue().trim();
        if (raw.isEmpty()) {
            return;
        }
        execute(raw);
    }

    /**
     * Dispatch a line exactly the way a player would: {@code /}-prefixed lines go to the server,
     * mod-prefixed lines to the command executor, everything else to chat.
     */
    private void execute(String raw) {
        Minecraft mc = Minecraft.getInstance();
        AltoClef mod = AltoClef.getInstance();

        try {
            if (raw.startsWith("/")) {
                ClientPacketListener conn = mc.getConnection();
                if (conn != null) {
                    conn.sendCommand(raw.substring(1));
                }
            } else if (raw.startsWith(prefix())) {
                if (mod != null && mod.getCommandExecutor() != null) {
                    mod.getCommandExecutor().executeWithPrefix(raw);
                } else {
                    this.status = Component.literal("Mod not ready.");
                    return;
                }
            } else {
                ClientPacketListener conn = mc.getConnection();
                if (conn != null) {
                    conn.sendChat(raw);
                }
            }
        } catch (RuntimeException e) {
            this.status = Component.literal("Failed: " + e.getMessage());
            return;
        }

        if (this.commandInput != null) {
            this.commandInput.setValue("");
        }
        // Close so the game keeps running with the command already dispatched.
        this.onClose();
    }

    /** The configured command prefix, falling back to {@code @} before settings load. */
    private String prefix() {
        AltoClef mod = AltoClef.getInstance();
        if (mod != null && mod.getModSettings() != null) {
            String p = mod.getModSettings().getCommandPrefix();
            if (p != null && !p.isBlank()) {
                return p;
            }
        }
        return "@";
    }

    /** {@code oak_log} -> {@code Oak Log}, the readable form of a catalogue name. */
    private static String prettyName(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.length() == 0 ? id : sb.toString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Dim the world behind the widgets (the base only renders renderables).
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (!this.status.getString().isEmpty()) {
            graphics.text(this.font, this.status, this.width / 2 - 204, this.height - 12, 0xFFFF5555, true);
        }
    }
}
