package com.istiak.equinox.mount;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

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
        return level.dimension().location().toString();
    }

    /** Resolves a "namespace:path" dimension string back to a ServerLevel. */
    public static ServerLevel resolveDim(net.minecraft.server.MinecraftServer server, String dim) {
        var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(dim));
        return server.getLevel(key);
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
    // NBT
    // ======================================================================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Owner", UUIDUtil.UUIDToNBT(ownerId));
        tag.put("Horse", UUIDUtil.UUIDToNBT(horseId));
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
        UUID owner = UUIDUtil.NBTToUUID(tag.getCompound("Owner"));
        UUID horse = UUIDUtil.NBTToUUID(tag.getCompound("Horse"));
        MountData data = new MountData(owner, horse);
        data.horseName = tag.contains("HorseName") ? tag.getString("HorseName") : "Equinox Mount";
        data.registeredAt = tag.contains("RegisteredAt") ? tag.getLong("RegisteredAt") : System.currentTimeMillis();

        data.homeDim = tag.contains("HomeDim") ? tag.getString("HomeDim") : "minecraft:overworld";
        data.homeX = tag.getDouble("HomeX");
        data.homeY = tag.getDouble("HomeY");
        data.homeZ = tag.getDouble("HomeZ");
        data.homeYaw = tag.getFloat("HomeYaw");
        data.homePitch = tag.getFloat("HomePitch");

        data.lastDim = tag.contains("LastDim") ? tag.getString("LastDim") : data.homeDim;
        data.lastX = tag.getDouble("LastX");
        data.lastY = tag.getDouble("LastY");
        data.lastZ = tag.getDouble("LastZ");
        data.lastYaw = tag.getFloat("LastYaw");
        data.lastPitch = tag.getFloat("LastPitch");
        return data;
    }
}
