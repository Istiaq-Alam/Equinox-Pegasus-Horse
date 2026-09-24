package com.istiak.equinox.mount;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Port of MountData - all persistent fields of one registered mount.
 *
 * HOME = permanent bind location, never changed automatically.
 * LAST KNOWN = tracked real-horse location used for chunk recovery.
 */
public final class MountData {

    private final UUID ownerId;
    private UUID horseId;
    private String horseName = "Equinox Mount";

    // Permanent home
    private String homeDim = "minecraft:overworld";
    private double homeX, homeY, homeZ;
    private float homeYaw, homePitch;

    // Last known
    private String lastDim = "minecraft:overworld";
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;

    private long registeredAt;

    public MountData(UUID ownerId, UUID horseId) {
        this.ownerId = ownerId;
        this.horseId = horseId;
        this.registeredAt = System.currentTimeMillis();
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getHorseId() {
        return horseId;
    }

    public String getHorseName() {
        return horseName;
    }

    public void setHorseName(String name) {
        this.horseName = name != null ? name : "Equinox Mount";
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long at) {
        this.registeredAt = at;
    }

    public static String dimKey(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    /** Resolves a "namespace:path" dimension string back to a ServerLevel. */
    public static ServerLevel resolveDim(MinecraftServer server, String dim) {
        try {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim));
            return server.getLevel(key);
        } catch (Exception e) {
            return null;
        }
    }

    public void setHome(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        this.homeDim = dimKey(level);
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
    }

    public void setLastKnown(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        this.lastDim = dimKey(level);
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    public String getHomeDim() {
        return homeDim;
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

    public String getLastDim() {
        return lastDim;
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

    // ======================================================================
    // NBT (26.x: no CompoundTag UUID helpers - store UUIDs as int arrays)
    // ======================================================================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Owner", UUIDUtil.uuidToIntArray(ownerId));
        tag.putIntArray("Horse", UUIDUtil.uuidToIntArray(horseId));
        tag.putString("HorseName", horseName);
        tag.putLong("RegisteredAt", registeredAt);

        tag.putString("HomeDim", homeDim);
        tag.putDouble("HomeX", homeX);
        tag.putDouble("HomeY", homeY);
        tag.putDouble("HomeZ", homeZ);
        tag.putFloat("HomeYaw", homeYaw);
        tag.putFloat("HomePitch", homePitch);

        tag.putString("LastDim", lastDim);
        tag.putDouble("LastX", lastX);
        tag.putDouble("LastY", lastY);
        tag.putDouble("LastZ", lastZ);
        tag.putFloat("LastYaw", lastYaw);
        tag.putFloat("LastPitch", lastPitch);
        return tag;
    }

    public static MountData load(CompoundTag tag) {
        int[] ownerArr = tag.getIntArray("Owner").orElse(null);
        int[] horseArr = tag.getIntArray("Horse").orElse(null);
        if (ownerArr == null || horseArr == null) {
            throw new IllegalArgumentException("Corrupt mount entry");
        }
        MountData data = new MountData(UUIDUtil.uuidFromIntArray(ownerArr), UUIDUtil.uuidFromIntArray(horseArr));
        data.horseName = tag.getStringOr("HorseName", "Equinox Mount");
        data.registeredAt = tag.getLongOr("RegisteredAt", System.currentTimeMillis());

        data.homeDim = tag.getStringOr("HomeDim", "minecraft:overworld");
        data.homeX = tag.getDoubleOr("HomeX", 0.0);
        data.homeY = tag.getDoubleOr("HomeY", 0.0);
        data.homeZ = tag.getDoubleOr("HomeZ", 0.0);
        data.homeYaw = tag.getFloatOr("HomeYaw", 0.0f);
        data.homePitch = tag.getFloatOr("HomePitch", 0.0f);

        data.lastDim = tag.getStringOr("LastDim", data.homeDim);
        data.lastX = tag.getDoubleOr("LastX", data.homeX);
        data.lastY = tag.getDoubleOr("LastY", data.homeY);
        data.lastZ = tag.getDoubleOr("LastZ", data.homeZ);
        data.lastYaw = tag.getFloatOr("LastYaw", data.homeYaw);
        data.lastPitch = tag.getFloatOr("LastPitch", data.homePitch);
        return data;
    }
}
