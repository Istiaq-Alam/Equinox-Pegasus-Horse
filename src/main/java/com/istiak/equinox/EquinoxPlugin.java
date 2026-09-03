package com.istiak.equinox;

import com.istiak.equinox.animations.MountAnimationManager;
import com.istiak.equinox.commands.EquinoxCommand;
import com.istiak.equinox.flight.FlightListener;
import com.istiak.equinox.flight.FlightManager;
import com.istiak.equinox.items.HorseArmorManager;
import com.istiak.equinox.items.WhistleManager;
import com.istiak.equinox.listeners.HorseArmorListener;
import com.istiak.equinox.listeners.HorseMovementListener;
import com.istiak.equinox.listeners.MountLocationListener;
import com.istiak.equinox.listeners.WhistleListener;
import com.istiak.equinox.mounts.MountManager;
import com.istiak.equinox.mounts.SummonManager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class EquinoxPlugin
        extends JavaPlugin {


    private HorseArmorManager horseArmorManager;

    private MountAnimationManager mountAnimationManager;

    private MountManager mountManager;

    private SummonManager summonManager;

    private HorseArmorListener horseArmorListener;

    private HorseMovementListener horseMovementListener;

    private WhistleManager whistleManager;

    private FlightManager flightManager;

    private WhistleListener whistleListener;


    /*
     * ========================================================
     * ENABLE
     * ========================================================
     */

    @Override
    public void onEnable() {

        saveDefaultConfig();


        /*
         * ====================================================
         * INITIALIZE MANAGERS
         * ====================================================
         */

        this.horseArmorManager =
                new HorseArmorManager(this);


        this.mountManager =
                new MountManager(this);


        this.summonManager =
                new SummonManager(this);


        this.whistleManager =
                new WhistleManager(this);


        this.flightManager =
                new FlightManager(this);


        this.mountAnimationManager =
                new MountAnimationManager(this);


        /*
         * ====================================================
         * COMMAND
         * ====================================================
         */

        PluginCommand command =
                Objects.requireNonNull(
                        getCommand("equinox"),
                        "Equinox command is missing from plugin.yml"
                );


        EquinoxCommand equinoxCommand =
                new EquinoxCommand(this);


        command.setExecutor(
                equinoxCommand
        );


        command.setTabCompleter(
                equinoxCommand
        );


        /*
         * ====================================================
         * HORSE ARMOR
         * ====================================================
         */

        this.horseArmorListener =
                new HorseArmorListener(this);


        this.horseArmorListener.start();


        /*
         * ====================================================
         * HORSE MOVEMENT
         * ====================================================
         */

        this.horseMovementListener =
                new HorseMovementListener(this);


        this.horseMovementListener.start();



        /*
         * ====================================================
         * WHISTLE
         * ====================================================
         */

        this.whistleListener =
                new WhistleListener(
                        this,
                        whistleManager
                );


        getServer()
                .getPluginManager()
                .registerEvents(
                        whistleListener,
                        this
                );


        /*
         * ====================================================
         * MOUNT LOCATION TRACKING
         * ====================================================
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new MountLocationListener(this),
                        this
                );


        /*
         * ====================================================
         * PEGASUS FLIGHT
         * ====================================================
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new FlightListener(this),
                        this
                );


        getLogger().info(
                "================================="
        );

        getLogger().info(
                "Equinox v"
                        + getPluginMeta().getVersion()
        );

        getLogger().info(
                "Legendary Mount System Enabled!"
        );

        getLogger().info(
                "Real Mount Chunk Recovery Enabled!"
        );

        getLogger().info(
                "Pegasus Flight System Enabled!"
        );

        getLogger().info(
                "================================="
        );
    }


    /*
     * ========================================================
     * DISABLE
     * ========================================================
     */

    @Override
    public void onDisable() {

        if (horseArmorListener != null) {

            horseArmorListener.stop();
        }


        if (horseMovementListener != null) {

            horseMovementListener.stop();
        }


        if (whistleListener != null) {

            whistleListener.shutdown();
        }


        if (flightManager != null) {

            flightManager.shutdown();
        }


        if (mountAnimationManager != null) {

            mountAnimationManager.shutdown();
        }


        /*
         * Save real horse locations before shutdown.
         */

        if (mountManager != null) {

            mountManager.shutdown();
        }


        getLogger().info(
                "Equinox has been disabled."
        );
    }


    /*
     * ========================================================
     * GETTERS
     * ========================================================
     */

    public HorseArmorManager getHorseArmorManager() {

        return horseArmorManager;
    }


    public MountManager getMountManager() {

        return mountManager;
    }


    public SummonManager getSummonManager() {

        return summonManager;
    }


    public WhistleManager getWhistleManager() {

        return whistleManager;
    }


    public FlightManager getFlightManager() {

        return flightManager;
    }


    public MountAnimationManager getMountAnimationManager() {

        return mountAnimationManager;
    }
}
