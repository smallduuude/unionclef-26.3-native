package adris.altoclef.chains.coverage;

import adris.altoclef.tasks.ResourceTask;
import net.minecraft.world.item.Item;

/**
 * Registry of the hand-written "new coverage" chains — the sources that are neither a recipe nor a
 * block: structure loot, archaeology, mob buckets and villager trades.
 *
 * <p>{@link adris.altoclef.TaskCatalogue#getItemTask(net.minecraft.world.item.Item, int)} tries the
 * curated catalogue first, then this registry, and only then falls back to the generic recipe-book
 * craft and generic mine chains. That order matters: a curated chain must win over the generic ones,
 * and a coverage chain must win over the generic ones too (they would otherwise fail and log "no
 * source"), but neither may replace a curated entry.
 *
 * <p>Each family lives in its own class so that one family can be developed or disabled without
 * touching the others; this class only orders them. All methods are null-returning on both "this
 * item is not mine" and "I cannot do it right now" — the caller treats both the same and falls
 * through to the next source.
 */
public final class CoverageRegistry {

    private CoverageRegistry() {
    }

    /**
     * @return a task that obtains {@code count} of {@code item}, or null when no coverage chain
     * claims it
     */
    public static ResourceTask tryResolve(Item item, int count) {
        if (item == null) return null;
        ResourceTask task;

        // Structure loot: locate a structure, travel, loot its containers.
        task = LootSearchChains.getTask(item, count);
        if (task != null) return task;

        // Archaeology: find suspicious sand/gravel, brush it.
        task = ArchaeologyChains.getTask(item, count);
        if (task != null) return task;

        // Buckets: use a bucket on a fish/tadpole/axolotl/sulfur cube.
        task = BucketChains.getTask(item, count);
        if (task != null) return task;

        // Villager trades: cartographer maps and other trade-only items.
        task = TradeChains.getTask(item, count);
        if (task != null) return task;

        return null;
    }
}
