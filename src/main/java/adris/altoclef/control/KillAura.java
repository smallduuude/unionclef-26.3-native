package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.versionedfields.Entities;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import kaptainwutax.tungsten.path.movements.Input;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controls and applies killaura
 */
public class KillAura {
    // Smart aura data
    private final List<Entity> targets = new ArrayList<>();
    boolean shielding = false;
    private double forceFieldRange = Double.POSITIVE_INFINITY;
    private Entity forceHit = null;
    public boolean attackedLastTick = false;

    /**
     * Put the best melee weapon in the pack into the hand.
     *
     * <p>THIS METHOD DID NOTHING AT ALL ON 1.21.11. Its entire body was inside the {@code
     * MC < 12111} half of a preprocessor split, because it was written around {@code instanceof
     * SwordItem} and that class is gone; the 1.21.11 half was a TODO comment. So the force field
     * -- which MobDefenseChain's own measurements say is what actually kills things -- swung with
     * whatever happened to already be in the hand, sword in the pack or not.
     *
     * <p>Rewritten without any version split: rank every stack in the pack by
     * {@link ItemHelper#meleeDps}, which asks the ITEM for its damage and swing speed instead of
     * asking its class what it is. Nothing is equipped unless it beats what is already held, so a
     * bot holding the best weapon it owns is left alone rather than re-equipping every tick.
     */
    /** G71: aura swings refused because the target was beyond melee reach (the hand left alone),
     *  and weapon equips the aura did make for a target in reach. Read kaAura=outOfReach/equip. */
    public static volatile int kaAuraOutOfReach, kaAuraEquip;
    /** Swings HELD because the crosshair was not on the target yet. Read as kaAuraNotFacing: the
     *  mechanism counter for "it attacks mobs it is not looking at". */
    public static volatile int kaAuraNotFacing;
    /** A swing is allowed only when the crosshair is this close to the target (degrees), and the ray
     *  is traced this far. Vanilla resolves a melee hit from the player's LOOK vector, so a swing
     *  while facing away lands nothing — which is why this is a correctness gate, not just cosmetics. */
    private static final double ATTACK_LOOK_DISTANCE = 6.0;
    private static final double ATTACK_LOOK_PRECISION_DEGREES = 8.0;

    public static void equipWeapon(AltoClef mod) {
        List<ItemStack> invStacks = mod.getItemStorage().getItemStacksPlayerInventory(true);
        if (invStacks.isEmpty()) {
            return;
        }
        Item handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
        Item best = handItem;
        float bestDps = ItemHelper.meleeDps(handItem);
        for (ItemStack invStack : invStacks) {
            Item item = invStack.getItem();
            float dps = ItemHelper.meleeDps(item);
            if (dps > bestDps) {
                best = item;
                bestDps = dps;
            }
        }
        // An empty hand and a pack of blocks is not a reason to equip a block: meleeDps returns the
        // bare-fist number for anything without weapon modifiers, so nothing wins by accident.
        if (best != null && best != handItem) {
            mod.getSlotHandler().forceEquipItem(best);
        }
    }

    public void tickStart() {
        targets.clear();
        forceHit = null;
        attackedLastTick = false;
    }

    public void applyAura(Entity entity) {
        targets.add(entity);
        // Always hit ghast balls.
        if (entity instanceof LargeFireball) forceHit = entity;
    }

    public void setRange(double range) {
        forceFieldRange = range;
    }

