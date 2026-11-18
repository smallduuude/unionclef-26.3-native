package adris.altoclef.multiversion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

public class BlockTagVer {


    public static boolean isWool(Block block) {
        return BuiltInRegistries.BLOCK.wrapAsHolder(block).tags().anyMatch(t -> t == BlockTags.WOOL);
    }

}
