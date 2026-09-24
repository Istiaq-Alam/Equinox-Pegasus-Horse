package com.istiak.equinox;

import com.istiak.equinox.mount.MountSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

public final class EquinoxMod implements ModInitializer {

    public static final String MOD_ID = "equinox";
    public static final String MOD_NAME = "Equinox";

    private static FlightManager flightManager;
    private static WhistleHandler whistleHandler;
    private static EnchantScanner enchantScanner;

    private static long serverTickCounter = 0;

    public static long serverTick() {
        return serverTickCounter;
    }

    @Override
    public void onInitialize() {
        flightManager = new FlightManager();
        whistleHandler = new WhistleHandler();
        enchantScanner = new EnchantScanner();

        // ------------------------------------------------------------------
        // /equinox command tree (port of EquinoxCommand).
        // ------------------------------------------------------------------
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EquinoxCommand.register(dispatcher));

        // ------------------------------------------------------------------
        // Whistle use: right-click with an Equinox Whistle (port of
        // WhistleListener.onWhistleUse).
        // ------------------------------------------------------------------
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer serverPlayer
                    && world instanceof ServerLevel
                    && EquinoxItems.isWhistle(stack)) {
                whistleHandler.onWhistleUse(serverPlayer);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });

        // ------------------------------------------------------------------
        // Per-tick work: flight movement + visuals, whistle tasks, sneak
        // toggles, enchant attribute scan and last-known location sync.
        // ------------------------------------------------------------------
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // ------------------------------------------------------------------
        // Persist the mount registry on shutdown (port of MountManager.shutdown).
        // ------------------------------------------------------------------
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            MountSavedData.get(server).saveAll();
            whistleHandler.shutdown();
            EquinoxMod.log("Equinox has been disabled.");
        });

        log("=================================");
        log("Equinox v2.0 (Fabric port)");
        log("Legendary Mount System Enabled!");
        log("Real Mount Chunk Recovery Enabled!");
        log("Pegasus Flight System Enabled!");
        log("=================================");
    }

    private void onServerTick(MinecraftServer server) {
        serverTickCounter++;

        whistleHandler.tick(server);
        flightManager.tick(server);
        enchantScanner.tick(server);

        // Port of FlightListener sneak handling (SHIFT = takeoff/land) and
        // HorseMovement/MountLocation location tracking.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            flightManager.handleSneak(player, player.isShiftKeyDown());
        }

        // Every 5 seconds, refresh last-known for loaded registered mounts
        // (parity with MountLocationListener + periodic save).
        if (serverTickCounter % 100L == 0L) {
            MountSavedData mounts = MountSavedData.get(server);
            for (var player : server.getPlayerList().getPlayers()) {
                var horse = mounts.getLoadedMount(server, player);
                if (horse != null) {
                    mounts.updateLastKnown(server, horse);
                }
            }
        }
    }

    public static FlightManager getFlightManager() {
        return flightManager;
    }

    public static WhistleHandler getWhistleHandler() {
        return whistleHandler;
    }

    public static EnchantScanner getEnchantScanner() {
        return enchantScanner;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void log(String message) {
        System.out.println("[Equinox] " + message);
    }

    /** Sends a prefixed chat message (port of MessageUtils.parse). */
    public static void sendPrefix(ServerPlayer player, String miniMessageLike) {
        player.displayClientMessage(
                EquinoxMessages.parse("<gold><bold>Equinox</bold></gold> <dark_gray>»</dark_gray> "
                        + miniMessageLike),
                false);
    }
}
