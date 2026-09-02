package com.istiak.equinox.listeners;

import com.istiak.equinox.EquinoxPlugin;
import com.istiak.equinox.enchantments.EnchantmentType;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public final class HorseArmorListener {

    private final EquinoxPlugin plugin;
    private BukkitRunnable armorScanner;

    private static final UUID SWIFT_UUID =
            UUID.fromString("e91c82d4-49b2-4a64-8a2e-111111111111");

    private static final UUID LEAP_UUID =
            UUID.fromString("e91c82d4-49b2-4a64-8a2e-222222222222");

    private static final UUID VITALITY_UUID =
            UUID.fromString("e91c82d4-49b2-4a64-8a2e-333333333333");

    public HorseArmorListener(EquinoxPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {

        if (armorScanner != null) {
            return;
        }

        armorScanner = new BukkitRunnable() {

            @Override
            public void run() {

                for (var world : Bukkit.getWorlds()) {

                    for (Horse horse : world.getEntitiesByClass(Horse.class)) {
                        updateHorse(horse);
                    }
                }
            }

        };

        armorScanner.runTaskTimer(
                plugin,
                10L,
                10L
        );
    }

    public void stop() {

        if (armorScanner != null) {
            armorScanner.cancel();
            armorScanner = null;
        }

        /*
         * Remove Equinox modifiers from all loaded horses.
         */
        for (var world : Bukkit.getWorlds()) {

            for (Horse horse : world.getEntitiesByClass(Horse.class)) {

                removeEquinoxModifiers(horse);
            }
        }
    }

    private void updateHorse(Horse horse) {

        ItemStack armor = horse.getInventory().getArmor();

        /*
         * Always remove our old modifiers first.
         *
         * This prevents duplicate modifiers and allows
         * enchantment changes to update automatically.
         */
        removeEquinoxModifiers(horse);

        if (!plugin.getHorseArmorManager().isEquinoxArmor(armor)) {
            return;
        }

        if (!horse.isTamed()) {
            return;
        }

        applySwift(horse, armor);
        applyTitanLeap(horse, armor);
        applyVitality(horse, armor);
    }

    private void applySwift(
            Horse horse,
            ItemStack armor
    ) {

        int level = plugin.getHorseArmorManager().getLevel(
                armor,
                EnchantmentType.SWIFT
        );

        if (level <= 0) {
            return;
        }

        AttributeInstance attribute =
                horse.getAttribute(Attribute.MOVEMENT_SPEED);

        if (attribute == null) {
            return;
        }

        double bonusPerLevel = plugin.getConfig().getDouble(
                "enchantments.swift.bonus-per-level",
                0.10
        );

        /*
         * MULTIPLY_SCALAR_1:
         *
         * Level 1 = +10%
         * Level 5 = +50%
         * Level 10 = +100%
         */
        double amount = level * bonusPerLevel;

        AttributeModifier modifier =
                new AttributeModifier(
                        SWIFT_UUID,
                        "equinox_swift",
                        amount,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );

        attribute.addModifier(modifier);
    }

    private void applyTitanLeap(
            Horse horse,
            ItemStack armor
    ) {

        int level = plugin.getHorseArmorManager().getLevel(
                armor,
                EnchantmentType.TITAN_LEAP
        );

        if (level <= 0) {
            return;
        }

        AttributeInstance attribute =
                horse.getAttribute(Attribute.JUMP_STRENGTH);

        if (attribute == null) {
            return;
        }

        double bonusPerLevel = plugin.getConfig().getDouble(
                "enchantments.titan-leap.bonus-per-level",
                0.10
        );

        double amount = level * bonusPerLevel;

        AttributeModifier modifier =
                new AttributeModifier(
                        LEAP_UUID,
                        "equinox_titan_leap",
                        amount,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );

        attribute.addModifier(modifier);
    }

    private void applyVitality(
            Horse horse,
            ItemStack armor
    ) {

        int level = plugin.getHorseArmorManager().getLevel(
                armor,
                EnchantmentType.VITALITY
        );

        if (level <= 0) {
            return;
        }

        AttributeInstance attribute =
                horse.getAttribute(Attribute.MAX_HEALTH);

        if (attribute == null) {
            return;
        }

        double bonusPerLevel = plugin.getConfig().getDouble(
                "enchantments.vitality.bonus-per-level",
                0.10
        );

        double amount = level * bonusPerLevel;

        AttributeModifier modifier =
                new AttributeModifier(
                        VITALITY_UUID,
                        "equinox_vitality",
                        amount,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );

        attribute.addModifier(modifier);

        /*
         * Heal the horse after increasing its maximum health.
         */
        horse.setHealth(attribute.getValue());
    }

    private void removeEquinoxModifiers(Horse horse) {

        removeModifier(
                horse.getAttribute(Attribute.MOVEMENT_SPEED),
                SWIFT_UUID
        );

        removeModifier(
                horse.getAttribute(Attribute.JUMP_STRENGTH),
                LEAP_UUID
        );

        removeModifier(
                horse.getAttribute(Attribute.MAX_HEALTH),
                VITALITY_UUID
        );
    }

    private void removeModifier(
            AttributeInstance attribute,
            UUID uuid
    ) {

        if (attribute == null) {
            return;
        }

        AttributeModifier modifier =
                attribute.getModifiers()
                        .stream()
                        .filter(
                                current ->
                                        current.getUniqueId()
                                                .equals(uuid)
                        )
                        .findFirst()
                        .orElse(null);

        if (modifier != null) {
            attribute.removeModifier(modifier);
        }
    }
}
