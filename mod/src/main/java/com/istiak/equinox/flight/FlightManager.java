package com.istiak.equinox.flight;

import com.istiak.equinox.items.EquinoxItems;
import com.istiak.equinox.mount.MountData;
import com.istiak.equinox.mount.MountSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Port of FlightManager + FlightListener. SHIFT while riding a registered,
 * armor-wearing mount toggles flight. Movement follows the rider's look
 * direction; the Bifrost pathway renders in front of the horse.
 */
public final class FlightManager {

    // Flight movement constants (from the plugin).
    private static final double FLIGHT_SPEED = 0.85;
    private static final double ASCENT_MULTIPLIER = 0.70;
    private static final double DESCENT_MULTIPLIER = 0.45;
    private static final double MAX_VERTICAL_SPEED = 0.65;
    private static final int TRANSITION_TICKS = 12;

    // Bifrost pathway constants.
    private static final double PATH_START_DISTANCE = 1.2;
    private static final double PATH_LENGTH = 9.0;
    private static final double PATH_STEP = 0.55;
    private static final double PATH_HALF_WIDTH = 1.25;
    private static final double PATH_BELOW_HORSE = 1.15;

    // Dust colors - fully opaque ARGB (alpha 0 would be invisible).
    private static final int CENTER_COLOR = 0xFF50DCFF;
    private static final int LEFT_COLOR = 0xFFB95AFF;
    private static final int RIGHT_COLOR = 0xFFFF5AD2;

    /** Horse UUIDs currently flying. */
    private final Set<UUID> flyingMounts = new HashSet<>();
    /** Horse UUIDs in takeoff/landing transition. */
    private final Set<UUID> transitionMounts = new HashSet<>();
    /** Player sneak-debounce so holding SHIFT does not retrigger. */
    private final Set<UUID> sneakArmed = new HashSet<>();
    /** Horse UUID -> tick when the transition flag expires. */
    private final Map<UUID, Long> pendingTransitions = new HashMap<>();
    private long tickCounter = 0;

    public boolean isFlying(Horse horse) {
        return horse != null && flyingMounts.contains(horse.getUUID());
    }

    public boolean isTransitioning(Horse horse) {
        return horse != null && transitionMounts.contains(horse.getUUID());
    }

    // ======================================================================
    // SNEAK HANDLING (called every tick for each online player)
    // ======================================================================

    public void handleSneak(ServerPlayer player, boolean sneaking) {
        if (!(player.getVehicle() instanceof Horse horse)) {
            sneakArmed.remove(player.getUUID());
            return;
        }

        MountSavedData mounts = MountSavedData.get(player.level().getServer());
        MountData data = mounts.get(player.getUUID());
        if (data == null || !mounts.isOwner(player, horse)) {
            return;
        }
        if (!EquinoxItems.isEquinoxArmor(horse.getItemBySlot(EquipmentSlot.BODY))) {
            if (sneaking && sneakArmed.contains(player.getUUID())) {
                actionbar(player, "Your mount needs Equinox Armor to fly.", ChatFormatting.RED);
            }
            sneakArmed.remove(player.getUUID());
            return;
        }

        if (sneaking && !sneakArmed.contains(player.getUUID())) {
            // Sneak start edge = SHIFT press.
            sneakArmed.add(player.getUUID());
            if (isFlying(horse)) {
                stopFlight(player, horse);
            } else {
                startFlight(player, horse);
            }
        } else if (!sneaking) {
            sneakArmed.remove(player.getUUID());
        }
    }

    // ======================================================================
    // FLIGHT STATE
    // ======================================================================

