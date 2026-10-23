package kaptainwutax.tungsten.mixin;

import java.util.ArrayList;
import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import kaptainwutax.tungsten.Debug;
import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.TungstenModDataContainer;
import kaptainwutax.tungsten.commandsystem.Command;
import kaptainwutax.tungsten.commandsystem.CommandExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonPacketListenerImpl {
	
	@Shadow
    private ClientLevel level;

    @Shadow
    public abstract void sendChat(String content);

    @Unique
    private boolean ignoreChatMessage;

    @Unique
    private boolean worldNotNull;
    
    protected MixinClientPlayNetworkHandler(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }
    
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (ignoreChatMessage) return;
		String prefix = TungstenMod.getCommandPrefix();
        if (message.startsWith(prefix)) {
            try {
            	if (message.contains("|")) {
//            		CommandExecutor.dispatch(message.split(";")[0].substring(prefix.length()));
                	Collection<Command> commands = new ArrayList<>(TungstenMod.getCommandExecutor().allCommands());
                	commands.removeIf((command) -> !message.contains(command.getName()));
                	// ⛔ FIXED 2026-09-05: `message.split("|")` treats "|" as a regex alternation
                	// between two empty patterns, which matches a zero-width string at every
                	// position -- splits between EVERY CHARACTER instead of on the literal pipe
                	// this chained-command feature is built around (see the `if
                	// (message.contains("|"))` check just above, and MixinSuggestionWindow /
                	// StringProcessorHelper.findClosestCharIndex(..., '|', ...), which both treat
                	// "|" as the real divider). A chat message like ";stop|;goto 1 2 3" was being
                	// split into single characters instead of the two intended command strings.
                	TungstenMod.getCommandExecutor().executeRecursive(commands.toArray(new Command[commands.size()]), message.split("\\|"), 0, () -> {
                    }, ex -> Debug.logWarning(ex.getMessage()));
            	} else CommandExecutor.dispatch(message.substring(prefix.length()));
            } catch (CommandSyntaxException e) {
                Debug.logWarning(e.getMessage());
            } catch (IllegalArgumentException e) {
                Debug.logWarning(e.getMessage());
            }

            minecraft.gui.hud.getChat().addRecentChat(message);
            ci.cancel();
        }
    }

    @Inject(method = "handleSetEntityData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;forceMainThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
        shift = At.Shift.AFTER), cancellable = false)
    public void onEntityTrackerUpdate(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        // Previously cancelled tracker updates for our player during execution,
        // which could desync pose/metadata. Now let vanilla handle them normally.
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;forceMainThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
        shift = At.Shift.AFTER), cancellable = false)
    public void onPlayerPositionLook(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if(TungstenModDataContainer.isExecutorRunning()) {
            // Server teleported us — stop executor and let vanilla handle the teleport.
            // Previously this ci.cancel()'d the packet, which caused a deadlock:
            // server waits for TeleportConfirmC2SPacket, client never sends it,
            // server ignores all subsequent movement packets → player stuck.
            Debug.logMessage("Server teleport received during execution — stopping executor");
            TungstenModDataContainer.EXECUTOR.stop = true;
            kaptainwutax.tungsten.path.PathFinder.noteStop("MixinClientPlayNetworkHandler@89");
            TungstenModDataContainer.PATHFINDER.stop.set(true);
        }
    }

}