    public void tickEnd(AltoClef mod) {
        Optional<Entity> entities = targets.stream().min(StlHelper.compareValues(entity -> entity.distanceToSqr(mod.getPlayer())));
        if (entities.isPresent() &&
                !mod.getEntityTracker().entityFound(AbstractThrownPotion.class) &&
                (Double.isInfinite(forceFieldRange) || entities.get().distanceToSqr(mod.getPlayer()) < forceFieldRange * forceFieldRange ||
                        entities.get().distanceToSqr(mod.getPlayer()) < 40) &&
                !mod.getMLGBucketChain().isFalling(mod) && mod.getMLGBucketChain().doneMLG() &&
                !mod.getMLGBucketChain().isChorusFruiting()) {
            PlayerSlot offhandSlot = PlayerSlot.OFFHAND_SLOT;
            Item offhandItem = StorageHelper.getItemStackInSlot(offhandSlot).getItem();
            if (entities.get().getClass() != Creeper.class && entities.get().getClass() != Hoglin.class &&
                    entities.get().getClass() != Zoglin.class && entities.get().getClass() != Entities.WARDEN &&
                    entities.get().getClass() != WitherBoss.class
                    && (mod.getItemStorage().hasItem(Items.SHIELD) || mod.getItemStorage().hasItemInOffhand(Items.SHIELD))
                    && !mod.getPlayer().getCooldowns().isOnCooldown(new ItemStack(offhandItem))
                    && (Nav.isSafeToCancel())) {
                LookHelper.smoothLookAt(mod, entities.get().getEyePosition());
                ItemStack shieldSlot = StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT);
                if (shieldSlot.getItem() != Items.SHIELD) {
                    mod.getSlotHandler().forceEquipItemToOffhand(Items.SHIELD);
                } else if (!WorldHelper.isSurroundedByHostiles()) {
                    startShielding(mod);
                }
            }
            performDelayedAttack(mod);
        } else {
            stopShielding(mod);
        }
        // Run force field on map
        switch (mod.getModSettings().getForceFieldStrategy()) {
            case FASTEST:
                performFastestAttack(mod);
                break;
            case SMART:
                // Attack force mobs ALWAYS. (currently used only for fireballs)
                if (forceHit != null) {
                    attack(mod, forceHit, true);
                    break;
                }

                if (!mod.getFoodChain().isTryingToEat() && !mod.getMLGBucketChain().isFalling(mod) &&
                        mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
                    performDelayedAttack(mod);
                }
                break;
            case DELAY:
                performDelayedAttack(mod);
                break;
            case OFF:
                break;
        }
    }

    private void performDelayedAttack(AltoClef mod) {
        // FoodChain declines eating near enemies. A hunger request alone must not
        // silence defence; only an active eating attempt owns the hand.
        if (!mod.getFoodChain().isTryingToEat() && !mod.getMLGBucketChain().isFalling(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
            if (forceHit != null) {
                attack(mod, forceHit, true);
            }
            // wait for the attack delay
            if (targets.isEmpty()) {
                return;
            }

            Optional<Entity> toHit = targets.stream().min(StlHelper.compareValues(entity -> entity.distanceToSqr(mod.getPlayer())));

            if (mod.getPlayer() == null || mod.getPlayer().getAttackStrengthScale(0) < 1) {
                return;
            }

            toHit.ifPresent(entity -> attack(mod, entity, true));
        }
    }

    private void performFastestAttack(AltoClef mod) {
        if (!mod.getFoodChain().isTryingToEat() && !mod.getMLGBucketChain().isFalling(mod) &&
                mod.getMLGBucketChain().doneMLG() && !mod.getMLGBucketChain().isChorusFruiting()) {
            // Just attack whenever you can
            for (Entity entity : targets) {
                attack(mod, entity);
            }
        }
    }

    private void attack(AltoClef mod, Entity entity) {
        attack(mod, entity, false);
    }

    private void attack(AltoClef mod, Entity entity, boolean equipSword) {
        if (entity == null) return;
        if (!(entity instanceof LargeFireball)) {
            double xAim = entity.getX();
            double yAim = entity.getY() + (entity.getBbHeight() / 1.4);
            double zAim = entity.getZ();
            LookHelper.smoothLookAt(mod, new Vec3(xAim, yAim, zAim));
        }
        if (Double.isInfinite(forceFieldRange) || entity.distanceToSqr(mod.getPlayer()) < forceFieldRange * forceFieldRange ||
                entity.distanceToSqr(mod.getPlayer()) < 40) {
            if (entity instanceof LargeFireball) {
                mod.getControllerExtras().attack(entity);
            }
            boolean canAttack;
            if (equipSword) {
                // ⛔ THE HAND IS TAKEN FOR A HIT THAT CAN LAND, NOT FOR A MOB IN VIEW (G71,
                // 2026-09-12). The 22:50 run stood three minutes on a coal ore, "Found better tool
                // in inventory, equipping." 4532 times, nothing broken: this branch put the sword
                // in the hand on every cooldown for a hostile the force field had in view but
                // out of reach, the miner and the fix chain put the pickaxe back, and a hand that
                // changes item resets vanilla's break progress. The server lands a melee hit
                // within three blocks and nowhere else, so a weapon is equipped -- and the dig
                // interrupted -- only for a target inside that reach; a mob further out is left
                // to the chain's own fight-or-flee verdict, and the pickaxe stays where it is.
                double reach = kaptainwutax.tungsten.combat.TriggerBot.REACH + 1.0;
                if (entity.distanceToSqr(mod.getPlayer()) > reach * reach) {
                    kaAuraOutOfReach++;
                    return;
                }
                kaAuraEquip++;
                equipWeapon(mod);
                canAttack = true;
            } else {
                // Equip non-tool
                canAttack = mod.getSlotHandler().forceDeequipHitTool();
            }
            if (canAttack) {
                // ⛔ LOOK AT WHAT YOU HIT (2026-10-15). This loop swings whenever a target is inside
                // the force field, and the aim above is WindMouse — smooth on purpose, so it is
                // still converging for several ticks after a target appears or moves. The body
                // therefore punched at mobs it was not facing, which the operator saw as "attacks
                // nearby enemies without even looking at them".
                //
                // It is also a WHIFF, not just a tell: vanilla resolves a melee hit from the
                // player's LOOK vector, so a swing aimed ninety degrees away lands nothing, spends
                // the attack cooldown, and leaves the mob alive. A person faces what they hit, then
                // swings. The aim above still runs every tick, so the crosshair closes on the
                // target and the swing happens on the tick it arrives.
                if (!LookHelper.isLookingAtEntity(mod, entity,
                        ATTACK_LOOK_DISTANCE, ATTACK_LOOK_PRECISION_DEGREES)) {
                    kaAuraNotFacing++;
                    return;
                }
                // isOnGround() intentionally removed — matches autoclef (allows sprint-crit attacks while jumping)
                if (mod.getPlayer().getDeltaMovement().y() < 0 || mod.getPlayer().isInWater()) {
                    attackedLastTick = true;
                    mod.getControllerExtras().attack(entity);
                }
            }
        }
    }

    public void startShielding(AltoClef mod) {
        shielding = true;
                    Nav.pause();
        mod.getExtraBaritoneSettings().setInteractionPaused(true);
        if (!mod.getPlayer().isBlocking()) {
            ItemStack handItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot());
            if (ItemVer.isFood(handItem)) {
                List<ItemStack> spaceSlots = mod.getItemStorage().getItemStacksPlayerInventory(false);
                if (!spaceSlots.isEmpty()) {
                    for (ItemStack spaceSlot : spaceSlots) {
                        if (spaceSlot.isEmpty()) {
                            mod.getSlotHandler().clickSlot(PlayerSlot.getEquipSlot(), 0, ContainerInput.QUICK_MOVE);
                            return;
                        }
                    }
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                garbage.ifPresent(slot -> mod.getSlotHandler().forceEquipItem(StorageHelper.getItemStackInSlot(slot).getItem()));
            }
        }
        mod.getInputControls().hold(Input.SNEAK);
        mod.getInputControls().hold(Input.CLICK_RIGHT);
    }

    public void stopShielding(AltoClef mod) {
        if (shielding) {
            ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
            if (ItemVer.isFood(cursor)) {
                Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
                if (toMoveTo.isPresent()) {
                    Slot garbageSlot = toMoveTo.get();
                    mod.getSlotHandler().clickSlot(garbageSlot, 0, ContainerInput.PICKUP);
                }
            }
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.CLICK_RIGHT);
            mod.getInputControls().release(Input.JUMP);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
            shielding = false;
        }
    }

    public boolean isShielding() {
        return shielding;
    }

    public enum Strategy {
        OFF,
        FASTEST,
        DELAY,
        SMART
    }
}