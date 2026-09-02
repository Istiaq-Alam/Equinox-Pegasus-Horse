package com.istiak.equinox.listeners;

import com.istiak.equinox.EquinoxPlugin;
import com.istiak.equinox.items.WhistleManager;
import com.istiak.equinox.mounts.MountData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ============================================================
 * EQUINOX WHISTLE LISTENER
 * ============================================================
 *
 * Whistle behavior:
 *
 * 1. Player riding own mount:
 *      -> Return mount to permanent home.
 *
 * 2. Horse is leashed:
 *      -> Do nothing. Never break the lead.
 *
 * 3. Horse is within RETURN_RADIUS:
 *      -> Return mount home.
 *
 * 4. Horse is within RUN_RADIUS:
 *      -> Horse physically runs toward owner.
 *
 * 5. Horse is farther than RUN_RADIUS:
 *      -> Horse teleports safely near owner.
 *
 * The permanent home location is created when:
 *
 *      /equinox mount bind
 *
 * is executed.
 */
public final class WhistleListener implements Listener {

    /*
     * ============================================================
     * CONFIGURATION
     * ============================================================
     */

    /**
     * If the horse is this close to the player,
     * whistle sends it home.
     */
    private static final double RETURN_RADIUS = 20.0;

    /**
     * If horse is inside this distance,
     * it physically runs toward the player.
     */
    private static final double RUN_RADIUS = 100.0;

    /**
     * Stop running when horse reaches this distance.
     */
    private static final double ARRIVAL_DISTANCE = 3.5;

    /**
     * Horizontal velocity used while calling the horse.
     */
    private static final double RUN_SPEED = 0.42;

    /**
     * Delay before teleporting.
     */
    private static final long TELEPORT_DELAY_TICKS = 12L;

    /**
     * Maximum running time.
     *
     * 20 ticks = 1 second.
     *
     * 300 ticks = 15 seconds.
     */
    private static final int MAX_RUN_TICKS = 300;

    /**
     * Prevent repeated whistle spam.
     */
    private static final long WHISTLE_COOLDOWN_MS = 750L;


    /*
     * ============================================================
     * FIELDS
     * ============================================================
     */

    private final EquinoxPlugin plugin;

    private final WhistleManager whistleManager;

    /**
     * Stores currently running horse-call tasks.
     *
     * Player UUID -> BukkitTask
     */
    private final Map<UUID, BukkitTask> activeRunTasks =
            new HashMap<>();

    /**
     * Whistle cooldown.
     *
     * Player UUID -> Last whistle time.
     */
    private final Map<UUID, Long> whistleCooldowns =
            new HashMap<>();


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */

    public WhistleListener(
            EquinoxPlugin plugin,
            WhistleManager whistleManager
    ) {

        this.plugin = plugin;
        this.whistleManager = whistleManager;
    }


