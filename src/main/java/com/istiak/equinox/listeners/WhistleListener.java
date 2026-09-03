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
import org.bukkit.block.BlockFace;
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

public final class WhistleListener implements Listener {

    private static final double RETURN_RADIUS = 20.0;

    private static final double RUN_RADIUS = 100.0;

    private static final double ARRIVAL_DISTANCE = 3.5;

    private static final double RUN_SPEED = 0.42;

    private static final long TELEPORT_DELAY_TICKS = 12L;

    private static final int MAX_CHUNK_LOAD_ATTEMPTS = 40;

    private static final long WHISTLE_COOLDOWN_MS = 750L;


    private final EquinoxPlugin plugin;

    private final WhistleManager whistleManager;


    private final Map<UUID, BukkitTask> activeRunTasks =
            new HashMap<>();


    private final Map<UUID, Long> whistleCooldowns =
            new HashMap<>();


    public WhistleListener(
            EquinoxPlugin plugin,
            WhistleManager whistleManager
    ) {

        this.plugin = plugin;
        this.whistleManager = whistleManager;
    }


    /*
     * ========================================================
     * WHISTLE EVENT
     * ========================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onWhistleUse(
            PlayerInteractEvent event
    ) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }


        Action action =
                event.getAction();


        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {

            return;
        }


        Player player =
                event.getPlayer();


        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();


        if (!whistleManager.isWhistle(item)) {
            return;
        }


        event.setCancelled(true);

        event.setUseItemInHand(
                org.bukkit.event.Event.Result.DENY
        );

        event.setUseInteractedBlock(
                org.bukkit.event.Event.Result.DENY
        );


        if (!canUseWhistle(player)) {

            player.sendActionBar(
                    Component.text(
                            "The whistle needs a moment...",
                            NamedTextColor.GRAY
                    )
            );

            return;
        }


        cancelRunTask(
                player.getUniqueId()
        );


        playWhistleEffect(player);


        handleWhistle(player);
    }


    /*
     * ========================================================
     * COOLDOWN
     * ========================================================
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
     * ========================================================
     * MAIN WHISTLE LOGIC
     * ========================================================
     */

    private void handleWhistle(
            Player player
    ) {

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


        Horse horse =
                plugin.getMountManager()
                        .getLoadedMount(player);


        /*
         * ====================================================
         * HORSE NOT CURRENTLY LOADED
         *
         * Search LAST KNOWN LOCATION FIRST.
         * ====================================================
         */

        if (horse == null) {

            summonFromUnloadedChunk(player);

            return;
        }


        handleLoadedHorse(
                player,
                horse,
                mountData
        );
    }



/*
 * ========================================================
 * FIND REAL HORSE FROM UNLOADED CHUNK
 * ========================================================
 */

private void summonFromUnloadedChunk(
        Player player
) {

    UUID playerId =
            player.getUniqueId();


    player.sendActionBar(
            Component.text(
                    "Locating your Equinox mount...",
                    NamedTextColor.LIGHT_PURPLE
            )
    );


    /*
     * Cancel any old recovery task.
     */

    cancelRunTask(
            playerId
    );


    BukkitTask task =
            plugin.getServer()
                    .getScheduler()
                    .runTaskTimer(
                            plugin,
                            new Runnable() {

                                private int attempts = 0;


                                @Override
                                public void run() {

                                    if (!player.isOnline()) {

                                        cancelRunTask(
                                                playerId
                                        );

                                        return;
                                    }


                                    attempts++;


                                    /*
                                     * ====================================================
                                     * TRY TO FIND THE REAL HORSE
                                     * ====================================================
                                     */

                                    Horse horse =
                                            plugin.getMountManager()
                                                    .findAndLoadRealMount(
                                                            player
                                                    );


                                    if (horse != null
                                            && horse.isValid()
                                            && !horse.isDead()) {

                                        cancelRunTask(
                                                playerId
                                        );


                                        MountData mountData =
                                                plugin.getMountManager()
                                                        .getMount(
                                                                player
                                                        );


                                        if (mountData == null) {

                                            return;
                                        }


                                        handleLoadedHorse(
                                                player,
                                                horse,
                                                mountData
                                        );

                                        return;
                                    }


                                    /*
                                     * Try again for a while.
                                     */

                                    if (attempts >= 80) {

                                        cancelRunTask(
                                                playerId
                                        );


                                        player.sendActionBar(
                                                Component.text(
                                                        "Your real Equinox mount could not be found.",
                                                        NamedTextColor.RED
                                                )
                                        );
                                    }
                                }
                            },
                            0L,
                            2L
                    );


    activeRunTasks.put(
            playerId,
            task
    );
}


