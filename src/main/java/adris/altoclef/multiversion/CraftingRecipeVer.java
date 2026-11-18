package adris.altoclef.multiversion;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

public class CraftingRecipeVer {


    public static ItemStack getOutput(CraftingRecipe craftingRecipe) {
        return ItemStack.EMPTY; // TODO [1.21.11] Recipe.getResult() API changed
    }

}
