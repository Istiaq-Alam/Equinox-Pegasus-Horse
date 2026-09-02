package com.istiak.equinox.mounts;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public final class SummonManager {

    private final EquinoxPlugin plugin;


    public SummonManager(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ========================================================
     * SUMMON MOUNT
     * ========================================================
     */

    public Horse summonMount(
            Player player
    ) {

        if (player == null
                || !player.isOnline()) {

            return null;
        }


        MountManager mountManager =
                plugin.getMountManager();


        if (!mountManager.hasMount(player)) {

            return null;
        }


        MountData mountData =
                mountManager.getMount(player);


        if (mountData == null) {

            return null;
        }


        /*
         * ====================================================
         * CHECK FOR EXISTING LOADED HORSE
         * ====================================================
         */

        Horse existingHorse =
                mountManager.getLoadedMount(player);


        if (existingHorse != null
                && existingHorse.isValid()
                && !existingHorse.isDead()) {

            return existingHorse;
        }


        /*
         * ====================================================
         * FIND SAFE LOCATION
         * ====================================================
         */

        Location location =
                findSafeLocationNearPlayer(player);


        if (location == null
                || location.getWorld() == null) {

            return null;
        }


        World world =
                location.getWorld();


        /*
         * ====================================================
         * SPAWN
         * ====================================================
         */

        Horse horse =
                world.spawn(
                        location,
                        Horse.class
                );


        /*
         * ====================================================
         * RESTORE HORSE
         * ====================================================
         */

        setupHorse(
                player,
                horse,
                mountData
        );


        /*
         * ====================================================
         * REPLACE UUID REGISTRATION
         * ====================================================
         */

        boolean replaced =
                mountManager.replaceMountHorse(
                        player,
                        horse
                );


        if (!replaced) {

            horse.remove();

            return null;
        }


        return horse;
    }


    /*
     * ========================================================
     * SETUP HORSE
     * ========================================================
     */

    private void setupHorse(
            Player player,
            Horse horse,
            MountData mountData
    ) {

        /*
         * Ownership.
         */

        horse.setTamed(true);

        horse.setOwner(player);


        /*
         * Name.
         */

        String horseName =
                mountData.getHorseName();


        if (horseName != null
                && !horseName.isEmpty()) {

            horse.setCustomName(horseName);

            horse.setCustomNameVisible(false);
        }


        /*
         * Appearance.
         */

        horse.setColor(
                mountData.getColor()
        );

        horse.setStyle(
                mountData.getStyle()
        );


        /*
         * Max health.
         */

        setAttribute(
                horse,
                Attribute.MAX_HEALTH,
                mountData.getMaxHealth()
        );


        /*
         * Movement speed.
         */

        setAttribute(
                horse,
                Attribute.MOVEMENT_SPEED,
                mountData.getMovementSpeed()
        );


        /*
         * Jump strength.
         */

        horse.setJumpStrength(
                mountData.getJumpStrength()
        );


        /*
         * Armor.
         *
         * This restores the ORIGINAL ItemStack.
         * Therefore all Equinox PDC enchantments remain intact.
         */

        ItemStack armor =
                mountData.getArmor();


        if (armor != null
                && plugin.getHorseArmorManager()
                .isEquinoxArmor(armor)) {

            horse.getInventory().setArmor(
                    armor.clone()
            );

        } else {

            /*
             * Fallback for old mounts.yml.
             */

            horse.getInventory().setArmor(
                    plugin.getHorseArmorManager()
                            .createArmor(
                                    Material.NETHERITE_HORSE_ARMOR
                            )
            );
        }


        /*
         * Health must be set AFTER max health.
         */

        double maxHealth =
                horse.getAttribute(
                        Attribute.MAX_HEALTH
                ).getValue();


        double health =
                Math.min(
                        Math.max(
                                1.0,
                                mountData.getHealth()
                        ),
                        maxHealth
                );


        horse.setHealth(health);


        /*
         * Safety.
         */

        horse.setInvulnerable(false);

        horse.setVelocity(
                horse.getVelocity()
                        .multiply(0.0)
        );
    }


    /*
     * ========================================================
     * ATTRIBUTE HELPER
     * ========================================================
     */

    private void setAttribute(
            Horse horse,
            Attribute attribute,
            double value
    ) {

        AttributeInstance instance =
                horse.getAttribute(attribute);

        if (instance == null) {

            return;
        }

        instance.setBaseValue(value);
    }


    /*
     * ========================================================
     * SAFE LOCATION
     * ========================================================
     */

    private Location findSafeLocationNearPlayer(
            Player player
    ) {

        World world =
                player.getWorld();

        Location origin =
                player.getLocation();


        /*
         * Search around the player.
         */

        for (int radius = 3;
             radius <= 8;
             radius++) {

            for (int x = -radius;
                 x <= radius;
                 x++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    /*
                     * Only search the edge of each square.
                     */

                    if (Math.abs(x) != radius
                            && Math.abs(z) != radius) {

                        continue;
                    }


                    int blockX =
                            origin.getBlockX() + x;

                    int blockZ =
                            origin.getBlockZ() + z;


                    int highestY =
                            world.getHighestBlockYAt(
                                    blockX,
                                    blockZ
                            );


                    Location candidate =
                            new Location(
                                    world,
                                    blockX + 0.5,
                                    highestY + 1.0,
                                    blockZ + 0.5,
                                    origin.getYaw(),
                                    0.0f
                            );


                    if (isSafeHorseLocation(candidate)) {

                        return candidate;
                    }
                }
            }
        }


        /*
         * Fallback.
         */

        Location fallback =
                origin.clone()
                        .add(4, 0, 0);


        int y =
                world.getHighestBlockYAt(
                        fallback.getBlockX(),
                        fallback.getBlockZ()
                );


        fallback.setY(y + 1.0);

        fallback.setPitch(0.0f);


        return fallback;
    }


    /*
     * ========================================================
     * SAFE LOCATION CHECK
     * ========================================================
     */

    private boolean isSafeHorseLocation(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return false;
        }


        Location ground =
                location.clone()
                        .subtract(0, 1, 0);


        if (!ground.getBlock()
                .getType()
                .isSolid()) {

            return false;
        }


        if (!location.getBlock()
                .isPassable()) {

            return false;
        }


        /*
         * Horse needs more than one block of vertical space.
         */

        Location upper =
                location.clone()
                        .add(0, 1, 0);


        Location upperSecond =
                location.clone()
                        .add(0, 2, 0);


        return upper.getBlock().isPassable()
                && upperSecond.getBlock().isPassable();
    }


    /*
     * ========================================================
     * CAN SUMMON
     * ========================================================
     */

    public boolean canSummon(
            Player player
    ) {

        return player != null
                && player.isOnline()
                && plugin.getMountManager()
                .hasMount(player);
    }
}
