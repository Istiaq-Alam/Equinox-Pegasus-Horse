package com.istiak.equinox.mount;

import com.istiak.equinox.EquinoxMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Port of MountManager storage. Persisted as {@code data/equinox_mounts.dat}
 * inside the world folder.
 *
 * Design invariants carried over from the plugin:
 *  - HOME is the permanent bind location and is NEVER changed automatically.
 *  - LAST KNOWN is the tracked real-horse position used to reload its chunk.
 *  - The system never creates a replacement or duplicate horse.
 */
public final class MountSavedData {

    private static final String FILE_NAME = "equinox_mounts.dat";

    /** One store per running server. */
    private static final Map<MinecraftServer, MountSavedData> INSTANCES = new HashMap<>();

    private final MinecraftServer server;

    /** Player UUID -> MountData */
    private final Map<UUID, MountData> playerMounts = new HashMap<>();

    /** Horse UUID -> Player UUID */
    private final Map<UUID, UUID> horseOwners = new HashMap<>();

    private boolean dirty = false;

    private MountSavedData(MinecraftServer server) {
        this.server = server;
        loadFromDisk();
    }

    public static MountSavedData get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, MountSavedData::new);
    }

    private Path file() {
        return server.getWorldPath(LevelResource.DATA).resolve(FILE_NAME);
    }

    private void loadFromDisk() {
        Path path = file();
        if (!Files.exists(path)) {
            EquinoxMod.log("Loaded 0 Equinox mount(s).");
            return;
        }
        try {
            CompoundTag root = NbtIo.read(path);
            ListTag list = root.getListOrEmpty("Mounts");
            for (int i = 0; i < list.size(); i++) {
                try {
                    MountData mount = MountData.load(list.getCompoundOrEmpty(i));
                    playerMounts.put(mount.getOwnerId(), mount);
                    horseOwners.put(mount.getHorseId(), mount.getOwnerId());
                } catch (Exception e) {
                    EquinoxMod.log("Could not load mount entry #" + i + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            EquinoxMod.log("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
        EquinoxMod.log("Loaded " + playerMounts.size() + " Equinox mount(s).");
    }

    public void markDirty() {
        dirty = true;
    }

    /** Called periodically from the tick loop and on server stop. */
    public void saveIfDirty() {
        if (dirty) {
            saveNow();
        }
    }

    public void saveNow() {
        try {
            Files.createDirectories(file().getParent());
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (MountData mount : playerMounts.values()) {
                list.add(mount.save());
            }
            root.put("Mounts", list);
            NbtIo.write(root, file());
            dirty = false;
        } catch (IOException e) {
            EquinoxMod.log("Could not save " + FILE_NAME + ": " + e.getMessage());
        }
    }

    // ======================================================================
    // REGISTRY
    // ======================================================================

    /**
     * Registers the horse. Callers must validate tamed/owner/armor first
     * (mirrors the plugin's command-side checks + MountManager.registerMount).
     */
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

        ServerLevel level = (ServerLevel) horse.level();
        var pos = horse.position();
        // HOME = permanent bind location.
        data.setHome(level, pos.x, pos.y, pos.z, horse.getYRot(), horse.getXRot());
        // LAST KNOWN starts equal to HOME.
        data.setLastKnown(level, pos.x, pos.y, pos.z, horse.getYRot(), horse.getXRot());

        if (horse.hasCustomName()) {
            data.setHorseName(horse.getCustomName().getString());
        } else if (oldName != null) {
            data.setHorseName(oldName);
        }

        playerMounts.put(playerId, data);
        horseOwners.put(horseId, playerId);
        markDirty();
        return true;
    }

    public boolean unregister(UUID horseId) {
        UUID ownerId = horseOwners.remove(horseId);
        if (ownerId == null) return false;
        playerMounts.remove(ownerId);
        markDirty();
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
    // ENTITY LOOKUP (never spawns or duplicates - only finds the real horse)
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

    /** True if this horse is a registered Equinox mount wearing Equinox armor. */
    public static boolean isValidMountArmor(Horse horse) {
        return EquinoxItemsHolder.isEquinoxArmor(horse.getItemBySlot(EquipmentSlot.BODY));
    }

    // ======================================================================
    // TRACKING
    // ======================================================================

    /**
     * Updates ONLY the last known location. Never touches HOME.
     */
    public void updateLastKnown(Horse horse) {
        UUID ownerId = horseOwners.get(horse.getUUID());
        if (ownerId == null) return;
        MountData data = playerMounts.get(ownerId);
        if (data == null) return;
        if (!(horse.level() instanceof ServerLevel level)) return;

        var pos = horse.position();
        data.setLastKnown(level, pos.x, pos.y, pos.z, horse.getYRot(), horse.getXRot());
        markDirty();
    }

    public void updateLastKnown(MinecraftServer server, UUID horseId) {
        Horse horse = getLoadedHorse(server, horseId);
        if (horse != null) {
            updateLastKnown(horse);
        }
    }

    public void saveAll() {
        saveNow();
    }

    /**
     * Tiny indirection so the store can reference EquinoxItems without a
     * package cycle at class-init time.
     */
    private static final class EquinoxItemsHolder {
        static boolean isEquinoxArmor(net.minecraft.world.item.ItemStack stack) {
            return com.istiak.equinox.items.EquinoxItems.isEquinoxArmor(stack);
        }
    }
}
