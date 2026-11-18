package adris.altoclef.multiversion;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TextVer {


    @Pattern
    public static MutableComponent empty() {
        return Component.empty();
    }

    @Pattern
    public static MutableComponent literal(String str) {
        return Component.literal(str);
    }

}
