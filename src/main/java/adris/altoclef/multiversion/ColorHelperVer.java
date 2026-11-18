package adris.altoclef.multiversion;

import net.minecraft.util.ARGB;

public class ColorHelperVer {
    public static int getAlpha(int color) {
        return ARGB.alpha(color);
    }
    public static int getRed(int color) {
        return ARGB.red(color);
    }
    public static int getGreen(int color) {
        return ARGB.green(color);
    }
    public static int getBlue(int color) {
        return ARGB.blue(color);
    }
    public static int getArgb(int a, int r, int g, int b) {
        return ARGB.color(a, r, g, b);
    }
}
