package adris.altoclef.multiversion;

import net.minecraft.client.Minecraft;

public class InGameHudVer {

    public static boolean shouldShowDebugHud() {
        return Minecraft.getInstance().getDebugOverlay().showDebugScreen();
    }

}
