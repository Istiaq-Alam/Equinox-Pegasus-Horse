package com.istiak.equinox;

import com.istiak.equinox.animations.MountAnimationManager;
import com.istiak.equinox.commands.EquinoxCommand;
import com.istiak.equinox.flight.FlightListener;
import com.istiak.equinox.flight.FlightManager;
import com.istiak.equinox.items.HorseArmorManager;
import com.istiak.equinox.items.WhistleManager;
import com.istiak.equinox.listeners.HorseArmorListener;
import com.istiak.equinox.listeners.HorseMovementListener;
import com.istiak.equinox.listeners.WhistleListener;
import com.istiak.equinox.mounts.MountManager;
import com.istiak.equinox.mounts.SummonManager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class EquinoxPlugin extends JavaPlugin {

    private HorseArmorManager horseArmorManager;

    private MountAnimationManager mountAnimationManager;

    private MountManager mountManager;

    private SummonManager summonManager;

    private HorseArmorListener horseArmorListener;

    private HorseMovementListener horseMovementListener;

    private WhistleManager whistleManager;

    private FlightManager flightManager;


    /*
     * ============================================================
     * ENABLE
     * ============================================================
     */

    @Override
    public void onEnable() {

        saveDefaultConfig();


        /*
         * ========================================================
         * INITIALIZE MANAGERS
         * ========================================================
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
         * ========================================================
         * REGISTER COMMAND
         * ========================================================
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
         * ========================================================
         * HORSE ARMOR SYSTEM
         * ========================================================
         */

        this.horseArmorListener =
                new HorseArmorListener(this);

        this.horseArmorListener.start();


        /*
         * ========================================================
         * HORSE MOVEMENT SYSTEM
         * ========================================================
         */

        this.horseMovementListener =
                new HorseMovementListener(this);

        this.horseMovementListener.start();


        /*
         * ========================================================
         * WHISTLE SYSTEM
         * ========================================================
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new WhistleListener(
                                this,
                                whistleManager
                        ),
                        this
                );


        /*
         * ========================================================
         * PEGASUS FLIGHT SYSTEM
         * ========================================================
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
                "Pegasus Flight System Enabled!"
        );

        getLogger().info(
                "================================="
        );
    }


    /*
     * ============================================================
     * DISABLE
     * ============================================================
     */

    @Override
    public void onDisable() {

        /*
         * Stop armor scanner.
         */

        if (horseArmorListener != null) {

            horseArmorListener.stop();
        }


        /*
         * Stop movement system.
         */

        if (horseMovementListener != null) {

            horseMovementListener.stop();
        }


        /*
         * Stop flight system.
         */

        if (flightManager != null) {

            flightManager.shutdown();
        }

        if (mountAnimationManager != null) {

            mountAnimationManager.shutdown();
        }


        /*
         * Save mounts.
         */

        if (mountManager != null) {

            mountManager.shutdown();
        }


        getLogger().info(
                "Equinox has been disabled."
        );
    }


    /*
     * ============================================================
     * GETTERS
     * ============================================================
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
