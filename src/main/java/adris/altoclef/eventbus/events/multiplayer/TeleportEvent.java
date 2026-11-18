package adris.altoclef.eventbus.events.multiplayer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class TeleportEvent {
    public Player entity;
    public Vec3 from;
    public Vec3 to;

    public TeleportEvent(Player entity, Vec3 tpFrom, Vec3 tpTo) {
        this.entity = entity;
        this.to = tpTo;
        this.from = tpFrom;
    }
}
