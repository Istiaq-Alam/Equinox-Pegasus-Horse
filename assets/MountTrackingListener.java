package com.istiak.equinox.listeners;

import com.istiak.equinox.EquinoxPlugin;
import com.istiak.equinox.mounts.MountManager;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class MountTrackingListener
        implements Listener {

    private final EquinoxPlugin plugin;


    public MountTrackingListener(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ========================================================
     * CHUNK UNLOAD
     *
     * Before Minecraft unloads the chunk, save the EXACT
     * current location of every registered Equinox horse.
     * ========================================================
     */

    @EventHandler
    public void onChunkUnload(
            ChunkUnloadEvent event
    ) {

        Chunk chunk =
                event.getChunk();

        MountManager mountManager =
                plugin.getMountManager();


        for (Entity entity
                : chunk.getEntities()) {

            if (!(entity instanceof Horse horse)) {
                continue;
            }


            if (!mountManager.isRegisteredMount(horse)) {
                continue;
            }


            /*
             * Save exact location before unloading.
             */

            mountManager.updateLastKnownLocation(
                    horse
            );
        }
    }
}