    /*
     * ========================================================
     * HANDLE LOADED HORSE
     * ========================================================
     */

    private void handleLoadedHorse(
            Player player,
            Horse horse,
            MountData mountData
    ) {

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
         * Riding the horse + whistle
         * = return horse home.
         */

        if (horse.getPassengers()
                .contains(player)) {

            returnMountHome(
                    player,
                    horse,
                    mountData
            );

            return;
        }


        double distance;


        if (!horse.getWorld()
                .equals(player.getWorld())) {

            distance = Double.MAX_VALUE;

        } else {

            distance =
                    horse.getLocation()
                            .distance(
                                    player.getLocation()
                            );
        }


        /*
         * Close horse = send home.
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
         * Medium distance = run normally.
         */

        if (distance <= RUN_RADIUS) {

            runMountToPlayer(
                    player,
                    horse
            );

            return;
        }


        /*
         * Far away = teleport the REAL horse.
         */

        teleportMountToPlayer(
                player,
                horse
        );
    }


    /*
 * ========================================================
 * RUN TO PLAYER
 * ========================================================
 */

private void runMountToPlayer(
        Player player,
        Horse horse
) {

    UUID playerId =
            player.getUniqueId();


    cancelRunTask(
            playerId
    );


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

                                    if (!player.isOnline()
                                            || !horse.isValid()
                                            || horse.isDead()) {

                                        cancelRunTask(
                                                playerId
                                        );

                                        return;
                                    }


                                    if (horse.isLeashed()) {

                                        cancelRunTask(
                                                playerId
                                        );

                                        return;
                                    }


                                    if (!horse.getWorld()
                                            .equals(
                                                    player.getWorld()
                                            )) {

                                        cancelRunTask(
                                                playerId
                                        );

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
                                     * ====================================================
                                     * ARRIVED
                                     * ====================================================
                                     */

                                    if (distance
                                            <= ARRIVAL_DISTANCE) {

                                        Vector velocity =
                                                horse.getVelocity();


                                        horse.setVelocity(
                                                new Vector(
                                                        0,
                                                        velocity.getY(),
                                                        0
                                                )
                                        );


                                        horse.setJumping(
                                                false
                                        );


                                        player.sendActionBar(
                                                Component.text(
                                                        "✦ Your Equinox mount has arrived.",
                                                        NamedTextColor.GREEN
                                                )
                                        );


                                        plugin.getMountManager()
                                                .updateLastKnownLocation(
                                                        horse
                                                );


                                        cancelRunTask(
                                                playerId
                                        );

                                        return;
                                    }


                                    /*
                                     * ====================================================
                                     * DIRECTION
                                     * ====================================================
                                     */

                                    Vector direction =
                                            playerLocation.toVector()
                                                    .subtract(
                                                            horseLocation.toVector()
                                                    );


                                    direction.setY(0);


                                    if (direction.lengthSquared()
                                            < 0.0001) {

                                        return;
                                    }


                                    direction.normalize();


                                    /*
                                     * ====================================================
                                     * FACE MOVEMENT DIRECTION
                                     * ====================================================
                                     */

                                    float yaw =
                                            (float) Math.toDegrees(
                                                    Math.atan2(
                                                            -direction.getX(),
                                                            direction.getZ()
                                                    )
                                            );


                                    horse.setRotation(
                                            yaw,
                                            0.0f
                                    );


                                    /*
                                     * ====================================================
                                     * JUMP DETECTION
                                     * ====================================================
                                     *
                                     * The horse jumps when:
                                     *
                                     * 1. There is a solid block directly ahead,
                                     *    OR
                                     *
                                     * 2. The player is significantly higher.
                                     *
                                     * The block above the obstacle must be passable.
                                     * ====================================================
                                     */

                                    boolean shouldJump =
                                            shouldHorseJump(
                                                    horse,
                                                    direction,
                                                    playerLocation
                                            );


                                    if (shouldJump) {

                                        horse.setJumping(
                                                true
                                        );


                                        Vector currentVelocity =
                                                horse.getVelocity();


                                        /*
                                         * Give the horse enough vertical
                                         * velocity to clear a one-block
                                         * obstacle.
                                         */

                                        double jumpVelocity =
                                                Math.max(
                                                        0.42D,
                                                        horse.getJumpStrength()
                                                                * 0.72D
                                                );


                                        if (currentVelocity.getY()
                                                < jumpVelocity) {

                                            currentVelocity =
                                                    currentVelocity.clone();


                                            currentVelocity.setY(
                                                    jumpVelocity
                                            );


                                            horse.setVelocity(
                                                    currentVelocity
                                            );
                                        }

                                    } else {

                                        /*
                                         * Don't force jumping continuously.
                                         */

                                        if (horse.isOnGround()) {

                                            horse.setJumping(
                                                    false
                                            );
                                        }
                                    }


                                    /*
                                     * ====================================================
                                     * HORIZONTAL MOVEMENT
                                     * ====================================================
                                     */

                                    Vector velocity =
                                            direction.clone()
                                                    .multiply(
                                                            RUN_SPEED
                                                    );


                                    /*
                                     * Preserve current vertical velocity.
                                     */

                                    velocity.setY(
                                            horse.getVelocity()
                                                    .getY()
                                    );


                                    horse.setVelocity(
                                            velocity
                                    );


                                    ticks++;


                                    /*
                                     * ====================================================
                                     * TIMEOUT
                                     * ====================================================
                                     */

                                    if (ticks >= 600) {

                                        cancelRunTask(
                                                playerId
                                        );


                                        plugin.getMountManager()
                                                .updateLastKnownLocation(
                                                        horse
                                                );


                                        player.sendActionBar(
                                                Component.text(
                                                        "Your Equinox mount could not reach you.",
                                                        NamedTextColor.YELLOW
                                                )
                                        );
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
     * ========================================================
     * TELEPORT REAL HORSE TO PLAYER
     * ========================================================
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


        playTeleportEffect(oldLocation);


        horse.setVelocity(
                new Vector(0, 0, 0)
        );


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()
                                    || !horse.isValid()
                                    || horse.isDead()
                                    || horse.isLeashed()) {

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


                            plugin.getMountManager()
                                    .updateLastKnownLocation(
                                            horse
                                    );


                            playTeleportEffect(destination);


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
     * ========================================================
     * RETURN HOME
     * ========================================================
     */

    private void returnMountHome(
            Player player,
            Horse horse,
            MountData mountData
    ) {

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
         * IMPORTANT:
         *
         * This is the PERMANENT BIND LOCATION.
         *
         * It is NEVER replaced with last-known location.
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


        cancelRunTask(
                player.getUniqueId()
        );


        player.sendActionBar(
                Component.text(
                        "Sending your Equinox mount home...",
                        NamedTextColor.LIGHT_PURPLE
                )
        );


        horse.eject();


        horse.setVelocity(
                new Vector(0, 0, 0)
        );


        Location oldLocation =
                horse.getLocation().clone();


        playTeleportEffect(oldLocation);


        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!horse.isValid()
                                    || horse.isDead()
                                    || horse.isLeashed()) {

                                return;
                            }


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


                            /*
                             * Update LAST KNOWN only.
                             *
                             * DO NOT update permanent HOME.
                             */

                            plugin.getMountManager()
                                    .updateLastKnownLocation(
                                            horse
                                    );


                            playTeleportEffect(
                                    safeHome
                            );


                            player.sendActionBar(
                                    Component.text(
                                            "✦ Your Equinox mount returned home.",
                                            NamedTextColor.GREEN
                                    )
                            );

                        },
                        TELEPORT_DELAY_TICKS
                );
    }


