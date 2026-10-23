package adris.altoclef.mixins;

import java.io.File;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screenshot.class)
public interface ScreenshotRecorderInvoker {
    @Invoker("getFile")
    static File invokeGetScreenshotFileName(File dir) {
        throw new AssertionError();
    }
}
