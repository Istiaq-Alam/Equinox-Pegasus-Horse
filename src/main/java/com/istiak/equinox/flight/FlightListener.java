package com.istiak.equinox.flight;

import com.istiak.equinox.EquinoxPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;

import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;


public final class FlightListener
        implements Listener {


    /*
     * ============================================================
     * FIELDS
     * ============================================================
     */

    private final EquinoxPlugin plugin;


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */

    public FlightListener(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ============================================================
     * SNEAK CONTROL
     * ============================================================
     *
     * SHIFT while riding:
     *
     * Ground -> Take off
     * Flying -> Land
     */

    @EventHandler(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
    )
    public void onToggleSneak(
            PlayerToggleSneakEvent event
    ) {

        /*
         * Only activate when the player STARTS sneaking.
         */
        if (!event.isSneaking()) {

            return;
        }


        Player player =
                event.getPlayer();


        Entity vehicle =
                player.getVehicle();


        /*
         * Player must be riding a horse.
         */
        if (!(vehicle instanceof Horse horse)) {

            return;
        }


        /*
         * ========================================================
         * CHECK REGISTERED MOUNT
         * ========================================================
         */

        if (!plugin.getMountManager()
                .isRegisteredMount(horse)) {

            return;
        }


        /*
         * ========================================================
         * CHECK OWNERSHIP
         * ========================================================
         */

        if (!plugin.getMountManager()
                .isOwner(
                        player,
                        horse
                )) {

            player.sendActionBar(
                    Component.text(
                            "This is not your Equinox mount.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        /*
         * ========================================================
         * CHECK EQUINOX ARMOR
         * ========================================================
         */

        if (!plugin.getHorseArmorManager()
                .isEquinoxArmor(
                        horse.getInventory()
                                .getArmor()
                )) {

            player.sendActionBar(
                    Component.text(
                            "Your mount needs Equinox Armor to fly.",
                            NamedTextColor.RED
                    )
            );

            return;
        }


        FlightManager flightManager =
                plugin.getFlightManager();


        /*
         * ========================================================
         * LAND
         * ========================================================
         */

        if (flightManager.isFlying(horse)) {

            event.setCancelled(true);


            boolean stopped =
                    flightManager.stopFlight(
                            player,
                            horse
                    );


            if (stopped) {

                player.sendActionBar(
                        Component.text(
                                "✦ Your Equinox mount is descending...",
                                NamedTextColor.AQUA
                        )
                );


                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_ENDER_DRAGON_FLAP,
                        0.8f,
                        0.8f
                );
            }


            return;
        }


        /*
         * ========================================================
         * TAKEOFF
         * ========================================================
         */

        event.setCancelled(true);


        boolean started =
                flightManager.startFlight(
                        player,
                        horse
                );


        if (started) {

            player.sendActionBar(
                    Component.text(
                            "✦ Your Equinox mount takes flight!",
                            NamedTextColor.LIGHT_PURPLE
                    )
            );


            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_FLAP,
                    1.0f,
                    1.25f
            );
        }
    }


    /*
     * ============================================================
     * PLAYER DISMOUNT
     * ============================================================
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onDismount(
            EntityDismountEvent event
    ) {

        if (!(event.getEntity()
                instanceof Player)) {

            return;
        }


        if (!(event.getDismounted()
                instanceof Horse horse)) {

            return;
        }


        if (plugin.getFlightManager()
                .isFlying(horse)) {

            plugin.getFlightManager()
                    .forceStopFlight(horse);
        }
    }


    /*
     * ============================================================
     * PLAYER QUIT
     * ============================================================
     */

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();


        Entity vehicle =
                player.getVehicle();


        if (vehicle instanceof Horse horse) {

            if (plugin.getFlightManager()
                    .isFlying(horse)) {

                plugin.getFlightManager()
                        .forceStopFlight(horse);
            }
        }
    }


    /*
     * ============================================================
     * HORSE DEATH
     * ============================================================
     */

    @EventHandler
    public void onHorseDeath(
            EntityDeathEvent event
    ) {

        if (!(event.getEntity()
                instanceof Horse horse)) {

            return;
        }


        plugin.getFlightManager()
                .forceStopFlight(horse);
    }


    /*
     * ============================================================
     * FALL DAMAGE PROTECTION
     * ============================================================
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onFallDamage(
            EntityDamageEvent event
    ) {

        if (!(event.getEntity()
                instanceof Horse horse)) {

            return;
        }


        if (event.getCause()
                != EntityDamageEvent.DamageCause.FALL) {

            return;
        }


        /*
         * Protect the horse while actively flying.
         */
        if (plugin.getFlightManager()
                .isFlying(horse)) {

            event.setCancelled(true);

            horse.setFallDistance(0.0f);
        }
    }
}
