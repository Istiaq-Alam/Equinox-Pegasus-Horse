package com.istiak.equinox.mounts;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * ============================================================
 * EQUINOX MOUNT MANAGER
 * ============================================================
 *
 * IMPORTANT DESIGN:
 *
 * HOME LOCATION
 * = Permanent location where the horse was bound.
 *
 * LAST KNOWN LOCATION
 * = Last tracked location of the REAL horse.
 *
 * The home location is NEVER automatically changed.
 *
 * The last known location is used to locate the real horse
 * when its chunk is unloaded.
 *
 * THIS SYSTEM NEVER CREATES A REPLACEMENT HORSE.
 * THIS SYSTEM NEVER DUPLICATES A HORSE.
 *
 * ============================================================
 */
public final class MountManager {

    private final EquinoxPlugin plugin;


    /*
     * ========================================================
     * MOUNT MAPS
     * ========================================================
     */

    /*
     * Player UUID -> MountData
     */
    private final Map<UUID, MountData> playerMounts =
            new HashMap<>();


    /*
     * Horse UUID -> Player UUID
     */
    private final Map<UUID, UUID> horseOwners =
            new HashMap<>();


    /*
     * ========================================================
     * PERSISTENT DATA KEYS
     * ========================================================
     */

    private final NamespacedKey ownerKey;

    private final NamespacedKey registeredKey;


    /*
     * ========================================================
     * FILE STORAGE
     * ========================================================
     */

    private File mountsFile;

    private FileConfiguration mountsConfig;


    /*
     * ========================================================
     * CONSTRUCTOR
     * ========================================================
     */

    public MountManager(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;


        this.ownerKey =
                new NamespacedKey(
                        plugin,
                        "mount_owner"
                );


        this.registeredKey =
                new NamespacedKey(
                        plugin,
                        "registered_mount"
                );


        setupStorage();

        loadMounts();
    }


    /*
     * ========================================================
     * STORAGE SETUP
     * ========================================================
     */

    private void setupStorage() {

        if (!plugin.getDataFolder().exists()) {

            plugin.getDataFolder().mkdirs();
        }


        mountsFile =
                new File(
                        plugin.getDataFolder(),
                        "mounts.yml"
                );


        if (!mountsFile.exists()) {

            try {

                mountsFile.createNewFile();

            } catch (IOException exception) {

                plugin.getLogger().severe(
                        "Could not create mounts.yml!"
                );

                exception.printStackTrace();
            }
        }


        mountsConfig =
                YamlConfiguration.loadConfiguration(
                        mountsFile
                );
    }


    /*
     * ========================================================
     * REGISTER MOUNT
     * ========================================================
     */

    public boolean registerMount(
            Player player,
            Horse horse
    ) {

        if (player == null || horse == null) {

            return false;
        }


        if (!horse.isTamed()) {

            return false;
        }


        if (horse.getOwner() == null
                || !horse.getOwner()
                .getUniqueId()
                .equals(player.getUniqueId())) {

            return false;
        }


        /*
         * Horse must have Equinox armor.
         */

        ItemStack armor =
                horse.getInventory().getArmor();


        if (!plugin.getHorseArmorManager()
                .isEquinoxArmor(armor)) {

            return false;
        }


        UUID playerId =
                player.getUniqueId();

        UUID horseId =
                horse.getUniqueId();


        /*
         * Preserve original registration time if replacing
         * an existing registration.
         */

        MountData oldMount =
                playerMounts.get(playerId);


        if (oldMount != null) {

            horseOwners.remove(
                    oldMount.getHorseId()
            );
        }


        long registeredAt =
                oldMount != null
                        ? oldMount.getRegisteredAt()
                        : System.currentTimeMillis();


        MountData mountData =
                createMountData(
                        player,
                        horse,
                        registeredAt
                );


        playerMounts.put(
                playerId,
                mountData
        );


        horseOwners.put(
                horseId,
                playerId
        );


        applyMountPdc(
                playerId,
                horse
        );


        saveMount(
                mountData
        );


        return true;
    }


    /*
     * ========================================================
     * CREATE MOUNT DATA
     * ========================================================
     */

