package adris.altoclef.util.helpers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

public class InputHelper {

    public static boolean isKeyPressed(int code) {
        // 26.2 takes the window explicitly; 26.3 drops it and reads the global window.
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), code);
    }
}
