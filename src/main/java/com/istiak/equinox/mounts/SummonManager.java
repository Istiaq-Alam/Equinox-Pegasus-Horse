package com.istiak.equinox.mounts;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;


public final class SummonManager {

    private final EquinoxPlugin plugin;


    public SummonManager(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ========================================================
     * FIND REAL MOUNT
     *
     * IMPORTANT:
     *
     * This method NEVER spawns a horse.
     *
     * It only:
     *
     * 1. Checks already loaded horse.
     * 2. Gets the last known location.
     * 3. Loads that chunk.
     * 4. Searches the chunk entities.
     * 5. Returns the REAL horse with the stored UUID.
     * ========================================================
     */

    public void findRealMount(
            Player player,
            Consumer<Horse> callback
    ) {

        if (player == null
                || !player.isOnline()) {

            callback.accept(null);

            return;
        }


        MountManager mountManager =
                plugin.getMountManager();


        MountData mountData =
                mountManager.getMount(player);


        if (mountData == null) {

            callback.accept(null);

            return;
        }


        /*
         * ====================================================
         * FIRST CHECK:
         *
         * Horse may already be loaded.
         * ====================================================
         */

        Horse loadedHorse =
                mountManager.getLoadedMount(player);


        if (loadedHorse != null
                && loadedHorse.isValid()
                && !loadedHorse.isDead()) {

            mountManager.updateLastKnownLocation(
                    loadedHorse
            );

            callback.accept(loadedHorse);

            return;
        }


        /*
         * ====================================================
         * GET LAST KNOWN LOCATION
         * ====================================================
         */

        Location lastLocation =
                mountData.getLastKnownLocation();


        if (lastLocation == null
                || lastLocation.getWorld() == null) {

            callback.accept(null);

            return;
        }


        World world =
                lastLocation.getWorld();


        int chunkX =
                lastLocation.getBlockX() >> 4;

        int chunkZ =
                lastLocation.getBlockZ() >> 4;


        /*
         * ====================================================
         * LOAD THE EXACT CHUNK ASYNCHRONOUSLY
         *
         * false = NEVER generate new terrain.
         * ====================================================
         */

        world.getChunkAtAsync(
                chunkX,
                chunkZ,
                false,
                chunk -> {

                    /*
                     * Run one tick later.
                     *
                     * This gives the server time to finish
                     * attaching/loading entities.
                     */

                    Bukkit.getScheduler()
                            .runTaskLater(
                                    plugin,
                                    () -> findHorseInChunk(
                                            mountData.getHorseId(),
                                            chunk,
                                            callback,
                                            0
                                    ),
                                    1L
                            );

                }
        );
    }


    /*
     * ========================================================
     * FIND HORSE IN LOADED CHUNK
     * ========================================================
     */

    private void findHorseInChunk(
            UUID horseId,
            Chunk chunk,
            Consumer<Horse> callback,
            int attempt
    ) {

        if (chunk == null
                || !chunk.isLoaded()) {

            callback.accept(null);

            return;
        }


        /*
         * Search entities directly inside this chunk.
         */

        for (Entity entity
                : chunk.getEntities()) {

            if (!(entity instanceof Horse horse)) {
                continue;
            }


            if (!horse.getUniqueId()
                    .equals(horseId)) {

                continue;
            }


            if (!horse.isValid()
                    || horse.isDead()) {

                callback.accept(null);

                return;
            }


            /*
             * REAL HORSE FOUND.
             */

            plugin.getMountManager()
                    .updateLastKnownLocation(
                            horse
                    );


            callback.accept(horse);

            return;
        }


        /*
         * ====================================================
         * ENTITY LOADING RETRY
         *
         * Sometimes the chunk terrain loads before its entities
         * are fully available.
         *
         * Retry for up to 10 ticks.
         * ====================================================
         */

        if (attempt < 10) {

            Bukkit.getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> findHorseInChunk(
                                    horseId,
                                    chunk,
                                    callback,
                                    attempt + 1
                            ),
                            1L
                    );

            return;
        }


        callback.accept(null);
    }


    /*
     * ========================================================
     * COMPATIBILITY METHOD
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