    private MountData createMountData(
            Player player,
            Horse horse,
            long registeredAt
    ) {

        /*
         * IMPORTANT:
         *
         * Home location is permanently the bind location.
         */

        Location homeLocation =
                horse.getLocation().clone();


        /*
         * Initially last known location is the same location.
         */

        Location lastKnownLocation =
                horse.getLocation().clone();


        double maxHealth =
                getAttributeValue(
                        horse,
                        Attribute.MAX_HEALTH,
                        20.0
                );


        double health =
                Math.min(
                        horse.getHealth(),
                        maxHealth
                );


        double movementSpeed =
                getAttributeValue(
                        horse,
                        Attribute.MOVEMENT_SPEED,
                        0.225
                );


        double jumpStrength =
                horse.getJumpStrength();


        return new MountData(
                player.getUniqueId(),
                horse.getUniqueId(),
                getHorseName(horse),

                /*
                 * Permanent bind location.
                 */
                homeLocation,

                /*
                 * Current / last known location.
                 */
                lastKnownLocation,

                horse.getColor(),
                horse.getStyle(),

                maxHealth,
                health,
                movementSpeed,
                jumpStrength,

                horse.getInventory().getArmor(),

                registeredAt
        );
    }


    /*
     * ========================================================
     * UPDATE MOUNT DATA FROM REAL HORSE
     * ========================================================
     */

    public void updateMountFromHorse(
            Horse horse
    ) {

        if (horse == null
                || horse.isDead()
                || !horse.isValid()) {

            return;
        }


        UUID ownerId =
                horseOwners.get(
                        horse.getUniqueId()
                );


        if (ownerId == null) {

            return;
        }


        MountData data =
                playerMounts.get(ownerId);


        if (data == null) {

            return;
        }


        data.setHorseName(
                getHorseName(horse)
        );


        data.setColor(
                horse.getColor()
        );


        data.setStyle(
                horse.getStyle()
        );


        data.setMaxHealth(
                getAttributeValue(
                        horse,
                        Attribute.MAX_HEALTH,
                        20.0
                )
        );


        data.setHealth(
                Math.min(
                        horse.getHealth(),
                        data.getMaxHealth()
                )
        );


        data.setMovementSpeed(
                getAttributeValue(
                        horse,
                        Attribute.MOVEMENT_SPEED,
                        0.225
                )
        );


        data.setJumpStrength(
                horse.getJumpStrength()
        );


        ItemStack armor =
                horse.getInventory().getArmor();


        if (plugin.getHorseArmorManager()
                .isEquinoxArmor(armor)) {

            data.setArmor(
                    armor
            );
        }


        /*
         * Update ONLY last known location.
         *
         * NEVER change home location here.
         */

        Location location =
                horse.getLocation();


        if (location != null
                && location.getWorld() != null) {

            data.setLastKnownLocation(
                    location.clone()
            );
        }
    }


    /*
     * ========================================================
     * GETTERS
     * ========================================================
     */

    public MountData getMount(
            Player player
    ) {

        if (player == null) {

            return null;
        }


        return playerMounts.get(
                player.getUniqueId()
        );
    }


    public MountData getMount(
            UUID playerId
    ) {

        if (playerId == null) {

            return null;
        }


        return playerMounts.get(
                playerId
        );
    }


    public boolean hasMount(
            Player player
    ) {

        return player != null
                && playerMounts.containsKey(
                player.getUniqueId()
        );
    }


    public UUID getMountOwner(
            UUID horseId
    ) {

        return horseOwners.get(horseId);
    }


    public boolean isRegisteredMount(
            Horse horse
    ) {

        return horse != null
                && horseOwners.containsKey(
                horse.getUniqueId()
        );
    }


    public boolean isOwner(
            Player player,
            Horse horse
    ) {

        if (player == null || horse == null) {

            return false;
        }


        UUID ownerId =
                horseOwners.get(
                        horse.getUniqueId()
                );


        return ownerId != null
                && ownerId.equals(
                player.getUniqueId()
        );
    }


    /*
     * ========================================================
     * GET LOADED MOUNT
     * ========================================================
     */

