package kaptainwutax.tungsten.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import kaptainwutax.tungsten.TungstenModDataContainer;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(LevelChunk.class)
public abstract class MixinWorldChunk {

	@Shadow public abstract Level getLevel();

	// 26.2: LevelChunk.replaceWithPacketData is (FriendlyByteBuf, Map<Heightmap.Types, long[]>,
	// Consumer<BlockEntityTagOutput>). 26.3 changed it to (int chunkX, int chunkZ,
	// ClientboundLevelChunkPacketData) — mixin handlers are not compile-checked, so the 26.2
	// signature has to be spelled out here or the game dies during Bootstrap.
	@Inject(method = "replaceWithPacketData", at = @At("RETURN"))
	private void loadFromPacket(net.minecraft.network.FriendlyByteBuf buf,
			java.util.Map<net.minecraft.world.level.levelgen.Heightmap.Types, long[]> heightmaps,
			java.util.function.Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntityTagOutput,
			CallbackInfo ci) {
		if (this.getLevel() != TungstenModDataContainer.world) {
			TungstenModDataContainer.world = this.getLevel();
		}
	}

}
