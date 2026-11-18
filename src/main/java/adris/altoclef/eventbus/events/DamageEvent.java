package adris.altoclef.eventbus.events;

import net.minecraft.world.entity.Entity;

public class DamageEvent {
    public Entity _entity;

    public DamageEvent(Entity entity) {
        _entity = entity;
    }
}
