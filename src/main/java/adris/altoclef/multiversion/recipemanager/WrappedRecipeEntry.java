package adris.altoclef.multiversion.recipemanager;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public record WrappedRecipeEntry(Identifier id, Recipe<?> value) {

    public RecipeHolder<?> asRecipe() {
        return null; // TODO [1.21.11] RecipeEntry constructor takes RegistryKey now
    }

}
