package com.istiak.equinox.animations;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Horse;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MountAnimationManager {

    /*
     * ============================================================
     * PLUGIN
     * ============================================================
     */

    private final EquinoxPlugin plugin;


    /*
     * ============================================================
     * ACTIVE FLYING MOUNTS
     * ============================================================
     */

    private final Set<UUID> flyingMounts =
            new HashSet<>();


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */

    public MountAnimationManager(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;

        startAnimationTask();
    }


    /*
     * ============================================================
     * REGISTER FLYING MOUNT
     * ============================================================
     */

    public void startFlying(
            Horse horse
    ) {

        if (horse == null) {

            return;
        }


        flyingMounts.add(
                horse.getUniqueId()
        );
    }


    /*
     * ============================================================
     * STOP FLYING
     * ============================================================
     */

    public void stopFlying(
            Horse horse
    ) {

        if (horse == null) {

            return;
        }


        flyingMounts.remove(
                horse.getUniqueId()
        );
    }


    /*
     * ============================================================
     * CHECK FLYING
     * ============================================================
     */

    public boolean isFlying(
            Horse horse
    ) {

        if (horse == null) {

            return false;
        }


        return flyingMounts.contains(
                horse.getUniqueId()
        );
    }


    /*
     * ============================================================
     * MAIN ANIMATION TASK
     * ============================================================
     */

    private void startAnimationTask() {

        new BukkitRunnable() {

            @Override
            public void run() {

                animateFlyingMounts();
            }

        }.runTaskTimer(
                plugin,
                1L,
                2L
        );
    }


    /*
     * ============================================================
     * ANIMATE FLYING MOUNTS
     * ============================================================
     */

    private void animateFlyingMounts() {

        flyingMounts.removeIf(
                uuid -> {

                    Horse horse =
                            plugin.getMountManager()
                                    .getLoadedHorse(
                                            uuid
                                    );


                    if (horse == null) {

                        return true;
                    }


                    if (!horse.isValid()
                            || horse.isDead()) {

                        return true;
                    }


                    playWingParticles(
                            horse
                    );


                    playFlightTrail(
                            horse
                    );


                    return false;
                }
        );
    }


    /*
     * ============================================================
     * WING PARTICLES
     * ============================================================
     *
     * Creates two magical wings.
     */

    private void playWingParticles(
            Horse horse
    ) {

        Location center =
                horse.getLocation()
                        .clone()
                        .add(
                                0,
                                1.2,
                                0
                        );


        World world =
                horse.getWorld();


        float yaw =
                center.getYaw();


        double yawRadians =
                Math.toRadians(
                        yaw
                );


        /*
         * Side direction.
         */

        Vector side =
                new Vector(
                        Math.cos(yawRadians),
                        0,
                        Math.sin(yawRadians)
                ).normalize();


        /*
         * Forward direction.
         */

        Vector forward =
                center.getDirection()
                        .setY(0)
                        .normalize();


        /*
         * Create both wings.
         */

        createWing(
                world,
                center,
                side.clone(),
                forward.clone(),
                1
        );


        createWing(
                world,
                center,
                side.clone(),
                forward.clone(),
                -1
        );
    }


    /*
     * ============================================================
     * CREATE ONE WING
     * ============================================================
     */

    private void createWing(
            World world,
            Location center,
            Vector side,
            Vector forward,
            int direction
    ) {

        for (int i = 0;
             i < 7;
             i++) {

            double progress =
                    i / 6.0;


            /*
             * Wing expands outward.
             */

            double sideOffset =
                    progress * 2.2
                            * direction;


            /*
             * Wing rises slightly.
             */

            double height =
                    Math.sin(
                            progress * Math.PI
                    ) * 0.7;


            Location point =
                    center.clone()
                            .add(
                                    side.clone()
                                            .multiply(
                                                    sideOffset
                                            )
                            )
                            .add(
                                    0,
                                    height,
                                    0
                            );


            world.spawnParticle(
                    Particle.END_ROD,
                    point,
                    1,
                    0,
                    0,
                    0,
                    0
            );


            /*
             * Feather effect.
             */

            world.spawnParticle(
                    Particle.ENCHANT,
                    point,
                    2,
                    0.08,
                    0.08,
                    0.08,
                    0.02
            );
        }
    }


    /*
     * ============================================================
     * FLIGHT TRAIL
     * ============================================================
     */

    private void playFlightTrail(
            Horse horse
    ) {

        Location location =
                horse.getLocation()
                        .clone();


        Vector direction =
                location.getDirection()
                        .normalize();


        /*
         * Move behind horse.
         */

        direction.multiply(
                -1.5
        );


        Location trail =
                location.add(
                        direction
                );


        trail.add(
                0,
                1.0,
                0
        );


        World world =
                horse.getWorld();


        /*
         * Main magical trail.
         */

        world.spawnParticle(
                Particle.END_ROD,
                trail,
                4,
                0.3,
                0.3,
                0.3,
                0.03
        );


        world.spawnParticle(
                Particle.ENCHANT,
                trail,
                8,
                0.45,
                0.45,
                0.45,
                0.12
        );


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                trail,
                3,
                0.25,
                0.25,
                0.25,
                0.05
        );
    }


    /*
     * ============================================================
     * TAKEOFF EFFECT
     * ============================================================
     */

    public void playTakeoffEffect(
            Horse horse
    ) {

        if (horse == null) {

            return;
        }


        Location location =
                horse.getLocation()
                        .clone();


        World world =
                horse.getWorld();


        Location center =
                location.clone()
                        .add(
                                0,
                                0.4,
                                0
                        );


        /*
         * Magical explosion.
         */

        world.spawnParticle(
                Particle.END_ROD,
                center,
                70,
                1.2,
                0.5,
                1.2,
                0.12
        );


        world.spawnParticle(
                Particle.ENCHANT,
                center,
                100,
                1.5,
                0.8,
                1.5,
                0.3
        );


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                center,
                45,
                1.0,
                0.5,
                1.0,
                0.18
        );


        createMagicRing(
                location,
                2.0
        );
    }


    /*
     * ============================================================
     * LANDING EFFECT
     * ============================================================
     */

    public void playLandingEffect(
            Horse horse
    ) {

        if (horse == null) {

            return;
        }


        Location location =
                horse.getLocation()
                        .clone();


        World world =
                horse.getWorld();


        /*
         * Landing burst.
         */

        world.spawnParticle(
                Particle.CLOUD,
                location,
                30,
                1.0,
                0.2,
                1.0,
                0.08
        );


        world.spawnParticle(
                Particle.ENCHANT,
                location,
                55,
                1.4,
                0.4,
                1.4,
                0.2
        );


        world.spawnParticle(
                Particle.END_ROD,
                location,
                25,
                1.0,
                0.3,
                1.0,
                0.05
        );


        createMagicRing(
                location,
                1.8
        );
    }


    /*
     * ============================================================
     * MAGIC RING
     * ============================================================
     */

    private void createMagicRing(
            Location location,
            double radius
    ) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }


        World world =
                location.getWorld();


        int points = 32;


        for (int i = 0;
             i < points;
             i++) {

            double angle =
                    (Math.PI * 2.0 * i)
                            / points;


            double x =
                    Math.cos(angle)
                            * radius;


            double z =
                    Math.sin(angle)
                            * radius;


            Location point =
                    location.clone()
                            .add(
                                    x,
                                    0.15,
                                    z
                            );


            world.spawnParticle(
                    Particle.END_ROD,
                    point,
                    1,
                    0,
                    0,
                    0,
                    0
            );
        }
    }


    /*
     * ============================================================
     * RUNNING PARTICLES
     * ============================================================
     */

    public void playRunningParticles(
            Horse horse
    ) {

        if (horse == null) {

            return;
        }


        /*
         * Don't play running effects during flight.
         */

        if (isFlying(horse)) {

            return;
        }


        /*
         * Only play when moving quickly.
         */

        if (horse.getVelocity()
                .lengthSquared()
                < 0.04) {

            return;
        }


        Location location =
                horse.getLocation()
                        .clone();


        World world =
                horse.getWorld();


        /*
         * Dust.
         */

        world.spawnParticle(
                Particle.CLOUD,
                location,
                4,
                0.45,
                0.05,
                0.45,
                0.04
        );


        /*
         * Magical sparks.
         */

        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                location.clone()
                        .add(
                                0,
                                0.25,
                                0
                        ),
                3,
                0.4,
                0.15,
                0.4,
                0.04
        );


        world.spawnParticle(
                Particle.ENCHANT,
                location.clone()
                        .add(
                                0,
                                0.5,
                                0
                        ),
                4,
                0.5,
                0.3,
                0.5,
                0.08
        );
    }


    /*
     * ============================================================
     * SHUTDOWN
     * ============================================================
     */

    public void shutdown() {

        flyingMounts.clear();
    }
}
