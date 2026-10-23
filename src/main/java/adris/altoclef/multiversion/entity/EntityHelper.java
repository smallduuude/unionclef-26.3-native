package adris.altoclef.multiversion.entity;

import adris.altoclef.mixins.PortalManagerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;

public class EntityHelper {

    public static boolean isInNetherPortal(Entity entity) {
       return (entity.portalProcess != null && ((PortalManagerAccessor)entity.portalProcess).accessPortal() instanceof NetherPortalBlock && entity.portalProcess.isInsidePortalThisTick())
               || entity.getPortalCooldown() > 0;
    }


}
