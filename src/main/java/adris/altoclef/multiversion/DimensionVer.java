package adris.altoclef.multiversion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Dimension classification, version-stable.
 *
 * <p>On MC 1.21.11 {@code DimensionType.ultrawarm()/natural()} were removed, so the old
 * preprocessor branch stubbed both predicates to {@code return false}. That silently made
 * {@link adris.altoclef.util.helpers.WorldHelper#getCurrentDimension()} fall through to END
 * for EVERY dimension (incl. the overworld) and disabled Nether fire-avoidance + MLG
 * water-bucket clutch gating. We now classify by the current client world's
 * {@code RegistryKey<World>} compared to the interned {@code World.OVERWORLD/NETHER/END}
 * singletons — the same pattern baritone uses unguarded on both 1.21.1 and 1.21.11
 * (e.g. ChunkPacker, CalculationContext), so no {@code //#if} guard is needed.
 *
 * <p>The {@code DimensionType} argument is unused but kept so the existing callsites
 * (all of which pass the current client world's {@code getDimension()}) compile unchanged.
 */
public class DimensionVer {
    private static net.minecraft.resources.ResourceKey<Level> currentDimensionKey() {
        ClientLevel world = Minecraft.getInstance().level;
        return world == null ? null : world.dimension();
    }

    /** ultrawarm == the Nether (used to gate fire/lava avoidance + "don't water-clutch in nether"). */
    public static boolean isUltrawarm(DimensionType dim) {
        return currentDimensionKey() == Level.NETHER;
    }

    /** natural == the overworld (the only natural vanilla dimension; classifies OVERWORLD vs END). */
    public static boolean isNatural(DimensionType dim) {
        return currentDimensionKey() == Level.OVERWORLD;
    }
}