    public Horse getLoadedMount(
            Player player
    ) {

        MountData mountData =
                getMount(player);


        if (mountData == null) {

            return null;
        }


        return getLoadedHorse(
                mountData.getHorseId()
        );
    }


    public Horse getLoadedHorse(
            UUID horseId
    ) {

        if (horseId == null) {

            return null;
        }


        for (World world : Bukkit.getWorlds()) {

            Entity entity =
                    world.getEntity(horseId);


            if (entity instanceof Horse horse
                    && horse.isValid()
                    && !horse.isDead()) {

                return horse;
            }
        }


        return null;
    }


    /*
     * ========================================================
     * LOAD A SINGLE CHUNK
     * ========================================================
     */

    private boolean loadChunkAtLocation(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        try {
            /*
             * Keep the chunk loaded while the whistle recovery
             * task is searching for the real horse entity.
             */
            world.addPluginChunkTicket(
                    chunkX,
                    chunkZ,
                    plugin
            );

            world.getChunkAt(
                    chunkX,
                    chunkZ,
                    false
            );

            return true;

        } catch (Exception exception) {
            return false;
        }
    }


    /**
     * Releases temporary whistle-recovery tickets around a location.
     */
    public void releaseMountRecoveryArea(
            Location location
    ) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.removePluginChunkTicket(
                        centerChunkX + x,
                        centerChunkZ + z,
                        plugin
                );
            }
        }
    }


    /*
     * ========================================================
     * LOAD AREA AROUND LOCATION
     * ========================================================
     *
     * Loads a 3x3 chunk area.
     *
     * This does NOT spawn anything.
     */

    private boolean loadAreaAroundLocation(
        Location location
) {

    if (location == null
            || location.getWorld() == null) {

        return false;
    }


    World world =
            location.getWorld();


    int centerChunkX =
            location.getBlockX() >> 4;


    int centerChunkZ =
            location.getBlockZ() >> 4;


    boolean loadedAnything =
            false;


    for (int x = -1; x <= 1; x++) {

        for (int z = -1; z <= 1; z++) {

            try {

                Chunk chunk =
                        world.getChunkAt(
                                centerChunkX + x,
                                centerChunkZ + z
                        );


                if (chunk != null) {

                    /*
                     * Keep this chunk loaded temporarily.
                     */

                    chunk.addPluginChunkTicket(
                            plugin
                    );


                    loadedAnything = true;
                }

            } catch (Exception exception) {

                plugin.getLogger().warning(
                        "Could not load mount chunk "
                                + (centerChunkX + x)
                                + ", "
                                + (centerChunkZ + z)
                );
            }
        }
    }


    return loadedAnything;
}


    /*
     * ========================================================
     * LOAD LAST KNOWN MOUNT AREA
     * ========================================================
     */

    public boolean loadMountLastKnownArea(
            Player player
    ) {

        if (player == null) {

            return false;
        }


        MountData mountData =
                getMount(player);


        if (mountData == null) {

            return false;
        }


        Location lastKnown =
                mountData.getLastKnownLocation();


        if (lastKnown == null) {

            return false;
        }


        return loadAreaAroundLocation(
                lastKnown
        );
    }


    /*
     * ========================================================
     * LOAD LAST KNOWN MOUNT CHUNK
     * ========================================================
     */

    public boolean loadMountLastKnownChunk(
            Player player
    ) {

        if (player == null) {

            return false;
        }


        MountData mountData =
                getMount(player);


        if (mountData == null) {

            return false;
        }


        return loadChunkAtLocation(
                mountData.getLastKnownLocation()
        );
    }


    /*
     * ========================================================
     * LOAD HOME CHUNK
     * ========================================================
     *
     * FALLBACK ONLY.
     *
     * Home is the permanent bind location.
     */

    public boolean loadMountHomeChunk(
            Player player
    ) {

        if (player == null) {

            return false;
        }


        MountData mountData =
                getMount(player);


        if (mountData == null) {

            return false;
        }


        return loadChunkAtLocation(
                mountData.getHomeLocation()
        );
    }


    /*
     * ========================================================
     * LOAD HOME AREA
     * ========================================================
     */

    public boolean loadMountHomeArea(
            Player player
    ) {

        if (player == null) {

            return false;
        }


        MountData mountData =
                getMount(player);


        if (mountData == null) {

            return false;
        }


        return loadAreaAroundLocation(
                mountData.getHomeLocation()
        );
    }


   /*
 * ========================================================
 * FIND AND LOAD THE REAL MOUNT
 * ========================================================
 *
 * SEARCH ORDER:
 *
 * 1. Already loaded horse.
 * 2. Last-known chunk.
 * 3. Exact UUID inside last-known chunk.
 * 4. Nearby last-known chunks.
 * 5. Permanent HOME as final recovery fallback.
 *
 * IMPORTANT:
 *
 * NEVER CREATE A NEW HORSE.
 *
 * We only recover the REAL registered horse.
 * ========================================================
 */

