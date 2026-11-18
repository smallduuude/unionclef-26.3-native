package adris.altoclef.multiversion;

import net.minecraft.world.food.FoodProperties;


public class FoodComponentWrapper {


    public static FoodComponentWrapper of(FoodProperties component) {
        if (component == null) return null;

        return new FoodComponentWrapper(component);
    }

    private final FoodProperties component;

    private FoodComponentWrapper(FoodProperties component) {
        this.component = component;
    }

    public int getHunger() {
        return component.nutrition();
    }

    public float getSaturationModifier() {
        return component.saturation();
    }
}
