package com.istiak.equinox;

import com.istiak.equinox.command.EquinoxCommand;
import com.istiak.equinox.enchant.EnchantmentType;
import com.istiak.equinox.flight.FlightManager;
import com.istiak.equinox.items.EquinoxItems;
import com.istiak.equinox.mount.MountSavedData;
import com.istiak.equinox.whistle.WhistleHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;

public final class EquinoxMod implements ModInitializer {

    /**
     * Fabric Loader instantiates the entrypoint class itself, so a public
     * no-arg constructor is required (a private one crashes at launch with
     * "Could not execute entrypoint stage 'main'").
     */
    public EquinoxMod() {
    }

    public static final String MOD_ID = "equinox";

    private static FlightManager flightManager;
    private static WhistleHandler whistleHandler;
    private static EnchantScanner enchantScanner;
    private static long tickCounter = 0;

    /** Global server tick counter, read by the enchant scanner. */
    public static long serverTick() {
        return tickCounter;
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
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer serverPlayer
                    && world instanceof ServerLevel
                    && EquinoxItems.isWhistle(stack)) {
                whistleHandler.onWhistleUse(serverPlayer);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
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
            log("Equinox has been disabled.");
        });

        log("=================================");
        log("Equinox v2.0 (Fabric port)");
        log("Legendary Mount System Enabled!");
        log("Real Mount Chunk Recovery Enabled!");
        log("Pegasus Flight System Enabled!");
        log("=================================");
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;

        whistleHandler.tick(server);
        flightManager.tick(server);
        enchantScanner.tick(server);

        // Port of FlightListener.onToggleSneak (SHIFT = takeoff / land).
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            flightManager.handleSneak(player, player.isShiftKeyDown());
        }

        // Port of MountLocationListener + periodic save: every 5 seconds
        // refresh last-known for loaded registered mounts.
        if (tickCounter % 100L == 0L) {
            MountSavedData mounts = MountSavedData.get(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Horse horse = mounts.getLoadedMount(server, player);
                if (horse != null) {
                    mounts.updateLastKnown(horse);
                }
            }
        }
        // Flush mount data every 30 seconds.
        if (tickCounter % 600L == 0L) {
            MountSavedData.get(server).saveIfDirty();
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

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void log(String message) {
        System.out.println("[Equinox] " + message);
    }

    /**
     * Sends a prefixed chat message (port of MessageUtils.parse).
     */
    public static void sendPrefix(ServerPlayer player, String miniMessageLike) {
        player.sendSystemMessage(
                EquinoxMessages.parse("<gold><bold>Equinox</bold></gold> <dark_gray>»</dark_gray> "
                        + miniMessageLike),
                false);
    }
}
