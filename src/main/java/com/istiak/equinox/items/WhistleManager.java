package com.istiak.equinox.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.istiak.equinox.EquinoxPlugin;

import java.util.List;

public final class WhistleManager {

    private final EquinoxPlugin plugin;

    /*
     * Internal identifier for the Equinox Whistle.
     *
     * This means a normal Goat Horn renamed by a player
     * cannot accidentally become an Equinox Whistle.
     */
    private final NamespacedKey whistleKey;


    public WhistleManager(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;

        this.whistleKey =
                new NamespacedKey(
                        plugin,
                        "equinox_whistle"
                );
    }


    /*
     * ============================================================
     * CREATE EQUINOX WHISTLE
     * ============================================================
     */

    public ItemStack createWhistle() {

        ItemStack whistle =
                new ItemStack(
                        Material.GOAT_HORN
                );

        ItemMeta meta =
                whistle.getItemMeta();

        if (meta == null) {
            return whistle;
        }

        /*
         * Display name.
         */
        meta.displayName(
                Component.text(
                        "✦ Equinox Whistle",
                        NamedTextColor.LIGHT_PURPLE
                ).decoration(
                        TextDecoration.ITALIC,
                        false
                ).decoration(
                        TextDecoration.BOLD,
                        true
                )
        );


        /*
         * Lore.
         */
        meta.lore(
                List.of(

                        Component.text(
                                "A mystical horn bound to your mount.",
                                NamedTextColor.GRAY
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.text(
                                "",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "Right-click to call your",
                                NamedTextColor.WHITE
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.text(
                                "Equinox Mount.",
                                NamedTextColor.LIGHT_PURPLE
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),

                        Component.text(
                                "",
                                NamedTextColor.GRAY
                        ),

                        Component.text(
                                "✦ Bound to Equinox",
                                NamedTextColor.DARK_PURPLE
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        )
                )
        );


        /*
         * Persistent Data Container.
         *
         * Value = 1
         *
         * Only this item is recognized by
         * WhistleListener.
         */
        meta.getPersistentDataContainer()
                .set(
                        whistleKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );


        whistle.setItemMeta(meta);

        return whistle;
    }


    /*
     * ============================================================
     * IDENTIFICATION
     * ============================================================
     */

    public boolean isWhistle(
            ItemStack item
    ) {

        if (item == null
                || item.getType() != Material.GOAT_HORN) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer()
                        .get(
                                whistleKey,
                                PersistentDataType.BYTE
                        );

        return value != null
                && value == (byte) 1;
    }


    /*
     * ============================================================
     * WHISTLE KEY
     * ============================================================
     */

    public NamespacedKey getWhistleKey() {

        return whistleKey;
    }
}

