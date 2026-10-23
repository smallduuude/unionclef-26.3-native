package kaptainwutax.tungsten.mixin;

import java.util.Set;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface AccessorEntity {

    @org.spongepowered.asm.mixin.gen.Invoker("getBlockPosBelowThatAffectsMyMovement")
    net.minecraft.core.BlockPos tungsten$getVelocityAffectingPos();

	@Accessor
	Vec3 getStuckSpeedMultiplier();

	@Accessor
	boolean getFirstTick();

}
