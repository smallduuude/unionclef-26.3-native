package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.butler.ButlerConfig;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.multiplayer.RejoinEvent;
import adris.altoclef.mixins.DeathScreenAccessor;
import adris.altoclef.multiversion.ConnectScreenVer;
import adris.altoclef.tasks.fix.StuckFixingTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.multiplayer.LobbyTask;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.agent.Pipeline;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import adris.altoclef.util.time.TimerReal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerInput;
import org.apache.commons.lang3.ArrayUtils;

public class GameMenuTaskChain extends SingleTaskChain {

    /** Steps of the death-disconnect-reconnect path; read over py4j in placeStats(). */
    public static volatile int gmDisconnect, gmReconnectSet, gmGuardBlocked, gmConnectCalled;

    private final TimerReal _reconnectTimer = new TimerReal(10);
    private final TimerReal _kickScreenTimer = new TimerReal(7);
    private boolean _waitingOnKickScreen = false;
    private ServerData _prevServerEntry = null;
    public ServerData _connectOverrideServerEntry = null;
    private boolean _reconnecting = false;
    private Class _prevScreen = null;
    public static boolean _needUnStuckFix = false;
    public static boolean _needDisconnect = false;
    public static boolean _needToStopTasksOnReconnect = false;
    public static boolean _reJoinAfterDisconnect = false;
    public static boolean NeedtoStopTasksOnDeath = false;
    public static String ServerIp = "";
    private final TimerReal _commandDelayTimer = new TimerReal(2);
    public static int reconnectAttemps = -1;

    public GameMenuTaskChain(TaskRunner runner) {
        super(runner);
    }

    private boolean shouldAutoReconnect() {
        AltoClef mod = AltoClef.getInstance();
        return mod != null && mod.getModSettings().isAutoReconnect();
    }

    @Override
    protected void onStop() {
    }

    @Override
    public void onInterrupt(TaskChain other) {
    }

    @Override
    protected void onTick() {
    }

    public String getServerIp() {
        return ServerIp;
    }

    public static boolean isMinigamePipeline(Pipeline pipeline) {
        return pipeline.equals(Pipeline.SkyWars)
                || pipeline.equals(Pipeline.MurderMystery)
                || pipeline.equals(Pipeline.BedWars)
                || pipeline.equals(Pipeline.SkyPvP);
    }

    private boolean _clicked = false;
    private boolean _joined = false;
    public TimerReal clickTimer = new TimerReal(0.7);
    public TimerReal _reloadInfoSenderTimer = new TimerReal(10);
    private boolean _infoSenderLoaded = true;

    // Timers for different click operations
    private final TimerReal _slotClickTimer = new TimerReal(0.5);
    private final TimerReal _worldJoinTimer = new TimerReal(3);
    private final TimerReal _minigameButtonTimer = new TimerReal(1.0);
    private final TimerReal _lobbyButtonTimer = new TimerReal(1.0);
    private final TimerReal _mouseClickTimer = new TimerReal(0.3);

    public boolean rejoinEventPublished = false;

