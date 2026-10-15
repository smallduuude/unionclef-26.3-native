package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.AnimEvent;
import adris.altoclef.eventbus.events.DamageEvent;
import adris.altoclef.eventbus.events.multiplayer.ItemUseEvent;
import adris.altoclef.eventbus.events.multiplayer.ProjectileEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    public void onEntityTrackerUpdate(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        try {
            Entity entity = level != null ? level.getEntity(packet.id()) : null;
            if (entity != null) {
                if ((entity instanceof Player player && player.getName() != null) ||
                        (entity instanceof Projectile)) {
                    if (packet.packedItems() != null &&
                            packet.packedItems().size() == 1 &&
                            packet.packedItems().getFirst() != null &&
                            packet.packedItems().getFirst().id() == 8 &&
                            packet.packedItems().getFirst().value() instanceof Byte byteVal) {
                        int value = (byte) byteVal & 1;
                        boolean released = value <= 0;
                        if (entity instanceof Player)
                            EventBus.publish(new ItemUseEvent(entity, released));
                        if (entity instanceof Projectile projectile && projectile.position() != null)
                            EventBus.publish(new ProjectileEvent(projectile, released));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void onEntityDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        Entity entity = level != null ? level.getEntity(packet.entityId()) : null;
        if (entity != null) {
            EventBus.publish(new DamageEvent(entity));
        }
    }

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    public void onEntityAnimation(ClientboundAnimatePacket packet, CallbackInfo ci) {
        Entity entity = level != null ? level.getEntity(packet.getId()) : null;
        if (entity != null) {
            EventBus.publish(new AnimEvent(entity, packet.getAction()));
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void onScreenHandlerSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        try {
            AltoClef mod = AltoClef.getInstance();
            if (mod != null && AltoClef.inGame()) {
                mod.getSlotHandler().onServerSlotUpdate(packet.getContainerId(), packet.getSlot(), packet.getItem());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        try {
            AltoClef mod = AltoClef.getInstance();
            if (mod != null && AltoClef.inGame()) {
                List<ItemStack> contents = packet.items();
                for (int i = 0; i < contents.size(); i++) {
                    mod.getSlotHandler().onServerSlotUpdate(packet.containerId(), i, contents.get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
