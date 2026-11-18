package kaptainwutax.tungsten.helpers;

import net.minecraft.world.level.dimension.DimensionType;

public class DimensionVer {
    public static boolean isUltrawarm(DimensionType dim) {
        //#if MC < 12111
        //$$ return dim.ultrawarm();
        //#else
        return false; // TODO: use EnvironmentAttributes API
        //#endif
    }
}
