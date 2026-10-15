package kaptainwutax.tungsten.mixin;

import net.minecraft.server.commands.FillCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(FillCommand.class)
public class MixinFillCommand {

    /**
     * @author KaptainWutax
     */
    @ModifyConstant(method = "fillBlocks", constant = @Constant(intValue = 32768))
    private static int execute(int constant) {
       return Integer.MAX_VALUE;
    }

}