public Horse findAndLoadRealMount(
        Player player
) {

    if (player == null) {

        return null;
    }


    MountData mountData =
            getMount(player);


    if (mountData == null) {

        return null;
    }


    UUID horseId =
            mountData.getHorseId();


    /*
     * ====================================================
     * STEP 1
     * ====================================================
     *
     * Is the real horse already loaded?
     */

    Horse horse =
            getLoadedHorse(
                    horseId
            );


    if (horse != null) {

        return horse;
    }


    /*
     * ====================================================
     * STEP 2
     * ====================================================
     *
     * LAST KNOWN LOCATION.
     */

    Location lastKnown =
            mountData.getLastKnownLocation();


    if (lastKnown != null
            && lastKnown.getWorld() != null) {

        /*
         * Search the exact last-known chunk.
         */

        horse =
                findHorseInChunk(
                        lastKnown,
                        horseId
                );


        if (horse != null) {

            return horse;
        }


        /*
         * Search the surrounding 3x3 chunks.
         *
         * This handles cases where the horse moved slightly
         * after the last location was persisted.
         */

        World world =
                lastKnown.getWorld();


        int centerChunkX =
                lastKnown.getBlockX() >> 4;


        int centerChunkZ =
                lastKnown.getBlockZ() >> 4;


        for (int radius = 1;
             radius <= 2;
             radius++) {

            for (int x = -radius;
                 x <= radius;
                 x++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    Location chunkLocation =
                            new Location(
                                    world,
                                    (centerChunkX + x) * 16 + 8,
                                    lastKnown.getY(),
                                    (centerChunkZ + z) * 16 + 8
                            );


                    horse =
                            findHorseInChunk(
                                    chunkLocation,
                                    horseId
                            );


                    if (horse != null) {

                        return horse;
                    }
                }
            }
        }
    }


    /*
     * ====================================================
     * STEP 3
     * ====================================================
     *
     * Final fallback: permanent HOME.
     *
     * This does NOT modify HOME.
     */

    Location home =
            mountData.getHomeLocation();


    if (home != null
            && home.getWorld() != null) {

        horse =
                findHorseInChunk(
                        home,
                        horseId
                );


        if (horse != null) {

            return horse;
        }
    }


    return null;
}

/*
 * ========================================================
 * RELEASE MOUNT CHUNK TICKET
 * ========================================================
 */

