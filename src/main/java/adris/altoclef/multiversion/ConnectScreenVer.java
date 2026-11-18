package adris.altoclef.multiversion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class ConnectScreenVer {


    // some weird bugs with patterns cuz 1.19.4 is missing the quickPlay argument
    public static void connect(Screen screen, Minecraft client, ServerAddress address, ServerData info, boolean quickPlay) {
        ConnectScreen.startConnecting(screen, client, address, info, quickPlay,null);
    }

}
