package adris.altoclef.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftClientSessionMixin {
    @Accessor("user")
    @Mutable
    void setSession(User session);

    @Accessor("user")
    User getSession();
}
