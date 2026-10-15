package adris.altoclef.multiversion;

import net.minecraft.client.Minecraft;

public class OptionsVer {


    public static void setGamma(double value) {
        Minecraft.getInstance().options.gamma().set(value);
    }

    public static void setAutoJump(boolean value) {
        Minecraft.getInstance().options.autoJump().set(value);
    }

}