    /*
     * ========================================================
     * SAFE LOCATION NEAR PLAYER
     * ========================================================
     */

    private Location findSafeLocationNearPlayer(
            Player player
    ) {

        Location base =
                player.getLocation();

        World world =
                player.getWorld();


        for (int attempt = 0;
             attempt < 20;
             attempt++) {

            double angle =
                    ThreadLocalRandom.current()
                            .nextDouble(
                                    Math.PI * 2.0
                            );


            double radius =
                    3.0
                            + ThreadLocalRandom.current()
                            .nextDouble(4.0);


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


        return null;
    }


    /*
     * ========================================================
     * SAFE HOME LOCATION
     * ========================================================
     */

    private Location findSafeLocation(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return null;
        }


        if (isSafeForHorse(location)) {

            return location.clone();
        }


        World world =
                location.getWorld();


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
                                    blockZ + 0.5
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
     * ========================================================
     * SAFE CHECK
     * ========================================================
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


        if (!ground.getType().isSolid()) {

            return false;
        }


        if (!feet.isPassable()) {

            return false;
        }


        if (!head.isPassable()) {

            return false;
        }


        Material groundType =
                ground.getType();


        return groundType != Material.LAVA
                && groundType != Material.MAGMA_BLOCK
                && groundType != Material.CAMPFIRE
                && groundType != Material.SOUL_CAMPFIRE;
    }


