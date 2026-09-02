package com.istiak.equinox.listeners;

import com.istiak.equinox.EquinoxPlugin;
import com.istiak.equinox.enchantments.EnchantmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

import org.bukkit.entity.Horse;

import org.bukkit.inventory.ItemStack;

import org.bukkit.scheduler.BukkitRunnable;


public final class HorseMovementListener {


    /*
     * ============================================================
     * FIELDS
     * ============================================================
     */

    private final EquinoxPlugin plugin;

    private BukkitRunnable movementTask;


    /*
     * Stores previous horse locations.
     *
     * This allows us to detect actual movement.
     */
    private final Map<UUID, Location> previousLocations =
            new HashMap<>();


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */

    public HorseMovementListener(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ============================================================
     * START
     * ============================================================
     */

    public void start() {

        if (movementTask != null) {

            return;
        }


        long interval =
                plugin.getConfig().getLong(
                        "settings.particles.interval-ticks",
                        5L
                );


        if (interval < 1) {

            interval = 5L;
        }


        final double minimumMovement =
                plugin.getConfig().getDouble(
                        "settings.particles.minimum-speed",
                        0.05D
                );


        movementTask =
                new BukkitRunnable() {


                    @Override
                    public void run() {

                        for (var world
                                : Bukkit.getWorlds()) {

                            for (Horse horse
                                    : world.getEntitiesByClass(
                                            Horse.class
                                    )) {

                                handleHorseMovement(
                                        horse,
                                        minimumMovement
                                );
                            }
                        }
                    }

                };


        movementTask.runTaskTimer(
                plugin,
                interval,
                interval
        );
    }


    /*
     * ============================================================
     * STOP
     * ============================================================
     */

    public void stop() {

        if (movementTask != null) {

            movementTask.cancel();

            movementTask = null;
        }


        previousLocations.clear();
    }


    /*
     * ============================================================
     * HANDLE HORSE MOVEMENT
     * ============================================================
     */

    private void handleHorseMovement(
            Horse horse,
            double minimumMovement
    ) {


        /*
         * ========================================================
         * IMPORTANT:
         *
         * Do not spawn normal running particles while flying.
         *
         * FlightManager handles the Bifrost pathway instead.
         * ========================================================
         */

        if (plugin.getFlightManager() != null
                && plugin.getFlightManager()
                .isFlying(horse)) {

            /*
             * Update the stored location so movement tracking
             * remains correct after landing.
             */
            previousLocations.put(
                    horse.getUniqueId(),
                    horse.getLocation().clone()
            );

            return;
        }


        ItemStack armor =
                horse.getInventory()
                        .getArmor();


        UUID horseId =
                horse.getUniqueId();


        Location currentLocation =
                horse.getLocation()
                        .clone();


        Location previousLocation =
                previousLocations.get(
                        horseId
                );


        /*
         * Save the current location.
         */
        previousLocations.put(
                horseId,
                currentLocation
        );


        /*
         * First movement check needs a previous location.
         */
        if (previousLocation == null) {

            return;
        }


        /*
         * Don't compare different worlds.
         */
        if (!previousLocation.getWorld()
                .equals(
                        currentLocation.getWorld()
                )) {

            return;
        }


        /*
         * Calculate horizontal movement.
         *
         * Y movement is ignored.
         */
        double deltaX =
                currentLocation.getX()
                        - previousLocation.getX();


        double deltaZ =
                currentLocation.getZ()
                        - previousLocation.getZ();


        double horizontalMovement =
                Math.sqrt(
                        deltaX * deltaX
                                +
                                deltaZ * deltaZ
                );


        /*
         * Horse is not moving fast enough.
         */
        if (horizontalMovement
                < minimumMovement) {

            return;
        }


        /*
         * ========================================================
         * CHECK EQUINOX ARMOR
         * ========================================================
         */

        if (!plugin.getHorseArmorManager()
                .isEquinoxArmor(armor)) {

            return;
        }


        /*
         * ========================================================
         * CHECK SWIFT ENCHANTMENT
         * ========================================================
         */

        int swiftLevel =
                plugin.getHorseArmorManager()
                        .getLevel(
                                armor,
                                EnchantmentType.SWIFT
                        );


        if (swiftLevel <= 0) {

            return;
        }


        /*
         * Spawn running particles.
         */
        spawnHorseParticles(
                horse,
                swiftLevel
        );
    }


    /*
     * ============================================================
     * HORSE RUNNING PARTICLES
     * ============================================================
     */

    private void spawnHorseParticles(
            Horse horse,
            int swiftLevel
    ) {

        Location base =
                horse.getLocation();


        /*
         * Horse yaw.
         */
        double yaw =
                Math.toRadians(
                        base.getYaw()
                );


        /*
         * Forward direction.
         */
        double forwardX =
                -Math.sin(yaw);


        double forwardZ =
                Math.cos(yaw);


        /*
         * Side direction.
         */
        double sideX =
                Math.cos(yaw);


        double sideZ =
                Math.sin(yaw);


        /*
         * ========================================================
         * LEG POSITIONS
         * ========================================================
         */

        Location frontLeft =
                base.clone().add(
                        forwardX * 0.75
                                + sideX * 0.28,
                        0.15,
                        forwardZ * 0.75
                                + sideZ * 0.28
                );


        Location frontRight =
                base.clone().add(
                        forwardX * 0.75
                                - sideX * 0.28,
                        0.15,
                        forwardZ * 0.75
                                - sideZ * 0.28
                );


        Location backLeft =
                base.clone().add(
                        -forwardX * 0.75
                                + sideX * 0.28,
                        0.15,
                        -forwardZ * 0.75
                                + sideZ * 0.28
                );


        Location backRight =
                base.clone().add(
                        -forwardX * 0.75
                                - sideX * 0.28,
                        0.15,
                        -forwardZ * 0.75
                                - sideZ * 0.28
                );


        /*
         * More Swift levels = slightly more particles.
         */
        int particleCount =
                Math.min(
                        1 + swiftLevel / 3,
                        4
                );


        /*
         * Spawn particles around all legs.
         */
        spawnLegParticles(
                frontLeft,
                particleCount
        );


        spawnLegParticles(
                frontRight,
                particleCount
        );


        spawnLegParticles(
                backLeft,
                particleCount
        );


        spawnLegParticles(
                backRight,
                particleCount
        );
    }


    /*
     * ============================================================
     * SPAWN LEG PARTICLES
     * ============================================================
     */

    private void spawnLegParticles(
            Location location,
            int count
    ) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }


        location.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                location,
                count,
                0.12,
                0.05,
                0.12,
                0.01
        );
    }
}
