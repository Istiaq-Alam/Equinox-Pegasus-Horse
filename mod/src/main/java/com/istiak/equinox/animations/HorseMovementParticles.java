package com.istiak.equinox.animations;

import com.istiak.equinox.enchant.EnchantmentType;
import com.istiak.equinox.flight.FlightManager;
import com.istiak.equinox.items.EquinoxItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Port of HorseMovementListener: the running/foot particle system.
 *
 * Any loaded horse wearing Equinox armor enchanted with Swift emits
 * ELECTRIC_SPARK particles at its four hoof positions every
 * {@link #INTERVAL_TICKS} ticks while it moves horizontally at least
 * {@link #MINIMUM_MOVEMENT} blocks - walking, being led, or carrying a rider.
 *
 * While flying, the horse is skipped (FlightManager renders the Bifrost
 * pathway instead) but its position keeps being recorded so landing does
 * not produce a false movement spike.
 */
public final class HorseMovementParticles {

    /** Plugin config "settings.particles.interval-ticks" (default 5). */
    private static final int INTERVAL_TICKS = 5;
    /** Plugin config "settings.particles.minimum-speed" (default 0.05). */
    private static final double MINIMUM_MOVEMENT = 0.05;

    /** Spacing of the sparks left behind on the path just traveled. */
    private static final double TRAIL_SPACING = 0.5;

    private final FlightManager flightManager;

    /** Horse UUID -> last sampled position, to detect real movement. */
    private final Map<UUID, Sample> previousLocations = new HashMap<>();

    private long tickCounter = 0;

    private record Sample(ServerLevel level, double x, double y, double z) {
    }

    public HorseMovementParticles(FlightManager flightManager) {
        this.flightManager = flightManager;
    }

    /** Called every server tick; internally gates to INTERVAL_TICKS. */
    public void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Horse horse && horse.isAlive()) {
                    handleHorseMovement(horse);
                }
            }
        }
    }

    private void handleHorseMovement(Horse horse) {
        // FlightManager draws the Bifrost pathway instead - but keep the
        // location sample current so landing doesn't fake a movement spike.
        if (flightManager.isFlying(horse)) {
            previousLocations.put(horse.getUUID(), sampleOf(horse));
            return;
        }

        UUID horseId = horse.getUUID();
        Sample current = sampleOf(horse);
        Sample previous = previousLocations.put(horseId, current);

        if (previous == null || previous.level() != current.level()) {
            return;
        }

        double dx = current.x() - previous.x();
        double dz = current.z() - previous.z();
        double horizontalMovement = Math.sqrt(dx * dx + dz * dz);

        if (horizontalMovement < MINIMUM_MOVEMENT) {
            return;
        }

        ItemStack armor = horse.getItemBySlot(EquipmentSlot.BODY);
        if (!EquinoxItems.isEquinoxArmor(armor)) {
            return;
        }

        int swiftLevel = EquinoxItems.getLevel(armor, EnchantmentType.SWIFT);
        if (swiftLevel <= 0) {
            return;
        }

        spawnHorseParticles(horse, swiftLevel);
        spawnLeftBehindSparks(horse, previous, swiftLevel);
    }

    private Sample sampleOf(Horse horse) {
        Vec3 pos = horse.position();
        return new Sample((ServerLevel) horse.level(), pos.x, pos.y, pos.z);
    }

    /** Plugin parity: HorseMovementListener.spawnHorseParticles. */
    private void spawnHorseParticles(Horse horse, int swiftLevel) {
        ServerLevel level = (ServerLevel) horse.level();
        Vec3 base = horse.position();

        // Body rotation is what the legs visually follow.
        double yaw = Math.toRadians(horse.yBodyRot);

        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);

        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);

        Vec3 frontLeft = base.add(
                forwardX * 0.75 + sideX * 0.28, 0.15,
                forwardZ * 0.75 + sideZ * 0.28);
        Vec3 frontRight = base.add(
                forwardX * 0.75 - sideX * 0.28, 0.15,
                forwardZ * 0.75 - sideZ * 0.28);
        Vec3 backLeft = base.add(
                -forwardX * 0.75 + sideX * 0.28, 0.15,
                -forwardZ * 0.75 + sideZ * 0.28);
        Vec3 backRight = base.add(
                -forwardX * 0.75 - sideX * 0.28, 0.15,
                -forwardZ * 0.75 - sideZ * 0.28);

        // More Swift levels = slightly more particles.
        int particleCount = Math.min(1 + swiftLevel / 3, 4);

        spawnLegParticles(level, frontLeft, particleCount);
        spawnLegParticles(level, frontRight, particleCount);
        spawnLegParticles(level, backLeft, particleCount);
        spawnLegParticles(level, backRight, particleCount);
    }

    /** Plugin parity: HorseMovementListener.spawnLegParticles. */
    private void spawnLegParticles(ServerLevel level, Vec3 location, int count) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                location.x, location.y, location.z,
                count, 0.12, 0.05, 0.12, 0.01);
    }

    /**
     * Leaves the running particle behind: ELECTRIC_SPARK puffs along the
     * ground the horse just crossed (between the previous and current
     * sample). As the horse moves on, the sparks stay where they were
     * spawned and briefly fade, forming a sparkling trail of the SAME
     * running particle - no flight pathway dust is used.
     */
    private void spawnLeftBehindSparks(Horse horse, Sample previous, int swiftLevel) {
        ServerLevel level = (ServerLevel) horse.level();
        Vec3 current = horse.position();

        double dx = current.x - previous.x();
        double dz = current.z - previous.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.01) {
            return;
        }

        int points = Math.max(1, (int) Math.ceil(distance / TRAIL_SPACING));
        // Same scaling as the hoof sparks.
        int count = Math.min(1 + swiftLevel / 3, 4);

        for (int i = 1; i <= points; i++) {
            double t = (double) i / (points + 1);
            double x = previous.x() + dx * t;
            double y = previous.y() + (current.y - previous.y()) * t + 0.15;
            double z = previous.z() + dz * t;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z,
                    count, 0.12, 0.05, 0.12, 0.01);
        }
    }

    public void shutdown() {
        previousLocations.clear();
    }
}