    /*
     * ========================================================
     * RUNNING / JUMP HELPERS
     * ========================================================
     */

    private boolean hasOneBlockObstacleAhead(
            Horse horse,
            Vector direction
    ) {

        Location base = horse.getLocation();

        Vector step = direction.clone()
                .setY(0)
                .normalize()
                .multiply(0.9);

        Location feetLocation = base.clone()
                .add(step)
                .add(0, 0.15, 0);

        Block obstacle = feetLocation.getBlock();
        Block aboveObstacle = obstacle.getRelative(BlockFace.UP);

        /*
         * A one-block obstacle must be solid, with free headroom
         * above it. This prevents jumping into walls or ceilings.
         */
        return obstacle.getType().isSolid()
                && aboveObstacle.isPassable();
    }


    private void releaseRecoveryTickets(
            Player player
    ) {

        MountData mountData =
                plugin.getMountManager().getMount(player);

        if (mountData == null) {
            return;
        }

        plugin.getMountManager()
                .releaseMountRecoveryArea(
                        mountData.getLastKnownLocation()
                );

        plugin.getMountManager()
                .releaseMountRecoveryArea(
                        mountData.getHomeLocation()
                );
    }


    /*
     * ========================================================
     * CANCEL TASK
     * ========================================================
     */

    private void cancelRunTask(
            UUID playerId
    ) {

        BukkitTask task =
                activeRunTasks.remove(playerId);


        if (task != null) {

            task.cancel();
        }
    }


    /*
     * ========================================================
     * TELEPORT EFFECT
     * ========================================================
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
                        .add(0, 1, 0);


        world.spawnParticle(
                Particle.END_ROD,
                center,
                40,
                0.8,
                1.0,
                0.8,
                0.08
        );


        world.spawnParticle(
                Particle.ENCHANT,
                center,
                60,
                1.0,
                1.0,
                1.0,
                0.3
        );


        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                center,
                25,
                0.6,
                0.8,
                0.6,
                0.1
        );


        world.playSound(
                center,
                Sound.BLOCK_PORTAL_AMBIENT,
                0.7f,
                1.4f
        );
    }


    /*
     * ========================================================
     * WHISTLE EFFECT
     * ========================================================
     */

