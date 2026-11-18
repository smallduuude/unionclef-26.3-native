package adris.altoclef.multiversion.entity;

import adris.altoclef.multiversion.Pattern;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerVer {


    public static void sendChatMessage(LocalPlayer player,String content) {
        player.connection.sendChat(content);
    }

    public static void sendChatCommand(LocalPlayer player,String content) {
        player.connection.sendCommand(content);
    }

    @Pattern
    private static ItemStack getCursorStack(Player player) {
        return player.containerMenu.getCarried();
    }

    @Pattern
    private static Container getInventory(Player player) {
        return player.getInventory();
    }

    public static boolean inPowderedSnow(Player player) {
        return player.isInPowderSnow;
    }

    @Pattern
    public static int getSelectedSlot(net.minecraft.world.entity.player.Inventory inv) {
        return inv.getSelectedSlot();
    }

    /**
     * ⛔ SYNCS THE SLOT TO THE SERVER TOO, because every caller here is one swing away from the
     * race this shim used to hide: the field is written locally and vanilla posts the packet on
     * its own tick, so anything acting in the same tick acts with the PREVIOUS item. Measured on
     * allround as ~12 hits of exactly 1.0 hp a run -- a bow's attack damage -- with an iron_sword
     * drawn on the client. Behind syncSlotToServer while it is being measured.
     */
    public static void setSelectedSlot(net.minecraft.world.entity.player.Inventory inv, int slot) {
        inv.setSelectedSlot(slot);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.getInventory() == inv) {
            kaptainwutax.tungsten.combat.WeaponSelector.syncSlot(mc.player, slot);
        }
    }

}