public void releaseMountChunkTicket(
        Horse horse
) {

    if (horse == null
            || horse.getWorld() == null) {

        return;
    }


    Chunk chunk =
            horse.getChunk();


    try {

        chunk.removePluginChunkTicket(
                plugin
        );

    } catch (Exception ignored) {
    }
}


    /*
     * ========================================================
     * UPDATE LAST KNOWN LOCATION
     * ========================================================
     *
     * IMPORTANT:
     *
     * DOES NOT CHANGE HOME LOCATION.
     */

    public void updateLastKnownLocation(
            Horse horse
    ) {

        if (horse == null
                || horse.isDead()
                || !horse.isValid()) {

            return;
        }


        UUID ownerId =
                horseOwners.get(
                        horse.getUniqueId()
                );


        if (ownerId == null) {

            return;
        }


        MountData mountData =
                playerMounts.get(ownerId);


        if (mountData == null) {

            return;
        }


        Location location =
                horse.getLocation();


        if (location == null
                || location.getWorld() == null) {

            return;
        }


        /*
         * ONLY update last known location.
         */

        mountData.setLastKnownLocation(
                location.clone()
        );


        saveMount(
                mountData
        );
    }


    /*
     * ========================================================
     * RECORD MOUNT LOCATION
     * ========================================================
     *
     * updateHome = false
     *
     * Normal horse movement should always use false.
     *
     * updateHome = true
     *
     * Only use this if you intentionally want to move the
     * permanent bind location.
     */

    public void recordMountLocation(
            Horse horse,
            boolean updateHome
    ) {

        if (horse == null
                || horse.isDead()
                || !horse.isValid()) {

            return;
        }


        UUID ownerId =
                horseOwners.get(
                        horse.getUniqueId()
                );


        if (ownerId == null) {

            return;
        }


        MountData mountData =
                playerMounts.get(ownerId);


        if (mountData == null) {

            return;
        }


        Location location =
                horse.getLocation();


        if (location == null
                || location.getWorld() == null) {

            return;
        }


        /*
         * Always update last known location.
         */

        mountData.setLastKnownLocation(
                location.clone()
        );


        /*
         * Home is changed ONLY when explicitly requested.
         */

        if (updateHome) {

            mountData.setHomeLocation(
                    location.clone()
            );
        }


        saveMount(
                mountData
        );
    }


    /*
     * ========================================================
     * UNREGISTER
     * ========================================================
     */

    public boolean unregisterMount(
            UUID horseId
    ) {

        if (horseId == null) {

            return false;
        }


        UUID ownerId =
                horseOwners.remove(horseId);


        if (ownerId == null) {

            return false;
        }


        playerMounts.remove(
                ownerId
        );


        removeMountFromFile(
                ownerId
        );


        Horse horse =
                getLoadedHorse(horseId);


        if (horse != null) {

            PersistentDataContainer container =
                    horse.getPersistentDataContainer();


            container.remove(ownerKey);

            container.remove(registeredKey);
        }


        return true;
    }


    public boolean unregisterPlayerMount(
            Player player
    ) {

        if (player == null) {

            return false;
        }


        MountData mountData =
                getMount(player);


        if (mountData == null) {

            return false;
        }


        return unregisterMount(
                mountData.getHorseId()
        );
    }


    /*
     * ========================================================
     * APPLY PDC
     * ========================================================
     */

    private void applyMountPdc(
            UUID ownerId,
            Horse horse
    ) {

        PersistentDataContainer container =
                horse.getPersistentDataContainer();


        container.set(
                ownerKey,
                PersistentDataType.STRING,
                ownerId.toString()
        );


        container.set(
                registeredKey,
                PersistentDataType.BYTE,
                (byte) 1
        );
    }


    /*
     * ========================================================
     * SAVE ALL MOUNTS
     * ========================================================
     */

    public void saveAllMounts() {

        /*
         * Update all currently loaded real horses.
         */

        for (MountData mountData
                : playerMounts.values()) {

            Horse horse =
                    getLoadedHorse(
                            mountData.getHorseId()
                    );


            if (horse != null) {

                updateMountFromHorse(
                        horse
                );
            }
        }


        /*
         * Clear old mount section.
         */

        mountsConfig.set(
                "mounts",
                null
        );


        /*
         * Save all current data.
         */

        for (MountData mountData
                : playerMounts.values()) {

            saveMountToConfig(
                    mountData
            );
        }


        saveFile();
    }


    private void saveMount(
            MountData mountData
    ) {

        if (mountData == null) {

            return;
        }


        saveMountToConfig(
                mountData
        );


        saveFile();
    }


    /*
     * ========================================================
     * SAVE MOUNT TO CONFIG
     * ========================================================
     */

    private void saveMountToConfig(
            MountData mountData
    ) {

        String path =
                "mounts."
                        + mountData.getOwnerId();


        /*
         * BASIC
         */

        mountsConfig.set(
                path + ".owner",
                mountData.getOwnerId().toString()
        );


        mountsConfig.set(
                path + ".horse",
                mountData.getHorseId().toString()
        );


        mountsConfig.set(
                path + ".horse-name",
                mountData.getHorseName()
        );


        mountsConfig.set(
                path + ".registered-at",
                mountData.getRegisteredAt()
        );


        /*
         * ====================================================
         * PERMANENT HOME / BIND LOCATION
         * ====================================================
         */

        if (mountData.getHomeWorldId() != null) {

            mountsConfig.set(
                    path + ".home.world",
                    mountData.getHomeWorldId().toString()
            );
        }


        mountsConfig.set(
                path + ".home.x",
                mountData.getHomeX()
        );


        mountsConfig.set(
                path + ".home.y",
                mountData.getHomeY()
        );


        mountsConfig.set(
                path + ".home.z",
                mountData.getHomeZ()
        );


        mountsConfig.set(
                path + ".home.yaw",
                mountData.getHomeYaw()
        );


        mountsConfig.set(
                path + ".home.pitch",
                mountData.getHomePitch()
        );


        /*
         * ====================================================
         * LAST KNOWN LOCATION
         * ====================================================
         */

        Location lastKnown =
                mountData.getLastKnownLocation();


        if (lastKnown != null
                && lastKnown.getWorld() != null) {

            mountsConfig.set(
                    path + ".last-known.world",
                    lastKnown.getWorld()
                            .getUID()
                            .toString()
            );


            mountsConfig.set(
                    path + ".last-known.x",
                    lastKnown.getX()
            );


            mountsConfig.set(
                    path + ".last-known.y",
                    lastKnown.getY()
            );


            mountsConfig.set(
                    path + ".last-known.z",
                    lastKnown.getZ()
            );


            mountsConfig.set(
                    path + ".last-known.yaw",
                    lastKnown.getYaw()
            );


            mountsConfig.set(
                    path + ".last-known.pitch",
                    lastKnown.getPitch()
            );
        }


        /*
         * ====================================================
         * APPEARANCE
         * ====================================================
         */

        mountsConfig.set(
                path + ".appearance.color",
                mountData.getColor().name()
        );


        mountsConfig.set(
                path + ".appearance.style",
                mountData.getStyle().name()
        );


        /*
         * ====================================================
         * ATTRIBUTES
         * ====================================================
         */

        mountsConfig.set(
                path + ".attributes.max-health",
                mountData.getMaxHealth()
        );


        mountsConfig.set(
                path + ".attributes.health",
                mountData.getHealth()
        );


        mountsConfig.set(
                path + ".attributes.movement-speed",
                mountData.getMovementSpeed()
        );


        mountsConfig.set(
                path + ".attributes.jump-strength",
                mountData.getJumpStrength()
        );


        /*
         * ====================================================
         * ARMOR
         * ====================================================
         */

        mountsConfig.set(
                path + ".armor",
                mountData.getArmor()
        );
    }


    /*
     * ========================================================
     * REMOVE MOUNT
     * ========================================================
     */

    private void removeMountFromFile(
            UUID ownerId
    ) {

        mountsConfig.set(
                "mounts." + ownerId,
                null
        );


        saveFile();
    }


    /*
     * ========================================================
     * SAVE FILE
     * ========================================================
     */

    private void saveFile() {

        try {

            mountsConfig.save(
                    mountsFile
            );

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Could not save mounts.yml!"
            );

            exception.printStackTrace();
        }
    }


    /*
     * ========================================================
     * LOAD MOUNTS
     * ========================================================
     */

    private void loadMounts() {

        ConfigurationSection mountsSection =
                mountsConfig.getConfigurationSection(
                        "mounts"
                );


        if (mountsSection == null) {

            return;
        }


        for (String ownerString
                : mountsSection.getKeys(false)) {

            try {

                String path =
                        "mounts." + ownerString;


                /*
                 * BASIC UUID DATA
                 */

                String ownerValue =
                        mountsConfig.getString(
                                path + ".owner"
                        );


                String horseValue =
                        mountsConfig.getString(
                                path + ".horse"
                        );


                if (ownerValue == null
                        || horseValue == null) {

                    continue;
                }


                UUID ownerId =
                        UUID.fromString(
                                ownerValue
                        );


                UUID horseId =
                        UUID.fromString(
                                horseValue
                        );


                String horseName =
                        mountsConfig.getString(
                                path + ".horse-name",
                                "Equinox Mount"
                        );


                long registeredAt =
                        mountsConfig.getLong(
                                path + ".registered-at",
                                System.currentTimeMillis()
                        );


                /*
                 * ====================================================
                 * LOAD HOME LOCATION
                 * ====================================================
                 */

                String homeWorldString =
                        mountsConfig.getString(
                                path + ".home.world"
                        );


                if (homeWorldString == null) {

                    plugin.getLogger().warning(
                            "No home world found for mount: "
                                    + horseId
                    );

                    continue;
                }


                World homeWorld =
                        Bukkit.getWorld(
                                UUID.fromString(
                                        homeWorldString
                                )
                        );


                if (homeWorld == null) {

                    plugin.getLogger().warning(
                            "Home world is not loaded for mount: "
                                    + horseId
                    );

                    continue;
                }


                Location homeLocation =
                        new Location(
                                homeWorld,

                                mountsConfig.getDouble(
                                        path + ".home.x"
                                ),

                                mountsConfig.getDouble(
                                        path + ".home.y"
                                ),

                                mountsConfig.getDouble(
                                        path + ".home.z"
                                ),

                                (float) mountsConfig.getDouble(
                                        path + ".home.yaw"
                                ),

                                (float) mountsConfig.getDouble(
                                        path + ".home.pitch"
                                )
                        );


                /*
                 * ====================================================
                 * LOAD LAST KNOWN LOCATION
                 * ====================================================
                 */

                Location lastKnownLocation =
                        homeLocation.clone();


                String lastWorldString =
                        mountsConfig.getString(
                                path + ".last-known.world"
                        );


                if (lastWorldString != null) {

                    try {

                        World lastWorld =
                                Bukkit.getWorld(
                                        UUID.fromString(
                                                lastWorldString
                                        )
                                );


                        if (lastWorld != null) {

                            lastKnownLocation =
                                    new Location(
                                            lastWorld,

                                            mountsConfig.getDouble(
                                                    path + ".last-known.x"
                                            ),

                                            mountsConfig.getDouble(
                                                    path + ".last-known.y"
                                            ),

                                            mountsConfig.getDouble(
                                                    path + ".last-known.z"
                                            ),

                                            (float) mountsConfig.getDouble(
                                                    path + ".last-known.yaw"
                                            ),

                                            (float) mountsConfig.getDouble(
                                                    path + ".last-known.pitch"
                                            )
                                    );
                        }

                    } catch (Exception ignored) {

                        /*
                         * Old mounts.yml compatibility:
                         *
                         * If last-known data is invalid,
                         * use the permanent home location.
                         */

                        lastKnownLocation =
                                homeLocation.clone();
                    }
                }


                /*
                 * ====================================================
                 * APPEARANCE
                 * ====================================================
                 */

                Horse.Color color =
                        parseColor(
                                mountsConfig.getString(
                                        path + ".appearance.color"
                                )
                        );


                Horse.Style style =
                        parseStyle(
                                mountsConfig.getString(
                                        path + ".appearance.style"
                                )
                        );


                /*
                 * ====================================================
                 * ATTRIBUTES
                 * ====================================================
                 */

                double maxHealth =
                        mountsConfig.getDouble(
                                path + ".attributes.max-health",
                                20.0
                        );


                double health =
                        mountsConfig.getDouble(
                                path + ".attributes.health",
                                maxHealth
                        );


                double movementSpeed =
                        mountsConfig.getDouble(
                                path + ".attributes.movement-speed",
                                0.225
                        );


                double jumpStrength =
                        mountsConfig.getDouble(
                                path + ".attributes.jump-strength",
                                0.7
                        );


                ItemStack armor =
                        mountsConfig.getItemStack(
                                path + ".armor"
                        );


                /*
                 * ====================================================
                 * CREATE DATA
                 * ====================================================
                 */

                MountData mountData =
                        new MountData(
                                ownerId,
                                horseId,
                                horseName,

                                /*
                                 * Permanent bind location.
                                 */
                                homeLocation,

                                /*
                                 * Last tracked location.
                                 */
                                lastKnownLocation,

                                color,
                                style,

                                maxHealth,
                                health,
                                movementSpeed,
                                jumpStrength,

                                armor,

                                registeredAt
                        );


                playerMounts.put(
                        ownerId,
                        mountData
                );


                horseOwners.put(
                        horseId,
                        ownerId
                );


            } catch (Exception exception) {

                plugin.getLogger().warning(
                        "Could not load mount data for: "
                                + ownerString
                );

                exception.printStackTrace();
            }
        }


        plugin.getLogger().info(
                "Loaded "
                        + playerMounts.size()
                        + " Equinox mount(s)."
        );
    }


    /*
     * ========================================================
     * ATTRIBUTE UTILITY
     * ========================================================
     */

    private double getAttributeValue(
            Horse horse,
            Attribute attribute,
            double fallback
    ) {

        AttributeInstance instance =
                horse.getAttribute(
                        attribute
                );


        if (instance == null) {

            return fallback;
        }


        return instance.getBaseValue();
    }


    /*
     * ========================================================
     * PARSE COLOR
     * ========================================================
     */

    private Horse.Color parseColor(
            String value
    ) {

        if (value == null) {

            return Horse.Color.WHITE;
        }


        try {

            return Horse.Color.valueOf(
                    value
            );

        } catch (IllegalArgumentException ignored) {

            return Horse.Color.WHITE;
        }
    }


    /*
     * ========================================================
     * PARSE STYLE
     * ========================================================
     */

    private Horse.Style parseStyle(
            String value
    ) {

        if (value == null) {

            return Horse.Style.NONE;
        }


        try {

            return Horse.Style.valueOf(
                    value
            );

        } catch (IllegalArgumentException ignored) {

            return Horse.Style.NONE;
        }
    }


    /*
     * ========================================================
     * HORSE NAME
     * ========================================================
     */

    private String getHorseName(
            Horse horse
    ) {

        if (horse.getCustomName() != null
                && !horse.getCustomName().isEmpty()) {

            return horse.getCustomName();
        }


        return "Equinox Mount";
    }

    /*
 * ========================================================
 * FIND REAL HORSE IN LOADED CHUNK
 * ========================================================
 *
 * Searches the actual chunk entity list for the exact
 * registered horse UUID.
 *
 * This is more reliable for unloaded-chunk recovery than
 * relying only on World#getEntity(UUID).
 * ========================================================
 */

