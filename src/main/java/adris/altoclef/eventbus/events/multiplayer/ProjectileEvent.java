package adris.altoclef.eventbus.events.multiplayer;

import net.minecraft.world.entity.projectile.Projectile;

public class ProjectileEvent {
    public Projectile entity;
    public boolean sticked;

    public ProjectileEvent(Projectile entity, boolean sticked) {
        this.entity = entity;
        this.sticked = sticked;
    }
}
