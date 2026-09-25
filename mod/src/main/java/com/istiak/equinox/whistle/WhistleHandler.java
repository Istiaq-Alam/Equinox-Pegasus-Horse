package com.istiak.equinox.whistle;

import com.istiak.equinox.EquinoxMod;
import com.istiak.equinox.mount.MountData;
import com.istiak.equinox.mount.MountSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Port of WhistleListener: the whole whistle pipeline.
 *
 * Distances match the plugin constants:
 *   RETURN_RADIUS 20, RUN_RADIUS 100, ARRIVAL 3.5, RUN_SPEED 0.42,
 *   cooldown 750ms, recovery retry window 80 attempts.
 */
public final class WhistleHandler {

    private static final double RETURN_RADIUS = 20.0;
    private static final double RUN_RADIUS = 100.0;
    private static final double ARRIVAL_DISTANCE = 3.5;
    private static final double RUN_SPEED = 0.42;
    private static final long WHISTLE_COOLDOWN_MS = 750L;
    private static final int RECOVERY_MAX_ATTEMPTS = 80;
    private static final int RUN_TIMEOUT_TICKS = 600;
    private static final int TELEPORT_DELAY_TICKS = 12;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, RecoveryTask> recoveryTasks = new HashMap<>();
    private final Map<UUID, RunTask> runTasks = new HashMap<>();
    private final List<PendingTeleport> pendingTeleports = new ArrayList<>();
    private long tickCounter = 0;

    // ======================================================================
    // ENTRY POINT (called when a player uses the Equinox Whistle)
    // ======================================================================

    public boolean onWhistleUse(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUUID());
        if (last != null && now - last < WHISTLE_COOLDOWN_MS) {
            actionbar(player, "The whistle needs a moment...", ChatFormatting.GRAY);
            return true;
        }
        cooldowns.put(player.getUUID(), now);

