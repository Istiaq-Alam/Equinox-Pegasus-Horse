package com.istiak.equinox.mounts;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;


/**
 * ============================================================
 * EQUINOX MOUNT DATA
 * ============================================================
 *
 * Stores persistent information about a player's registered mount.
 *
 * The home location is permanent and is created when the horse
 * is bound.
 *
 * Horse identity information is also stored so that a replacement
 * horse can be recreated if the original entity disappears.
 *
 * ============================================================
 */
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
     * HOME LOCATION
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
     * HORSE APPEARANCE
     * ========================================================
     */

    private Horse.Color color;

    private Horse.Style style;


    /*
     * ========================================================
     * HORSE ATTRIBUTES
     * ========================================================
     */

    private double maxHealth;

    private double health;

    private double movementSpeed;

    private double jumpStrength;


    /*
     * ========================================================
     * HORSE ARMOR
     * ========================================================
     *
     * ItemStack keeps Equinox PDC enchantments.
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
     * NEW MOUNT CONSTRUCTOR
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


    /*
     * ========================================================
     * LOAD CONSTRUCTOR
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
    }


    /*
     * ========================================================
     * BASIC GETTERS
     * ========================================================
     */

    public UUID getOwnerId() {

        return ownerId;
    }


    public UUID getHorseId() {

        return horseId;
    }


    public void setHorseId(
            UUID horseId
    ) {

        this.horseId = horseId;
    }


    public String getHorseName() {

        return horseName;
    }


    public void setHorseName(
            String horseName
    ) {

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
     * APPEARANCE
     * ========================================================
     */

    public Horse.Color getColor() {

        return color;
    }


    public void setColor(
            Horse.Color color
    ) {

        this.color = color;
    }


    public Horse.Style getStyle() {

        return style;
    }


    public void setStyle(
            Horse.Style style
    ) {

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


    public void setMaxHealth(
            double maxHealth
    ) {

        this.maxHealth = maxHealth;
    }


    public double getHealth() {

        return health;
    }


    public void setHealth(
            double health
    ) {

        this.health = health;
    }


    public double getMovementSpeed() {

        return movementSpeed;
    }


    public void setMovementSpeed(
            double movementSpeed
    ) {

        this.movementSpeed = movementSpeed;
    }


    public double getJumpStrength() {

        return jumpStrength;
    }


    public void setJumpStrength(
            double jumpStrength
    ) {

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


    public void setArmor(
            ItemStack armor
    ) {

        this.armor =
                armor != null
                        ? armor.clone()
                        : null;
    }


    /*
     * ========================================================
     * HOME LOCATION
     * ========================================================
     */

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


    public void setHomeLocation(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return;
        }

        this.homeWorldId =
                location.getWorld().getUID();

        this.homeX =
                location.getX();

        this.homeY =
                location.getY();

        this.homeZ =
                location.getZ();

        this.homeYaw =
                location.getYaw();

        this.homePitch =
                location.getPitch();
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


    /*
     * ========================================================
     * COMPATIBILITY METHODS
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


    @Deprecated
    public void updateLocation(
            Location location
    ) {

        setHomeLocation(location);
    }
}
