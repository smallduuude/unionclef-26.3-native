package adris.altoclef.multiversion.item;

import adris.altoclef.multiversion.FoodComponentWrapper;
import adris.altoclef.multiversion.Pattern;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class ItemVer {

    public static FoodComponentWrapper getFoodComponent(Item item) {
        return FoodComponentWrapper.of(item.components().get(net.minecraft.core.component.DataComponents.FOOD));
    }

    public static boolean isFood(ItemStack stack) {
        return isFood(stack.getItem());
    }

    public static boolean hasCustomName(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
    }

    public static boolean isFood(Item item) {
        return item.components().has(net.minecraft.core.component.DataComponents.FOOD);
    }

    @Pattern
    private static boolean isSuitableFor(Item item, BlockState state) {
        return item.getDefaultInstance().isCorrectToolForDrops(state);
    }

    // the fact that this works is insane...
    @Pattern
    private static Item RAW_GOLD() {
        return Items.RAW_GOLD;
    }

    @Pattern
    private static Item RAW_IRON() {
        return Items.RAW_IRON;
    }


}