    private void playWhistleEffect(
            Player player
    ) {

        World world =
                player.getWorld();


        Location location =
                player.getLocation()
                        .clone()
                        .add(0, 1, 0);


        world.playSound(
                location,
                Sound.ITEM_GOAT_HORN_SOUND_0,
                2.0f,
                0.75f
        );


        world.playSound(
                location,
                Sound.BLOCK_BEACON_ACTIVATE,
                0.55f,
                1.65f
        );


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


        player.sendActionBar(
                Component.text(
                        "✦ Equinox Whistle",
                        NamedTextColor.LIGHT_PURPLE
                )
        );
    }


    /*
     * ========================================================
     * SHUTDOWN
     * ========================================================
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

    /*
 * ========================================================
 * HORSE JUMP DECISION
 * ========================================================
 *
 * Determines whether the horse needs to jump because the
 * path toward the player is blocked by a 1-block-high
 * obstacle.
 *
 * This is specifically for the whistle-follow system.
 * It allows the horse to climb normal 1-block terrain
 * instead of getting stuck against the block.
 * ========================================================
 */
private boolean shouldHorseJump(
        Horse horse,
        Vector direction,
        Location target
) {

    if (horse == null
            || !horse.isValid()
            || horse.isDead()
            || direction == null
            || target == null) {

        return false;
    }


    Location horseLocation =
            horse.getLocation();


    World world =
            horseLocation.getWorld();


    if (world == null
            || target.getWorld() == null
            || !world.equals(target.getWorld())) {

        return false;
    }


    /*
     * Horizontal movement direction.
     */
    Vector horizontal =
            direction.clone();

    horizontal.setY(0);


    if (horizontal.lengthSquared() < 0.0001) {

        return false;
    }


    horizontal.normalize();


    /*
     * Check approximately one block in front of
     * the horse.
     */
    Location front =
            horseLocation.clone()
                    .add(
                            horizontal.getX() * 0.9,
                            0,
                            horizontal.getZ() * 0.9
                    );


    /*
     * Block at the horse's feet.
     */
    Block frontFeet =
            front.getBlock();


    /*
     * Block at the horse's head.
     */
    Block frontHead =
            front.clone()
                    .add(0, 1, 0)
                    .getBlock();


    /*
     * Block above the obstacle.
     */
    Block frontAbove =
            front.clone()
                    .add(0, 2, 0)
                    .getBlock();


    /*
     * No obstacle in front.
     */
    if (frontFeet.isPassable()) {

        return false;
    }


    /*
     * There is a solid block at foot level.
     *
     * For a normal 1-block obstacle, the horse needs:
     *
     *   feet       -> obstacle
     *   head       -> free
     *   above      -> free
     *
     * This allows the horse to jump over it.
     */
    if (!frontHead.isPassable()) {

        return false;
    }


    if (!frontAbove.isPassable()) {

        return false;
    }


    /*
     * Make sure the obstacle is not an unsafe block.
     */
    Material obstacleType =
            frontFeet.getType();


    if (obstacleType == Material.LAVA
            || obstacleType == Material.WATER
            || obstacleType == Material.CACTUS
            || obstacleType == Material.MAGMA_BLOCK
            || obstacleType == Material.CAMPFIRE
            || obstacleType == Material.SOUL_CAMPFIRE) {

        return false;
    }


    /*
     * Check that there is actually enough room to land
     * on the other side of the obstacle.
     */
    Location landing =
            front.clone()
                    .add(
                            horizontal.getX() * 1.1,
                            1.0,
                            horizontal.getZ() * 1.1
                    );


    Block landingFeet =
            landing.getBlock();


    Block landingHead =
            landing.clone()
                    .add(0, 1, 0)
                    .getBlock();


    Block landingGround =
            landing.clone()
                    .add(0, -1, 0)
                    .getBlock();


    /*
     * We need:
     *
     * - solid ground
     * - free feet space
     * - free head space
     */
    if (!landingGround.getType().isSolid()) {

        return false;
    }


    if (!landingFeet.isPassable()) {

        return false;
    }


    if (!landingHead.isPassable()) {

        return false;
    }


    return true;
}

}
