package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.recipemanager.RecipeManagerWrapper;
import adris.altoclef.multiversion.recipemanager.WrappedRecipeEntry;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// TODO remove those ugly "ensureUpdate" statements, realistically we only need to update only upon joining a world
public class CraftingRecipeTracker extends Tracker{


    private final HashMap<Item, List<adris.altoclef.util.CraftingRecipe>> itemRecipeMap = new HashMap<>();
    private final HashMap<adris.altoclef.util.CraftingRecipe, ItemStack> recipeResultMap = new HashMap<>();

    private boolean shouldRebuild;

    /**
     * How many distinct items the tracker has a recipe for. Read as recipesKnown.
     *
     * <p>Published because it is the claim: this was structurally 0 on 1.21.11, and "the port is
     * done" is only worth saying if the number moved. A course can fail on it, which is the point.
     */
    public static volatile int recipesKnown;

    public CraftingRecipeTracker(TrackerManager manager) {
        super(manager);
        shouldRebuild = true;
    }

    public List<adris.altoclef.util.CraftingRecipe> getRecipeForItem(Item item) {
        ensureUpdated();

        if (!hasRecipeForItem(item)) {
            mod.logWarning("trying to access recipe for unknown item: "+item);
            return null;
        }

        return itemRecipeMap.get(item);
    }

    public adris.altoclef.util.CraftingRecipe getFirstRecipeForItem(Item item) {
        ensureUpdated();

        if (!hasRecipeForItem(item)) {
            mod.logWarning("trying to access recipe for unknown item: "+item);
            return null;
        }

        return itemRecipeMap.get(item).get(0);
    }

    public List<RecipeTarget> getRecipeTarget(Item item, int targetCount) {
        ensureUpdated();

        List<RecipeTarget> targets = new ArrayList<>();
        for (adris.altoclef.util.CraftingRecipe recipe : getRecipeForItem(item)) {
            targets.add(new RecipeTarget(item, targetCount, recipe));
        }

        return targets;
    }

    public RecipeTarget getFirstRecipeTarget(Item item, int targetCount) {
        ensureUpdated();

        return new RecipeTarget(item, targetCount, getFirstRecipeForItem(item));
    }

    public boolean hasRecipeForItem(Item item) {
        ensureUpdated();
        return itemRecipeMap.containsKey(item);
    }

    public ItemStack getRecipeResult(adris.altoclef.util.CraftingRecipe recipe) {
        ensureUpdated();

        if (!hasRecipe(recipe)) {
            mod.logWarning("Trying to get result for unknown recipe: "+recipe);
            return null;
        }
        ItemStack result = recipeResultMap.get(recipe);

        return new ItemStack(result.getItem(), result.getCount());
    }

    public boolean hasRecipe(adris.altoclef.util.CraftingRecipe recipe) {
        ensureUpdated();
        return recipeResultMap.containsKey(recipe);
    }


