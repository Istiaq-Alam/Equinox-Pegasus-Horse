package com.istiak.equinox.listeners;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;


/**
 * ============================================================
 * EQUINOX MOUNT LOCATION LISTENER
 * ============================================================
 *
 * Tracks the REAL Equinox horse.
 *
 * IMPORTANT:
 *
 * Chunk unload / teleport only update LAST KNOWN LOCATION.
 *
 * They NEVER change the permanent HOME location.
 *
 * ============================================================
 */
public final class MountLocationListener
        implements Listener {


    private final EquinoxPlugin plugin;


    public MountLocationListener(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ========================================================
     * CHUNK UNLOAD
     * ========================================================
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onChunkUnload(
            ChunkUnloadEvent event
    ) {

        Chunk chunk =
                event.getChunk();


        for (Entity entity : chunk.getEntities()) {

            if (!(entity instanceof Horse horse)) {

                continue;
            }


            if (!plugin.getMountManager()
                    .isRegisteredMount(horse)) {

                continue;
            }


            /*
             * IMPORTANT:
             *
             * ONLY update LAST KNOWN.
             *
             * NEVER update HOME.
             */

            plugin.getMountManager()
                    .updateLastKnownLocation(
                            horse
                    );
        }
    }


    /*
     * ========================================================
     * ENTITY TELEPORT
     * ========================================================
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onHorseTeleport(
            EntityTeleportEvent event
    ) {

        if (!(event.getEntity()
                instanceof Horse horse)) {

            return;
        }


        if (!plugin.getMountManager()
                .isRegisteredMount(horse)) {

            return;
        }


        /*
         * Wait one tick for the teleport to complete.
         */

        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!horse.isValid()
                                    || horse.isDead()) {

                                return;
                            }


                            /*
                             * ONLY LAST KNOWN.
                             */

                            plugin.getMountManager()
                                    .updateLastKnownLocation(
                                            horse
                                    );
                        }
                );
    }
}
