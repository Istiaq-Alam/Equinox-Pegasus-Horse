package com.istiak.equinox;

import com.istiak.equinox.enchant.EnchantmentType;
import com.istiak.equinox.items.EquinoxItems;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Port of HorseArmorListener: every 10 ticks, re-applies the enchantment
 * attribute modifiers (Swift / Titan Leap / Vitality) onto every loaded
 * horse wearing Equinox armor.
 */
public final class EnchantScanner {

    private static final int SCAN_INTERVAL = 10;

    private final Identifier swiftId = EquinoxMod.id("swift");
    private final Identifier leapId = EquinoxMod.id("titan_leap");
    private final Identifier vitalityId = EquinoxMod.id("vitality");

    public EnchantScanner() {
    }

    public void tick(MinecraftServer server) {
        if (EquinoxMod.serverTick() % SCAN_INTERVAL != 0L) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Horse horse && horse.isAlive()) {
                    updateHorse(horse);
                }
            }
        }
    }

    private void updateHorse(Horse horse) {
        ItemStack armor = horse.getItemBySlot(EquipmentSlot.BODY);

        // Always remove old modifiers first so enchantment changes apply
        // immediately and never stack.
        removeEquinoxModifiers(horse);

        if (!EquinoxItems.isEquinoxArmor(armor) || !horse.isTamed()) {
            return;
        }

        applySwift(horse, armor);
        applyTitanLeap(horse, armor);
        applyVitality(horse, armor);
    }

    private void applySwift(Horse horse, ItemStack armor) {
        int level = EquinoxItems.getLevel(armor, EnchantmentType.SWIFT);
        if (level <= 0) return;

        AttributeInstance attribute = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;

        // Level 1 = +10%, Level 10 = +100% (MULTIPLY_SCALAR_1 in Bukkit terms).
        double amount = level * EnchantmentType.SWIFT.getBonusPerLevel();
        attribute.addTransientModifier(new AttributeModifier(swiftId, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void applyTitanLeap(Horse horse, ItemStack armor) {
        int level = EquinoxItems.getLevel(armor, EnchantmentType.TITAN_LEAP);
        if (level <= 0) return;

        AttributeInstance attribute = horse.getAttribute(Attributes.JUMP_STRENGTH);
        if (attribute == null) return;

        double amount = level * EnchantmentType.TITAN_LEAP.getBonusPerLevel();
        attribute.addTransientModifier(new AttributeModifier(leapId, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void applyVitality(Horse horse, ItemStack armor) {
        int level = EquinoxItems.getLevel(armor, EnchantmentType.VITALITY);
        if (level <= 0) return;

        AttributeInstance attribute = horse.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;

        double amount = level * EnchantmentType.VITALITY.getBonusPerLevel();
        attribute.addTransientModifier(new AttributeModifier(vitalityId, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        // Heal the horse up to the raised max health (plugin parity).
        horse.setHealth((float) attribute.getValue());
    }

    private void removeEquinoxModifiers(Horse horse) {
        remove(horse.getAttribute(Attributes.MOVEMENT_SPEED), swiftId);
        remove(horse.getAttribute(Attributes.JUMP_STRENGTH), leapId);
        remove(horse.getAttribute(Attributes.MAX_HEALTH), vitalityId);
    }

    private void remove(AttributeInstance attribute, Identifier id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }
}