    public boolean startFlight(ServerPlayer player, Horse horse) {
        if (isFlying(horse) || isTransitioning(horse)) {
            return false;
        }

        transitionMounts.add(horse.getUUID());
        flyingMounts.add(horse.getUUID());

        horse.fallDistance = 0.0f;

        playTakeoffEffect((ServerLevel) horse.level(), horse.position());

        horse.level().playSound(null, horse.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 1.0f, 1.25f);
        horse.level().playSound(null, horse.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM,
                SoundSource.PLAYERS, 0.8f, 1.5f);

        // Initial upward boost. hurtMarked = true forces the server to send
        // the velocity to the client - required because the client controls
        // a ridden vehicle and silently ignores unsynced server motion.
        Vec3 vel = horse.getDeltaMovement();
        horse.setDeltaMovement(vel.x, 0.55, vel.z);
        horse.hurtMarked = true;

        // Clear the transition flag after 12 ticks.
        pendingTransitions.put(horse.getUUID(), tickCounter + TRANSITION_TICKS);

        actionbar(player, "✦ Your Equinox mount takes flight!", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    public boolean stopFlight(ServerPlayer player, Horse horse) {
        if (!isFlying(horse)) {
            return false;
        }

        flyingMounts.remove(horse.getUUID());
        transitionMounts.add(horse.getUUID());

        Vec3 vel = horse.getDeltaMovement();
        horse.setDeltaMovement(vel.x, Math.min(vel.y, -0.15), vel.z);
        horse.hurtMarked = true;

        playLandingEffect((ServerLevel) horse.level(), horse.position());

        horse.level().playSound(null, horse.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 0.8f, 0.8f);

        pendingTransitions.put(horse.getUUID(), tickCounter + 10);

        actionbar(player, "✦ Your Equinox mount is descending...", ChatFormatting.AQUA);
        return true;
    }

    public void forceStopFlight(Horse horse) {
        if (horse == null) return;
        flyingMounts.remove(horse.getUUID());
        transitionMounts.remove(horse.getUUID());
        horse.fallDistance = 0.0f;
    }

    // ======================================================================
    // MAIN TICK
    // ======================================================================

    public void tick(MinecraftServer server) {
        tickCounter++;

        // Expire transition flags.
        if (!pendingTransitions.isEmpty()) {
            pendingTransitions.entrySet().removeIf(e -> {
                if (e.getValue() <= tickCounter) {
                    transitionMounts.remove(e.getKey());
                    return true;
                }
                return false;
            });
        }

        for (UUID horseId : new HashSet<>(flyingMounts)) {
            Horse horse = MountSavedData.get(server).getLoadedHorse(server, horseId);

            // Horse disappeared.
            if (horse == null || !horse.isAlive()) {
                flyingMounts.remove(horseId);
                transitionMounts.remove(horseId);
                continue;
            }

            // Must have a player rider who is the owner and still armored.
            ServerPlayer rider = null;
            for (var passenger : horse.getPassengers()) {
                if (passenger instanceof ServerPlayer p) {
                    rider = p;
                    break;
                }
            }

            if (rider == null) {
                stopFlyingWithoutPlayer(horse);
                continue;
            }

            MountSavedData mounts = MountSavedData.get(server);
            if (!mounts.isOwner(rider, horse)
                    || !EquinoxItems.isEquinoxArmor(horse.getItemBySlot(EquipmentSlot.BODY))) {
                forceStopFlight(horse);
                continue;
            }

            // Prevent fall damage while flying.
            horse.fallDistance = 0.0f;

            applyFlightMovement(rider, horse);
            playFlightParticles(horse, rider);
        }
    }

    private void stopFlyingWithoutPlayer(Horse horse) {
        flyingMounts.remove(horse.getUUID());
        transitionMounts.remove(horse.getUUID());
        horse.fallDistance = 0.0f;
    }

    private void applyFlightMovement(ServerPlayer rider, Horse horse) {
        Vec3 look = rider.getLookAngle();

        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() > 0.0001) {
            horizontal = horizontal.normalize().scale(FLIGHT_SPEED);
        } else {
            horizontal = Vec3.ZERO;
        }

        // Look up = rise, look down = descend (plugin pitch thresholds).
        float pitch = rider.getXRot();
        double verticalVelocity;
        if (pitch <= -20.0f) {
            verticalVelocity = Math.min(MAX_VERTICAL_SPEED, Math.abs(pitch / 90.0) * ASCENT_MULTIPLIER);
        } else if (pitch >= 25.0f) {
            verticalVelocity = -Math.min(MAX_VERTICAL_SPEED, Math.abs(pitch / 90.0) * DESCENT_MULTIPLIER);
        } else {
            verticalVelocity = 0.0;
        }

        horse.setDeltaMovement(horizontal.x, verticalVelocity, horizontal.z);
        // Sync to the client every tick, otherwise the client keeps
        // overwriting our velocity and the horse never moves.
        horse.hurtMarked = true;
    }

    // ======================================================================
    // EFFECTS
    // ======================================================================

    private void playTakeoffEffect(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.3, pos.z,
                35, 0.8, 0.2, 0.8, 0.08);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z,
                35, 0.8, 0.8, 0.8, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 1.0, pos.z,
                60, 1.0, 1.0, 1.0, 0.4);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.5, pos.z,
                25, 0.9, 0.4, 0.9, 0.12);
    }

    private void playLandingEffect(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                25, 0.7, 0.3, 0.7, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z,
                20, 0.6, 0.7, 0.6, 0.03);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.4, pos.z,
                20, 0.8, 0.25, 0.8, 0.08);
    }

    private void playFlightParticles(Horse horse, ServerPlayer rider) {
        playBifrostPathway(horse, rider);

        ServerLevel level = (ServerLevel) horse.level();
        Vec3 pos = horse.position();

        // Small magical energy around the horse.
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z,
                2, 0.35, 0.25, 0.35, 0.01);

        // Sparkle trail behind the mount.
        Vec3 backwards = horizontalDirection(rider).scale(-1.0);
        Vec3 trail = pos.add(backwards.x, 0.7, backwards.z);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, trail.x, trail.y, trail.z,
                2, 0.2, 0.2, 0.2, 0.02);
    }

    private void playBifrostPathway(Horse horse, ServerPlayer rider) {
        ServerLevel level = (ServerLevel) horse.level();
        Vec3 horsePos = horse.position();
        Vec3 forward = horizontalDirection(rider);
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();

        Vec3 base = horsePos.add(0, -PATH_BELOW_HORSE, 0);

        for (double distance = PATH_START_DISTANCE; distance <= PATH_LENGTH; distance += PATH_STEP) {
            Vec3 center = base.add(forward.scale(distance));

            dust(level, center, CENTER_COLOR, 1.6f, 2);

            Vec3 leftLane = center.add(side.scale(PATH_HALF_WIDTH));
            dust(level, leftLane, LEFT_COLOR, 1.5f, 2);

            Vec3 rightLane = center.subtract(side.scale(PATH_HALF_WIDTH));
            dust(level, rightLane, RIGHT_COLOR, 1.5f, 2);

            Vec3 leftInner = center.add(side.scale(0.6));
            Vec3 rightInner = center.subtract(side.scale(0.6));
            level.sendParticles(ParticleTypes.END_ROD, leftInner.x, leftInner.y, leftInner.z,
                    1, 0.05, 0.03, 0.05, 0.001);
            level.sendParticles(ParticleTypes.END_ROD, rightInner.x, rightInner.y, rightInner.z,
                    1, 0.05, 0.03, 0.05, 0.001);

            if (((int) (distance * 10)) % 11 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x, center.y + 0.1, center.z,
                        2, 0.35, 0.05, 0.35, 0.02);
            }
        }

        Vec3 farEnd = base.add(forward.scale(PATH_LENGTH));
        level.sendParticles(ParticleTypes.END_ROD, farEnd.x, farEnd.y + 0.15, farEnd.z,
                4, 0.35, 0.08, 0.35, 0.02);
        level.sendParticles(ParticleTypes.ENCHANT, farEnd.x, farEnd.y, farEnd.z,
                5, 0.5, 0.15, 0.5, 0.1);
    }

    private static void dust(ServerLevel level, Vec3 pos, int argb, float size, int count) {
        level.sendParticles(new DustParticleOptions(argb, size), pos.x, pos.y, pos.z,
                count, 0.10, 0.03, 0.10, 0.0);
    }

    private static Vec3 horizontalDirection(ServerPlayer rider) {
        Vec3 look = rider.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() <= 0.0001) {
            return new Vec3(0, 0, 1);
        }
        return flat.normalize();
    }

    private static void actionbar(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color), true);
    }
}
