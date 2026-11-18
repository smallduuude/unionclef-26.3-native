package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChunkLoadEvent;
import adris.altoclef.eventbus.events.ChunkUnloadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import java.util.BitSet;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ClientChunkCache.class)
public class LoadChunkMixin {

    /**
     * Loads a chunk from a packet and executes necessary actions.
     *
     * @param x        The x-coordinate of the chunk.
     * @param z        The z-coordinate of the chunk.
     * @param buf      The packet containing the chunk data.
     * @param nbt      The NBT compound of the chunk.
     * @param consumer A consumer for visiting block entities in the chunk.
     * @param ci       The callback info returnable object.
     */
    @Inject(
            method = "replaceWithPacketData",
            at = @At("RETURN")
    )
    // 26.2: replaceWithPacketData is (int, int, FriendlyByteBuf, Map<Heightmap.Types, long[]>,
    // Consumer<BlockEntityTagOutput>). 26.3 replaced the last three with a single
    // ClientboundLevelChunkPacketData. Mixin handlers are matched by descriptor at runtime, so this
    // must be the 26.2 shape or the world join dies with "Network Protocol Error".
    private void onLoadChunk(int chunkX, int chunkZ,
            FriendlyByteBuf buf,
            java.util.Map<net.minecraft.world.level.levelgen.Heightmap.Types, long[]> heightmaps,
            Consumer<net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntityTagOutput,
            CallbackInfoReturnable<LevelChunk> cir) {
        // Publish a ChunkLoadEvent with the return value of the method as the argument
        EventBus.publish(new ChunkLoadEvent(cir.getReturnValue()));
    }

    /**
     * Publishes a ChunkUnloadEvent when a chunk is unloaded.
     *
     * @param pos The position of the unloaded chunk.
     * @param ci  The callback info object.
     */
    @Inject(
            method = "drop",
            at = @At("TAIL")
    )
    private void onChunkUnload(ChunkPos pos, CallbackInfo ci) {
        EventBus.publish(new ChunkUnloadEvent(pos));
    }
}
