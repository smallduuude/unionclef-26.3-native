package adris.altoclef.multiversion.box;

import adris.altoclef.multiversion.Pattern;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BoxVer {


   @Pattern
    public AABB of(Vec3 center, double x, double y, double z) {
       return AABB.ofSize(center, x, y, z);
   }


}