    @Override
    public float getPriority() {
        AltoClef mod = AltoClef.getInstance();
        if (mod == null) return Float.NEGATIVE_INFINITY;

        if (!AltoClef.inGame()) {
            rejoinEventPublished = false;
            _worldJoinTimer.reset();
        } else {
            if (_worldJoinTimer.elapsed() && !rejoinEventPublished) {
                EventBus.publish(new RejoinEvent());
                rejoinEventPublished = true;
            }
        }

        // Python sender auto-reload
        if (mod.getModSettings().shouldReloadInfoSender() && _reloadInfoSenderTimer.elapsed()) {
            if (!mod.getInfoSender().IsCallbackServerStarted()) {
                if (_infoSenderLoaded) {
                    Debug.logMessage("[InfoSender] Python callback connection lost. Retry...");
                    _infoSenderLoaded = false;
                }
                mod.reloadPythonSender();
            } else {
                if (!_infoSenderLoaded) {
                    Debug.logMessage("[InfoSender] Python callback connection established!");
                    _infoSenderLoaded = true;
                }
            }
            _reloadInfoSenderTimer.reset();
        }

        // Don't touch autojoin chest-menus while a SERVER SWITCH is pending (@connect / reconnect):
        // clicking a menu on the CURRENT server bounced the agent back into its lobby instead of
        // actually switching servers ("@connect returns ok but client stays" bug). Let the
        // disconnect+reconnect (onTickPost) run unobstructed.
        boolean switchPending = _needDisconnect || _reconnecting || _connectOverrideServerEntry != null;
        if (ButlerConfig.getInstance().autoJoin && !switchPending) {
            if (ContainerType.screenHandlerMatches(ContainerType.CHEST)) {
                Component title = Minecraft.getInstance().gui.screen() != null
                        ? Minecraft.getInstance().gui.screen().getTitle() : null;

                if (title != null && title.getString() != null) {
                    String t = title.getString().toLowerCase();

                    // Check if this is an autojoin menu (server select, minigames, mode select).
                    // Cyrillic literals are Russian server menu/item titles, kept verbatim:
                    // "выбор сервера" = server select, "мини-игры" = minigames, "выбор режима" /
                    // "выбери режим" = mode select, "скайварс" / "бедварс" / "МардерМистери" = game names.
                    boolean isAutoJoinMenu = t.contains("выбор сервера") || t.contains("мини-игры")
                            || t.contains("выбор режима") || t.contains("выбери режим");

                    if (isAutoJoinMenu) {
                        String[] MinigamesTitles = new String[]{"мини-игры", "МИНИ-ИГРЫ", "МИНИИГРЫ"};

                        String[] ClickTitles;
                        switch (AltoClef.getPipeline()) {
                            case SkyWars:
                                ClickTitles = new String[]{"SkyWars", "skywars", "скайварс", "скай-варс"};
                                break;
                            case BedWars:
                                ClickTitles = new String[]{"BedWars", "bedwars", "бедварс"};
                                break;
                            case MurderMystery:
                                ClickTitles = new String[]{"MurderMystery", "murdermystery", "МардерМистери", "Murder"};
                                break;
                            default:
                                ClickTitles = null;
                                break;
                        }
                        if (ClickTitles != null && _worldJoinTimer.elapsed()) {
                            Slot slot = ItemHelper.getCustomItemSlot(mod, ArrayUtils.addAll(MinigamesTitles));

                            if (slot != null && _slotClickTimer.elapsed()) {
                                mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP);
                                _slotClickTimer.reset();
                                return 90;
                            } else {
                                slot = ItemHelper.getCustomItemSlot(mod, ArrayUtils.addAll(MinigamesTitles, ClickTitles));
                                if (slot != null && _slotClickTimer.elapsed()) {
                                    mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP);
                                    _slotClickTimer.reset();
                                    _clicked = true;
                                    return 90;
                                }
                            }
                            // Keep high priority while autojoin menu is open (SW/BW/MM),
                            // prevents PlayerInteractionFixChain from closing the screen
                            return 90;
                        }
                        // SkyPvP handles its own menu in SkyPvpTask — don't block it with priority 90
                    }
                }
            }

