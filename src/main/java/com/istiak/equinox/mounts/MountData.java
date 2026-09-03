package com.istiak.equinox.mounts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class MountData {

    /*
     * ========================================================
     * OWNER / HORSE
     * ========================================================
     */

    private final UUID ownerId;

    private UUID horseId;

    private String horseName;


    /*
     * ========================================================
     * PERMANENT HOME LOCATION
     *
     * This is the location where the mount was bound.
     *
     * IMPORTANT:
     * This location NEVER automatically changes.
     * ========================================================
     */

    private UUID homeWorldId;

    private double homeX;
    private double homeY;
    private double homeZ;

    private float homeYaw;
    private float homePitch;


    /*
     * ========================================================
     * LAST KNOWN REAL HORSE LOCATION
     *
     * This is ONLY used to find the real horse when its chunk
     * becomes unloaded.
     *
     * This does NOT replace the permanent home location.
     * ========================================================
     */

    private UUID lastWorldId;

    private double lastX;
    private double lastY;
    private double lastZ;

    private float lastYaw;
    private float lastPitch;


    /*
     * ========================================================
     * APPEARANCE
     * ========================================================
     */

    private Horse.Color color;

    private Horse.Style style;


    /*
     * ========================================================
     * ATTRIBUTES
     * ========================================================
     */

    private double maxHealth;
    private double health;
    private double movementSpeed;
    private double jumpStrength;


    /*
     * ========================================================
     * ARMOR
     * ========================================================
     */

    private ItemStack armor;


    /*
     * ========================================================
     * REGISTRATION
     * ========================================================
     */

    private final long registeredAt;


    /*
     * ========================================================
     * CONSTRUCTORS
     * ========================================================
     */

    public MountData(
            UUID ownerId,
            UUID horseId,
            String horseName,
            Location homeLocation,
            Horse.Color color,
            Horse.Style style,
            double maxHealth,
            double health,
            double movementSpeed,
            double jumpStrength,
            ItemStack armor
    ) {

        this(
                ownerId,
                horseId,
                horseName,
                homeLocation,
                homeLocation,
                color,
                style,
                maxHealth,
                health,
                movementSpeed,
                jumpStrength,
                armor,
                System.currentTimeMillis()
        );
    }


    public MountData(
            UUID ownerId,
            UUID horseId,
            String horseName,
            Location homeLocation,
            Location lastKnownLocation,
            Horse.Color color,
            Horse.Style style,
            double maxHealth,
            double health,
            double movementSpeed,
            double jumpStrength,
            ItemStack armor,
            long registeredAt
    ) {

        this.ownerId = ownerId;
        this.horseId = horseId;

        this.horseName =
                horseName != null
                        ? horseName
                        : "Equinox Mount";

        this.color =
                color != null
                        ? color
                        : Horse.Color.WHITE;

        this.style =
                style != null
                        ? style
                        : Horse.Style.NONE;

        this.maxHealth = maxHealth;
        this.health = health;
        this.movementSpeed = movementSpeed;
        this.jumpStrength = jumpStrength;

        this.armor =
                armor != null
                        ? armor.clone()
                        : null;

        this.registeredAt = registeredAt;

        setHomeLocation(homeLocation);
        setLastKnownLocation(lastKnownLocation);
    }


    /*
     * ========================================================
     * BASIC
     * ========================================================
     */

    public UUID getOwnerId() {
        return ownerId;
    }


    public UUID getHorseId() {
        return horseId;
    }


    public void setHorseId(UUID horseId) {
        this.horseId = horseId;
    }


    public String getHorseName() {
        return horseName;
    }


    public void setHorseName(String horseName) {

        this.horseName =
                horseName != null
                        ? horseName
                        : "Equinox Mount";
    }


    public long getRegisteredAt() {
        return registeredAt;
    }


    /*
     * ========================================================
     * HOME
     * ========================================================
     */

    public void setHomeLocation(Location location) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }

        homeWorldId = location.getWorld().getUID();

        homeX = location.getX();
        homeY = location.getY();
        homeZ = location.getZ();

        homeYaw = location.getYaw();
        homePitch = location.getPitch();
    }


    public Location getHomeLocation() {

        if (homeWorldId == null) {
            return null;
        }

        World world =
                Bukkit.getWorld(homeWorldId);

        if (world == null) {
            return null;
        }

        return new Location(
                world,
                homeX,
                homeY,
                homeZ,
                homeYaw,
                homePitch
        );
    }


    public UUID getHomeWorldId() {
        return homeWorldId;
    }


    public double getHomeX() {
        return homeX;
    }


    public double getHomeY() {
        return homeY;
    }


    public double getHomeZ() {
        return homeZ;
    }


    public float getHomeYaw() {
        return homeYaw;
    }


    public float getHomePitch() {
        return homePitch;
    }


    /*
     * ========================================================
     * LAST KNOWN LOCATION
     * ========================================================
     */

    public void setLastKnownLocation(Location location) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }

        lastWorldId = location.getWorld().getUID();

        lastX = location.getX();
        lastY = location.getY();
        lastZ = location.getZ();

        lastYaw = location.getYaw();
        lastPitch = location.getPitch();
    }


    public Location getLastKnownLocation() {

        if (lastWorldId == null) {

            /*
             * Old mounts fallback.
             */

            return getHomeLocation();
        }

        World world =
                Bukkit.getWorld(lastWorldId);

        if (world == null) {
            return getHomeLocation();
        }

        return new Location(
                world,
                lastX,
                lastY,
                lastZ,
                lastYaw,
                lastPitch
        );
    }


    public UUID getLastWorldId() {
        return lastWorldId;
    }


    public double getLastX() {
        return lastX;
    }


    public double getLastY() {
        return lastY;
    }


    public double getLastZ() {
        return lastZ;
    }


    public float getLastYaw() {
        return lastYaw;
    }


    public float getLastPitch() {
        return lastPitch;
    }


    /*
     * ========================================================
     * APPEARANCE
     * ========================================================
     */

    public Horse.Color getColor() {
        return color;
    }


    public void setColor(Horse.Color color) {
        this.color = color;
    }


    public Horse.Style getStyle() {
        return style;
    }


    public void setStyle(Horse.Style style) {
        this.style = style;
    }


    /*
     * ========================================================
     * ATTRIBUTES
     * ========================================================
     */

    public double getMaxHealth() {
        return maxHealth;
    }


    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }


    public double getHealth() {
        return health;
    }


    public void setHealth(double health) {
        this.health = health;
    }


    public double getMovementSpeed() {
        return movementSpeed;
    }


    public void setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }


    public double getJumpStrength() {
        return jumpStrength;
    }


    public void setJumpStrength(double jumpStrength) {
        this.jumpStrength = jumpStrength;
    }


    /*
     * ========================================================
     * ARMOR
     * ========================================================
     */

    public ItemStack getArmor() {

        return armor != null
                ? armor.clone()
                : null;
    }


    public void setArmor(ItemStack armor) {

        this.armor =
                armor != null
                        ? armor.clone()
                        : null;
    }


    /*
     * ========================================================
     * OLD COMPATIBILITY
     * ========================================================
     */

    public UUID getWorldId() {
        return getHomeWorldId();
    }


    public double getX() {
        return getHomeX();
    }


    public double getY() {
        return getHomeY();
    }


    public double getZ() {
        return getHomeZ();
    }


    public float getYaw() {
        return getHomeYaw();
    }


    public float getPitch() {
        return getHomePitch();
    }


    public Location getStoredLocation() {
        return getHomeLocation();
    }
}
