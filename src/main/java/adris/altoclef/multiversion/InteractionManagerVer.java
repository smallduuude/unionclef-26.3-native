package adris.altoclef.multiversion;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class InteractionManagerVer {


    @Pattern
    public InteractionResult interactItem(MultiPlayerGameMode interactionManager, Player player, InteractionHand hand) {
        return interactionManager.useItem(player,hand);
    }

    @Pattern
    public InteractionResult interactBlock(MultiPlayerGameMode interactionManager, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        return interactionManager.useItemOn(player,hand, hitResult);
    }

}