    /*
     * ============================================================
     * WHISTLE INTERACTION
     * ============================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onWhistleUse(
            PlayerInteractEvent event
    ) {

        /*
         * Only main hand.
         */
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }


        /*
         * Only right-click.
         */
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {

            return;
        }


        Player player = event.getPlayer();


        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();


        /*
         * Only Equinox Whistle.
         */
        if (!whistleManager.isWhistle(item)) {
            return;
        }


        /*
         * Prevent normal Goat Horn behavior.
         */
        event.setCancelled(true);

        event.setUseItemInHand(
                org.bukkit.event.Event.Result.DENY
        );

        event.setUseInteractedBlock(
                org.bukkit.event.Event.Result.DENY
        );


        /*
         * Cooldown protection.
         */
        if (!canUseWhistle(player)) {

            player.sendActionBar(
                    Component.text(
                            "The whistle needs a moment...",
                            NamedTextColor.GRAY
                    )
            );

            return;
        }


        /*
         * Cancel any previous running task.
         */
        cancelRunTask(player.getUniqueId());


        /*
         * Whistle effects.
         */
        playWhistleEffect(player);


        /*
         * Handle mount.
         */
        handleWhistle(player);
    }


    /*
     * ============================================================
     * COOLDOWN
     * ============================================================
     */

    private boolean canUseWhistle(
            Player player
    ) {

        UUID playerId =
                player.getUniqueId();

        long now =
                System.currentTimeMillis();

        Long lastUse =
                whistleCooldowns.get(playerId);

        if (lastUse != null
                && now - lastUse < WHISTLE_COOLDOWN_MS) {

            return false;
        }


        whistleCooldowns.put(
                playerId,
                now
        );

        return true;
    }


    /*
     * ============================================================
     * MAIN WHISTLE LOGIC
     * ============================================================
     */

    private void handleWhistle(
            Player player
    ) {

        /*
         * Check mount.
         */
        if (!plugin.getMountManager()
                .hasMount(player)) {

            player.sendActionBar(
                    Component.text(
                            "No Equinox mount is bound.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * Get persistent mount data.
         */
        MountData mountData =
                plugin.getMountManager()
                        .getMount(player);

        if (mountData == null) {

            player.sendActionBar(
                    Component.text(
                            "Mount data could not be found.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * Find horse.
         */
        Horse horse =
                plugin.getMountManager()
                        .getLoadedMount(player);


        /*
         * Horse is not loaded.
         */
        if (horse == null) {

            player.sendActionBar(
                    Component.text(
                            "Your Equinox mount is not loaded.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * ========================================================
         * LEASH SAFETY
         * ========================================================
         */

        if (horse.isLeashed()) {

            player.sendActionBar(
                    Component.text(
                            "Your Equinox mount is leashed.",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        /*
         * ========================================================
         * PLAYER IS RIDING
         * ========================================================
         */

        if (horse.getPassengers().contains(player)) {

            returnMountHome(
                    player,
                    horse,
                    mountData
            );

            return;
        }


        /*
         * ========================================================
         * DISTANCE
         * ========================================================
         */

        double distance;


        /*
         * Different worlds = far away.
         */
        if (!horse.getWorld().equals(player.getWorld())) {

            distance = Double.MAX_VALUE;

        } else {

            distance =
                    horse.getLocation()
                            .distance(
                                    player.getLocation()
                            );
        }


        /*
         * ========================================================
         * CLOSE TO PLAYER
         *
         * Return home.
         * ========================================================
         */

        if (distance <= RETURN_RADIUS) {

            returnMountHome(
                    player,
                    horse,
                    mountData
            );

            return;
        }


        /*
         * ========================================================
         * WITHIN RUN RANGE
         *
         * Physically run toward player.
         * ========================================================
         */

        if (distance <= RUN_RADIUS) {

            runMountToPlayer(
                    player,
                    horse
            );

            return;
        }


        /*
         * ========================================================
         * FAR AWAY
         *
         * Teleport near player.
         * ========================================================
         */

        teleportMountToPlayer(
                player,
                horse
        );
    }


    /*
     * ============================================================
     * RUN MOUNT TO PLAYER
     * ============================================================
     */

    private void runMountToPlayer(
            Player player,
            Horse horse
    ) {

        UUID playerId =
                player.getUniqueId();


        player.sendActionBar(
                Component.text(
                        "Your Equinox mount is coming...",
                        NamedTextColor.LIGHT_PURPLE
                )
        );


        BukkitTask task =
                plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(
                                plugin,
                                new Runnable() {

                                    private int ticks = 0;

                                    @Override
                                    public void run() {

                                        /*
                                         * Player safety.
                                         */
                                        if (!player.isOnline()) {

                                            cancelRunTask(playerId);

                                            return;
                                        }


                                        /*
                                         * Horse safety.
                                         */
                                        if (!horse.isValid()
                                                || horse.isDead()) {

                                            cancelRunTask(playerId);

                                            return;
                                        }


                                        /*
                                         * Never interfere with leash.
                                         */
                                        if (horse.isLeashed()) {

                                            cancelRunTask(playerId);

                                            return;
                                        }


                                        /*
                                         * Player changed world.
                                         */
                                        if (!horse.getWorld()
                                                .equals(player.getWorld())) {

                                            cancelRunTask(playerId);

                                            return;
                                        }


                                        Location horseLocation =
                                                horse.getLocation();

                                        Location playerLocation =
                                                player.getLocation();


                                        double distance =
                                                horseLocation.distance(
                                                        playerLocation
                                                );


                                        /*
                                         * Mount arrived.
                                         */
                                        if (distance <= ARRIVAL_DISTANCE) {

                                            horse.setVelocity(
                                                    new Vector(
                                                            0,
                                                            horse.getVelocity().getY(),
                                                            0
                                                    )
                                            );

                                            player.sendActionBar(
                                                    Component.text(
                                                            "✦ Your Equinox mount has arrived.",
                                                            NamedTextColor.GREEN
                                                    )
                                            );

                                            cancelRunTask(playerId);

                                            return;
                                        }


                                        /*
                                         * ====================================================
                                         * MOVE TOWARD PLAYER
                                         * ====================================================
                                         */

                                        Vector direction =
                                                playerLocation.toVector()
                                                        .subtract(
                                                                horseLocation.toVector()
                                                        );


                                        /*
                                         * Horizontal movement only.
                                         */
                                        direction.setY(0);


                                        if (direction.lengthSquared()
                                                > 0.0001) {

                                            direction.normalize();

                                            direction.multiply(
                                                    RUN_SPEED
                                            );


                                            /*
                                             * Preserve vertical motion.
                                             */
                                            direction.setY(
                                                    horse.getVelocity().getY()
                                            );


                                            horse.setVelocity(
                                                    direction
                                            );
                                        }


                                        /*
                                         * Face the player.
                                         */
                                        Location facing =
                                                horseLocation.clone();

                                        facing.setDirection(
                                                playerLocation.toVector()
                                                        .subtract(
                                                                horseLocation.toVector()
                                                        )
                                        );

                                        horse.setRotation(
                                                facing.getYaw(),
                                                horse.getLocation().getPitch()
                                        );


                                        /*
                                         * Magical trail.
                                         */
                                        horse.getWorld()
                                                .spawnParticle(
                                                        Particle.END_ROD,
                                                        horseLocation.clone()
                                                                .add(
                                                                        0,
                                                                        1.0,
                                                                        0
                                                                ),
                                                        2,
                                                        0.2,
                                                        0.25,
                                                        0.2,
                                                        0.01
                                                );


                                        /*
                                         * Timeout.
                                         */
                                        ticks++;

                                        if (ticks >= MAX_RUN_TICKS) {

                                            player.sendActionBar(
                                                    Component.text(
                                                            "Your Equinox mount could not reach you.",
                                                            NamedTextColor.YELLOW
                                                    )
                                            );

                                            cancelRunTask(playerId);
                                        }
                                    }

                                },
                                0L,
                                1L
                        );


        activeRunTasks.put(
                playerId,
                task
        );
    }


    /*
     * ============================================================
     * CANCEL RUN TASK
     * ============================================================
     */

    private void cancelRunTask(
            UUID playerId
    ) {

        BukkitTask task =
                activeRunTasks.remove(
                        playerId
                );

        if (task != null) {

            task.cancel();
        }
    }


    /*
     * ============================================================
     * TELEPORT MOUNT TO PLAYER
     * ============================================================
     */

    private void teleportMountToPlayer(
            Player player,
            Horse horse
    ) {

        player.sendActionBar(
                Component.text(
                        "Summoning your Equinox mount...",
                        NamedTextColor.LIGHT_PURPLE
                )
        );


        Location oldLocation =
                horse.getLocation().clone();


        /*
         * Departure effect.
         */
        playTeleportEffect(oldLocation);


        /*
         * Stop movement.
         */
        horse.setVelocity(
                new Vector(0, 0, 0)
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            /*
                             * Safety checks.
                             */
                            if (!player.isOnline()) {
                                return;
                            }

                            if (!horse.isValid()
                                    || horse.isDead()) {
                                return;
                            }

                            if (horse.isLeashed()) {
                                return;
                            }


                            Location destination =
                                    findSafeLocationNearPlayer(
                                            player
                                    );


                            if (destination == null) {

                                player.sendActionBar(
                                        Component.text(
                                                "Could not find a safe place for your mount.",
                                                NamedTextColor.RED
                                        )
                                );

                                return;
                            }


                            /*
                             * Teleport horse.
                             */
                            boolean success =
                                    horse.teleport(
                                            destination
                                    );


                            if (!success) {

                                player.sendActionBar(
                                        Component.text(
                                                "Could not summon your mount.",
                                                NamedTextColor.RED
                                        )
                                );

                                return;
                            }


                            /*
                             * Arrival effects.
                             */
                            playTeleportEffect(
                                    destination
                            );


                            destination.getWorld()
                                    .playSound(
                                            destination,
                                            Sound.ENTITY_ENDERMAN_TELEPORT,
                                            1.0f,
                                            1.1f
                                    );


                            player.sendActionBar(
                                    Component.text(
                                            "✦ Your Equinox mount has arrived.",
                                            NamedTextColor.GREEN
                                    )
                            );

                        },
                        TELEPORT_DELAY_TICKS
                );
    }


    /*
     * ============================================================
     * RETURN MOUNT HOME
     * ============================================================
     */

    private void returnMountHome(
            Player player,
            Horse horse,
            MountData mountData
    ) {

        /*
         * Never teleport leashed horse.
         */
        if (horse.isLeashed()) {

            player.sendActionBar(
                    Component.text(
                            "Your Equinox mount is leashed.",
                            NamedTextColor.YELLOW
                    )
            );

            return;
        }


        /*
         * Get permanent home.
         */
        Location home =
                mountData.getHomeLocation();


        if (home == null
                || home.getWorld() == null) {

            player.sendActionBar(
                    Component.text(
                            "Your mount's home location is unavailable.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * Cancel movement.
         */
        cancelRunTask(
                player.getUniqueId()
        );


        player.sendActionBar(
                Component.text(
                        "Sending your Equinox mount home...",
                        NamedTextColor.LIGHT_PURPLE
                )
        );


        /*
         * Eject passengers.
         */
        horse.eject();


        /*
         * Stop horse.
         */
        horse.setVelocity(
                new Vector(0, 0, 0)
        );


        Location oldLocation =
                horse.getLocation().clone();


        playTeleportEffect(
                oldLocation
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!horse.isValid()
                                    || horse.isDead()) {
                                return;
                            }

                            if (horse.isLeashed()) {
                                return;
                            }


                            /*
                             * Check whether home is safe.
                             */
                            Location safeHome =
                                    findSafeLocation(
                                            home
                                    );


                            if (safeHome == null) {

                                player.sendActionBar(
                                        Component.text(
                                                "Your mount's home is blocked.",
                                                NamedTextColor.RED
                                        )
                                );

                                return;
                            }


                            boolean success =
                                    horse.teleport(
                                            safeHome
                                    );


                            if (!success) {

                                player.sendActionBar(
                                        Component.text(
                                                "Could not return your mount home.",
                                                NamedTextColor.RED
                                        )
                                );

                                return;
                            }


                            playTeleportEffect(
                                    safeHome
                            );


                            safeHome.getWorld()
                                    .playSound(
                                            safeHome,
                                            Sound.ENTITY_ENDERMAN_TELEPORT,
                                            1.0f,
                                            0.85f
                                    );


                            if (player.isOnline()) {

                                player.sendActionBar(
                                        Component.text(
                                                "✦ Your Equinox mount returned home.",
                                                NamedTextColor.GREEN
                                        )
                                );
                            }

                        },
                        TELEPORT_DELAY_TICKS
                );
    }


    /*
     * ============================================================
     * FIND SAFE LOCATION NEAR PLAYER
     * ============================================================
     */

    private Location findSafeLocationNearPlayer(
            Player player
    ) {

        Location base =
                player.getLocation();

        World world =
                player.getWorld();


        /*
         * Try random locations.
         */
        for (int attempt = 0;
             attempt < 16;
             attempt++) {

            double angle =
                    ThreadLocalRandom.current()
                            .nextDouble(
                                    Math.PI * 2.0
                            );


            double radius =
                    3.0
                            + ThreadLocalRandom.current()
                            .nextDouble(3.0);


            int blockX =
                    base.getBlockX()
                            + (int) Math.round(
                            Math.cos(angle) * radius
                    );


            int blockZ =
                    base.getBlockZ()
                            + (int) Math.round(
                            Math.sin(angle) * radius
                    );


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
                            player.getYaw(),
                            0.0f
                    );


            if (isSafeForHorse(candidate)) {

                return candidate;
            }
        }


        /*
         * Try positions directly around player.
         */
        for (int x = -4;
             x <= 4;
             x += 2) {

            for (int z = -4;
                 z <= 4;
                 z += 2) {

                if (x == 0 && z == 0) {
                    continue;
                }


                int blockX =
                        base.getBlockX() + x;

                int blockZ =
                        base.getBlockZ() + z;


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
                                player.getYaw(),
                                0.0f
                        );


                if (isSafeForHorse(candidate)) {

                    return candidate;
                }
            }
        }


        return null;
    }


    /*
     * ============================================================
     * FIND SAFE HOME LOCATION
     * ============================================================
     */

    private Location findSafeLocation(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return null;
        }


        /*
         * First try exact home.
         */
        if (isSafeForHorse(location)) {

            return location.clone();
        }


        World world =
                location.getWorld();


        /*
         * Search around home.
         */
        for (int radius = 1;
             radius <= 4;
             radius++) {

            for (int x = -radius;
                 x <= radius;
                 x++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    int blockX =
                            location.getBlockX() + x;

                    int blockZ =
                            location.getBlockZ() + z;


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
                                    location.getYaw(),
                                    location.getPitch()
                            );


                    if (isSafeForHorse(candidate)) {

                        return candidate;
                    }
                }
            }
        }


        return null;
    }


    /*
     * ============================================================
     * SAFE HORSE LOCATION CHECK
     * ============================================================
     */

    private boolean isSafeForHorse(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return false;
        }


        Block feet =
                location.getBlock();

        Block head =
                location.clone()
                        .add(0, 1, 0)
                        .getBlock();

        Block ground =
                location.clone()
                        .add(0, -1, 0)
                        .getBlock();


        /*
         * Horse needs solid ground.
         */
        if (!ground.getType().isSolid()) {

            return false;
        }


        /*
         * Horse needs air/passable space.
         */
        if (!feet.isPassable()) {

            return false;
        }

        if (!head.isPassable()) {

            return false;
        }


        /*
         * Avoid dangerous blocks.
         */
        Material groundType =
                ground.getType();

        if (groundType == Material.LAVA
                || groundType == Material.MAGMA_BLOCK
                || groundType == Material.CAMPFIRE
                || groundType == Material.SOUL_CAMPFIRE) {

            return false;
        }


        return true;
    }


    /*
     * ============================================================
     * TELEPORT EFFECT
     * ============================================================
     */

    private void playTeleportEffect(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }


        World world =
                location.getWorld();


        Location center =
                location.clone()
                        .add(
                                0,
                                1.0,
                                0
                        );


        /*
         * Magical burst.
         */
        world.spawnParticle(
                Particle.END_ROD,
                center,
                45,
                0.8,
                1.0,
                0.8,
                0.08
        );


        world.spawnParticle(
                Particle.ENCHANT,
                center,
                80,
                1.0,
                1.2,
                1.0,
                0.4
        );


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                center,
                30,
                0.7,
                0.9,
                0.7,
                0.15
        );


        /*
         * Ground ring.
         */
        int points = 32;

        double radius = 1.5;


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


            world.spawnParticle(
                    Particle.END_ROD,
                    location.clone()
                            .add(
                                    x,
                                    0.15,
                                    z
                            ),
                    1,
                    0,
                    0,
                    0,
                    0
            );
        }


        world.playSound(
                center,
                Sound.BLOCK_PORTAL_AMBIENT,
                0.7f,
                1.4f
        );
    }


    /*
     * ============================================================
     * WHISTLE EFFECT
     * ============================================================
     */

    private void playWhistleEffect(
            Player player
    ) {

        World world =
                player.getWorld();


        Location location =
                player.getLocation()
                        .clone()
                        .add(
                                0,
                                1.0,
                                0
                        );


        /*
         * Whistle sound.
         */
        world.playSound(
                location,
                Sound.ITEM_GOAT_HORN_SOUND_0,
                2.0f,
                0.75f
        );


        /*
         * Magical sound.
         */
        world.playSound(
                location,
                Sound.BLOCK_BEACON_ACTIVATE,
                0.55f,
                1.65f
        );


        /*
         * Particles.
         */
        world.spawnParticle(
                Particle.END_ROD,
                location,
                18,
                0.35,
                0.45,
                0.35,
                0.05
        );


        world.spawnParticle(
                Particle.ENCHANT,
                location,
                35,
                0.55,
                0.65,
                0.55,
                0.25
        );


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                location,
                12,
                0.45,
                0.35,
                0.45,
                0.08
        );


        /*
         * Ring.
         */
        int points = 24;

        double radius = 0.8;


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


            world.spawnParticle(
                    Particle.END_ROD,
                    location.clone()
                            .add(
                                    x,
                                    -0.75,
                                    z
                            ),
                    1,
                    0,
                    0,
                    0,
                    0
            );
        }


        player.sendActionBar(
                Component.text(
                        "✦ Equinox Whistle",
                        NamedTextColor.LIGHT_PURPLE
                )
        );
    }


    /*
     * ============================================================
     * CLEANUP
     * ============================================================
     *
     * Call this from your plugin's onDisable().
     */

    public void shutdown() {

        for (BukkitTask task
                : activeRunTasks.values()) {

            if (task != null) {
                task.cancel();
            }
        }


        activeRunTasks.clear();

        whistleCooldowns.clear();
    }
}