        playWhistleEffect(player);
        handleWhistle(player);
        return true;
    }

    // ======================================================================
    // MAIN LOGIC
    // ======================================================================

    private void handleWhistle(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        MountSavedData mounts = MountSavedData.get(server);
        MountData data = mounts.get(player.getUUID());

        if (data == null) {
            actionbar(player, "No Equinox mount is bound.", ChatFormatting.RED);
            return;
        }

        Horse horse = mounts.getLoadedMount(server, player);

        if (horse == null) {
            summonFromUnloadedChunk(player);
            return;
        }

        handleLoadedHorse(player, horse, data);
    }

    private void handleLoadedHorse(ServerPlayer player, Horse horse, MountData data) {
        if (horse.isLeashed()) {
            actionbar(player, "Your Equinox mount is leashed.", ChatFormatting.YELLOW);
            return;
        }

        // Riding the horse + whistle = send it home.
        if (horse.hasPassenger(player)) {
            returnMountHome(player, horse, data);
            return;
        }

        double distance;
        if (horse.level() != player.level()) {
            distance = Double.MAX_VALUE;
        } else {
            distance = horse.distanceTo(player);
        }

        if (distance <= RETURN_RADIUS) {
            returnMountHome(player, horse, data);
        } else if (distance <= RUN_RADIUS) {
            runMountToPlayer(player, horse);
        } else {
            teleportMountToPlayer(player, horse);
        }
    }

    // ======================================================================
    // UNLOADED CHUNK RECOVERY
    // ======================================================================

    private void summonFromUnloadedChunk(ServerPlayer player) {
        actionbar(player, "Locating your Equinox mount...", ChatFormatting.LIGHT_PURPLE);
        recoveryTasks.put(player.getUUID(), new RecoveryTask(player.getUUID(), 0));
    }

    private final class RecoveryTask {
        final UUID playerId;
        int attempts;

        RecoveryTask(UUID playerId, int attempts) {
            this.playerId = playerId;
            this.attempts = attempts;
        }

        boolean tick(MinecraftServer server) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return false;
            }

            attempts++;

            MountSavedData mounts = MountSavedData.get(server);
            MountData data = mounts.get(playerId);
            if (data == null) {
                return false;
            }

            // Step 1: already loaded?
            Horse horse = mounts.getLoadedHorse(server, data.getHorseId());
            if (horse == null) {
                // Step 2: force-load chunks around the last known position.
                horse = findHorseNearLastKnown(server, data);
            }
            if (horse == null) {
                // Step 3: final fallback - permanent home chunk.
                horse = findHorseAtHome(server, data);
            }

            if (horse != null && horse.isAlive()) {
                handleLoadedHorse(player, horse, data);
                return false;
            }

            if (attempts >= RECOVERY_MAX_ATTEMPTS) {
                actionbar(player, "Your real Equinox mount could not be found.", ChatFormatting.RED);
                return false;
            }
            return true;
        }
    }

    /**
     * Loads the chunks around the last known location and searches them for
     * the exact registered horse UUID. Never spawns a horse - only recovers
     * the real one.
     */
    private Horse findHorseNearLastKnown(MinecraftServer server, MountData data) {
        ServerLevel level = MountData.resolveDim(server, data.getLastDim());
        if (level == null) return null;

        int baseX = (int) Math.floor(data.getLastX()) >> 4;
        int baseZ = (int) Math.floor(data.getLastZ()) >> 4;

        for (int radius = 0; radius <= 2; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    Horse found = searchChunk(level, baseX + x, baseZ + z, data.getHorseId());
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private Horse findHorseAtHome(MinecraftServer server, MountData data) {
        ServerLevel level = MountData.resolveDim(server, data.getHomeDim());
        if (level == null) return null;

        int cx = (int) Math.floor(data.getHomeX()) >> 4;
        int cz = (int) Math.floor(data.getHomeZ()) >> 4;
        return searchChunk(level, cx, cz, data.getHorseId());
    }

    /**
     * Force-loads the chunk (getChunk does this) and searches all loaded
     * horses whose chunk position matches.
     */
    private Horse searchChunk(ServerLevel level, int cx, int cz, UUID horseId) {
        // Hold the chunk (and neighbors) loaded with a ticket while recovery
        // runs - without this it can unload again before the teleport fires.
        ChunkPos pos = new ChunkPos(cx, cz);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, pos, 1);
        level.getChunk(cx, cz); // force load - loads its entities
        ChunkPos target = new ChunkPos(cx, cz);
        List<? extends Horse> horses =
                level.getEntities(EntityTypeTest.forClass(Horse.class),
                        h -> h.chunkPosition().equals(target) && h.isAlive());
        for (Horse horse : horses) {
            if (horse.getUUID().equals(horseId)) {
                return horse;
            }
        }
        return null;
    }

    // ======================================================================
    // RUN TO PLAYER
    // ======================================================================

    private void runMountToPlayer(ServerPlayer player, Horse horse) {
        runTasks.put(player.getUUID(), new RunTask(player.getUUID(), horse.getUUID(), 0));
        actionbar(player, "Your Equinox mount is coming...", ChatFormatting.LIGHT_PURPLE);
    }

    private final class RunTask {
        final UUID playerId;
        final UUID horseId;
        int ticks;

        RunTask(UUID playerId, UUID horseId, int ticks) {
            this.playerId = playerId;
            this.horseId = horseId;
            this.ticks = ticks;
        }

        boolean tick(MinecraftServer server) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            Horse horse = MountSavedData.get(server).getLoadedHorse(server, horseId);

            if (player == null || horse == null || !horse.isAlive()
                    || horse.isLeashed() || horse.level() != player.level()) {
                return false;
            }

            double distance = horse.distanceTo(player);

            if (distance <= ARRIVAL_DISTANCE) {
                horse.setDeltaMovement(0, horse.getDeltaMovement().y, 0);
                horse.setJumping(false);
                actionbar(player, "✦ Your Equinox mount has arrived.", ChatFormatting.GREEN);
                MountSavedData.get(server).updateLastKnown(horse);
                return false;
            }

            Vec3 horsePos = horse.position();
            Vec3 playerPos = player.position();

            Vec3 direction = new Vec3(playerPos.x - horsePos.x, 0, playerPos.z - horsePos.z);
            if (direction.lengthSqr() < 0.0001) {
                return true;
            }
            direction = direction.normalize();

            // Face movement direction.
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            horse.setYRot(yaw);
            horse.yBodyRot = yaw;
            horse.yHeadRot = yaw;

            // Jump detection: solid 1-block obstacle ahead with headroom.
            if (shouldHorseJump(horse, direction)) {
                horse.setJumping(true);
                double jumpVelocity = Math.max(0.42, horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH) * 0.72);
                Vec3 vel = horse.getDeltaMovement();
                if (vel.y < jumpVelocity) {
                    horse.setDeltaMovement(vel.x, jumpVelocity, vel.z);
                    horse.hurtMarked = true;
                }
            } else if (horse.onGround()) {
                horse.setJumping(false);
            }

            // Horizontal movement toward the player, preserve vertical velocity.
            // hurtMarked syncs the motion to clients ( Bukkit setVelocity parity).
            horse.setDeltaMovement(new Vec3(
                    direction.x * RUN_SPEED,
                    horse.getDeltaMovement().y,
                    direction.z * RUN_SPEED
            ));
            horse.hurtMarked = true;
            horse.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.0);

            ticks++;
            if (ticks >= RUN_TIMEOUT_TICKS) {
                actionbar(player, "Your Equinox mount could not reach you.", ChatFormatting.YELLOW);
                MountSavedData.get(server).updateLastKnown(horse);
                return false;
            }
            return true;
        }
    }

    private boolean shouldHorseJump(Horse horse, Vec3 direction) {
        ServerLevel level = (ServerLevel) horse.level();
        Vec3 front = horse.position().add(direction.x * 0.9, 0, direction.z * 0.9);

        BlockPos feetPos = BlockPos.containing(front.x, horse.getY(), front.z);
        BlockPos headPos = feetPos.above();
        BlockPos abovePos = headPos.above();

        var feetState = level.getBlockState(feetPos);
        if (feetState.isPathfindable(PathComputationType.LAND)) {
            return false;
        }
        if (!level.getBlockState(headPos).isPathfindable(PathComputationType.LAND)) {
            return false;
        }
        if (!level.getBlockState(abovePos).isPathfindable(PathComputationType.LAND)) {
            return false;
        }

        var block = feetState.getBlock();
        if (block == Blocks.LAVA || block == Blocks.WATER
                || block == Blocks.CACTUS || block == Blocks.MAGMA_BLOCK
                || block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE) {
            return false;
        }

        // Enough room to land on the far side?
        Vec3 landing = front.add(direction.x * 1.1, 1.0, direction.z * 1.1);
        BlockPos landingFeet = BlockPos.containing(landing.x, landing.y, landing.z);
        BlockPos landingHead = landingFeet.above();
        BlockPos landingGround = landingFeet.below();

        return level.getBlockState(landingGround).isSolid()
                && level.getBlockState(landingFeet).isPathfindable(PathComputationType.LAND)
                && level.getBlockState(landingHead).isPathfindable(PathComputationType.LAND);
    }

    // ======================================================================
    // TELEPORT / RETURN HOME
    // ======================================================================

    private void teleportMountToPlayer(ServerPlayer player, Horse horse) {
        actionbar(player, "Summoning your Equinox mount...", ChatFormatting.LIGHT_PURPLE);

        spawnTeleportEffect((ServerLevel) horse.level(), horse.position());
        horse.setDeltaMovement(Vec3.ZERO);

        pendingTeleports.add(new PendingTeleport(
                tickCounter + TELEPORT_DELAY_TICKS,
                player.getUUID(), horse.getUUID(), null));
    }

    private void returnMountHome(ServerPlayer player, Horse horse, MountData data) {
        if (horse.isLeashed()) {
            actionbar(player, "Your Equinox mount is leashed.", ChatFormatting.YELLOW);
            return;
        }

        actionbar(player, "Sending your Equinox mount home...", ChatFormatting.LIGHT_PURPLE);

        horse.ejectPassengers();
        horse.setDeltaMovement(Vec3.ZERO);

        spawnTeleportEffect((ServerLevel) horse.level(), horse.position());

        pendingTeleports.add(new PendingTeleport(
                tickCounter + TELEPORT_DELAY_TICKS,
                player.getUUID(), horse.getUUID(),
                new HomeTarget(data.getHomeDim(), data.getHomeX(), data.getHomeY(), data.getHomeZ())));
    }

    private record HomeTarget(String dim, double x, double y, double z) {
    }

    private record PendingTeleport(long dueTick, UUID playerId, UUID horseId, HomeTarget target) {
    }

    private void processTeleports(MinecraftServer server) {
        tickCounter++;

        if (pendingTeleports.isEmpty()) {
            return;
        }

        List<PendingTeleport> remaining = new ArrayList<>();
        for (PendingTeleport tp : pendingTeleports) {
            if (tp.dueTick() > tickCounter) {
                remaining.add(tp);
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(tp.playerId());
            Horse horse = MountSavedData.get(server).getLoadedHorse(server, tp.horseId());
            if (player == null || horse == null || !horse.isAlive() || horse.isLeashed()) {
                continue;
            }

            SafeSpot spot;
            if (tp.target() == null) {
                spot = findSafeNearPlayer(player);
                if (spot == null) {
                    actionbar(player, "Could not find a safe place for your mount.", ChatFormatting.RED);
                    continue;
                }
            } else {
                HomeTarget t = tp.target();
                ServerLevel level = MountData.resolveDim(server, t.dim());
                if (level == null) {
                    actionbar(player, "Your mount's home location is unavailable.", ChatFormatting.RED);
                    continue;
                }
                spot = findSafeHome(level, t.x(), t.y(), t.z());
                if (spot == null) {
                    actionbar(player, "Your mount's home is blocked.", ChatFormatting.RED);
                    continue;
                }
            }

            doTeleport(player, horse, spot);
        }
        pendingTeleports.clear();
        pendingTeleports.addAll(remaining);
    }

    private void doTeleport(ServerPlayer player, Horse horse, SafeSpot spot) {
        // Ensure the destination chunk is ticket-loaded BEFORE teleporting,
        // otherwise the teleport lands in an unloaded chunk.
        ServerLevel dest = spot.level();
        BlockPos destBlock = BlockPos.containing(spot.pos());
        ChunkPos destChunk = new ChunkPos(destBlock.getX() >> 4, destBlock.getZ() >> 4);
        dest.getChunkSource().addTicketWithRadius(TicketType.FORCED, destChunk, 1);

        boolean ok = horse.teleportTo(dest, spot.pos().x, spot.pos().y, spot.pos().z,
                java.util.Set.of(), horse.getYRot(), horse.getXRot(), false);
        if (!ok) {
            actionbar(player, "Could not summon your mount.", ChatFormatting.RED);
            return;
        }

        MountSavedData.get(player.level().getServer()).updateLastKnown(horse);

        spawnTeleportEffect(dest, spot.pos());
        dest.playSound(null, BlockPos.containing(spot.pos()),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.1f);

        actionbar(player, "✦ Your Equinox mount has arrived.", ChatFormatting.GREEN);
    }

    // ======================================================================
    // SAFE LOCATION SEARCH
    // ======================================================================

    private record SafeSpot(ServerLevel level, Vec3 pos) {
    }

    private SafeSpot findSafeNearPlayer(ServerPlayer player) {
        ServerLevel level = player.level();

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
            double radius = 3.0 + ThreadLocalRandom.current().nextDouble(4.0);

            int bx = player.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int bz = player.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
            Vec3 candidate = new Vec3(bx + 0.5, y + 1.0, bz + 0.5);

            if (isSafeForHorse(level, candidate)) {
                return new SafeSpot(level, candidate);
            }
        }
        return null;
    }

    private SafeSpot findSafeHome(ServerLevel level, double x, double y, double z) {
        Vec3 base = new Vec3(x, y, z);
        if (isSafeForHorse(level, base)) {
            return new SafeSpot(level, base);
        }

        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int bx = (int) Math.floor(x) + dx;
                    int bz = (int) Math.floor(z) + dz;
                    int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
                    Vec3 candidate = new Vec3(bx + 0.5, by + 1.0, bz + 0.5);
                    if (isSafeForHorse(level, candidate)) {
                        return new SafeSpot(level, candidate);
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeForHorse(ServerLevel level, Vec3 pos) {
        BlockPos feet = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockPos head = feet.above();
        BlockPos ground = feet.below();

        // Plugin parity: ground.isSolid() + feet/head passable.
        if (!level.getBlockState(ground).isSolid()) return false;
        if (!level.getBlockState(feet).isPathfindable(PathComputationType.LAND)) return false;
        if (!level.getBlockState(head).isPathfindable(PathComputationType.LAND)) return false;

        var block = level.getBlockState(ground).getBlock();
        return block != Blocks.LAVA
                && block != Blocks.MAGMA_BLOCK
                && block != Blocks.CAMPFIRE
                && block != Blocks.SOUL_CAMPFIRE;
    }

    // ======================================================================
    // EFFECTS
    // ======================================================================

    private void playWhistleEffect(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 pos = player.position().add(0, 1, 0);

        // Plugin used ITEM_GOAT_HORN_SOUND_0 - first vanilla horn variant.
        var hornSound = SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value();
        level.playSound(null, BlockPos.containing(pos), hornSound,
                SoundSource.PLAYERS, 2.0f, 0.75f);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.55f, 1.65f);

        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                18, 0.35, 0.45, 0.35, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z,
                35, 0.55, 0.65, 0.55, 0.25);

        actionbar(player, "✦ Equinox Whistle", ChatFormatting.LIGHT_PURPLE);
    }

    private void spawnTeleportEffect(ServerLevel level, Vec3 pos) {
        Vec3 center = pos.add(0, 1, 0);

        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                40, 0.8, 1.0, 0.8, 0.08);
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                60, 1.0, 1.0, 1.0, 0.3);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                25, 0.6, 0.8, 0.6, 0.1);

        level.playSound(null, BlockPos.containing(center), SoundEvents.PORTAL_AMBIENT,
                SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    // ======================================================================
    // TICK
    // ======================================================================

    public void tick(MinecraftServer server) {
        processTeleports(server);

        recoveryTasks.entrySet().removeIf(e -> {
            try {
                return !e.getValue().tick(server);
            } catch (Exception ex) {
                EquinoxMod.log("Recovery task error: " + ex);
                return true;
            }
        });

        runTasks.entrySet().removeIf(e -> {
            try {
                return !e.getValue().tick(server);
            } catch (Exception ex) {
                EquinoxMod.log("Run task error: " + ex);
                return true;
            }
        });
    }

    public void shutdown() {
        recoveryTasks.clear();
        runTasks.clear();
        pendingTeleports.clear();
        cooldowns.clear();
    }

    // ======================================================================
    // HELPERS
    // ======================================================================

    private static void actionbar(ServerPlayer player, String text, ChatFormatting color) {
        // 26.x: the boolean variant renders as overlay (action bar).
        player.sendSystemMessage(Component.literal(text).withStyle(color), true);
    }
}