private Horse findHorseInChunk(
        Location location,
        UUID horseId
) {

    if (location == null
            || location.getWorld() == null
            || horseId == null) {

        return null;
    }


    World world =
            location.getWorld();


    int chunkX =
            location.getBlockX() >> 4;


    int chunkZ =
            location.getBlockZ() >> 4;


    try {

        /*
         * Force the chunk to load.
         */

        Chunk chunk =
                world.getChunkAt(
                        chunkX,
                        chunkZ
                );


        /*
         * Keep the chunk loaded while we search.
         *
         * This prevents it from immediately unloading again
         * while the whistle recovery process is running.
         */

        chunk.addPluginChunkTicket(
                plugin
        );


        /*
         * Search the REAL entities inside the chunk.
         */

        for (Entity entity :
                chunk.getEntities()) {

            if (!(entity instanceof Horse horse)) {

                continue;
            }


            if (!horse.isValid()
                    || horse.isDead()) {

                continue;
            }


            if (horse.getUniqueId()
                    .equals(horseId)) {

                return horse;
            }
        }

    } catch (Exception exception) {

        plugin.getLogger().warning(
                "Could not load/search mount chunk at "
                        + chunkX
                        + ", "
                        + chunkZ
        );
    }


    return null;
}


    /*
     * ========================================================
     * SHUTDOWN
     * ========================================================
     */

    public void shutdown() {

        saveAllMounts();

        playerMounts.clear();

        horseOwners.clear();
    }
}
