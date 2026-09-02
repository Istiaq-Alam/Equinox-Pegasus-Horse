package com.istiak.equinox.mounts;

import com.istiak.equinox.EquinoxPlugin;

import org.bukkit.Bukkit;
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


public final class MountManager {

    private final EquinoxPlugin plugin;


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


    private final NamespacedKey ownerKey;

    private final NamespacedKey registeredKey;


    private File mountsFile;

    private FileConfiguration mountsConfig;


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
     * STORAGE
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

        if (player == null
                || horse == null) {

            return false;
        }

        if (!horse.isTamed()) {

            return false;
        }

        if (horse.getOwner() == null
                || !horse.getOwner()
                .getUniqueId()
                .equals(
                        player.getUniqueId()
                )) {

            return false;
        }

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
         * Remove OLD horse mapping only.
         *
         * Do NOT call unregisterMount() here because that would
         * delete the player's complete mount data.
         */

        MountData oldMount =
                playerMounts.get(playerId);

        if (oldMount != null) {

            horseOwners.remove(
                    oldMount.getHorseId()
            );
        }


        MountData mountData =
                createMountData(
                        player,
                        horse,
                        oldMount != null
                                ? oldMount.getRegisteredAt()
                                : System.currentTimeMillis()
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

        Location homeLocation =
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


        MountData data =
                new MountData(
                        player.getUniqueId(),
                        horse.getUniqueId(),
                        getHorseName(horse),
                        homeLocation,
                        horse.getColor(),
                        horse.getStyle(),
                        maxHealth,
                        health,
                        movementSpeed,
                        jumpStrength,
                        horse.getInventory()
                                .getArmor()
                );


        /*
         * Preserve original registration timestamp.
         */

        if (registeredAt != data.getRegisteredAt()) {

            return new MountData(
                    player.getUniqueId(),
                    horse.getUniqueId(),
                    getHorseName(horse),
                    homeLocation,
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

        return data;
    }


    /*
     * ========================================================
     * UPDATE MOUNT FROM LOADED HORSE
     * ========================================================
     *
     * This is useful before saving.
     */

    public void updateMountFromHorse(
            Horse horse
    ) {

        if (horse == null) {

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

            data.setArmor(armor);
        }
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


        playerMounts.remove(ownerId);

        removeMountFromFile(ownerId);


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

        return playerMounts.get(playerId);
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

        if (player == null
                || horse == null) {

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
     * LOADED HORSE
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
     * REPLACE HORSE UUID
     * ========================================================
     *
     * Used by SummonManager when a replacement horse is created.
     */

    public boolean replaceMountHorse(
            Player player,
            Horse newHorse
    ) {

        if (player == null
                || newHorse == null) {

            return false;
        }

        MountData mountData =
                getMount(player);

        if (mountData == null) {

            return false;
        }


        UUID oldHorseId =
                mountData.getHorseId();

        UUID newHorseId =
                newHorse.getUniqueId();


        horseOwners.remove(oldHorseId);


        mountData.setHorseId(newHorseId);


        horseOwners.put(
                newHorseId,
                player.getUniqueId()
        );


        applyMountPdc(
                player.getUniqueId(),
                newHorse
        );


        saveMount(mountData);

        return true;
    }


    /*
     * ========================================================
     * PDC
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
     * SAVE
     * ========================================================
     */

    public void saveAllMounts() {

        /*
         * First update information from currently loaded horses.
         */

        for (MountData mountData
                : playerMounts.values()) {

            Horse horse =
                    getLoadedHorse(
                            mountData.getHorseId()
                    );

            if (horse != null) {

                updateMountFromHorse(horse);
            }
        }


        mountsConfig.set(
                "mounts",
                null
        );


        for (MountData mountData
                : playerMounts.values()) {

            saveMountToConfig(mountData);
        }


        saveFile();
    }


    private void saveMount(
            MountData mountData
    ) {

        saveMountToConfig(mountData);

        saveFile();
    }


    private void saveMountToConfig(
            MountData mountData
    ) {

        String path =
                "mounts."
                        + mountData.getOwnerId();


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
         * Home.
         */

        if (mountData.getHomeWorldId() != null) {

            mountsConfig.set(
                    path + ".home.world",
                    mountData.getHomeWorldId()
                            .toString()
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
         * Appearance.
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
         * Attributes.
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
         * Complete ItemStack including PDC enchantments.
         */

        mountsConfig.set(
                path + ".armor",
                mountData.getArmor()
        );
    }


    private void removeMountFromFile(
            UUID ownerId
    ) {

        mountsConfig.set(
                "mounts." + ownerId,
                null
        );

        saveFile();
    }


    private void saveFile() {

        try {

            mountsConfig.save(mountsFile);

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Could not save mounts.yml!"
            );

            exception.printStackTrace();
        }
    }


    /*
     * ========================================================
     * LOAD
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


                UUID ownerId =
                        UUID.fromString(
                                mountsConfig.getString(
                                        path + ".owner"
                                )
                        );


                UUID horseId =
                        UUID.fromString(
                                mountsConfig.getString(
                                        path + ".horse"
                                )
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


                String worldString =
                        mountsConfig.getString(
                                path + ".home.world"
                        );


                /*
                 * Old format compatibility.
                 */

                if (worldString == null) {

                    worldString =
                            mountsConfig.getString(
                                    path + ".world"
                            );
                }


                if (worldString == null) {

                    plugin.getLogger().warning(
                            "No home world found for mount: "
                                    + horseId
                    );

                    continue;
                }


                World world =
                        Bukkit.getWorld(
                                UUID.fromString(worldString)
                        );


                if (world == null) {

                    plugin.getLogger().warning(
                            "Home world not loaded for mount: "
                                    + horseId
                    );

                    continue;
                }


                double x =
                        mountsConfig.contains(path + ".home.x")
                                ? mountsConfig.getDouble(path + ".home.x")
                                : mountsConfig.getDouble(path + ".x");


                double y =
                        mountsConfig.contains(path + ".home.y")
                                ? mountsConfig.getDouble(path + ".home.y")
                                : mountsConfig.getDouble(path + ".y");


                double z =
                        mountsConfig.contains(path + ".home.z")
                                ? mountsConfig.getDouble(path + ".home.z")
                                : mountsConfig.getDouble(path + ".z");


                float yaw =
                        (float) (
                                mountsConfig.contains(path + ".home.yaw")
                                        ? mountsConfig.getDouble(path + ".home.yaw")
                                        : mountsConfig.getDouble(path + ".yaw")
                        );


                float pitch =
                        (float) (
                                mountsConfig.contains(path + ".home.pitch")
                                        ? mountsConfig.getDouble(path + ".home.pitch")
                                        : mountsConfig.getDouble(path + ".pitch")
                        );


                Location home =
                        new Location(
                                world,
                                x,
                                y,
                                z,
                                yaw,
                                pitch
                        );


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


                MountData mountData =
                        new MountData(
                                ownerId,
                                horseId,
                                horseName,
                                home,
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
     * UTILITIES
     * ========================================================
     */

    private double getAttributeValue(
            Horse horse,
            Attribute attribute,
            double fallback
    ) {

        AttributeInstance instance =
                horse.getAttribute(attribute);

        if (instance == null) {

            return fallback;
        }

        return instance.getBaseValue();
    }


    private Horse.Color parseColor(
            String value
    ) {

        if (value == null) {

            return Horse.Color.WHITE;
        }

        try {

            return Horse.Color.valueOf(value);

        } catch (IllegalArgumentException ignored) {

            return Horse.Color.WHITE;
        }
    }


    private Horse.Style parseStyle(
            String value
    ) {

        if (value == null) {

            return Horse.Style.NONE;
        }

        try {

            return Horse.Style.valueOf(value);

        } catch (IllegalArgumentException ignored) {

            return Horse.Style.NONE;
        }
    }


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
     * SHUTDOWN
     * ========================================================
     */

    public void shutdown() {

        saveAllMounts();

        playerMounts.clear();

        horseOwners.clear();
    }
}
