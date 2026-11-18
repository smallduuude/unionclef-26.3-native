package adris.altoclef.util.helpers;

import net.minecraft.network.chat.Component;

public class StringHelper {
    public static String removeMCFormatCodes(String message) {
        return message.replaceAll("§.", "");
    }

    public static String mcTextToString(Component message) {
        StringBuilder result = new StringBuilder();
        result.append(message.getString());
        return removeMCFormatCodes(result.toString().trim());
    }
}
