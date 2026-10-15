package adris.altoclef.multiversion;

import net.minecraft.client.Minecraft;

public class MinecraftClientVer {


    @Pattern
    private static float getTickDelta(Minecraft client) {
        return client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

}
