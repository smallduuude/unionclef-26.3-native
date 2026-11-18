package adris.altoclef.chains.coverage;

import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.FillBucketWithEntityTask;
import adris.altoclef.tasks.resources.CollectPowderSnowBucketTask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Mob-bucket coverage: the buckets that are not crafted at all but made by using a bucket on a live
 * mob.
 *
 * <p>The 26.3 jar's own interaction code decides the held item for each mob:
 *
 * <ul>
 *   <li>cod / salmon / pufferfish / tropical fish / tadpole / axolotl implement
 *       {@code Bucketable} but do not override {@code canBePickedUpWithBucket}, so they require a
 *       <b>water bucket</b> in hand.</li>
 *   <li>{@code SulfurCube.canBePickedUpWithBucket} checks the <b>empty bucket</b> instead, and its
 *       {@code mobInteract} bypasses bucket pickup entirely for babies — hence the
 *       {@code !isBaby()} filter (also matches the wiki statement that only small sulfur cubes can
 *       be bucketed).</li>
 * </ul>
 *
 * <p>Every one of these mobs lives in the overworld (sulfur cubes spawn in the sulfur caves biome),
 * so {@link FillBucketWithEntityTask} goes to the overworld first when it is not already there.
 *
 * <p>Fish need water, so the approach is left to the ordinary entity-pursuit machinery
 * ({@code AbstractDoToEntityTask} → {@code GetToEntityTask}), which can swim; the tracker's
 * reachability pricing makes the bot prefer mobs it already has a route to.
 */
public final class BucketChains {

    /** Everything one bucket needs to know: target item, held item, mob name and mob selection. */
    private record BucketTarget(Item bucket, Item requiredItem, String entityName,
                                Predicate<Entity> filter, List<Class<? extends Entity>> entityClasses) {
    }

    private static final Map<String, BucketTarget> TARGETS = new HashMap<>();

    static {
        put("cod_bucket", water(Items.COD_BUCKET, "cod", Cod.class));
        put("salmon_bucket", water(Items.SALMON_BUCKET, "salmon", Salmon.class));
        put("pufferfish_bucket", water(Items.PUFFERFISH_BUCKET, "pufferfish", Pufferfish.class));
        put("tropical_fish_bucket", water(Items.TROPICAL_FISH_BUCKET, "tropical fish",
                TropicalFish.class));
        put("tadpole_bucket", water(Items.TADPOLE_BUCKET, "tadpole", Tadpole.class));
        put("axolotl_bucket", water(Items.AXOLOTL_BUCKET, "axolotl", Axolotl.class));
        put("sulfur_cube_bucket", new BucketTarget(Items.SULFUR_CUBE_BUCKET, Items.BUCKET,
                "sulfur cube", entity -> !((SulfurCube) entity).isBaby(),
                List.of(SulfurCube.class)));
    }

    private BucketChains() {
    }

    private static BucketTarget water(Item bucket, String entityName,
                                      Class<? extends Entity> entityClass) {
        return new BucketTarget(bucket, Items.WATER_BUCKET, entityName, entity -> true,
                List.of(entityClass));
    }

    private static void put(String itemPath, BucketTarget target) {
        TARGETS.put("minecraft:" + itemPath, target);
    }

    public static ResourceTask getTask(Item item, int count) {
        if (item == null) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
        // Not a mob bucket: powder snow is a BucketPickup block, so the same "right-click with an
        // empty bucket" mechanic fills it, just handled by a block task.
        if (id.toString().equals("minecraft:powder_snow_bucket")) {
            return new CollectPowderSnowBucketTask(count);
        }
        BucketTarget target = TARGETS.get(id.toString());
        if (target == null) return null;
        return new FillBucketWithEntityTask(target.bucket(), count, target.requiredItem(),
                target.entityName(), target.filter(), target.entityClasses());
    }
}
