package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.DamageSourceWrapper;
import adris.altoclef.multiversion.MethodWrapper;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Helper functions to interpret entity state
 */
public class EntityHelper {
    public static final double ENTITY_GRAVITY = 0.08; // per second

    public static boolean isAngryAtPlayer(AltoClef mod, Entity mob) {
        boolean hostile = isProbablyHostileToPlayer(mod, mob);
        if (mob instanceof LivingEntity entity) {
            return hostile && entity.hasLineOfSight(mod.getPlayer());
        }
        return hostile;
    }

    public static boolean isProbablyHostileToPlayer(AltoClef mod, Entity entity) {
        if (entity instanceof Mob mob) {
            if (mob instanceof Slime slime) {
                return slime.getAttributeValue(Attributes.ATTACK_DAMAGE) > 0;
            }
            if (mob instanceof Piglin piglin) {
                return piglin.isAggressive() && !isTradingPiglin(mob) && piglin.isAdult();
            }
            if (mob instanceof Enderman enderman) {
                return enderman.isCreepy();
            }
            if (mob instanceof ZombifiedPiglin zombifiedPiglin) {
                return zombifiedPiglin.isAggressive();
            }

            return mob.isAggressive() || mob instanceof Enemy;
        }

        return false;
    }

    public static boolean isTradingPiglin(Entity entity) {
        if (entity instanceof Piglin pig) {
            {
                for (ItemStack stack : java.util.List.of(pig.getMainHandItem(), pig.getOffhandItem())) {
                    if (stack.getItem().equals(Items.GOLD_INGOT)) {
                        // We're trading with this one, ignore it.
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Calculate the resulting damage dealt to a player as a result of some damage.
     * If this player were to receive this damage, the player's health will be subtracted by the resulting value.
     */
    public static double calculateResultingPlayerDamage(Player player, DamageSource src, double damageAmount) {
        // Copied logic from `Player.applyDamage`
        DamageSourceWrapper source = DamageSourceWrapper.of(src);

        if (false) // isInvulnerableTo requires ServerWorld in 1.21.11 — skip on client
            return 0;

        // Armor Base
        if (!source.bypassesArmor()) {
            damageAmount = MethodWrapper.getDamageLeft(player, damageAmount,src,player.getArmorValue(),player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }

        // Enchantments & Potions
        if (!source.bypassesShield()) {
            float k;
            if (player.hasEffect(MobEffects.RESISTANCE) && source.isOutOfWorld()) {
                //noinspection ConstantConditions
                k = (player.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                float j = 25 - k;
                double f = damageAmount * (double) j;
                double g = damageAmount;
                damageAmount = Math.max(f / 25.0F, 0.0F);
            }

            if (damageAmount <= 0.0) {
                damageAmount = 0.0;
            } else {
                // getProtectionAmount requires ServerWorld for LootContext, but we're on the client.
                // Try to get the integrated server's world; if unavailable, skip enchantment protection calc.
                try {
                    var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
                    if (server != null) {
                        var serverWorld = server.getLevel(player.level().dimension());
                        k = EnchantmentHelper.getDamageProtection(serverWorld, player, src);
                    } else {
                        k = 0;
                    }
                } catch (Exception e) {
                    k = 0;
                }
                if (k > 0) {
                    damageAmount = CombatRules.getDamageAfterMagicAbsorb((float) damageAmount, (float) k);
                }
            }
        }

        // Absorption
        damageAmount = Math.max(damageAmount - player.getAbsorptionAmount(), 0.0F);
        return damageAmount;
    }
}
