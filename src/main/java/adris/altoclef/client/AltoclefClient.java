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
            new KeyMapping("key.unionclef.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_K, KeyMapping.Category.MISC));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
        });
    }

    private static void openSettings(Minecraft client) {
        client.setScreenAndShow(new UnionClefScreen());
    }
}