            boolean isMinigame = isMinigamePipeline(AltoClef.getPipeline());
            if (isMinigame) {
                if (clickTimer.elapsed() && _worldJoinTimer.elapsed()) {
                    // Server item names: server select / lobby select / mode select; then
                    // "new game" / "start game" / "quick play (right click)".
                    if (_lobbyButtonTimer.elapsed() && ItemHelper.clickCustomItem(mod, "Выбор сервера", "Выбор лобби", "Выбор режима")) {
                        clickTimer.reset();
                        _lobbyButtonTimer.reset();
                    }
                    if (_minigameButtonTimer.elapsed() && ItemHelper.clickCustomItem(mod, "новая игра", "начать игру", "быстро играть (пкм)")) {
                        clickTimer.reset();
                        _minigameButtonTimer.reset();
                    }
                }
            }
        }

        if (_clicked) {
            _joined = true;
            _clicked = false;
        }
        setTask(null);
        return Float.NEGATIVE_INFINITY;
    }

    public void onTickPost(AltoClef mod) {
        Screen screen = Minecraft.getInstance().gui.screen();

        if (AltoClef.inGame()) {
            reconnectAttemps = 0;
            _waitingOnKickScreen = false;
            _prevServerEntry = Minecraft.getInstance().getCurrentServer();
            if (_prevServerEntry != null) {
                ServerIp = _prevServerEntry.ip.toString();
            }
            if (mod.getPlayer().hasEffect(MobEffects.INVISIBILITY)) {
                if (mod.getPlayer().getEffect(MobEffects.INVISIBILITY).getAmplifier() >= 3) {
                    if (NeedtoStopTasksOnDeath) {
                        mod.getMessageSender().enqueueChat("/hub", MessagePriority.TIMELY);
                        NeedtoStopTasksOnDeath = false;
                        _commandDelayTimer.reset();
                        if (_commandDelayTimer.elapsed()) {
                            _commandDelayTimer.reset();
                            Debug.logMessage("Bailing out to /hub");
                        }
                    }
                }
            }
        }

        if (screen instanceof DisconnectedScreen disconnectedScreen) {
            if (!_waitingOnKickScreen) {
                Component reason = disconnectedScreen.getTitle();
                String kickMessage = reason != null ? reason.getString() : "UNKNOWN REASON";
                if (kickMessage == null) kickMessage = "UNKNOWN REASON";

                Debug.logMessage("Kicked from server with reason: " + kickMessage);

                if (shouldAutoReconnect()) {
                    double delay = mod.getModSettings().getReconnectDelaySec();
                    Debug.logMessage("RECONNECTING: Waiting " + delay + "s on kick screen...");
                    _kickScreenTimer.setInterval(delay);
                    _kickScreenTimer.reset();
                    _waitingOnKickScreen = true;
                } else {
                    mod.cancelUserTask();
                }
            } else if (_kickScreenTimer.elapsed()) {
                Debug.logMessage("RECONNECTING: Going to Multiplayer Screen");
                _waitingOnKickScreen = false;
                gmReconnectSet++; _reconnecting = true;
                _reconnectTimer.reset();
                Minecraft.getInstance().setScreenAndShow(new JoinMultiplayerScreen(new TitleScreen()));
            }
        } else if (_needUnStuckFix) {
            if (AltoClef.inGame()) {
                ServerData srv = Minecraft.getInstance().getCurrentServer();
                if (srv != null) {
                    Debug.logMessage("Starting UNSTUCK FIX >> server=" + srv.ip.toString());
                    _prevServerEntry = srv;
                }

                Debug.logMessage("OPEN GAME MENU");
                Minecraft client = Minecraft.getInstance();
                client.gui.setScreen(new PauseScreen(true));
                disconnect(client);
                SelectWorldScreen worldScreen = new SelectWorldScreen(new TitleScreen());
                Minecraft.getInstance().setScreenAndShow(worldScreen);
                Debug.logMessage("worldScreen.isMouseOver() " + worldScreen.isMouseOver(0, 0));

                double x = worldScreen.width / 2.0 - 154;
                double y = worldScreen.height - 52;

                if (_mouseClickTimer.elapsed()) {
                    net.minecraft.client.input.MouseButtonEvent worldClick = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0));
                    worldScreen.mouseClicked(worldClick, false);
                    worldScreen.mouseReleased(worldClick);
                
                    if (worldScreen.getChildAt(x, y).isPresent()) {
                        GuiEventListener hoveredElement = worldScreen.getChildAt(x, y).get();
                        net.minecraft.client.input.MouseButtonEvent elementClick = new net.minecraft.client.input.MouseButtonEvent(0, 0, new net.minecraft.client.input.MouseButtonInfo(0, 0));
                        hoveredElement.mouseClicked(elementClick, false);
                        hoveredElement.mouseReleased(elementClick);
                        mod.cancelUserTask();
                        ServerData finalSrv = srv;
                        Runnable doOnStuckFixFinish = new Thread(() -> {
                            Minecraft clientt = Minecraft.getInstance();
                            clientt.setScreenAndShow(new PauseScreen(true));
                            Debug.logMessage("[STUCKFIX] DISCONNECT STAGE 2");
                            disconnect(clientt);
                            mod.cancelUserTask();
                            mod.runUserTask(new GetToXZTask(0, 0));
                            Debug.logMessage("[STUCKFIX] SET MP SCREEN");
                            Minecraft.getInstance().setScreenAndShow(new JoinMultiplayerScreen(new TitleScreen()));
                            Debug.logMessage("[STUCKFIX] RECONNECT TO SERVER");
                            if (_prevServerEntry == null) {
                                _prevServerEntry = finalSrv;
                            }
                            gmReconnectSet++; _reconnecting = true;
                            _reconnectTimer.reset();
                        });
                        mod.runUserTask(new StuckFixingTask(), doOnStuckFixFinish);
                    }
                    _mouseClickTimer.reset();
                }
                _needUnStuckFix = false;
            }
        } else if (_needDisconnect) {
            if (AltoClef.inGame()) {
                ServerData srv;
                if (_connectOverrideServerEntry != null) {
                    srv = _connectOverrideServerEntry;
                    _connectOverrideServerEntry = null;
                } else {
                    srv = Minecraft.getInstance().getCurrentServer();
                }

                if (srv != null) {
                    Debug.logMessage("Starting DISCONNECT CHAIN >> server=" + srv.ip.toString());
                    _prevServerEntry = srv;
                }

                if (_prevServerEntry != null && _reJoinAfterDisconnect) {
                    gmReconnectSet++; _reconnecting = true;
                    _reconnectTimer.reset();
                    _reJoinAfterDisconnect = false;
                }

                Debug.logMessage("[DISCONNECT CHAIN] OPEN GAME MENU");
                Minecraft client = Minecraft.getInstance();
                client.gui.setScreen(new PauseScreen(true));
                screen = client.gui.screen();
                client.getReportingContext().draftReportHandled(client, screen, _innerDisconnect(client), true);
                // 1.21.11: an altoclef-spawned MultiplayerScreen crashes when ticked
                // before init (null server-list/lan fields). TitleScreen is safe.
                Minecraft.getInstance().setScreenAndShow(new TitleScreen());
                _needDisconnect = false;
            }
        } else if (screen instanceof JoinMultiplayerScreen || screen instanceof TitleScreen) {
            if (_reconnecting && _reconnectTimer.elapsed()) {
                // 1.21.11: connecting before the MultiplayerScreen ran init() crashes
                // the client (removed() NPEs on the null server list widget). Wait until
                // the screen is initialized (has children) before connecting.
                if (screen.children().isEmpty()) {
                    gmGuardBlocked++;
                    return;
                }
                reconnectAttemps += 1;
                Debug.logMessage("RECONNECTING: Going to reconnect");
                _reconnecting = false;

                if (_prevServerEntry == null) {
                    Debug.logWarning("Failed to re-connect to server, no server entry cached.");
                } else {
                    Debug.logMessage("RECONNECTING: Connect to " + _prevServerEntry.ip.toString());
                    Minecraft client = Minecraft.getInstance();
                    try {
                        gmConnectCalled++;
                        ConnectScreenVer.connect(screen, client, ServerAddress.parseString(_prevServerEntry.ip), _prevServerEntry, false);
                    } catch (Throwable t) {
                        Debug.logWarning("Reconnect threw " + t.getClass().getSimpleName() + " — retry next cycle instead of crashing");
                        gmReconnectSet++; _reconnecting = true;
                        _reconnectTimer.reset();
                    }
                    if (_needToStopTasksOnReconnect) {
                        mod.cancelUserTask();
                        _needToStopTasksOnReconnect = false;
                    }
                }
            }
        }

        if (screen != null) {
            _prevScreen = screen.getClass();
        }
    }

    /**
     * Leave deliberately and cancel queued menu reconnects. Call on the client
     * thread. Internal server switches still use _innerDisconnect so their
     * scheduled reconnect is preserved.
     */
    public void disconnectFromServer(Minecraft client) {
        // A ConnectScreen owns its own network future. Replacing its screen
        // does not cancel that future; refuse to acknowledge an offline exit
        // while it can still join. The caller must keep defence active.
        if (client.gui.screen() instanceof net.minecraft.client.gui.screens.ConnectScreen) {
            throw new IllegalStateException("Connection still in progress; logout not confirmed");
        }
        _reconnecting = false;
        _waitingOnKickScreen = false;
        _needDisconnect = false;
        _reJoinAfterDisconnect = false;
        _needUnStuckFix = false;
        _needToStopTasksOnReconnect = false;
        _connectOverrideServerEntry = null;
        _innerDisconnect(client).run();
        // A kick screen would schedule a fresh automatic retry on the next tick.
        client.gui.setScreen(new TitleScreen());
    }

    public Runnable _innerDisconnect(Minecraft client) {
        return () -> {
            // WHICH PART OF THE RECONNECT NEVER HAPPENS? The bot leaves the server on death by
            // design, and then sits on the title screen for the rest of the run -- inGame=false,
            // every poll returning zeros, which produced six separate "bugs" this session.
            gmDisconnect++;
            if (client.level != null) {
                boolean bl = client.isLocalServer();
                client.level.disconnect(Component.empty());
                if (bl) {
                    client.disconnectFromWorld(Component.empty());
                } else {
                    client.disconnectFromWorld(Component.empty());
                }
            }
        };
    }

    public void disconnect(Minecraft client) {
        if (AltoClef.inGame() && client.level != null) {
            Debug.logMessage("DISCONNECT");
            boolean bl = client.isLocalServer();
            client.level.disconnect(Component.empty());
            if (bl) {
                client.disconnectFromWorld(Component.nullToEmpty("DEATH"));
            } else {
                client.disconnectFromWorld(Component.empty());
            }
        } else {
            Debug.logMessage("Tried to disconnect >>> world is null or not in game");
        }
    }

    public void connectToServer(String ip) {
        _connectOverrideServerEntry = new ServerData("CustomServer", ip, ServerData.Type.OTHER);
        if (AltoClef.inGame()) {
            _reJoinAfterDisconnect = true;
            _needDisconnect = true;
        } else {
            _prevServerEntry = _connectOverrideServerEntry;
            Screen screen = Minecraft.getInstance().gui.screen();
            Debug.logMessage("RECONNECT CHAIN: Not in game; Connecting to server from menu: " + ip);
            if (!(screen instanceof JoinMultiplayerScreen || screen instanceof TitleScreen)) {
                Debug.logMessage("RECONNECT CHAIN: Parking on TitleScreen before connect to " + ip);
                Minecraft.getInstance().setScreenAndShow(new TitleScreen());
            }
            _needDisconnect = false;
            gmReconnectSet++; _reconnecting = true;
            _reconnectTimer.reset();
        }
    }

    public static void StuckFixActivate() {
        _needUnStuckFix = true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
    }

    @Override
    public String getName() {
        return "Game Menu Chain";
    }
}
