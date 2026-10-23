package adris.altoclef.multiversion.entity;

import adris.altoclef.multiversion.Pattern;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class LivingEntityVer {

    @SuppressWarnings("unchecked")
    private static Iterable<ItemStack> reflectMethod(LivingEntity entity, String name) {
        try {
            return (Iterable<ItemStack>) LivingEntity.class.getMethod(name).invoke(entity);
        } catch (Exception e) {
            return List.of();
        }
    }

    static Iterable<ItemStack> callEquippedItems(LivingEntity e) { return reflectMethod(e, "getEquippedItems"); }
    static Iterable<ItemStack> callHandItems(LivingEntity e) { return reflectMethod(e, "getHandItems"); }
    static Iterable<ItemStack> callArmorItems(LivingEntity e) { return reflectMethod(e, "getArmorItems"); }

    public static boolean hasTrident(LivingEntity entity) {
        return entity.getMainHandItem().is(Items.TRIDENT)
            || entity.getOffhandItem().is(Items.TRIDENT);
    }

    @Pattern
    private static boolean isSuitableFor(Item item, BlockState state) {
        return item.getDefaultInstance().isCorrectToolForDrops(state);
    }

}
