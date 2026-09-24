package com.istiak.equinox.mount;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Port of MountManager storage. One SavedData (equinox_mounts.dat) in the
 * overworld data folder holds every registered mount.
 *
 * Design invariants carried over from the plugin:
 *  - HOME is the permanent bind location and is NEVER changed automatically.
 *  - LAST KNOWN is the tracked real-horse position used to reload its chunk.
 *  - The system never creates a replacement or duplicate horse.
 */
public final class MountSavedData extends SavedData {

    private static final String DATA_NAME = "equinox_mounts";

    /** Player UUID -> MountData */
    private final Map<UUID, MountData> playerMounts = new HashMap<>();

    /** Horse UUID -> Player UUID */
    private final Map<UUID, UUID> horseOwners = new HashMap<>();

    public static MountSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MountSavedData::new, MountSavedData::load),
                DATA_NAME
        );
    }

    public static MountSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MountSavedData data = new MountSavedData();
        ListTag list = tag.getList("Mounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            MountData mount = MountData.load(list.getCompound(i));
            data.playerMounts.put(mount.getOwnerId(), mount);
            data.horseOwners.put(mount.getHorseId(), mount.getOwnerId());
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (MountData mount : playerMounts.values()) {
            list.add(mount.save());
        }
        tag.put("Mounts", list);
        return tag;
    }

    // ======================================================================
    // REGISTRY
    // ======================================================================

    public boolean register(ServerPlayer player, Horse horse) {
        UUID playerId = player.getUUID();
        UUID horseId = horse.getUUID();

        MountData old = playerMounts.get(playerId);
        long registeredAt = old != null ? old.getRegisteredAt() : System.currentTimeMillis();
        String oldName = old != null ? old.getHorseName() : null;

        if (old != null) {
            horseOwners.remove(old.getHorseId());
        }

        MountData data = new MountData(playerId, horseId);
        data.setRegisteredAt(registeredAt);

        ServerLevel level = player.serverLevel();
        var pos = horse.position();
        data.setHome(level, pos.x, pos.y, pos.z, horse.getYRot(), horse.getXRot());
        data.setLastKnown(level, pos.x, pos.y, pos.z, horse.getYRot(), horse.getXRot());

        if (horse.hasCustomName()) {
            data.setHorseName(horse.getCustomName().getString());
        } else if (oldName != null) {
            data.setHorseName(oldName);
        }

        playerMounts.put(playerId, data);
        horseOwners.put(horseId, playerId);
        setDirty();
        return true;
    }

    public boolean unregister(UUID horseId) {
        UUID ownerId = horseOwners.remove(horseId);
        if (ownerId == null) return false;
        playerMounts.remove(ownerId);
        setDirty();
        return true;
    }

    public MountData get(UUID playerId) {
        return playerId == null ? null : playerMounts.get(playerId);
    }

    public UUID getOwnerOf(UUID horseId) {
        return horseOwners.get(horseId);
    }

    public boolean isRegistered(Horse horse) {
        return horse != null && horseOwners.containsKey(horse.getUUID());
    }

    public boolean isOwner(ServerPlayer player, Horse horse) {
        UUID ownerId = horseOwners.get(horse.getUUID());
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    // ======================================================================
    // ENTITY LOOKUP
    // ======================================================================

    public Horse getLoadedHorse(MinecraftServer server, UUID horseId) {
        if (horseId == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(horseId);
            if (entity instanceof Horse horse && horse.isAlive()) {
                return horse;
            }
        }
        return null;
    }

    public Horse getLoadedMount(MinecraftServer server, ServerPlayer player) {
        MountData data = get(player.getUUID());
        return data == null ? null : getLoadedHorse(server, data.getHorseId());
    }

    // ======================================================================
    // TRACKING
    // ======================================================================

    /**
     * Updates ONLY the last known location. Never touches HOME.
     */
    public void updateLastKnown(MinecraftServer server, Horse horse) {
        UUID ownerId = horseOwners.get(horse.getUUID());
        if (ownerId == null) return;
        MountData data = playerMounts.get(ownerId);
        if (data == null) return;
        if (!(horse.level() instanceof ServerLevel level)) return;

        var pos = horse.position();
        data.setLastKnown(level, pos.x, pos.y, pos.z, horse.getYRot(), horse.getXRot());
        setDirty();
    }

    public void updateLastKnown(MinecraftServer server, UUID horseId) {
        Horse horse = getLoadedHorse(server, horseId);
        if (horse != null) {
            updateLastKnown(server, horse);
        }
    }

    public void saveAll() {
        setDirty();
    }
}
