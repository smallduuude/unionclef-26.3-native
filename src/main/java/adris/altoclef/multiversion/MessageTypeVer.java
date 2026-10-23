package adris.altoclef.multiversion;

import net.minecraft.network.chat.ChatType;

public class MessageTypeVer {

    public static ChatType getMessageType(ChatType.Bound parameters) {

        return parameters.chatType().value();
    }
}
