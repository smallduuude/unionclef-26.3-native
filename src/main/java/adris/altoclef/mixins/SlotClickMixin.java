package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Publishes {@link SlotClickChangedEvent} when a container click changes slot contents.
 *
 * <p>26.3 port: the old {@code @Redirect} of the private {@code internalOnSlotClick} is impossible
 * here — {@code clicked(...)} delegates to a private {@code doClick(...)}, and re-calling
 * {@code clicked} from a redirect of it would recurse. Snapshot at HEAD of the public
 * {@code clicked(int,int,ContainerInput,Player)} and diff at RETURN instead.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class SlotClickMixin {

    @Unique
    private final List<ItemStack> unionclef$beforeStacks = new ArrayList<>();

    @Inject(method = "clicked", at = @At("HEAD"))
    private void unionclef$beforeClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        unionclef$beforeStacks.clear();
        for (Slot slot : ((AbstractContainerMenu) (Object) this).slots) {
            unionclef$beforeStacks.add(slot.getItem().copy());
        }
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void unionclef$afterClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        List<Slot> slots = ((AbstractContainerMenu) (Object) this).slots;
        int n = Math.min(unionclef$beforeStacks.size(), slots.size());
        for (int i = 0; i < n; i++) {
            ItemStack before = unionclef$beforeStacks.get(i);
            ItemStack after = slots.get(i).getItem();
            if (!ItemStack.matches(before, after)) {
                adris.altoclef.util.slots.Slot slot = adris.altoclef.util.slots.Slot.getFromCurrentScreen(i);
                EventBus.publish(new SlotClickChangedEvent(slot, before, after));
            }
        }
    }
}
