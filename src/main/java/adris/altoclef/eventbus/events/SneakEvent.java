package adris.altoclef.eventbus.events;

import net.minecraft.world.entity.player.Player;

public class SneakEvent {
    public Player entity;
    public boolean sneak;

    public SneakEvent(Player entity, boolean sneaking) {
        this.entity = entity;
        this.sneak = sneaking;
    }
}
