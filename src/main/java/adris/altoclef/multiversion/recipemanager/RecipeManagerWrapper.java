package adris.altoclef.multiversion.recipemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.item.crafting.RecipeAccess;
import java.util.Collection;

public class RecipeManagerWrapper {

    private final RecipeAccess recipeManager;

    public static RecipeManagerWrapper of(RecipeAccess recipeManager) {
        if (recipeManager == null) return null;

        return new RecipeManagerWrapper(recipeManager);
    }


    private RecipeManagerWrapper(RecipeAccess recipeManager) {
        this.recipeManager = recipeManager;
    }

    public Collection<WrappedRecipeEntry> values() {
        return java.util.Collections.emptySet(); // TODO [1.21.11] RecipeEntry.id() returns RegistryKey now
    }



}
