package adris.altoclef.eventbus.events;

import net.minecraft.world.damagesource.DamageSource;

public class EntityDamageEvent {
    public String attacker;

    public EntityDamageEvent(DamageSource source, float amount) {
    }
}