    @Override
    protected void updateState() {
        if (!shouldRebuild) return;

        // rebuild once we are in game
        if (!AltoClef.inGame()) return;

        ClientPacketListener networkHandler =  Minecraft.getInstance().getConnection();
        if (networkHandler == null) return;

        // 1.21.11 STOPPED SENDING RECIPES TO THE CLIENT; IT SENDS DISPLAYS.
        // The client's RecipeManager is a two-method interface now (property sets and
        // stonecutting), so there is nothing to iterate and the port left this switched off
        // with shouldRebuild = false. That is the fuel-map shape again: no crash, no log, and
        // every caller reading a permanent no. getRecipeForItem returns null for EVERY item,
        // so CraftingHelper.canCraftItemNow is false for everything, so every
        // CraftItemPriorityTask in BeatMinecraftTask -- which is what @gamer runs -- is
        // permanently unavailable.
        //
        // What the client DOES have is its recipe book, which the server fills with
        // RecipeDisplayEntry values. CraftGenericWithRecipeBooksTask already walks exactly
        // that structure to send crafts, so this is a proven source in this codebase rather
        // than a hopeful reading of the mappings.
        //
        // HONEST DIFFERENCE FROM WHAT THIS USED TO BE: the old map came from the full recipe
        // manager, so it knew every recipe in the game. The book holds what the SERVER has told
        // this player about. That is not a shortcut, it is the only thing a 1.21.11 client is
        // given -- and it is why recipesKnown is published rather than assumed: the number is
        // the claim.
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || mod.getWorld() == null) return;
        net.minecraft.util.context.ContextMap ctx =
                net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(mod.getWorld());
        for (net.minecraft.client.gui.screens.recipebook.RecipeCollection col
                : player.getRecipeBook().getCollections()) {
            for (net.minecraft.world.item.crafting.display.RecipeDisplayEntry entry : col.getRecipes()) {
                java.util.List<net.minecraft.world.item.crafting.display.SlotDisplay> ingredients;
                int width, height;
                if (entry.display() instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
                    ingredients = shaped.ingredients();
                    width = shaped.width();
                    height = shaped.height();
                } else if (entry.display() instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
                    // Shapeless has no shape to honour, so lay it out left to right. The old
                    // code did the same and said so: it is always stored shaped, and for a
                    // shapeless recipe the arrangement does not matter.
                    ingredients = shapeless.ingredients();
                    width = Math.min(Math.max(ingredients.size(), 1), 3);
                    height = (ingredients.size() + width - 1) / width;
                } else {
                    continue;   // smelting, stonecutting, smithing: not this tracker's business
                }
                java.util.List<ItemStack> outputs = entry.resultItems(ctx);
                if (outputs.isEmpty() || ingredients.isEmpty()) continue;
                ItemStack result = new ItemStack(outputs.get(0).getItem(), outputs.get(0).getCount());
                // A RECIPE THAT FITS THE 2x2 MUST BE STORED AS A 2x2.
                // Size is how the rest of altoclef decides whether a table is needed
                // (CraftingRecipe.isBig), so widening everything to 3x3 would send the bot
                // looking for a table to make planks.
                boolean small = width <= 2 && height <= 2;
                int gridWidth = small ? 2 : 3;
                Item[][] cells = new Item[small ? 4 : 9][];
                boolean usable = true;
                for (int i = 0; i < ingredients.size(); i++) {
                    int cell = (i / width) * gridWidth + (i % width);
                    if (cell >= cells.length) { usable = false; break; }
                    java.util.List<ItemStack> matching = ingredients.get(i).resolveForStacks(ctx);
                    // FIXME kept from the pre-1.21.11 version: the catalogue is built around one
                    // item per slot, so an "any log" slot collapses to the first match.
                    cells[cell] = matching.isEmpty() ? null : new Item[]{matching.get(0).getItem()};
                }
                if (!usable) continue;
                adris.altoclef.util.CraftingRecipe altoclefRecipe =
                        adris.altoclef.util.CraftingRecipe.newShapedRecipe(cells, result.getCount());
                if (altoclefRecipe == null) continue;
                itemRecipeMap.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(altoclefRecipe);
                recipeResultMap.put(altoclefRecipe, result);
            }
        }
        // NOTHING LEARNED IS NOT A REBUILD. The book arrives over the network a moment after the
        // world does, so an empty pass here means "too early", not "there are none" -- and
        // latching shouldRebuild = false on it would freeze the tracker empty forever, which is
        // exactly the bug being removed.
        if (itemRecipeMap.isEmpty()) return;
        itemRecipeMap.replaceAll((k, v) -> Collections.unmodifiableList(v));
        recipesKnown = itemRecipeMap.size();
        shouldRebuild = false;
    }


    @Override
    protected void reset() {
       shouldRebuild = true;
       itemRecipeMap.clear();
       recipeResultMap.clear();
    }

    @Override
    protected boolean isDirty() {
        return shouldRebuild;
    }
}
