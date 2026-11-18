package kaptainwutax.tungsten.agent;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import kaptainwutax.tungsten.Debug;
import kaptainwutax.tungsten.TungstenConfig;
import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.TungstenModDataContainer;
import kaptainwutax.tungsten.TungstenModRenderContainer;
import kaptainwutax.tungsten.helpers.DimensionVer;
import kaptainwutax.tungsten.helpers.render.RenderHelper;
import kaptainwutax.tungsten.mixin.AccessorEntity;
import kaptainwutax.tungsten.mixin.AccessorLivingEntity;
import kaptainwutax.tungsten.path.Node;
import kaptainwutax.tungsten.path.PathInput;
import kaptainwutax.tungsten.render.Color;
import kaptainwutax.tungsten.render.Cuboid;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import kaptainwutax.tungsten.agent.TungstenPlayerInput;

public class Agent {

    public static Agent INSTANCE;

    /** Optional client-thread observer of the exact replay state compared after vanilla
     *  movement. Read-only: the observer must not mutate this agent or player control.
     *  Null by default; bounded diagnostic recording does not require verbose logging. */
    public static java.util.function.Consumer<Agent> comparisonObserver;

    public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6f, 1.8f);
    public static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2f, 0.2f);

    public static final Map<Pose, EntityDimensions> POSE_DIMENSIONS =
        ImmutableMap.<Pose, EntityDimensions>builder()
        .put(Pose.STANDING, STANDING_DIMENSIONS)
        .put(Pose.SLEEPING, SLEEPING_DIMENSIONS)
        .put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6f, 0.6f))
        .put(Pose.SWIMMING, EntityDimensions.scalable(0.6f, 0.6f))
        .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6f, 0.6f))
        .put(Pose.CROUCHING, EntityDimensions.scalable(0.6f, 1.5f))
        .put(Pose.DYING, EntityDimensions.fixed(0.2f, 0.2f)).build();

    public boolean keyForward;
    public boolean keyBack;
    public boolean keyLeft;
    public boolean keyRight;
    public boolean keyJump;
    public boolean keySneak;
    public boolean keySprint;
    public AgentInput input = new AgentInput(this);

    public Pose pose;
    public boolean inSneakingPose;
    public boolean isDamaged = false;
    public int hurtTicks = 0;
    public boolean usingItem;

    public float sidewaysSpeed;
    public float upwardSpeed;
    public float forwardSpeed;
    public float yaw;
    public float pitch;

    public double posX, posY, posZ;
    public int blockX, blockY, blockZ;
    public double velX, velY, velZ;
    public double mulX, mulY, mulZ;

    public Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap<>(2);
    private final Set<TagKey<Fluid>> submergedFluids = new HashSet<>();
    public boolean firstUpdate = true;

    public EntityDimensions dimensions;
    public AABB box;
    public float standingEyeHeight;

    public boolean onGround;
    public boolean sleeping;
    public boolean sneaking; //flag 1
    public boolean sprinting; //flag 3
    public boolean swimming; //flag 4
    public boolean fallFlying; //flag 7
    public float stepHeight = 0.6F;
    public float fallDistance;
    public boolean touchingWater;
    public boolean isSubmergedInWater;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean collidedSoftly;
    public boolean slimeBounce;

    public boolean jumping;

    public int speed = -1;
    public int blindness = -1;
    public int jumpBoost = -1;
    public int slowFalling = -1;
    public int dolphinsGrace = -1;
    public int levitation = -1;

    public int depthStrider;

    public FoodData hunger = new FoodData();
    public double health;
    public float movementSpeed;
    public float airStrafingSpeed;
    public int jumpingCooldown;
    public int ticksToNextAutojump;
    private List<String> extra = new ArrayList<>();
    private int scannedBlocks;

    public Vec3 getPos() {
        return new Vec3(this.posX, this.posY, this.posZ);
    }
    
    public BlockPos getBlockPos() {
        return new BlockPos(this.blockX, this.blockY, this.blockZ);
    }
    
    public BlockState getBlockState(LevelReader world) {
    	return world.getBlockState(getBlockPos());
    }

    public void setPos(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.blockX = Mth.floor(x);
        this.blockY = Mth.floor(y);
        this.blockZ = Mth.floor(z);
//		this.setBoundingBox(this.calculateBoundingBox());
    }
    
    public final void setBoundingBox(AABB boundingBox) {
		this.box = boundingBox;
	}
    
    protected AABB calculateBoundingBox() {
		return this.dimensions.makeBoundingBox(this.getPos());
	}

    /** Shallow copy of this agent's state — enough for MPC forward simulation. */
    public Agent copy() {
        Agent a = new Agent();
        a.keyForward = this.keyForward; a.keyBack = this.keyBack;
        a.keyLeft = this.keyLeft;       a.keyRight = this.keyRight;
        a.keyJump = this.keyJump;       a.keySneak = this.keySneak;
        a.keySprint = this.keySprint;
        a.input = new AgentInput(a);
        a.input.playerInput = new TungstenPlayerInput(
            a.keyForward, a.keyBack, a.keyLeft, a.keyRight,
            a.keyJump, a.keySneak, a.keySprint);

        a.pose = this.pose; a.inSneakingPose = this.inSneakingPose;
        a.isDamaged = this.isDamaged; a.hurtTicks = this.hurtTicks; a.usingItem = this.usingItem;
        a.sidewaysSpeed = this.sidewaysSpeed; a.upwardSpeed = this.upwardSpeed;
        a.forwardSpeed = this.forwardSpeed;
        a.yaw = this.yaw; a.pitch = this.pitch;
        a.posX = this.posX; a.posY = this.posY; a.posZ = this.posZ;
        a.blockX = this.blockX; a.blockY = this.blockY; a.blockZ = this.blockZ;
        a.velX = this.velX; a.velY = this.velY; a.velZ = this.velZ;
        a.mulX = this.mulX; a.mulY = this.mulY; a.mulZ = this.mulZ;
        a.fluidHeight.putAll(this.fluidHeight);
        a.firstUpdate = this.firstUpdate;
        a.dimensions = this.dimensions; a.box = this.box;
        a.standingEyeHeight = this.standingEyeHeight;
        a.onGround = this.onGround; a.sleeping = this.sleeping;
        a.sneaking = this.sneaking; a.sprinting = this.sprinting;
        a.swimming = this.swimming; a.fallFlying = this.fallFlying;
        a.stepHeight = this.stepHeight; a.fallDistance = this.fallDistance;
        a.touchingWater = this.touchingWater;
        a.isSubmergedInWater = this.isSubmergedInWater;
        a.horizontalCollision = this.horizontalCollision;
        a.verticalCollision = this.verticalCollision;
        a.collidedSoftly = this.collidedSoftly;
        a.slimeBounce = this.slimeBounce; a.jumping = this.jumping;
        a.speed = this.speed; a.blindness = this.blindness;
        a.jumpBoost = this.jumpBoost; a.slowFalling = this.slowFalling;
        a.dolphinsGrace = this.dolphinsGrace; a.levitation = this.levitation;
        a.depthStrider = this.depthStrider;
        a.hunger.setFoodLevel(this.hunger.getFoodLevel());
        a.hunger.setSaturation(this.hunger.getSaturationLevel());
        a.health = this.health; a.movementSpeed = this.movementSpeed;
        a.airStrafingSpeed = this.airStrafingSpeed;
        a.jumpingCooldown = this.jumpingCooldown;
        a.ticksToNextAutojump = this.ticksToNextAutojump;
        return a;
    }

    public Agent tick(LevelReader world) {
        this.tickPlayer(world);
        return this;
    }

    public void tickPlayer(LevelReader world) {
        //Sleeping code
        this.updateWaterSubmersionState();
        this.tickLiving(world);
        //Hunger stuff
        //Turtle helmet
        //Item cooldown
        this.updateSize(world);
    }

    private void tickLiving(LevelReader world) {
        // Track hurt ticks from block damage (cactus, fire, etc.)
        if (this.hurtTicks > 0) {
            this.hurtTicks--;
        }

        this.baseTickLiving(world);
        //more sleep stuff
        this.tickMovementClientPlayer(world);

        if(this.sleeping) {
            this.pitch = 0.0F;
        }
    }

    private void baseTickLiving(LevelReader world) {
        this.baseTickEntity(world);
        //Suffocate in walls
        //Drown in water
        //Soulspeed and frost walker
        //Update potion effects
    }

    private void baseTickEntity(LevelReader world) {
        this.updateWaterState(world);
        this.updateSubmergedInWaterState(world);
        this.updateSwimming(world);

        if(this.isInLava()) {
            this.fallDistance *= 0.5F;
        }

        this.firstUpdate = false;
    }

    public boolean isSubmergedIn(TagKey<Fluid> tag) {
        return this.submergedFluids.contains(tag);
    }

    public boolean updateWaterState(LevelReader world) {
        this.fluidHeight.clear();
        this.checkWaterState(world);
        double d = DimensionVer.isUltrawarm(world.dimensionType()) ? 0.007D : 0.0023333333333333335D;
        boolean bl = this.updateMovementInFluid(world, FluidTags.LAVA, d);
        return this.touchingWater || bl;
    }

    private void updateSubmergedInWaterState(LevelReader world) {
        this.isSubmergedInWater = this.isSubmergedIn(FluidTags.WATER);
        this.submergedFluids.clear();
        double d = this.getEyeY();

        BlockPos blockPos = new BlockPos(Mth.floor(this.posX), Mth.floor(d), Mth.floor(this.posZ));
        FluidState fluidState = world.getFluidState(blockPos);
        double e = (float)blockPos.getY() + fluidState.getHeight(world, blockPos);

        if(e > d) {
            fluidState.getType().builtInRegistryHolder().tags().forEach(this.submergedFluids::add);
        }
    }

    /**
     * 26.3 removed {@code Entity.fluidOnEyes}/{@code submergedFluidTag}; recompute the
     * eye-submerged fluid tags the same way {@link #updateSubmergedInWaterState} does for
     * this agent.
     */
    public static Set<TagKey<Fluid>> getPlayerSubmergedFluids(Entity player) {
        Set<TagKey<Fluid>> result = new HashSet<>();
        var world = player.level();
        double eyeY = player.getEyeY();
        BlockPos blockPos = new BlockPos(Mth.floor(player.getX()), Mth.floor(eyeY), Mth.floor(player.getZ()));
        FluidState fluidState = world.getFluidState(blockPos);
        double top = (float) blockPos.getY() + fluidState.getHeight(world, blockPos);
        if (top > eyeY) {
            fluidState.getType().builtInRegistryHolder().tags().forEach(result::add);
        }
        return result;
    }

    public double getEyeY() {
        return this.posY + (double)this.standingEyeHeight;
    }

    public void updateSwimming(LevelReader world) {
        if(this.swimming) {
            this.swimming = this.sprinting && this.touchingWater;
        } else {
            FluidState fluid = world.getFluidState(new BlockPos(this.blockX, this.blockY, this.blockZ));
            this.swimming = this.sprinting && this.isSubmergedInWater && fluid.is(FluidTags.WATER);
        }
    }

    public void updateWaterSubmersionState() {
        this.isSubmergedInWater = this.isSubmergedIn(FluidTags.WATER);
    }

    public void updateSize(LevelReader world) {
        if(!this.wouldPoseNotCollide(world, Pose.SWIMMING)) return;
        Pose newPose;

        if(this.fallFlying) {
            newPose = Pose.FALL_FLYING;
        } else {
            if(this.sleeping) {
                newPose = Pose.SLEEPING;
            } else {
                if(this.swimming) {
                    newPose = Pose.SWIMMING;
                } else {
                    if(this.input.playerInput.sneak()) {
                        newPose = Pose.CROUCHING;
                    } else {
                        newPose = Pose.STANDING;
                    }
                }
            }
        }

        if(!this.wouldPoseNotCollide(world, newPose)) {
            if(this.wouldPoseNotCollide(world, Pose.CROUCHING)) {
                newPose = Pose.CROUCHING;
            } else {
                newPose = Pose.SWIMMING;
            }
        }

        this.setPose(world, newPose);
    }

    public void setPose(LevelReader world, Pose pose) {
        this.pose = pose;
        this.calculateDimensions(world);
    }

    public void calculateDimensions(LevelReader world) {
        EntityDimensions oldDimensions = this.dimensions;
        this.dimensions = POSE_DIMENSIONS.getOrDefault(this.pose, STANDING_DIMENSIONS);

        this.standingEyeHeight = this.getEyeHeight(this.pose, this.dimensions);

        if(this.dimensions.width() < oldDimensions.width()) {
            double d = (double)this.dimensions.width() / 2.0;
            this.box = new AABB(this.posX - d, this.posY, this.posZ - d, this.posX + d,
                this.posY + (double)this.dimensions.height(), this.posZ + d);
            return;
        }

        this.box = new AABB(this.box.minX, this.box.minY, this.box.minZ, this.box.minX + (double)this.dimensions.width(),
            this.box.minY + (double)this.dimensions.height(), this.box.minZ + (double)this.dimensions.width());

        if(this.dimensions.width() > oldDimensions.width() && !this.firstUpdate) {
            float f = oldDimensions.width() - this.dimensions.width();
            this.move(world, MoverType.SELF, f, 0.0D, f);
        }
    }

    public final float getEyeHeight(Pose pose, EntityDimensions dimensions) {
         return switch(pose) {
            case Pose.SWIMMING, Pose.FALL_FLYING, Pose.SPIN_ATTACK -> 0.4F;
            case Pose.CROUCHING -> 1.27F;
            case Pose.SLEEPING -> 0.2F;
            default -> 1.62F;
        };
    }

    public void tickMovementClientPlayer(LevelReader world) {
        this.inSneakingPose = !this.swimming && this.wouldPoseNotCollide(world, Pose.CROUCHING)
            && (this.input.playerInput.sneak() || !this.sleeping && !this.wouldPoseNotCollide(world, Pose.STANDING));
        this.input.tick();

        if(this.ticksToNextAutojump > 0) {
            --this.ticksToNextAutojump;
            this.input.playerInput = new TungstenPlayerInput(
            		this.input.playerInput.forward(),
            		this.input.playerInput.backward(),
            		this.input.playerInput.left(),
            		this.input.playerInput.right(),
        			true,
        			this.input.playerInput.sneak(),
        			this.input.playerInput.sprint()
        		);
        }

        double width = this.dimensions.width();
        this.pushOutOfBlocks(world, this.posX - width * 0.35D, this.posZ + width * 0.35D);
        this.pushOutOfBlocks(world, this.posX - width * 0.35D, this.posZ - width * 0.35D);
        this.pushOutOfBlocks(world, this.posX + width * 0.35D, this.posZ - width * 0.35D);
        this.pushOutOfBlocks(world, this.posX + width * 0.35D, this.posZ + width * 0.35D);


        if(this.sprinting) {
            if(this.swimming) {
            	if (this.shouldStopSwimSprinting()) {
					this.setSprinting(false);
				}
            } else if (this.shouldStopSprinting()) {
				this.setSprinting(false);
			}
        }
        
        if(this.canStartSprinting()) {
            if(this.keySprint) {
                this.setSprinting(true);
            }
        }

        if(this.touchingWater && this.input.playerInput.sneak()) {
            this.velY -= 0.04F;
        }

        this.tickMovementPlayer(world);
    }

    public void tickMovementPlayer(LevelReader world) {
        this.tickMovementLiving(world);

        this.airStrafingSpeed = 0.02F;

        if(this.sprinting) {
            this.airStrafingSpeed += 0.006F;
        }
    }

    public void tickMovementLiving(LevelReader world) {
        if(this.jumpingCooldown > 0) {
            --this.jumpingCooldown;
        }
        
        Vec2 vec2f = this.applyMovementSpeedFactors(this.input.getMovementInput());
		this.sidewaysSpeed = vec2f.x;
		this.forwardSpeed = vec2f.y;
        this.jumping = this.input.playerInput.jump();

        
        //#if MC < 12111
        //$$ if(Math.abs(this.velX) < 0.003) this.velX = 0.0;
        //$$ if(Math.abs(this.velZ) < 0.003) this.velZ = 0.0;
        //#else
        // 1.21.11 LivingEntity.tickMovement treats PLAYER horizontal velocity as
        // a vector. A small strafe component persists while the other axis moves;
        // snapping each axis separately changes a parkour landing by centimetres.
        if (this.velX * this.velX + this.velZ * this.velZ < 9.0E-6) {
            this.velX = 0.0;
            this.velZ = 0.0;
        }
        //#endif
        if(Math.abs(this.velY) < 0.003) this.velY = 0.0;

        if(this.jumping) {
            double k = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean bl = this.touchingWater && k > 0.0;
            double l = (double)this.standingEyeHeight < 0.4D ? 0.0D : 0.4D;

            if(bl && (!this.onGround || k > l)) {
                this.velY += 0.04F;
            } else if(!this.isInLava() || this.onGround && !(k > l)) {
                // Block damage triggers scheduleVelocityUpdate() on the server,
                // which resyncs velocity and cancels any jump on the same tick.
                // Suppress jump until hurtTicks expire (immunity frames).
                boolean jumpSuppressed = TungstenConfig.get().predictDamageFromBlocks
                        && this.hurtTicks > 8;
                if((this.onGround || bl) && this.jumpingCooldown == 0 && !jumpSuppressed) {
                    this.jump(world);
                    this.jumpingCooldown = 10;
                }
            } else {
                this.velY += 0.04F;
            }
        } else {
            this.jumpingCooldown = 0;
        }
        


        this.travelPlayer(world);

        /* Entity pushing
        if(this.riptideTicks > 0) {
            --this.riptideTicks;
            this.tickRiptide(box, this.getBoundingBox());
        }
        this.tickCramming();*/
    }

    public void jump(LevelReader world) {
        float newY = this.getJumpVelocity(world);

        if(this.jumpBoost >= 0) {
            newY += 0.1F * (float)(this.jumpBoost + 1);
        }

        this.velY = newY;

        if(this.sprinting) {
            float g = this.yaw * (float)(Math.PI / 180);
            this.velX += (double)(-Mth.sin(g)) * 0.2;
            this.velZ +=  (double)Mth.cos(g) * 0.2;
        }
    }

    public float getJumpVelocity(LevelReader world) {
        return 0.42F * this.getJumpVelocityMultiplier(world);
    }

    public float getJumpVelocityMultiplier(LevelReader world) {
        BlockPos pos1 = new BlockPos(this.blockX, this.blockY, this.blockZ);
        BlockPos pos2 = new BlockPos(this.blockX, Mth.floor(this.box.minY - 0.500001D), this.blockZ);
        float f = world.getBlockState(pos1).getBlock().getJumpFactor();
        float g = world.getBlockState(pos2).getBlock().getJumpFactor();
        return (double)f == 1.0D ? g : f;
    }

    public void updateVelocity(float speed) {
        double squaredMagnitude = (double)this.sidewaysSpeed * (double)this.sidewaysSpeed
                                + (double)this.upwardSpeed * (double)this.upwardSpeed
                                + (double)this.forwardSpeed * (double)this.forwardSpeed;

        if (squaredMagnitude < 1.0E-7) return;

        double sideways = this.sidewaysSpeed, upward = this.upwardSpeed, forward = this.forwardSpeed;

        if(squaredMagnitude > 1.0D) {
            double magnitude = Math.sqrt(squaredMagnitude);
            if (magnitude < 1.0E-4) { return; }
            else { sideways /= magnitude; upward /= magnitude; forward /= magnitude; }
        }

        sideways *= speed; upward *= speed; forward *= speed;
        float f = Mth.sin(yaw * (float) (Math.PI / 180.0));
		float g = Mth.cos(yaw * (float) (Math.PI / 180.0));

        this.velX += sideways * (double)g - forward * (double)f;
        this.velY += upward;
        this.velZ += forward * (double)g + sideways * (double)f;
    }

    public void travelPlayer(LevelReader world) {
        if(this.swimming) {
            float g = -Mth.sin(this.pitch * ((float)Math.PI / 180));
            double h = g < -0.2 ? 0.085 : 0.06;

            BlockPos pos = new BlockPos(this.blockX, Mth.floor(this.posY + 1.0D - 0.1D), this.blockZ);

            if(g <= 0.0D || this.jumping || !world.getBlockState(pos).getFluidState().isEmpty()) {
                this.velY += (g - this.velY) * h;
            }
        }

        this.travelLiving(world);
    }

    public void travelLiving(LevelReader world) {
        boolean falling = this.velY <= 0.0D;
        double fallSpeed = 0.08D;

        if(falling && this.slowFalling >= 0) {
            fallSpeed = 0.01D;
            this.fallDistance = 0.0F;
        }

        if(this.touchingWater) {
            double startY = this.posY;
            float swimSpeed = this.sprinting ? 0.9F : 0.8F;
            float speed = 0.02F;

            float h = this.depthStrider;
            if(h > 3.0F) h = 3.0F;
            if(!this.onGround) h *= 0.5F;

            if(h > 0.0F) {
                swimSpeed += (0.54600006F - swimSpeed) * h / 3.0F;
                speed += (this.movementSpeed - speed) * h / 3.0F;
            }

            if(this.dolphinsGrace >= 0) {
                swimSpeed = 0.96F;
            }

            this.updateVelocity(speed);
            this.move(world, MoverType.SELF, this.velX, this.velY, this.velZ);

            if(this.horizontalCollision && this.isClimbing(world)) { this.velY = 0.2D; }
            this.velX *= swimSpeed; this.velY *= 0.8F; this.velZ *= swimSpeed;

            this.method_26317(fallSpeed, falling);

            boolean moveNoCollisions = this.doesNotCollide(world, this.velX, this.velY + (double)0.6F - this.posY + startY, this.velZ);

            if(this.horizontalCollision && moveNoCollisions) {
                this.velY = 0.3F;
            }
        } else if(this.isInLava()) {
            double startY = this.posY;
            this.updateVelocity(0.02F);

            this.move(world, MoverType.SELF, this.velX, this.velY, this.velZ);

            if(this.getFluidHeight(FluidTags.LAVA) <= ((double)this.standingEyeHeight < 0.4D ? 0.0D : 0.4D)) {
                this.velX *= 0.5D; this.velY *= 0.8F; this.velZ *= 0.5D;
                this.method_26317(fallSpeed, falling);
            } else {
                this.velX *= 0.5D; this.velY *= 0.5D; this.velZ *= 0.5D;
            }

            this.velY -= fallSpeed / 4.0D;

            boolean moveNoCollisions = this.doesNotCollide(world, this.velX, this.velY + (double)0.6F - this.posY + startY, this.velZ);

            if(this.horizontalCollision && moveNoCollisions) {
                this.velY = 0.3F;
            }
        } else if(this.fallFlying) {
            //No elytra controls
            if(this.velY > -0.5D) {
                this.fallDistance = 1.0F;
            }

            float cYaw = Mth.cos(-this.yaw * 0.017453292F);
            float sYaw = Mth.sin(-this.yaw * 0.017453292F);
            float cPitch = Mth.cos(this.pitch * 0.017453292F);
            float sPitch = Mth.sin(this.pitch * 0.017453292F);
            double facingX = sYaw * cPitch;
            double facingY = -sPitch;
            double facingZ = cYaw * cPitch;

            double facingHM = Math.sqrt(facingX * facingX + facingZ * facingZ);
            double velHM = Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);

            float cPitchSq = (float)((double)cPitch * (double)cPitch);
            this.velY += fallSpeed * (-1.0D + (double)cPitchSq * 0.75D);

            if(this.velY < 0.0D && facingHM > 0.0D) {
                double q = this.velY * -0.1D * (double)cPitchSq;
                this.velX += facingX * q / facingHM;
                this.velY += q;
                this.velZ += facingZ * q / facingHM;
            }

            if(this.pitch < 0.0F && facingHM > 0.0D) {
                double q = velHM * facingY * 0.04D;
                this.velX -= facingX * q / facingHM;
                this.velY += q * 3.2D;
                this.velZ -= facingZ * q / facingHM;
            }

            if(facingHM > 0.0D) {
                this.velX += (facingX / facingHM * velHM - this.velX) * 0.1D;
                this.velZ += (facingZ / facingHM * velHM - this.velZ) * 0.1D;
            }

            this.velX *= 0.9900000095367432D;
            this.velY *= 0.9800000190734863D;
            this.velZ *= 0.9900000095367432D;
            this.move(world, MoverType.SELF, this.velX, this.velY, this.velZ);

            //Mojang why? WHYYYYYYYYYYYYYYY???
            if(this.onGround /*&& !world.isClient*/) {
                this.fallFlying = false;
            }
        } else {
            BlockPos pos = this.getVelocityAffectingPos(world);
            float slipperiness = stateOrAir(world, pos).getBlock().getFriction();   // off-thread null-safe (G107b)
            float xzDrag = this.onGround ? slipperiness * 0.91F : 0.91F;
            double ajuVelY = this.applyMovementInput(world, slipperiness);

            if (this.levitation >= 0) {
                ajuVelY += (0.05D * (double)(this.levitation + 1) - ajuVelY) * 0.2D;
                this.fallDistance = 0.0F;
            } else {
                ajuVelY -= fallSpeed;
            }

            this.velX *= xzDrag;
            this.velY = ajuVelY * (double)0.98F;
            this.velZ *= xzDrag;
        }
    }

    public double applyMovementInput(LevelReader world, float f) {
        this.updateVelocity(this.getMovementSpeed(f));
        this.applyClimbingSpeed(world);

        this.move(world, MoverType.SELF, this.velX, this.velY, this.velZ);

        if((this.horizontalCollision || this.jumping) && this.isClimbing(world)) {
            return 0.2D;
        }

        return this.velY;
    }

    private float getMovementSpeed(float slipperiness) {
        if(this.onGround) {
            return this.movementSpeed * (0.21600002F / (slipperiness * slipperiness * slipperiness));
        }	

        return this.sprinting ? 0.025999999F : 0.02F;
    }

    private void applyClimbingSpeed(LevelReader world) {
        if(this.isClimbing(world)) {
            this.fallDistance = 0.0f;
            this.velX = Mth.clamp(this.velX, -0.15000000596046448D, 0.15000000596046448D);
            this.velY = Math.max(this.velY, -0.15000000596046448D);
            this.velZ = Mth.clamp(this.velZ, -0.15000000596046448D, 0.15000000596046448D);

            BlockState state = stateOrAir(world, new BlockPos(this.blockX, this.blockY, this.blockZ));   // off-thread null-safe (G107b)

            if(this.velY < 0.0D && !state.is(Blocks.SCAFFOLDING) && this.input.playerInput.sneak()) {
                this.velY = 0.0D;
            }
        }
    }

    public Iterable<VoxelShape> getBlockCollisions(LevelReader world, AABB box) {
        return () -> new AgentBlockCollisions(world, this, box);
    }

    public boolean isSpaceEmpty(LevelReader world, AABB box) {
        for(VoxelShape voxelShape : this.getBlockCollisions(world, box)) {
            if(!voxelShape.isEmpty()) {
                return false;
            }
        }

        // Skip entity collisions — not thread-safe (ConcurrentModificationException
        // from pathfinder thread) and unpredictable for future ticks anyway.
        return true;
    }

    private boolean doesNotCollide(LevelReader world, double offsetX, double offsetY, double offsetZ) {
        AABB box = this.box.move(offsetX, offsetY, offsetZ);
        return this.isSpaceEmpty(world, box) && !world.containsAnyLiquid(box);
    }

    public void method_26317(double fallSpeed, boolean falling) {
        if(!this.sprinting) {
            boolean b = falling && Math.abs(this.velY - 0.005D) >= 0.003D && Math.abs(this.velY - fallSpeed / 16.0D) < 0.003D;
            this.velY = b ? -0.003D : this.velY - fallSpeed / 16.0D;
        }
    }

    public void move(LevelReader world, MoverType type, double movX, double movY, double movZ) {
        //if(type == MovementType.PISTON && (movement = this.adjustMovementForPiston(movement)).equals(Vec3d.ZERO)) {
        //    return;
        //}

        if(this.mulX * this.mulX + this.mulY * this.mulY + this.mulZ * this.mulZ > 0.0000001D) {
            movX *= this.mulX; movY *= this.mulY; movZ *= this.mulZ;
            this.mulX = 0; this.mulY = 0; this.mulZ = 0;
            this.velX = 0; this.velY = 0; this.velZ = 0;
        }


        Vec3 vec1 = this.adjustMovementForSneaking(world, type, new Vec3(movX, movY, movZ));
        movX = vec1.x; movY = vec1.y; movZ = vec1.z;

        Vec3 vec2 = this.adjustMovementForCollisions(world, new Vec3(movX, movY, movZ));
        double ajuX = vec2.x, ajuY = vec2.y, ajuZ = vec2.z;

        double magnitudeSq = ajuX * ajuX + ajuY * ajuY + ajuZ * ajuZ;

		if(magnitudeSq > 0.0000001D) {
            if(this.fallDistance != 0.0F && magnitudeSq >= 1.0D) {
                ClipContext context = new AgentRaycastContext(this.getPos(), this.getPos().add(new Vec3(ajuX, ajuY, ajuZ)),
                    ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this);
                BlockHitResult result = world.clip(context);

                if(result.getType() != HitResult.Type.MISS) {
                    this.fallDistance = 0.0F;
                }
            }

            this.setPos(this.posX + ajuX, this.posY + ajuY, this.posZ + ajuZ);
			this.box = this.dimensions.makeBoundingBox(this.posX, this.posY, this.posZ);
        }

        boolean xSimilar = !Mth.equal(movX, ajuX);
        boolean zSimilar = !Mth.equal(movZ, ajuZ);
        this.horizontalCollision = xSimilar || zSimilar;
        this.verticalCollision = movY != ajuY;
        this.collidedSoftly = this.horizontalCollision && this.hasCollidedSoftly(ajuX, ajuY, ajuZ);
        this.onGround = this.verticalCollision && movY < 0.0D;

        BlockPos landingPos = this.getLandingPos(world);
        BlockState landingState = stateOrAir(world, landingPos);   // off-thread null-safe (G107b)

        this.fall(world, ajuY, landingState);

        if(this.horizontalCollision) {
            if(xSimilar) this.velX = 0.0D;
            if(zSimilar) this.velZ = 0.0D;
        }

        Block block = landingState.getBlock();

        if(movY != ajuY) {
            if(block instanceof SlimeBlock && !this.input.playerInput.sneak()) {
                if(this.velY < 0.0D) {
                	this.velY *= -1;
                	this.slimeBounce = true;
                }
            } else if(block instanceof BedBlock) {
                if(this.velY < 0.0D) this.velY *= -1 * (double)0.66F;
            } else {
                this.velY = 0;
            }
        }

        if(this.onGround && !this.input.playerInput.sneak()) {
            if(block instanceof MagmaBlock) {
                if (TungstenConfig.get().predictDamageFromBlocks) {
                    this.hurtTicks = 10; this.isDamaged = true;
                }
            } else if(block instanceof SlimeBlock) {
                double d = Math.abs(this.velY);

                if(d < 0.1D) {
                    this.velX *= 0.4D + d * 0.2D;
                    this.velZ *= 0.4D + d * 0.2D;
                	this.slimeBounce = true;
                }
            } else if(block instanceof TurtleEggBlock) {
                //eggs can break (1/100)
            }
        }

        this.checkBlockCollision(world);

        float i = this.getVelocityMultiplier(world);
        this.velX *= i; this.velZ *= i;

		/*
		if (this.world.method_29556(this.getBoundingBox().contract(0.001))
		.noneMatch(blockState -> blockState.isIn(BlockTags.FIRE) || blockState.isOf(Blocks.LAVA)) && this.fireTicks <= 0) {
			this.setFireTicks(-this.getBurningDuration());
		}*/
    }

    private boolean hasCollidedSoftly(double ajuX, double ajuY, double ajuZ) {
        float f = this.yaw * ((float)Math.PI / 180);
        double d = Mth.sin(f);
        double e = Mth.cos(f);
        double g = (double)this.sidewaysSpeed * e - (double)this.forwardSpeed * d;
        double h = (double)this.forwardSpeed * e + (double)this.sidewaysSpeed * d;
        double i = Mth.square(g) + Mth.square(h);
        double j = Mth.square(ajuX) + Mth.square(ajuZ);

        if(i < (double)1.0E-5F || j < (double)1.0E-5F) {
            return false;
        }

        double k = g * ajuX + h * ajuZ;
        double l = Math.acos(k / Math.sqrt(i * j));
        return l < 0.13962633907794952D;
    }

    public Vec3 adjustMovementForSneaking(LevelReader world, MoverType type, Vec3 movement) {
        if(this.input.playerInput.sneak() && (type == MoverType.SELF || type == MoverType.PLAYER)
            && (this.onGround || this.fallDistance < this.stepHeight
            && !this.isSpaceEmpty(world, this.box.move(0.0, this.fallDistance - this.stepHeight, 0.0)))) {
            double d = movement.x;
            double e = movement.z;

            while(d != 0.0 && this.isSpaceEmpty(world, this.box.move(d, -this.stepHeight, 0.0))) {
                if(d < 0.05 && d >= -0.05) { d = 0.0; continue; }
                if(d > 0.0) { d -= 0.05; continue; }
                d += 0.05;
            }

            while(e != 0.0 && this.isSpaceEmpty(world, this.box.move(0.0, -this.stepHeight, e))) {
                if(e < 0.05 && e >= -0.05) { e = 0.0; continue; }
                if(e > 0.0) { e -= 0.05; continue; }
                e += 0.05;
            }

            while(d != 0.0 && e != 0.0 && this.isSpaceEmpty(world, this.box.move(d, -this.stepHeight, e))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? (d -= 0.05) : (d += 0.05));
                if(e < 0.05 && e >= -0.05) { e = 0.0; continue; }
                if(e > 0.0) { e -= 0.05; continue; }
                e += 0.05;
            }

            movement = new Vec3(d, movement.y, e);
        }

        return movement;
    }

    private Vec3 adjustMovementForCollisions(LevelReader world, Vec3 movement) {
        AABB box = this.box;
        // Skip entity collisions — not thread-safe from pathfinder thread
        // and entities are unpredictable for pathfinding anyway.
        List<VoxelShape> list = List.of();
        Vec3 vec3d = movement.lengthSqr() == 0.0 ? movement : this.adjustMovementForCollisions(movement, box, world, list);
        boolean bl = movement.x != vec3d.x;
        boolean bl2 = movement.y != vec3d.y;
        boolean bl3 = movement.z != vec3d.z;
		boolean bl4 = bl2 && movement.y < 0.0;
        boolean bl5 = this.onGround || bl4;

        if(this.stepHeight > 0.0f && bl5 && (bl || bl3)) {
//            Vec3d vec3d2 = this.adjustMovementForCollisions(new Vec3d(movement.x, this.stepHeight, movement.z), box, world, list);
//            Vec3d vec3d3 = this.adjustMovementForCollisions(new Vec3d(0.0, this.stepHeight, 0.0), box.stretch(movement.x, 0.0, movement.z), world, list);
//            Vec3d vec3d4 = this.adjustMovementForCollisions(new Vec3d(movement.x, 0.0, movement.z), box.offset(vec3d3), world, list).add(vec3d3);
//
//            if(vec3d3.y < (double)this.stepHeight && vec3d4.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
//                vec3d2 = vec3d4;
//            }
//
//            if(vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
//                return vec3d2.add(this.adjustMovementForCollisions(new Vec3d(0.0, -vec3d2.y + movement.y, 0.0), box.offset(vec3d2), world, list));
//            }
        	AABB box2 = bl4 ? box.move(0.0, vec3d.y, 0.0) : box;
			AABB box3 = box2.expandTowards(movement.x, (double)this.stepHeight, movement.z);
			if (!bl4) {
				box3 = box3.expandTowards(0.0, -1.0E-5F, 0.0);
			}

			List<VoxelShape> list2 = this.findCollisionsForMovement(world, list, box3);
			float f = (float)vec3d.y;
			float[] fs = collectStepHeights(box2, list2, this.stepHeight, f);

			for (float g : fs) {
				Vec3 vec3d2 = adjustMovementForCollisions(new Vec3(movement.x, (double)g, movement.z), box2, list2);
				if (vec3d2.horizontalDistanceSqr() > vec3d.horizontalDistanceSqr()) {
					double d = box.minY - box2.minY;
					return vec3d2.add(0.0, -d, 0.0);
				}
			}
        }

        return vec3d;
    }
    
    private List<VoxelShape> findCollisionsForMovement(LevelReader world, List<VoxelShape> regularCollisions, AABB movingEntityBoundingBox
    	) {
    		Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(regularCollisions.size() + 1);
    		if (!regularCollisions.isEmpty()) {
    			builder.addAll(regularCollisions);
    		}

    		builder.addAll(this.getBlockCollisions(world, movingEntityBoundingBox));
    		return builder.build();
    	}
    
    private static float[] collectStepHeights(AABB collisionBox, List<VoxelShape> collisions, float f, float stepHeight) {
		FloatSet floatSet = new FloatArraySet(4);

		for (VoxelShape voxelShape : collisions) {
			for (double d : voxelShape.getCoords(Axis.Y)) {
				float g = (float)(d - collisionBox.minY);
				if (!(g < 0.0F) && g != stepHeight) {
					if (g > f) {
						break;
					}

					floatSet.add(g);
				}
			}
		}

		float[] fs = floatSet.toFloatArray();
		FloatArrays.unstableSort(fs);
		return fs;
	}

    public Vec3 adjustMovementForCollisions(Vec3 movement, AABB entityBoundingBox, LevelReader world, List<VoxelShape> entityCollisions) {
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(entityCollisions.size() + 1);

        if(!entityCollisions.isEmpty()) {
            builder.addAll(entityCollisions);
        }

        builder.addAll(this.getBlockCollisions(world, entityBoundingBox.expandTowards(movement)));
        return this.adjustMovementForCollisions(movement, entityBoundingBox, builder.build());
    }

    private Vec3 adjustMovementForCollisions(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        if(collisions.isEmpty()) {
            return movement;
        }

        double d = movement.x;
        double e = movement.y;
        double f = movement.z;
        boolean bl = Math.abs(d) < Math.abs(f);

        if(e != 0.0D) {
            e = Shapes.collide(Direction.Axis.Y, entityBoundingBox, collisions, e);
            if (e != 0.0D) entityBoundingBox = entityBoundingBox.move(0.0D, e, 0.0D);
        }

        if(bl && f != 0.0D) {
            f = Shapes.collide(Direction.Axis.Z, entityBoundingBox, collisions, f);
            if(f != 0.0D) entityBoundingBox = entityBoundingBox.move(0.0D, 0.0D, f);
        }

        if(d != 0.0D) {
            d = Shapes.collide(Direction.Axis.X, entityBoundingBox, collisions, d);
            if(!bl && d != 0.0D) entityBoundingBox = entityBoundingBox.move(d, 0.0D, 0.0D);
        }

        if (!bl && f != 0.0D) {
            f = Shapes.collide(Direction.Axis.Z, entityBoundingBox, collisions, f);
        }

        return new Vec3(d, e, f);
    }

    /**
     * A block read that never returns null, for the off-thread physics simulation (G107b). Every
     * getBlockState in this class runs on the FastNavigator-plan thread against a live ClientWorld
     * the render thread is swapping chunk sections on, so a read can hand back null for one tick.
     * The physics predicates then NPE'd on it (isClimbing was one, fixed in 243a2cab; the fall
     * landing read and getVelocityMultiplier were the two the second-pass review flagged next).
     * An unloaded/racing cell behaves like AIR for physics -- default slipperiness (0.6), velocity
     * multiplier 1.0, isOf(...) false -- which is exactly what vanilla's EmptyChunk returns, so
     * this is purely additive: it changes nothing when the read is non-null.
     */
    private static BlockState stateOrAir(LevelReader world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        return s == null ? Blocks.AIR.defaultBlockState() : s;
    }

    public boolean isClimbing(LevelReader world) {
        BlockState state = world.getBlockState(new BlockPos(this.blockX, this.blockY, this.blockZ));
        // ⛔ AN OFF-THREAD SEARCH READS A LIVE ClientWorld, AND A LIVE WORLD RETURNS null MID-SWAP.
        // The planner runs on the FastNavigator-plan thread while the render thread keeps swapping
        // chunk sections in and out under the flee's wide search; for one read getBlockState can
        // hand back null, and a null here NPE'd the WHOLE plan on `.isIn(CLIMBABLE)` — observed once
        // as "FastNavigator plan failed: BlockState.isIn(...) $$1 null" at the moment the bot tried
        // to flee a cave mob nest at y=2, so the escape route never computed and it died (2026-09-18
        // @gamer run). This method is called on nearly every node expansion, so it is the site that
        // meets the race most often. A block that is not there to be read is not climbable.
        if (state == null) return false;
        if(state.is(BlockTags.CLIMBABLE)) return true;
        if(state.getBlock() instanceof TrapDoorBlock && this.canEnterTrapdoor(world, state)) return true;
        return false;
    }

    private boolean canEnterTrapdoor(LevelReader world, BlockState trapdoor) {
        if(!trapdoor.getValue(TrapDoorBlock.OPEN)) return false;

        BlockState ladder = world.getBlockState(new BlockPos(this.blockX, this.blockY - 1, this.blockZ));

        if(ladder.is(Blocks.LADDER) && ladder.getValue(LadderBlock.FACING) == trapdoor.getValue(TrapDoorBlock.FACING)) {
            return true;
        }

        return false;
    }

    void checkWaterState(LevelReader world) {
        if(this.updateMovementInFluid(world, FluidTags.WATER, 0.014D)) {
            this.fallDistance = 0.0F;
            this.touchingWater = true;
        } else {
            this.touchingWater = false;
        }
    }

    public boolean updateMovementInFluid(LevelReader world, TagKey<Fluid> tag, double d) {
        int n;
        AABB box = this.box.deflate(0.001D);
        int i = Mth.floor(box.minX);
        int j = Mth.ceil(box.maxX);
        int k = Mth.floor(box.minY);
        int l = Mth.ceil(box.maxY);
        int m = Mth.floor(box.minZ);

        if (!world.hasChunksAt(i, k, m, j, l, n = Mth.ceil(box.maxZ))) {
            return false;
        }

        double e = 0.0;
        boolean bl2 = false;
        Vec3 vec3d = Vec3.ZERO;
        int o = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for(int p = i; p < j; ++p) {
            for(int q = k; q < l; ++q) {
                for(int r = m; r < n; ++r) {
                    double f;
                    mutable.set(p, q, r);
                    FluidState fluidState = world.getFluidState(mutable);
                    if (!fluidState.is(tag) || !((f = (float)q + fluidState.getHeight(world, mutable)) >= box.minY)) continue;
                    bl2 = true;
                    e = Math.max(f - box.minY, e);

                    Vec3 vec3d2 = fluidState.getFlow(world, mutable);

                    if(e < 0.4) {
                        vec3d2 = vec3d2.scale(e);
                    }

                    vec3d = vec3d.add(vec3d2);
                    ++o;
                }
            }
        }

        if(vec3d.length() > 0.0) {
            if(o > 0) {
                vec3d = vec3d.scale(1.0 / (double)o);
            }

            Vec3 vec3d3 = new Vec3(this.velX, this.velY, this.velZ);
            vec3d = vec3d.scale(d);

            if(Math.abs(vec3d3.x) < 0.003 && Math.abs(vec3d3.z) < 0.003 && vec3d.length() < 0.0045000000000000005) {
                vec3d = vec3d.normalize().scale(0.0045000000000000005);
            }

            this.velX += vec3d.x;
            this.velY += vec3d.y;
            this.velZ += vec3d.z;
        }

        this.fluidHeight.put(tag, e);
        return bl2;
    }

    public void fall(LevelReader world, double heightDifference, BlockState landedState) {
        if(!this.touchingWater) {
            this.checkWaterState(world);
        }

        //add soulspeed movement boost

        if(this.onGround) {
            if(this.fallDistance > 0.0F) {
                if(landedState.getBlock() instanceof BedBlock) {
                    this.handleFallDamage(this.fallDistance * 0.6F, 1.0F);
                } else if(landedState.getBlock() instanceof FarmlandBlock) {
                    //grief
                } else if(landedState.getBlock() instanceof HayBlock) {
                    this.handleFallDamage(this.fallDistance, 0.2F);
                } else if(landedState.getBlock() instanceof SlimeBlock) {
                    this.handleFallDamage(this.fallDistance, this.input.playerInput.sneak() ? 1.0F : 0.0F);
                } else if(landedState.getBlock() instanceof TurtleEggBlock) {
                    //eggs can break (1/3)
                    this.handleFallDamage(this.fallDistance, 1.0F);
                } else {
                    this.handleFallDamage(this.fallDistance, 1.0F);
                }
            }

            this.fallDistance = 0.0F;
        } else if(heightDifference < 0.0D) {
            this.fallDistance -= (float)heightDifference;
        }

        if(this.touchingWater) {
            this.fallDistance = 0F;
        }
    }

    public boolean handleFallDamage(double fallDistance, float damageMultiplier) {
        int i = this.computeFallDamage(fallDistance, damageMultiplier);

        if(i > 0) {
            //this.damage(DamageSource.FALL, i);
        	this.isDamaged = true; 
        	this.velX = 0;
        	this.velZ = 0;
            return true;
        }

        return false;
    }

    public int computeFallDamage(double fallDistance, float damageMultiplier) {
    	if (TungstenModDataContainer.player == null) return 0;
    	if (TungstenModDataContainer.player.getType().builtInRegistryHolder().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
    		return 0;
    	}
        float f = this.jumpBoost < 0 ? 0.0F : (float)(this.jumpBoost + 1);
        return Mth.ceil((fallDistance - 3.0f - f) * damageMultiplier);
    }

    public void checkBlockCollision(LevelReader world) {
    	double minOffset = 0.001; // default 0.001
    	double maxOffset = 0.001; // default 0.001
        BlockPos blockPos = new BlockPos(Mth.floor(this.box.minX + minOffset), Mth.floor(this.box.minY + minOffset), Mth.floor(this.box.minZ + minOffset));
        BlockPos blockPos2 = new BlockPos(Mth.floor(this.box.maxX - maxOffset), Mth.floor(this.box.maxY - maxOffset), Mth.floor(this.box.maxZ - maxOffset));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if(world.hasChunksAt(blockPos, blockPos2)) {
            for(int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                for(int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                    for(int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                        pos.set(i, j, k);
                        BlockState state = world.getBlockState(pos);

                        if(state.getBlock() instanceof BaseFireBlock) {
                            if (TungstenConfig.get().predictDamageFromBlocks) {
                                this.hurtTicks = 10; this.isDamaged = true;
                            }
                        } else if(state.getBlock() instanceof BasePressurePlateBlock) {
                            //change block state
                        } else if(state.getBlock() instanceof BubbleColumnBlock) {
                            BlockState surface = world.getBlockState(pos.above());
                            boolean drag = surface.hasProperty(BubbleColumnBlock.DRAG_DOWN) && surface.getValue(BubbleColumnBlock.DRAG_DOWN);

                            if(surface.isAir()) {
                                this.velY = drag ? Math.max(-0.9D, this.velY - 0.03D) : Math.min(1.8D, this.velY + 0.1D);
                            } else {
                                this.velY = drag ? Math.max(-0.3D, this.velY - 0.03D) : Math.min(0.7D, this.velY + 0.06D);
                                this.fallDistance = 0.0F;
                            }
                        } else if(state.getBlock() instanceof CactusBlock) {
                            if (TungstenConfig.get().predictDamageFromBlocks) {
                                this.hurtTicks = 10; this.isDamaged = true;
                            }
                        } else if(state.getBlock() instanceof CampfireBlock) {
                            if (TungstenConfig.get().predictDamageFromBlocks) {
                                this.hurtTicks = 10; this.isDamaged = true;
                            }
                        } else if(state.getBlock() instanceof CauldronBlock) {
                            //extinguish the entity
                        } else if(state.getBlock() instanceof WebBlock) {
                            this.fallDistance = 0.0F;
                            this.mulX = 0.25D; this.mulY = 0.05F; this.mulZ = 0.25D;
                        } else if(state.getBlock() instanceof EndPortalBlock) {
                            //fuck
                        } else if(state.getBlock() instanceof HoneyBlock) {
                            if(this.isSliding(pos)) {
                                if(this.velY < -0.13D) {
                                    double m = -0.05D / this.velY;
                                    this.velX *= m; this.velY = -0.05D; this.velZ *= m;
                                } else {
                                    this.velY = -0.05D;
                                }

                                this.fallDistance = 0.0F;
                            }
                        } else if(state.getBlock() instanceof NetherPortalBlock) {
                            //eh?
                        } else if(state.getBlock() instanceof SweetBerryBushBlock) {
                            this.mulX = 0.8f; this.mulY = 0.75D; this.mulZ = 0.8F;
                            if (TungstenConfig.get().predictDamageFromBlocks) {
                                this.hurtTicks = 10; this.isDamaged = true;
                            }
                        } else if(state.getBlock() instanceof TripWireBlock) {
                            //change block state
                        } else if(state.getBlock() instanceof WitherRoseBlock) {
                            if (TungstenConfig.get().predictDamageFromBlocks) {
                                this.hurtTicks = 10; this.isDamaged = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSliding(BlockPos pos) {
        if(this.onGround) return false;
        if(this.posY > (double)pos.getY() + 0.9375D - 1.0E-07D) return false;
        if(this.velY >= -0.08D) return false;

        double d = Math.abs((double)pos.getX() + 0.5D - this.posX);
        double e = Math.abs((double)pos.getZ() + 0.5D - this.posZ);
        double f = 0.4375D + (double)(this.dimensions.width() / 2.0F);
        return d + 1.0E-7D > f || e + 1.0E-7D > f;
    }

    public float getVelocityMultiplier(LevelReader world) {
    	BlockState blockState = stateOrAir(world, new BlockPos(this.blockX, this.blockY, this.blockZ));   // off-thread null-safe (G107b)
		float f = blockState.getBlock().getSpeedFactor();
		if (!blockState.is(Blocks.WATER) && !blockState.is(Blocks.BUBBLE_COLUMN)) {
			return (double)f == 1.0 ? stateOrAir(world, this.getLandingPos(world)).getBlock().getSpeedFactor() : f;
		} else {
			return f;
		}
    }
    
    protected static Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
		double d = movementInput.lengthSqr();
		if (d < 1.0E-7) {
			return Vec3.ZERO;
		} else {
			Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
			float f = Mth.sin(yaw * (float) (Math.PI / 180.0));
			float g = Mth.cos(yaw * (float) (Math.PI / 180.0));
			return new Vec3(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
		}
	}

    /**
     * Matches vanilla Entity.getPosWithYOffset(0.500001f) logic.
     * Vanilla uses cached supportingBlockPos with fence/wall special case.
     * We don't cache, but we replicate the fence/wall/gate check so that
     * friction is calculated from the correct block when standing on fences.
     */
    private static boolean isFenceLike(BlockState state) {
        // Null-safe for the same off-thread reason as isClimbing: a transient null read is not a
        // fence. Callers here pass getBlockState results straight in (getVelocityAffectingPos).
        if (state == null) return false;
        return state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)
                || state.getBlock() instanceof FenceGateBlock;
    }

    public BlockPos getVelocityAffectingPos(LevelReader world) {
        BlockPos pos = new BlockPos(Mth.floor(this.posX),
            Mth.floor(this.posY - 0.5000001D), Mth.floor(this.posZ));
        // Only do the fence check when on ground — in air, friction doesn't
        // matter and we save 2 getBlockState calls per tick per node.
        if (!this.onGround) return pos;
        BlockState state = world.getBlockState(pos);
        if (isFenceLike(state)) return pos;
        BlockPos below = pos.below();
        if (isFenceLike(world.getBlockState(below))) return below;
        return pos;
    }

    public BlockPos getLandingPos(LevelReader world) {
        BlockPos pos = new BlockPos(this.blockX, Mth.floor(this.posY - (double)0.2F), this.blockZ);

        // Off-thread null reads (see isClimbing): a null is treated as "nothing to land on here".
        BlockState here = world.getBlockState(pos);
        if(here != null && !here.isAir()) {
            return pos;
        }

        BlockState state = world.getBlockState(pos.below());

        if(state != null && (state.getBlock() instanceof FenceGateBlock || state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS))) {
            return pos.below();
        }

        return pos;
    }
    
    public boolean canSprint() {
		return this.hunger.getFoodLevel() > 6.0F;
	}
    
    public boolean shouldSlowDown() {
		return this.sneaking || this.keySneak;
	}
    

	private Vec2 applyMovementSpeedFactors(Vec2 input) {
		if (input.lengthSquared() == 0.0F) {
			return input;
		} else {
			Vec2 vec2f = input.scale(0.98F);
			if (this.usingItem) {
				vec2f = vec2f.scale(0.2F);
			}

			if (this.shouldSlowDown()) {
				float f = 0.3F;
				vec2f = vec2f.scale(f);
			}
			
			// Diagonal movement normalization (MC-271065), added in MC 1.21.4+.
			vec2f = applyDirectionalMovementSpeedFactors(vec2f);

			return vec2f;
		}
	}

	private static Vec2 applyDirectionalMovementSpeedFactors(Vec2 vec) {
		float f = vec.length();
		if (f <= 0.0F) {
			return vec;
		} else {
			Vec2 vec2f = vec.scale(1.0F / f);
			float g = getDirectionalMovementSpeedMultiplier(vec2f);
			float h = Math.min(f * g, 1.0F);
			return vec2f.scale(h);
		}
	}

	private static float getDirectionalMovementSpeedMultiplier(Vec2 vec) {
		float f = Math.abs(vec.x);
		float g = Math.abs(vec.y);
		float h = g > f ? f / g : g / f;
		return Mth.sqrt(1.0F + Mth.square(h));
	}
    
    private boolean canStartSprinting() {
		return Math.abs(this.forwardSpeed) > -0.1
			&& this.canSprint()
			&& this.keyForward
			&& !this.horizontalCollision
			&& !this.usingItem
			&& this.blindness < 0
			&& (!this.shouldSlowDown() || this.isSubmergedInWater)
			&& (!this.touchingWater || this.isSubmergedInWater);
	}

	private boolean shouldStopSprinting() {
		
		return this.forwardSpeed == 0
			|| !this.keyForward
			|| !this.canSprint()
			|| this.horizontalCollision && !this.collidedSoftly
			|| this.touchingWater && !this.isSubmergedInWater;
	}

	private boolean shouldStopSwimSprinting() {
		return !this.touchingWater
			|| !this.input.hasForwardMovement() && !this.onGround && !this.input.playerInput.sneak()
			|| !this.canSprint();
	}

    public AABB calculateBoundsForPose(Pose pose) {
        EntityDimensions size = POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
        float f = size.width() / 2.0F;
        Vec3 min = new Vec3(this.posX - (double)f, this.posY, this.posZ - (double)f);
        Vec3 max = new Vec3(this.posX + (double)f, this.posY + (double)size.height(), this.posZ + (double)f);
        return new AABB(min, max);
    }

    public boolean wouldPoseNotCollide(LevelReader world, Pose pose) {
        return this.isSpaceEmpty(world, this.calculateBoundsForPose(pose).deflate(1.0E-7));
    }

    private void pushOutOfBlocks(LevelReader world, double x, double d) {
        Direction[] directions = new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH };
        BlockPos blockPos = new BlockPos(Mth.floor(x), Mth.floor(this.posY), Mth.floor(d));
        if (!this.wouldCollideAt(world, blockPos)) {
            return;
        }
        double e = x - (double)blockPos.getX();
        double f = d - (double)blockPos.getZ();
        Direction direction = null;
        double g = Double.MAX_VALUE;

        for (Direction direction2 : directions) {
            double i;
            double h = direction2.getAxis().choose(e, 0.0D, f);
            double d2 = i = direction2.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - h : h;
            if (!(i < g) || this.wouldCollideAt(world, blockPos.relative(direction2))) continue;
            g = i;
            direction = direction2;
        }

        if(direction != null) {
            if(direction.getAxis() == Direction.Axis.X) {
                this.velX = 0.1D * (double)direction.getStepX();
            } else {
                this.velZ = 0.1D * (double)direction.getStepZ();
            }
        }
    }

    public boolean canCollide(LevelReader world, AABB box) {
        AgentBlockCollisions collisions = new AgentBlockCollisions(world, this, box, true);
        
        while (collisions.hasNext()) {
			if (!collisions.next().isEmpty()) {
				return true;
			}
		}

		return false;

//        if(!collisions.hasNext()) {
//            this.scannedBlocks += collisions.scannedBlocks;
//            return false;
//        }
//
//        while(collisions.next().isEmpty()) {
//            if(!collisions.hasNext()) {
//                this.scannedBlocks += collisions.scannedBlocks;
//                return false;
//            }
//        }
//
//        this.scannedBlocks += collisions.scannedBlocks;
//        return true;
    }

    /**
     * ⛔ FIXED 2026-09-04: this used to test collision against {@code this.box} -- the agent's
     * ACTUAL current bounding box, which does not depend on {@code pos} at all. The correctly
     * computed hypothetical box (vanilla Entity.wouldCollideAt: a 1x1xN column AT the given
     * block position, spanning the agent's own Y range) was already written out here, just
     * commented out and never used. Every caller of this method tests SEVERAL different
     * candidate positions expecting a different answer for each (pushOutOfBlocks's direction
     * loop tries four offsets to find a clear one) -- with the old body every candidate got the
     * identical answer (whatever this.box's own collision state happened to be), so the
     * direction search could never actually distinguish a clear direction from a blocked one.
     */
    private boolean wouldCollideAt(LevelReader world, BlockPos pos) {
    	AABB box = this.box;
		AABB box2 = new AABB((double)pos.getX(), box.minY, (double)pos.getZ(), (double)pos.getX() + 1.0, box.maxY, (double)pos.getZ() + 1.0).deflate(1.0E-7);
        return this.canCollide(world, box2);
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
        this.movementSpeed = 0.1F;

        if(sprinting) {
            this.movementSpeed *= (1.0D + (double)0.3F);
        }

        if(this.speed >= 0) {
            double amplifier = 0.20000000298023224D * (double)(this.speed + 1);
            this.movementSpeed *= (1.0D + amplifier);
        }
    }

    public double getFluidHeight(TagKey<Fluid> fluid) {
        return this.fluidHeight.getDouble(fluid);
    }

    public boolean isInLava() {
        return !this.firstUpdate && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0D;
    }

    /*

    public void tickCramming(WorldView world) {
        List<Entity> list = world.getOtherEntities(this, this.box, EntityPredicates.canBePushedBy(this));

        if(!list.isEmpty()) {
            int j;
            int i = this.world.getGameRules().getInt(GameRules.MAX_ENTITY_CRAMMING);
            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                j = 0;
                for (int k = 0; k < list.size(); ++k) {
                    if (list.get(k).hasVehicle()) continue;
                    ++j;
                }
                if (j > i - 1) {
                    this.damage(DamageSource.CRAMMING, 6.0f);
                }
            }
            for (j = 0; j < list.size(); ++j) {
                Entity entity = list.get(j);
                this.pushAway(entity);
            }
        }
    }

    public void pushAway(Entity entity) {
        double e;
        double d = this.posX - entity.getX();
        double f = MathHelper.absMax(d, e = this.posZ - entity.getZ());

        if (f >= (double)0.01F) {
            f = MathHelper.sqrt(f);
            d /= f;
            e /= f;

            double g = 1.0 / f;
            if(g > 1.0) g = 1.0;

            d *= g;
            e *= g;
            d *= 0.05F;
            e *= 0.05F;
            d *= 1.0F - entity.pushSpeedReduction;
            e *= 1.0F - entity.pushSpeedReduction;

            entity.addVelocity(-d, 0.0, -e);
            this.velX += d;
            this.velZ += e;
        }
    }

    public void tickRiptide(Box a, Box b) {
        Box box = a.union(b);
        List<Entity> list = this.world.getOtherEntities(this, box);
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); ++i) {
                Entity entity = list.get(i);
                if (!(entity instanceof LivingEntity)) continue;
                this.attackLivingEntity((LivingEntity)entity);
                this.riptideTicks = 0;
                this.setVelocity(this.getVelocity().multiply(-0.2));
                break;
            }
        } else if (this.horizontalCollision) {
            this.riptideTicks = 0;
        }
        if (!this.world.isClient && this.riptideTicks <= 0) {
            this.setLivingFlag(4, false);
        }
    }
    */

    /** Check if two doubles differ by more than the logging threshold. */
    private static boolean mismatch(double a, double b) {
        return Math.abs(a - b) > TungstenConfig.get().mismatchLogThreshold;
    }

    public void compare(Player player, TungstenPlayerInput playerInput, boolean executor) {
        if (executor && comparisonObserver != null) comparisonObserver.accept(this);
        List<String> values = new ArrayList<>();
        double logThreshold = TungstenConfig.get().mismatchLogThreshold;

        double posDrift = Math.abs(this.posX - player.getX())
                        + Math.abs(this.posY - player.getY())
                        + Math.abs(this.posZ - player.getZ());
        if(posDrift > logThreshold) {
            values.add(String.format("Position mismatch (drift=%.2e)\n  real: %20.12f %20.12f %20.12f\n  sim:  %20.12f %20.12f %20.12f",
                posDrift,
                player.getX(), player.getY(), player.getZ(),
                this.posX, this.posY, this.posZ));
            if (TungstenModDataContainer.EXECUTOR.isRunning()) {
                if (TungstenConfig.get().driftCorrectionEnabled) {
                    // Upstream behavior: snap player to simulated position
                    player.setPos(this.posX, this.posY, this.posZ);
                } else {
                    // Our default behavior: log + stop path on large drift
                    double drift = player.position().distanceTo(new Vec3(this.posX, this.posY, this.posZ));
                    // THE BOUND GROWS BECAUSE THE ERROR GROWS. Each replayed tick adds a little
                    // integration error, so a fixed bound means the longer a path runs correctly
                    // the likelier it is to be thrown away. Measured on a live @gamer run:
                    // "drift 0.830 (threshold 0.8) at tick 14" — a path abandoned for three
                    // centimetres. The tick-1 guarantee this check exists for is untouched.
                    int replayTick = Math.max(0, TungstenModDataContainer.EXECUTOR.getCurrentTick());
                    double allowed = kaptainwutax.tungsten.TungstenConfig.get().driftThreshold
                            + kaptainwutax.tungsten.TungstenConfig.get().driftPerTick * replayTick;
                    if (drift > allowed) {
                        Debug.logMessage(String.format(
                            "§c[Tungsten] Path stopped: drift %.3f blocks (threshold %.1f) at tick %d. " +
                            "Expected (%.2f, %.2f, %.2f), actual (%.2f, %.2f, %.2f)",
                            drift, allowed,
                            TungstenModDataContainer.EXECUTOR.getCurrentTick(),
                            this.posX, this.posY, this.posZ,
                            player.getX(), player.getY(), player.getZ()));
                        // Abandon the REPLAY (the simulated chain no longer matches
                        // reality), but do NOT kill the search: a drift abort used
                        // to set PATHFINDER.stop as well, so a single mismatch
                        // ended the whole navigation and the bot stood there. On a
                        // real-terrain chase that froze it for 111 s while the prey
                        // ran off (stand-measured). A replay mismatch is a reason to
                        // RE-PLAN, not to give up; the follow/goto layer re-issues a
                        // search on its next tick because the executor is now idle.
                        // NOTE (2026-07-28): gating this on "the path is non-empty" to stop
                        // it wiping a mining queue REGRESSED nav_gaps from a stable 6/6 to
                        // failing — the parkour hand-off relies on this abort firing. The
                        // silent-breakQueue-wipe is real (PathExecutor.tick logs it now) but
                        // must be fixed on the EXECUTOR side, not by weakening this abort.
                        TungstenModDataContainer.EXECUTOR.stop = true;
                    }
                }
                if (TungstenModRenderContainer.ERROR.size() > 1000) TungstenModRenderContainer.ERROR.clear();
            }
        }
        
        double velDrift = Math.abs(this.velX - player.getDeltaMovement().x)
                        + Math.abs(this.velY - player.getDeltaMovement().y)
                        + Math.abs(this.velZ - player.getDeltaMovement().z);
        if(velDrift > logThreshold) {
            values.add(String.format("Velocity mismatch (drift=%.2e)\n  real: %20.12f %20.12f %20.12f\n  sim:  %20.12f %20.12f %20.12f",
                velDrift,
                player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z,
                this.velX, this.velY, this.velZ));
            if (TungstenModDataContainer.EXECUTOR.isRunning()) {
                values.add(String.format("Velocity mismatch by (%s, %s, %s)",
                        player.getDeltaMovement().x - this.velX,
                        player.getDeltaMovement().y - this.velY,
                        player.getDeltaMovement().z - this.velZ));
                if (TungstenConfig.get().driftCorrectionEnabled) {
                    // Upstream behavior: snap velocity to simulated velocity
                    player.setDeltaMovement(this.velX, this.velY, this.velZ);
                }
                Node node = TungstenModDataContainer.EXECUTOR.getCurrentNode();
                if (TungstenModRenderContainer.ERROR.size() > 1000) TungstenModRenderContainer.ERROR.clear();
                if (node != null) {
                    RenderHelper.renderNode(node, TungstenModRenderContainer.ERROR);
                }
            }
        }

        if(this.mulX != ((AccessorEntity)player).getStuckSpeedMultiplier().x
            || this.mulY != ((AccessorEntity)player).getStuckSpeedMultiplier().y
            || this.mulZ != ((AccessorEntity)player).getStuckSpeedMultiplier().z) {
            values.add(String.format("Movement Multiplier mismatch (%s, %s, %s) vs (%s, %s, %s)",
                ((AccessorEntity)player).getStuckSpeedMultiplier().x,
                ((AccessorEntity)player).getStuckSpeedMultiplier().y,
                ((AccessorEntity)player).getStuckSpeedMultiplier().z,
                ((AccessorEntity)player).getStuckSpeedMultiplier().x == this.mulX ? "x" : this.mulX,
                ((AccessorEntity)player).getStuckSpeedMultiplier().y == this.mulY ? "y" : this.mulY,
                ((AccessorEntity)player).getStuckSpeedMultiplier().z == this.mulZ ? "z" : this.mulZ));
        }

        // Input Speed mismatch (diagonal normalize ~29% diff) is expected and
        // consistent — pathfinder uses the same normalize, so paths are valid.
        // Only log if explicitly requested via very low threshold.
        if(logThreshold < 0.01 && (mismatch(this.forwardSpeed, player.zza) || mismatch(this.sidewaysSpeed, player.xxa) || mismatch(this.upwardSpeed, player.yya))) {
            values.add(String.format("Input Speed mismatch (%s, %s, %s) vs (%s, %s, %s)",
                player.zza == this.forwardSpeed ? "f" : player.zza,
                player.yya == this.upwardSpeed ? "u" : player.yya,
                player.xxa == this.sidewaysSpeed ? "s" : player.xxa,
                player.zza == this.forwardSpeed ? "f" : this.forwardSpeed,
                player.yya == this.upwardSpeed ? "u" : this.upwardSpeed,
                player.xxa == this.sidewaysSpeed ? "s" : this.sidewaysSpeed));
        }

        if(mismatch(this.movementSpeed, player.getSpeed())) {

        	if (TungstenModDataContainer.LOG_DEBUG_DATA) {
        		Node node = TungstenModDataContainer.EXECUTOR.getCurrentNode();
            	if (node != null) {
    	        	StringBuilder string = new StringBuilder();
    	
    	    		string.append("{\n");
    	    		if (node.input.forward != playerInput.forward()) {
    	        		string.append("forward: ");
    	        		string.append(playerInput.forward());
    	        		string.append(" vs ");
    	        		string.append(node.input.forward);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.back != playerInput.backward()) {
    	        		string.append("back: ");
    	        		string.append(playerInput.backward());
    	        		string.append(" vs ");
    	        		string.append(node.input.back);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.right != playerInput.right()) {
    	        		string.append("right: ");
    	        		string.append(playerInput.right());
    	        		string.append(" vs ");
    	        		string.append(node.input.right);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.left != playerInput.left()) {
    	        		string.append("left: ");
    	        		string.append(playerInput.left());
    	        		string.append(" vs ");
    	        		string.append(node.input.left);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.jump != playerInput.jump()) {
    	        		string.append("jump: ");
    	        		string.append(playerInput.jump());
    	        		string.append(" vs ");
    	        		string.append(node.input.jump);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.sneak != playerInput.sneak()) {
    	        		string.append("sneak: ");
    	        		string.append(playerInput.sneak());
    	        		string.append(" vs ");
    	        		string.append(node.input.sneak);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.sprint != playerInput.sprint()) {
    	        		string.append("sprint: ");
    	        		string.append(playerInput.sprint());
    	        		string.append(" vs ");
    	        		string.append(node.input.sprint);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.pitch != player.getXRot()) {
    	        		string.append("pitch: ");
    	        		string.append(player.getXRot());
    	        		string.append(" vs ");
    	        		string.append(node.input.pitch);
    	        		string.append("\n");
    	    		}
    	    		if (node.input.yaw != player.getYRot()) {
    	        		string.append("yaw: ");
    	        		string.append(player.getYRot());
    	        		string.append(" vs ");
    	        		string.append(node.input.yaw);
    	        		string.append("\n");
    	    		}
    	    		if (node.agent.onGround != player.onGround()) {
    	        		string.append("isOnGround: ");
    	        		string.append(player.onGround());
    	        		string.append(" vs ");
    	        		string.append(node.agent.onGround);
    	        		string.append("\n");
    	    		}
    	    		string.append("\n");
    	    		string.append("}");
    	
    	        	if (string.toString().length() > 4) {
    		        	Debug.logMessage("------------");
    		        	Debug.logMessage(string.toString());
    		        	Debug.logMessage("Current Movement Speed: " + player.getSpeed() + " \nExpected to be: " + this.movementSpeed + "");
    		        	Debug.logMessage("------------");
    	        	} else {
    		        	Debug.logMessage("Current Movement Speed: " + player.getSpeed() + " \nExpected to be: " + this.movementSpeed + "");
    	        	}
            	}	
        	}
        	
            values.add(String.format("Movement Speed mismatch\n  real: %.10f\n  sim:  %.10f", player.getSpeed(), this.movementSpeed));
        }

        if(this.pose != player.getPose()) {
            values.add(String.format("Pose mismatch %s vs %s", player.getPose(), this.pose));
        }

        // ⛔ FIXED 2026-09-04: this checked isSubmergedInWater (a copy-paste from the check
        // below) while labeling and printing sprinting values -- so a real sprint-state drift
        // between the simulation and the actual player was never detected by this diagnostic,
        // and a submerged-water drift produced a bogus "Sprinting mismatch" line alongside the
        // correct "Submerged in water mismatch" line a few lines down.
        if(this.sprinting != player.isSprinting()) {
            values.add(String.format("Sprinting mismatch %s vs %s", player.isSprinting(), this.sprinting));
        }

        if(this.touchingWater != player.isInWater()) {
            values.add(String.format("Touching water mismatch %s vs %s", player.isInWater(), this.touchingWater));
        }

        if(this.isSubmergedInWater != player.isUnderWater()) {
            values.add(String.format("Submerged in water mismatch %s vs %s", player.isUnderWater(), this.isSubmergedInWater));
        }

	    if(this.input.playerInput.sneak() != playerInput.sneak()) {
		    values.add(String.format("Sneaking mismatch %s vs %s", playerInput.sneak(), this.input.playerInput.sneak()));
	    }

        if(this.swimming != player.isSwimming()) {
            values.add(String.format("Swimming mismatch %s vs %s", player.isSwimming(), this.swimming));
        }

        if(this.standingEyeHeight != player.getEyeHeight()) {
            values.add(String.format("Eye height mismatch %s vs %s", player.getEyeHeight(), this.standingEyeHeight));
        }

        //#if MC < 12111
        //$$ if(mismatch(this.fallDistance, player.fallDistance)) {
            //$$ values.add(String.format("Fall distance mismatch %s vs %s", player.fallDistance, this.fallDistance));
        //$$ }
        //#else
        if(mismatch(this.fallDistance, (float) player.fallDistance)) {
            values.add(String.format("Fall distance mismatch %s vs %s", (float) player.fallDistance, this.fallDistance));
        }
        //#endif

        if(this.horizontalCollision != player.horizontalCollision) {
            values.add(String.format("Horizontal Collision mismatch %s vs %s", player.horizontalCollision, this.horizontalCollision));
        }

        if(this.verticalCollision != player.verticalCollision) {
            values.add(String.format("Vertical Collision mismatch %s vs %s", player.verticalCollision, this.verticalCollision));
        }

        if(this.collidedSoftly != player.minorHorizontalCollision) {
            values.add(String.format("Soft Collision mismatch %s vs %s", player.minorHorizontalCollision, this.collidedSoftly));
        }

        if(this.jumping != ((AccessorLivingEntity)player).getJumping()) {
            values.add(String.format("Jumping mismatch %s vs %s", ((AccessorLivingEntity)player).getJumping(), this.jumping));
        }

        if(this.jumpingCooldown != ((AccessorLivingEntity)player).getNoJumpDelay()) {
            values.add(String.format("Jumping Cooldown mismatch %s vs %s", ((AccessorLivingEntity)player).getNoJumpDelay(), this.jumpingCooldown));
        }

//        if(this.airStrafingSpeed != player.airStrafingSpeed) {
//            values.add(String.format("Air Strafe Speed mismatch %s vs %s", player.airStrafingSpeed, this.airStrafingSpeed));
//        }
        
//        if(!this.box.equals(player.getBoundingBox())) {
//            values.add(String.format("Bounding box mismatch %s vs %s", player.getBoundingBox(), this.box));
//            this.box = player.getBoundingBox();
//        }


        if(this.firstUpdate != ((AccessorEntity)player).getFirstTick()) {
            values.add(String.format("First Update mismatch %s vs %s", ((AccessorEntity)player).getFirstTick(), this.firstUpdate));
        }

        if(!this.submergedFluids.equals(getPlayerSubmergedFluids(player))) {
            values.add(String.format("Submerged Fluids mismatch %s vs %s", getPlayerSubmergedFluids(player), this.submergedFluids));
        }

        if(!values.isEmpty() && TungstenConfig.get().verboseDebugLogging) {
            System.out.println("Tick " + player.tickCount + " ===========================================");
            values.forEach(System.out::println);
        }
    }

    /**
     * ⛔ ADDED 2026-09-04: {@code depthStrider} (used at {@link #travelLiving} for swim speed) was
     * declared, copied node-to-node by {@code copy()} and {@link #of(Agent, boolean, boolean,
     * boolean, boolean, boolean, boolean, boolean, float, float)}, and read at travelLiving --
     * but never once INITIALIZED from a real player. Every root agent (built by the two
     * PlayerEntity-based factories below) started at the field's Java default of 0 regardless of
     * the boots actually equipped, so the swim-speed boost formula never applied any depth
     * strider bonus for the whole lifetime of the process. Same pattern as
     * {@code MovementHelperB.frostWalkerLevel} (this file's sibling enchantment reader) --
     * copied here rather than duplicated logic invented fresh.
     */
    private static int depthStriderLevel(Player player) {
        int level = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemEnchantments itemEnchantments = player.getItemBySlot(slot).getEnchantments();
            for (Holder<Enchantment> enchant : itemEnchantments.keySet()) {
                if (enchant.is(Enchantments.DEPTH_STRIDER)) {
                    level = itemEnchantments.getLevel(enchant);
                }
            }
        }
        return level;
    }

    public static Agent of(Player player) {
        Agent agent = new Agent();
        agent.keyForward = TungstenPlayerInput.DEFAULT.forward();
        agent.keyBack = TungstenPlayerInput.DEFAULT.backward();
        agent.keyLeft = TungstenPlayerInput.DEFAULT.left();
        agent.keyRight = TungstenPlayerInput.DEFAULT.right();
        agent.keyJump = TungstenPlayerInput.DEFAULT.jump();
        agent.keySneak = TungstenPlayerInput.DEFAULT.sneak();
        agent.keySprint = TungstenPlayerInput.DEFAULT.sprint();

        agent.pose = player.getPose();
        agent.sprinting = player.isSprinting();
        agent.inSneakingPose = player.isCrouching();
        agent.usingItem = player.isUsingItem();
        agent.sidewaysSpeed = player.xxa;
        agent.upwardSpeed = player.yya;
        agent.forwardSpeed = player.zza;
        agent.yaw = player.getYRot();
        agent.pitch = player.getXRot();
        agent.posX = player.getX();
        agent.posY = player.getY();
        agent.posZ = player.getZ();
        agent.blockX = player.blockPosition().getX();
        agent.blockY = player.blockPosition().getY();
        agent.blockZ = player.blockPosition().getZ();
        agent.velX = player.getDeltaMovement().x;
        agent.velY = player.getDeltaMovement().y;
        agent.velZ = player.getDeltaMovement().z;
        agent.mulX = ((AccessorEntity)player).getStuckSpeedMultiplier().x;
        agent.mulY = ((AccessorEntity)player).getStuckSpeedMultiplier().y;
        agent.mulZ = ((AccessorEntity)player).getStuckSpeedMultiplier().z;
        agent.fluidHeight.put(FluidTags.WATER, player.getFluidHeight(FluidTags.WATER));
        agent.fluidHeight.put(FluidTags.LAVA, player.getFluidHeight(FluidTags.LAVA));
        agent.submergedFluids.addAll(getPlayerSubmergedFluids(player));
        agent.firstUpdate = ((AccessorEntity)player).getFirstTick();
        agent.box = player.getBoundingBox();
        agent.dimensions = player.getDimensions(player.getPose());
        agent.standingEyeHeight = player.getEyeHeight();
        agent.onGround = player.onGround();
        agent.sleeping = player.isSleeping();
        agent.sneaking = player.isDiscrete();
        agent.hunger = player.getFoodData();
        agent.depthStrider = depthStriderLevel(player);
        agent.sprinting = player.isSprinting();
        agent.swimming = player.isSwimming();
        agent.fallFlying = player.getAbilities().flying;
        agent.stepHeight = player.maxUpStep();
        //#if MC < 12111
        //$$ agent.fallDistance = player.fallDistance;
        //#else
        agent.fallDistance = (float) player.fallDistance;
        //#endif
        agent.touchingWater = player.isInWater();
        agent.isSubmergedInWater = player.isUnderWater();
        agent.horizontalCollision = player.horizontalCollision;
        agent.verticalCollision = player.verticalCollision;
        agent.collidedSoftly = player.minorHorizontalCollision;
        agent.jumping = ((AccessorLivingEntity)player).getJumping();
        agent.speed = player.hasEffect(MobEffects.SPEED) ? player.getEffect(MobEffects.SPEED).getAmplifier() : -1;
        agent.blindness = player.hasEffect(MobEffects.BLINDNESS) ? player.getEffect(MobEffects.BLINDNESS).getAmplifier() : -1;
        agent.jumpBoost = player.hasEffect(MobEffects.JUMP_BOOST) ? player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() : -1;
        agent.slowFalling = player.hasEffect(MobEffects.SLOW_FALLING) ? player.getEffect(MobEffects.SLOW_FALLING).getAmplifier() : -1;
        agent.dolphinsGrace = player.hasEffect(MobEffects.DOLPHINS_GRACE) ? player.getEffect(MobEffects.DOLPHINS_GRACE).getAmplifier() : -1;
        agent.levitation = player.hasEffect(MobEffects.LEVITATION) ? player.getEffect(MobEffects.LEVITATION).getAmplifier() : -1;
        agent.movementSpeed = player.getSpeed();
        agent.airStrafingSpeed = 0.02F; // LivingEntity.airStrafingSpeed removed in yarn 1.21.11; 0.02F is the vanilla default
        agent.jumpingCooldown = ((AccessorLivingEntity)player).getNoJumpDelay();
        agent.hunger.setFoodLevel(player.getFoodData().getFoodLevel());
        agent.hunger.setSaturation(player.getFoodData().getSaturationLevel());
        //TODO: frame.ticksToNextAutojump
        return agent;
    }


    public static Agent of(Player player, TungstenPlayerInput playerInput) {
        Agent agent = new Agent();
        agent.keyForward = playerInput.forward();
        agent.keyBack = playerInput.backward();
        agent.keyLeft = playerInput.left();
        agent.keyRight = playerInput.right();
        agent.keyJump = playerInput.jump();
        agent.keySneak = playerInput.sneak();
        agent.keySprint = playerInput.sprint();

        agent.pose = player.getPose();
        agent.sprinting = player.isSprinting();
        agent.inSneakingPose = player.isCrouching();
        agent.usingItem = player.isUsingItem();
        agent.sidewaysSpeed = player.xxa;
        agent.upwardSpeed = player.yya;
        agent.forwardSpeed = player.zza;
        agent.yaw = player.getYRot();
        agent.pitch = player.getXRot();
        agent.posX = player.getX();
        agent.posY = player.getY();
        agent.posZ = player.getZ();
        agent.blockX = player.blockPosition().getX();
        agent.blockY = player.blockPosition().getY();
        agent.blockZ = player.blockPosition().getZ();
        agent.velX = player.getDeltaMovement().x;
        agent.velY = player.getDeltaMovement().y;
        agent.velZ = player.getDeltaMovement().z;
        agent.mulX = ((AccessorEntity)player).getStuckSpeedMultiplier().x;
        agent.mulY = ((AccessorEntity)player).getStuckSpeedMultiplier().y;
        agent.mulZ = ((AccessorEntity)player).getStuckSpeedMultiplier().z;
        agent.fluidHeight.put(FluidTags.WATER, player.getFluidHeight(FluidTags.WATER));
        agent.fluidHeight.put(FluidTags.LAVA, player.getFluidHeight(FluidTags.LAVA));
        agent.submergedFluids.addAll(getPlayerSubmergedFluids(player));
        agent.firstUpdate = ((AccessorEntity)player).getFirstTick();
        agent.box = player.getBoundingBox();
        agent.dimensions = player.getDimensions(player.getPose());
        agent.standingEyeHeight = player.getEyeHeight();
        agent.onGround = player.onGround();
        agent.sleeping = player.isSleeping();
        agent.sneaking = player.isDiscrete();
        agent.hunger = player.getFoodData();
        agent.depthStrider = depthStriderLevel(player);
        agent.sprinting = player.isSprinting();
        agent.swimming = player.isSwimming();
        agent.fallFlying = player.getAbilities().flying;
        agent.stepHeight = player.maxUpStep();
        //#if MC < 12111
        //$$ agent.fallDistance = player.fallDistance;
        //#else
        agent.fallDistance = (float) player.fallDistance;
        //#endif
        agent.touchingWater = player.isInWater();
        agent.isSubmergedInWater = player.isUnderWater();
        agent.horizontalCollision = player.horizontalCollision;
        agent.verticalCollision = player.verticalCollision;
        agent.collidedSoftly = player.minorHorizontalCollision;
        agent.jumping = ((AccessorLivingEntity)player).getJumping();
        agent.speed = player.hasEffect(MobEffects.SPEED) ? player.getEffect(MobEffects.SPEED).getAmplifier() : -1;
        agent.blindness = player.hasEffect(MobEffects.BLINDNESS) ? player.getEffect(MobEffects.BLINDNESS).getAmplifier() : -1;
        agent.jumpBoost = player.hasEffect(MobEffects.JUMP_BOOST) ? player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() : -1;
        agent.slowFalling = player.hasEffect(MobEffects.SLOW_FALLING) ? player.getEffect(MobEffects.SLOW_FALLING).getAmplifier() : -1;
        agent.dolphinsGrace = player.hasEffect(MobEffects.DOLPHINS_GRACE) ? player.getEffect(MobEffects.DOLPHINS_GRACE).getAmplifier() : -1;
        agent.levitation = player.hasEffect(MobEffects.LEVITATION) ? player.getEffect(MobEffects.LEVITATION).getAmplifier() : -1;
        agent.movementSpeed = player.getSpeed();
        agent.airStrafingSpeed = 0.02F; // LivingEntity.airStrafingSpeed removed in yarn 1.21.11; 0.02F is the vanilla default
        agent.jumpingCooldown = ((AccessorLivingEntity)player).getNoJumpDelay();
        agent.hunger.setFoodLevel(player.getFoodData().getFoodLevel());
        agent.hunger.setSaturation(player.getFoodData().getSaturationLevel());
        //TODO: frame.ticksToNextAutojump
        return agent;
    }

    
    public static Agent of(LocalPlayer player, Options options) {
        Agent agent = new Agent();
        agent.keyForward = options.keyUp.isDown();
        agent.keyBack = options.keyDown.isDown();
        agent.keyLeft = options.keyLeft.isDown();
        agent.keyRight = options.keyRight.isDown();
        agent.keyJump = options.keyJump.isDown();
        agent.keySneak = options.keyShift.isDown();
        agent.keySprint = options.keySprint.isDown();

        agent.pose = player.getPose();
        agent.sprinting = player.isSprinting();
        agent.inSneakingPose = player.isCrouching();
        agent.usingItem = player.isUsingItem();
        agent.sidewaysSpeed = player.xxa;
        agent.upwardSpeed = player.yya;
        agent.forwardSpeed = player.zza;
        agent.yaw = player.getYRot();
        agent.pitch = player.getXRot();
        agent.posX = player.getX();
        agent.posY = player.getY();
        agent.posZ = player.getZ();
        agent.blockX = player.blockPosition().getX();
        agent.blockY = player.blockPosition().getY();
        agent.blockZ = player.blockPosition().getZ();
        agent.velX = player.getDeltaMovement().x;
        agent.velY = player.getDeltaMovement().y;
        agent.velZ = player.getDeltaMovement().z;
        agent.mulX = ((AccessorEntity)player).getStuckSpeedMultiplier().x;
        agent.mulY = ((AccessorEntity)player).getStuckSpeedMultiplier().y;
        agent.mulZ = ((AccessorEntity)player).getStuckSpeedMultiplier().z;
        agent.fluidHeight.put(FluidTags.WATER, player.getFluidHeight(FluidTags.WATER));
        agent.fluidHeight.put(FluidTags.LAVA, player.getFluidHeight(FluidTags.LAVA));
        agent.submergedFluids.addAll(getPlayerSubmergedFluids(player));
        agent.firstUpdate = ((AccessorEntity)player).getFirstTick();
        agent.box = player.getBoundingBox();
        agent.dimensions = player.getDimensions(player.getPose());
        agent.standingEyeHeight = player.getEyeHeight();
        agent.onGround = player.onGround();
        agent.sleeping = player.isSleeping();
        agent.sneaking = player.isDiscrete();
        agent.hunger = player.getFoodData();
        agent.depthStrider = depthStriderLevel(player);
        agent.sprinting = player.isSprinting();
        agent.swimming = player.isSwimming();
        agent.fallFlying = player.getAbilities().flying;
        agent.stepHeight = player.maxUpStep();
        //#if MC < 12111
        //$$ agent.fallDistance = player.fallDistance;
        //#else
        agent.fallDistance = (float) player.fallDistance;
        //#endif
        agent.touchingWater = player.isInWater();
        agent.isSubmergedInWater = player.isUnderWater();
        agent.horizontalCollision = player.horizontalCollision;
        agent.verticalCollision = player.verticalCollision;
        agent.collidedSoftly = player.minorHorizontalCollision;
        agent.jumping = ((AccessorLivingEntity)player).getJumping();
        agent.speed = player.hasEffect(MobEffects.SPEED) ? player.getEffect(MobEffects.SPEED).getAmplifier() : -1;
        agent.blindness = player.hasEffect(MobEffects.BLINDNESS) ? player.getEffect(MobEffects.BLINDNESS).getAmplifier() : -1;
        agent.jumpBoost = player.hasEffect(MobEffects.JUMP_BOOST) ? player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() : -1;
        agent.slowFalling = player.hasEffect(MobEffects.SLOW_FALLING) ? player.getEffect(MobEffects.SLOW_FALLING).getAmplifier() : -1;
        agent.dolphinsGrace = player.hasEffect(MobEffects.DOLPHINS_GRACE) ? player.getEffect(MobEffects.DOLPHINS_GRACE).getAmplifier() : -1;
        agent.levitation = player.hasEffect(MobEffects.LEVITATION) ? player.getEffect(MobEffects.LEVITATION).getAmplifier() : -1;
        agent.movementSpeed = player.getSpeed();
        agent.airStrafingSpeed = 0.02F; // LivingEntity.airStrafingSpeed removed in yarn 1.21.11; 0.02F is the vanilla default
        agent.jumpingCooldown = ((AccessorLivingEntity)player).getNoJumpDelay();
        agent.hunger.setFoodLevel(player.getFoodData().getFoodLevel());
        agent.hunger.setSaturation(player.getFoodData().getSaturationLevel());
        //TODO: frame.ticksToNextAutojump
        return agent;
    }

    public static Agent of(Agent other, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint, float pitch, float yaw) {
        Agent agent = new Agent();
        agent.keyForward = forward;
        agent.keyBack = back;
        agent.keyLeft = left;
        agent.keyRight = right;
        agent.keyJump = jump;
        agent.keySneak = sneak;
        agent.keySprint = sprint;
        agent.hunger.setFoodLevel(other.hunger.getFoodLevel());
        agent.hunger.setSaturation(other.hunger.getSaturationLevel());
        agent.sprinting = other.sprinting;
        agent.pose = other.pose;
        agent.inSneakingPose = other.inSneakingPose;
        agent.usingItem = other.usingItem;
        agent.sidewaysSpeed = other.sidewaysSpeed;
        agent.upwardSpeed = other.upwardSpeed;
        agent.forwardSpeed = other.forwardSpeed;
        agent.yaw = yaw;
        agent.pitch = pitch;
        agent.posX = other.posX;
        agent.posY = other.posY;
        agent.posZ = other.posZ;
        agent.blockX = other.blockX;
        agent.blockY = other.blockY;
        agent.blockZ = other.blockZ;
        agent.velX = other.velX;
        agent.velY = other.velY;
        agent.velZ = other.velZ;
        agent.mulX = other.mulX;
        agent.mulY = other.mulY;
        agent.mulZ = other.mulZ;
        agent.fluidHeight.put(FluidTags.WATER, other.getFluidHeight(FluidTags.WATER));
        agent.fluidHeight.put(FluidTags.LAVA, other.getFluidHeight(FluidTags.LAVA));
        agent.submergedFluids.addAll(other.submergedFluids);
        agent.firstUpdate = other.firstUpdate;
        agent.dimensions = other.dimensions;
        agent.box = other.box;
        agent.standingEyeHeight = other.standingEyeHeight;
        agent.onGround = other.onGround;
        agent.sleeping = other.sleeping;
        agent.sneaking = other.sneaking;
        agent.sprinting = other.sprinting;
        agent.swimming = other.swimming;
        agent.fallFlying = other.fallFlying;
        agent.stepHeight = other.stepHeight;
        agent.fallDistance = other.fallDistance;
        agent.touchingWater = other.touchingWater;
        agent.isSubmergedInWater = other.isSubmergedInWater;
        agent.horizontalCollision = other.horizontalCollision;
        agent.verticalCollision = other.verticalCollision;
        agent.collidedSoftly = other.collidedSoftly;
        agent.jumping = other.jumping;
        agent.speed = other.speed;
        agent.blindness = other.blindness;
        agent.jumpBoost = other.jumpBoost;
        agent.slowFalling = other.slowFalling;
        agent.dolphinsGrace = other.dolphinsGrace;
        agent.levitation = other.levitation;
        agent.depthStrider = other.depthStrider;
        agent.movementSpeed = other.movementSpeed;
        agent.airStrafingSpeed = other.airStrafingSpeed;
        agent.jumpingCooldown = other.jumpingCooldown;
        //TODO: frame.ticksToNextAutojump
        return agent;
    }

    public static Agent of(Agent other, AgentInput input) {
    	PathInput pI = input.toPathInput();
    	return of(other, pI);
    }

    public static Agent of(Agent agent, PathInput input) {
        return of(agent, input.forward, input.back, input.left, input.right, input.jump, input.sneak, input.sprint, input.pitch, input.yaw);
    }

}
