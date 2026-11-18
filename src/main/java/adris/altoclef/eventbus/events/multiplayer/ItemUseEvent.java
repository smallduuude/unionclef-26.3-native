package adris.altoclef.eventbus.events.multiplayer;

import net.minecraft.world.entity.Entity;

public class ItemUseEvent {
    public Entity entity;
    public boolean released;

    public ItemUseEvent(Entity entity, boolean released) {
        this.entity = entity;
        this.released = released;
    }
}
