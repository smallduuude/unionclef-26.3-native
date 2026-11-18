package adris.altoclef.client;

import adris.altoclef.ui.UnionClefScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Client-only entrypoint. Owns the "UnionClef Settings" keybind and opens the Cloth Config
 * settings screen. Kept out of the main {@code ModInitializer} so a dedicated server never
 * touches client keybind code.
 */
public class AltoclefClient implements ClientModInitializer {

    /** Default: O. Rebindable in Options &gt; Controls &gt; Miscellaneous. */
    public static final KeyMapping OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.unionclef.settings", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, KeyMapping.Category.MISC));

    /** One-shot guard so a broken first-tick init reports once, not 20 times a second. */
    private static boolean initFailureReported = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Quick-play / direct join skips the title screen, so the title-screen init hook never
            // fires; finish initialising on the first tick in a world instead. See
            // AltoClef#ensureInitialized for the full explanation.
            if (client.player == null) {
                return;
            }
            try {
                adris.altoclef.AltoClef mod = adris.altoclef.AltoClef.getInstance();
                if (mod != null && !mod.isInitialized()) {
                    mod.ensureInitialized();
                }
            } catch (Throwable t) {
                // A failed first-tick init must not spam a stack trace every tick.
                if (!initFailureReported) {
                    initFailureReported = true;
                    t.printStackTrace();
                }
            }
            while (OPEN_SETTINGS.consumeClick()) {
                openSettings(client);
            }
        });

        // ⛔ LET THE JVM EXIT. Every quit was writing a crash report
        // ("Client shutdown from post-main / Watchdog") because two sets of NON-DAEMON threads
        // outlived the window, and a JVM will not exit while one lives:
        //
        //   py4j.GatewayServer     -- blocked in ServerSocket.accept; altoclef only ever shut it
        //                             down from the dev harness, never on a real quit
        //   Baritone's thread pool -- nothing called destroyBaritone
        //
        // Both are released here. This is the only place that knows the client is going away.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                adris.altoclef.AltoClef mod = adris.altoclef.AltoClef.getInstance();
                if (mod != null) mod.stopPythonSender();
            } catch (Throwable ignored) {
                // a shutdown that throws is worse than one that does nothing
            }
            adris.altoclef.control.BaritoneBridge.shutdown();

            // LAST-RESORT EXIT GUARD. Baritone's shared cached-world executor is not released by
            // destroyBaritone; if its non-daemon workers outlive the window the JVM does not exit and
            // Minecraft's client-shutdown watchdog writes a crash report (measured 2026-10-22). The
            // reflective shutdown above is best effort and depends on Baritone's obfuscated name; this
            // daemon thread gives every other shutdown path a few seconds to finish, then halts the
            // process. It dies with a normal exit, so it costs nothing when the shutdown is clean.
            Thread forceExit = new Thread(() -> {
                try {
                    Thread.sleep(6000L);
                } catch (InterruptedException ignored) {
                }
                Runtime.getRuntime().halt(0);
            }, "unionclef-forced-exit");
            forceExit.setDaemon(true);
            forceExit.start();
        });
    }

    private static void openSettings(Minecraft client) {
        client.setScreenAndShow(new UnionClefScreen());
    }
}
