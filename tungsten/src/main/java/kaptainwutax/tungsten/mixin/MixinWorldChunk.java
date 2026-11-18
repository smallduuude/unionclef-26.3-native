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

	// 26.3: LevelChunk.replaceWithPacketData is now (int chunkX, int chunkZ, ClientboundLevelChunkPacketData).
	@Inject(method = "replaceWithPacketData", at = @At("RETURN"))
	private void loadFromPacket(int chunkX, int chunkZ, ClientboundLevelChunkPacketData chunkData, CallbackInfo ci) {
		if (this.getLevel() != TungstenModDataContainer.world) {
			TungstenModDataContainer.world = this.getLevel();
		}
	}

}
