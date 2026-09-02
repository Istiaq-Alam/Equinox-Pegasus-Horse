package com.istiak.equinox.flight;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FlightManager {

    /*
     * ============================================================
     * CONFIGURATION
     * ============================================================
     */

    private static final double FLIGHT_SPEED = 0.85;

    private static final double ASCENT_MULTIPLIER = 0.70;

    private static final double DESCENT_MULTIPLIER = 0.45;

    private static final double MAX_VERTICAL_SPEED = 0.65;

    /*
     * How often the flight visual system runs.
     */
    private static final long PARTICLE_INTERVAL = 2L;

    /*
     * ============================================================
     * BIFROST PATH CONFIGURATION
     * ============================================================
     */

    /*
     * Distance in front of the horse where the pathway begins.
     */
    private static final double PATH_START_DISTANCE = 1.2;

    /*
     * How far forward the visible pathway extends.
     */
    private static final double PATH_LENGTH = 9.0;

    /*
     * Distance between pathway sections.
     */
    private static final double PATH_STEP = 0.55;

    /*
     * Width of the magical pathway.
     */
    private static final double PATH_HALF_WIDTH = 1.25;

    /*
     * How far below the horse the pathway appears.
     *
     * This makes the horse look like it is running
     * above the magical bridge.
     */
    private static final double PATH_BELOW_HORSE = 1.15;


    /*
     * ============================================================
     * FIELDS
     * ============================================================
     */

    private final EquinoxPlugin plugin;

    /*
     * UUIDs of horses currently flying.
     */
    private final Set<UUID> flyingMounts =
            new HashSet<>();

    /*
     * UUIDs currently being processed for takeoff/landing.
     */
    private final Set<UUID> transitionMounts =
            new HashSet<>();


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */

    public FlightManager(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;

        startFlightTask();
    }


    /*
     * ============================================================
     * CHECK FLIGHT
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
     * CHECK TRANSITION
     * ============================================================
     */

    public boolean isTransitioning(
            Horse horse
    ) {

        if (horse == null) {

            return false;
        }

        return transitionMounts.contains(
                horse.getUniqueId()
        );
    }


    /*
     * ============================================================
     * START FLIGHT
     * ============================================================
     */

    public boolean startFlight(
            Player player,
            Horse horse
    ) {

        if (player == null
                || horse == null) {

            return false;
        }


        /*
         * Already flying.
         */
        if (isFlying(horse)) {

            return false;
        }


        /*
         * Prevent duplicate transitions.
         */
        if (isTransitioning(horse)) {

            return false;
        }


        transitionMounts.add(
                horse.getUniqueId()
        );


        /*
         * Enable flight.
         */
        flyingMounts.add(
                horse.getUniqueId()
        );


        /*
         * Prevent fall damage.
         */
        horse.setFallDistance(0.0f);


        /*
         * Takeoff animation.
         */
        playTakeoffEffect(
                horse.getLocation()
        );


        horse.getWorld().playSound(
                horse.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_FLAP,
                1.0f,
                1.25f
        );


        horse.getWorld().playSound(
                horse.getLocation(),
                Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM,
                0.8f,
                1.5f
        );


        /*
         * Initial upward boost.
         */
        Vector velocity =
                horse.getVelocity();

        velocity.setY(0.55);

        horse.setVelocity(
                velocity
        );


        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> transitionMounts.remove(
                        horse.getUniqueId()
                ),
                12L
        );


        return true;
    }


    /*
     * ============================================================
     * STOP FLIGHT
     * ============================================================
     */

    public boolean stopFlight(
            Player player,
            Horse horse
    ) {

        if (horse == null) {

            return false;
        }


        if (!isFlying(horse)) {

            return false;
        }


        flyingMounts.remove(
                horse.getUniqueId()
        );


        transitionMounts.add(
                horse.getUniqueId()
        );


        /*
         * Remove upward momentum.
         */
        Vector velocity =
                horse.getVelocity();

        velocity.setY(
                Math.min(
                        velocity.getY(),
                        -0.15
                )
        );

        horse.setVelocity(
                velocity
        );


        playLandingEffect(
                horse.getLocation()
        );


        horse.getWorld().playSound(
                horse.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_FLAP,
                0.8f,
                0.8f
        );


        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> transitionMounts.remove(
                        horse.getUniqueId()
                ),
                10L
        );


        return true;
    }


    /*
     * ============================================================
     * FORCE STOP
     * ============================================================
     */

    public void forceStopFlight(
            Horse horse
    ) {

        if (horse == null) {

            return;
        }


        flyingMounts.remove(
                horse.getUniqueId()
        );

        transitionMounts.remove(
                horse.getUniqueId()
        );


        horse.setFallDistance(
                0.0f
        );
    }


    /*
     * ============================================================
     * MAIN FLIGHT TASK
     * ============================================================
     */

    private void startFlightTask() {

        new BukkitRunnable() {

            private long particleTicks = 0L;


            @Override
            public void run() {

                /*
                 * Work on a copy because mounts may be removed
                 * while iterating.
                 */
                Set<UUID> mounts =
                        new HashSet<>(
                                flyingMounts
                        );


                for (UUID horseId : mounts) {

                    Horse horse =
                            plugin.getMountManager()
                                    .getLoadedHorse(
                                            horseId
                                    );


                    /*
                     * Horse disappeared.
                     */
                    if (horse == null
                            || !horse.isValid()
                            || horse.isDead()) {

                        flyingMounts.remove(horseId);

                        transitionMounts.remove(horseId);

                        continue;
                    }


                    /*
                     * Must have a rider.
                     */
                    if (horse.getPassengers().isEmpty()) {

                        stopFlyingWithoutPlayer(horse);

                        continue;
                    }


                    /*
                     * Find player rider.
                     */
                    Player rider = null;


                    for (var passenger
                            : horse.getPassengers()) {

                        if (passenger instanceof Player player) {

                            rider = player;

                            break;
                        }
                    }


                    if (rider == null
                            || !rider.isOnline()) {

                        stopFlyingWithoutPlayer(horse);

                        continue;
                    }


                    /*
                     * Ensure this is still the owner's
                     * registered Equinox mount.
                     */
                    if (!plugin.getMountManager()
                            .isOwner(
                                    rider,
                                    horse
                            )) {

                        forceStopFlight(horse);

                        continue;
                    }


                    /*
                     * Horse must still have Equinox armor.
                     */
                    if (!plugin.getHorseArmorManager()
                            .isEquinoxArmor(
                                    horse.getInventory()
                                            .getArmor()
                            )) {

                        forceStopFlight(horse);

                        continue;
                    }


                    /*
                     * Prevent fall damage while flying.
                     */
                    horse.setFallDistance(0.0f);


                    /*
                     * ====================================================
                     * FLIGHT MOVEMENT
                     * ====================================================
                     */

                    applyFlightMovement(
                            rider,
                            horse
                    );


                    /*
                     * ====================================================
                     * FLIGHT VISUALS
                     * ====================================================
                     */

                    if (particleTicks
                            % PARTICLE_INTERVAL == 0) {

                        playFlightParticles(
                                horse,
                                rider
                        );
                    }
                }


                particleTicks++;

            }

        }.runTaskTimer(
                plugin,
                1L,
                1L
        );
    }


    /*
     * ============================================================
     * APPLY FLIGHT MOVEMENT
     * ============================================================
     */

    private void applyFlightMovement(
            Player rider,
            Horse horse
    ) {

        Location riderLocation =
                rider.getLocation();


        Vector direction =
                riderLocation.getDirection()
                        .clone();


        /*
         * Normalize direction.
         */
        if (direction.lengthSquared()
                <= 0.0001) {

            return;
        }


        direction.normalize();


        /*
         * Horizontal movement direction.
         */
        Vector horizontal =
                direction.clone();

        horizontal.setY(0);


        if (horizontal.lengthSquared()
                > 0.0001) {

            horizontal.normalize();

            horizontal.multiply(
                    FLIGHT_SPEED
            );
        }


        /*
         * ========================================================
         * VERTICAL CONTROL
         * ========================================================
         *
         * Looking upward makes the horse rise.
         * Looking downward makes the horse descend.
         */

        double pitch =
                riderLocation.getPitch();


        double verticalVelocity;


        /*
         * Looking significantly upward.
         */
        if (pitch <= -20.0) {

            verticalVelocity =
                    Math.min(
                            MAX_VERTICAL_SPEED,
                            Math.abs(pitch / 90.0)
                                    * ASCENT_MULTIPLIER
                    );
        }


        /*
         * Looking significantly downward.
         */
        else if (pitch >= 25.0) {

            verticalVelocity =
                    -Math.min(
                            MAX_VERTICAL_SPEED,
                            Math.abs(pitch / 90.0)
                                    * DESCENT_MULTIPLIER
                    );
        }


        /*
         * Level flight.
         */
        else {

            verticalVelocity = 0.0;
        }


        horizontal.setY(
                verticalVelocity
        );


        /*
         * Apply movement.
         */
        horse.setVelocity(
                horizontal
        );
    }


    /*
     * ============================================================
     * STOP FLYING WITHOUT PLAYER
     * ============================================================
     */

    private void stopFlyingWithoutPlayer(
            Horse horse
    ) {

        flyingMounts.remove(
                horse.getUniqueId()
        );

        transitionMounts.remove(
                horse.getUniqueId()
        );


        horse.setFallDistance(
                0.0f
        );
    }


    /*
     * ============================================================
     * TAKEOFF EFFECT
     * ============================================================
     */

    private void playTakeoffEffect(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }


        location.getWorld().spawnParticle(
                Particle.CLOUD,
                location.clone().add(
                        0,
                        0.3,
                        0
                ),
                35,
                0.8,
                0.2,
                0.8,
                0.08
        );


        location.getWorld().spawnParticle(
                Particle.END_ROD,
                location.clone().add(
                        0,
                        1.0,
                        0
                ),
                35,
                0.8,
                0.8,
                0.8,
                0.05
        );


        location.getWorld().spawnParticle(
                Particle.ENCHANT,
                location.clone().add(
                        0,
                        1.0,
                        0
                ),
                60,
                1.0,
                1.0,
                1.0,
                0.4
        );


        /*
         * Magical launch sparks.
         */
        location.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                location.clone().add(
                        0,
                        0.5,
                        0
                ),
                25,
                0.9,
                0.4,
                0.9,
                0.12
        );
    }


    /*
     * ============================================================
     * LANDING EFFECT
     * ============================================================
     */

    private void playLandingEffect(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }


        location.getWorld().spawnParticle(
                Particle.CLOUD,
                location,
                25,
                0.7,
                0.3,
                0.7,
                0.05
        );


        location.getWorld().spawnParticle(
                Particle.END_ROD,
                location.clone().add(
                        0,
                        1,
                        0
                ),
                20,
                0.6,
                0.7,
                0.6,
                0.03
        );


        location.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                location.clone().add(
                        0,
                        0.4,
                        0
                ),
                20,
                0.8,
                0.25,
                0.8,
                0.08
        );
    }


    /*
     * ============================================================
     * FLIGHT PARTICLES
     * ============================================================
     *
     * IMPORTANT:
     *
     * The old wing particle animation has been removed.
     *
     * The main flight visual is now the magical
     * Bifrost-style pathway in FRONT of the horse.
     */

    private void playFlightParticles(
            Horse horse,
            Player rider
    ) {

        playBifrostPathway(
                horse,
                rider
        );


        /*
         * Small magical energy around the horse.
         *
         * This is NOT a wing animation.
         */
        Location location =
                horse.getLocation();


        horse.getWorld().spawnParticle(
                Particle.END_ROD,
                location.clone().add(
                        0,
                        1.0,
                        0
                ),
                2,
                0.35,
                0.25,
                0.35,
                0.01
        );


        /*
         * Small sparkle behind the mount.
         */
        Vector backwards =
                getHorizontalDirection(
                        rider
                ).multiply(-1.0);


        Location trail =
                location.clone()
                        .add(backwards)
                        .add(
                                0,
                                0.7,
                                0
                        );


        horse.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                trail,
                2,
                0.2,
                0.2,
                0.2,
                0.02
        );
    }


    /*
     * ============================================================
     * BIFROST MAGICAL PATHWAY
     * ============================================================
     *
     * Creates a wide magical road directly in front of the horse.
     *
     * The horse appears to run above the pathway.
     */

    private void playBifrostPathway(
            Horse horse,
            Player rider
    ) {

        Location horseLocation =
                horse.getLocation();


        Vector forward =
                getHorizontalDirection(
                        rider
                );


        /*
         * Side direction.
         *
         * Used to create the width of the pathway.
         */
        Vector side =
                new Vector(
                        -forward.getZ(),
                        0,
                        forward.getX()
                );


        if (side.lengthSquared() > 0.0001) {

            side.normalize();
        }


        /*
         * Pathway center begins in front of the horse
         * and slightly below it.
         */
        Location base =
                horseLocation.clone()
                        .add(
                                0,
                                -PATH_BELOW_HORSE,
                                0
                        );


        /*
         * ========================================================
         * CREATE PATH SECTIONS
         * ========================================================
         */

        for (double distance =
                     PATH_START_DISTANCE;

             distance <= PATH_LENGTH;

             distance += PATH_STEP) {


            Vector forwardOffset =
                    forward.clone()
                            .multiply(distance);


            Location center =
                    base.clone()
                            .add(forwardOffset);


            /*
             * ====================================================
             * CENTER ENERGY LANE
             * ====================================================
             */

            spawnColoredDust(
                    center,
                    Color.fromRGB(
                            80,
                            220,
                            255
                    ),
                    1.6f,
                    2
            );


            /*
             * ====================================================
             * LEFT MAGICAL LANE
             * ====================================================
             */

            Location leftLane =
                    center.clone()
                            .add(
                                    side.clone()
                                            .multiply(
                                                    PATH_HALF_WIDTH
                                            )
                            );


            spawnColoredDust(
                    leftLane,
                    Color.fromRGB(
                            185,
                            90,
                            255
                    ),
                    1.5f,
                    2
            );


            /*
             * ====================================================
             * RIGHT MAGICAL LANE
             * ====================================================
             */

            Location rightLane =
                    center.clone()
                            .subtract(
                                    side.clone()
                                            .multiply(
                                                    PATH_HALF_WIDTH
                                            )
                            );


            spawnColoredDust(
                    rightLane,
                    Color.fromRGB(
                            255,
                            90,
                            210
                    ),
                    1.5f,
                    2
            );


            /*
             * ====================================================
             * INNER PATH LIGHT
             * ====================================================
             */

            Location leftInner =
                    center.clone()
                            .add(
                                    side.clone()
                                            .multiply(0.6)
                            );


            Location rightInner =
                    center.clone()
                            .subtract(
                                    side.clone()
                                            .multiply(0.6)
                            );


            horse.getWorld().spawnParticle(
                    Particle.END_ROD,
                    leftInner,
                    1,
                    0.05,
                    0.03,
                    0.05,
                    0.001
            );


            horse.getWorld().spawnParticle(
                    Particle.END_ROD,
                    rightInner,
                    1,
                    0.05,
                    0.03,
                    0.05,
                    0.001
            );


            /*
             * ====================================================
             * RANDOM ENERGY SPARKS
             * ====================================================
             */

            if (((int) (distance * 10)) % 11 == 0) {

                horse.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        center.clone().add(
                                0,
                                0.1,
                                0
                        ),
                        2,
                        0.35,
                        0.05,
                        0.35,
                        0.02
                );
            }
        }


        /*
         * ========================================================
         * FAR END MAGICAL GLOW
         * ========================================================
         */

        Location farEnd =
                base.clone()
                        .add(
                                forward.clone()
                                        .multiply(PATH_LENGTH)
                        );


        horse.getWorld().spawnParticle(
                Particle.END_ROD,
                farEnd.clone().add(
                        0,
                        0.15,
                        0
                ),
                4,
                0.35,
                0.08,
                0.35,
                0.02
        );


        horse.getWorld().spawnParticle(
                Particle.ENCHANT,
                farEnd,
                5,
                0.5,
                0.15,
                0.5,
                0.1
        );
    }


    /*
     * ============================================================
     * COLORED DUST HELPER
     * ============================================================
     */

    private void spawnColoredDust(
            Location location,
            Color color,
            float size,
            int count
    ) {

        Particle.DustOptions dust =
                new Particle.DustOptions(
                        color,
                        size
                );


        location.getWorld().spawnParticle(
                Particle.DUST,
                location,
                count,
                0.10,
                0.03,
                0.10,
                0.0,
                dust
        );
    }


    /*
     * ============================================================
     * GET HORIZONTAL RIDER DIRECTION
     * ============================================================
     */

    private Vector getHorizontalDirection(
            Player rider
    ) {

        Vector direction =
                rider.getLocation()
                        .getDirection()
                        .clone();


        /*
         * Remove vertical direction.
         */
        direction.setY(0);


        /*
         * Safety fallback.
         */
        if (direction.lengthSquared()
                <= 0.0001) {

            return new Vector(
                    0,
                    0,
                    1
            );
        }


        return direction.normalize();
    }


    /*
     * ============================================================
     * SHUTDOWN
     * ============================================================
     */

    public void shutdown() {

        /*
         * Stop every active flight.
         */

        for (UUID horseId
                : new HashSet<>(flyingMounts)) {

            Horse horse =
                    plugin.getMountManager()
                            .getLoadedHorse(
                                    horseId
                            );


            if (horse != null) {

                horse.setFallDistance(
                        0.0f
                );
            }
        }


        flyingMounts.clear();

        transitionMounts.clear();
    }
}
