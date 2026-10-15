package adris.altoclef.multiversion;

import net.minecraft.resources.Identifier;

public class IdentifierVer {


    @Pattern
    private static Identifier newCreation(String str) {
        return Identifier.parse(str);
    }


}
