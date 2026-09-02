package com.istiak.equinox.commands;

import com.istiak.equinox.EquinoxPlugin;
import com.istiak.equinox.enchantments.EnchantmentType;
import com.istiak.equinox.items.HorseArmorManager;
import com.istiak.equinox.mounts.MountData;
import com.istiak.equinox.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public final class EquinoxCommand
        implements CommandExecutor, TabCompleter {

    private final EquinoxPlugin plugin;


    public EquinoxCommand(
            EquinoxPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /*
     * ============================================================
     * MAIN COMMAND
     * ============================================================
     */

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            sendHelp(sender);

            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help" -> {

                sendHelp(sender);

                return true;
            }

            case "armor" -> {

                return handleArmor(
                        sender,
                        args
                );
            }

            case "enchant" -> {

                return handleEnchant(
                        sender,
                        args
                );
            }

            case "disenchant" -> {

                return handleDisenchant(
                        sender,
                        args
                );
            }

            case "mount" -> {

                return handleMount(
                        sender,
                        args
                );
            }

            case "whistle" -> {

                return handleWhistle(
                        sender,
                        args
                );
            }

            default -> {

                sendHelp(sender);

                return true;
            }
        }
    }


    /*
     * ============================================================
     * ARMOR COMMAND
     * ============================================================
     */

    private boolean handleArmor(
            CommandSender sender,
            String[] args
    ) {

        if (!sender.hasPermission(
                "equinox.admin"
        )) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<red>You do not have permission.</red>"
                    )
            );

            return true;
        }

        if (args.length < 4
                || !args[1].equalsIgnoreCase("give")) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<yellow>Usage: /equinox armor give <player> <type></yellow>"
                    )
            );

            return true;
        }

        Player target =
                plugin.getServer()
                        .getPlayer(args[2]);

        if (target == null) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<red>Player not found.</red>"
                    )
            );

            return true;
        }

        Material material =
                getHorseArmorMaterial(
                        args[3]
                );

        if (material == null) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<red>Available types: leather, iron, golden, diamond, netherite</red>"
                    )
            );

            return true;
        }

        HorseArmorManager manager =
                plugin.getHorseArmorManager();

        ItemStack armor =
                manager.createArmor(material);

        target.getInventory().addItem(armor);

        sender.sendMessage(
                MessageUtils.parse(
                        "<green>Equinox Horse Armor given to "
                                + target.getName()
                                + "!</green>"
                )
        );

        target.sendMessage(
                MessageUtils.parse(
                        "<gold>You received Equinox Horse Armor!</gold>"
                )
        );

        return true;
    }


    /*
     * ============================================================
     * ENCHANT COMMAND
     * ============================================================
     */

    private boolean handleEnchant(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<red>This command must be used by a player.</red>"
                    )
            );

            return true;
        }

        if (!player.hasPermission(
                "equinox.enchant"
        )) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>You do not have permission.</red>"
                    )
            );

            return true;
        }

        if (args.length < 3) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<yellow>Usage: /equinox enchant <type> <level></yellow>"
                    )
            );

            return true;
        }

        EnchantmentType type =
                EnchantmentType.fromString(
                        args[1]
                );

        if (type == null) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Unknown enchantment.</red>"
                    )
            );

            return true;
        }

        int level;

        try {

            level = Integer.parseInt(
                    args[2]
            );

        } catch (NumberFormatException exception) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Level must be a number.</red>"
                    )
            );

            return true;
        }

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        HorseArmorManager manager =
                plugin.getHorseArmorManager();

        if (!manager.isEquinoxArmor(item)) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Hold Equinox Horse Armor in your main hand.</red>"
                    )
            );

            return true;
        }

        boolean success =
                manager.addEnchantment(
                        item,
                        type,
                        level
                );

        if (!success) {

            int maxLevel =
                    plugin.getConfig().getInt(
                            type.getConfigPath()
                                    + ".max-level"
                    );

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Level must be between 1 and "
                                    + maxLevel
                                    + ".</red>"
                    )
            );

            return true;
        }

        player.sendMessage(
                MessageUtils.parse(
                        "<green>"
                                + type.getDisplayName()
                                + " "
                                + level
                                + " added!</green>"
                )
        );

        return true;
    }


    /*
     * ============================================================
     * DISENCHANT COMMAND
     * ============================================================
     */

    private boolean handleDisenchant(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<red>This command must be used by a player.</red>"
                    )
            );

            return true;
        }

        if (!player.hasPermission(
                "equinox.enchant"
        )) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>You do not have permission.</red>"
                    )
            );

            return true;
        }

        if (args.length < 2) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<yellow>Usage: /equinox disenchant <type></yellow>"
                    )
            );

            return true;
        }

        EnchantmentType type =
                EnchantmentType.fromString(
                        args[1]
                );

        if (type == null) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Unknown enchantment.</red>"
                    )
            );

            return true;
        }

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        HorseArmorManager manager =
                plugin.getHorseArmorManager();

        if (!manager.isEquinoxArmor(item)) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Hold Equinox Horse Armor first.</red>"
                    )
            );

            return true;
        }

        manager.removeEnchantment(
                item,
                type
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<green>"
                                + type.getDisplayName()
                                + " removed.</green>"
                )
        );

        return true;
    }


    /*
     * ============================================================
     * MOUNT COMMAND
     * ============================================================
     */

    private boolean handleMount(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    MessageUtils.parse(
                            "<red>This command must be used by a player.</red>"
                    )
            );

            return true;
        }

        if (args.length < 2) {

            sendMountHelp(player);

            return true;
        }

        switch (args[1].toLowerCase()) {

            case "bind" -> {

                return handleMountBind(player);
            }

            case "info" -> {

                return handleMountInfo(player);
            }

            case "unbind" -> {

                return handleMountUnbind(player);
            }

            default -> {

                sendMountHelp(player);

                return true;
            }
        }
    }


    /*
     * ============================================================
     * BIND MOUNT
     * ============================================================
     */

    private boolean handleMountBind(Player player) {

        /*
         * Look for a horse within 6 blocks.
         */
        Entity target =
                player.getTargetEntity(6);

        if (!(target instanceof Horse horse)) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>You must look directly at your horse.</red>"
                    )
            );

            player.sendMessage(
                    MessageUtils.parse(
                            "<gray>Stand close to your horse and try again.</gray>"
                    )
            );

            return true;
        }

        /*
         * Check if the horse is tamed.
         */
        if (!horse.isTamed()) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>This horse is not tamed.</red>"
                    )
            );

            return true;
        }

        /*
         * Check ownership.
         */
        if (horse.getOwner() == null
                || !horse.getOwner()
                .getUniqueId()
                .equals(player.getUniqueId())) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>You do not own this horse.</red>"
                    )
            );

            return true;
        }

        /*
         * Check if horse has Equinox armor.
         */
        ItemStack armor =
                horse.getInventory()
                        .getArmor();

        if (!plugin.getHorseArmorManager()
                .isEquinoxArmor(armor)) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Your horse must wear Equinox Horse Armor.</red>"
                    )
            );

            return true;
        }

        /*
         * Register the horse.
         */
        boolean success =
                plugin.getMountManager()
                        .registerMount(
                                player,
                                horse
                        );

        if (!success) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Could not bind this horse.</red>"
                    )
            );

            return true;
        }

        /*
         * Success messages.
         */
        player.sendMessage(
                MessageUtils.parse(
                        "<gold><bold>✦ EQUINOX MOUNT BOUND ✦</bold></gold>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<green>Your horse has been successfully bound!</green>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>Mount: <white>"
                                + getHorseDisplayName(horse)
                                + "</white></gray>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<dark_gray>You can now use your Equinox Mount system.</dark_gray>"
                )
        );

        return true;
    }



        /* Whistle Code */
private boolean handleWhistle(
        CommandSender sender,
        String[] args
) {

    /*
     * Permission check.
     */
    if (!sender.hasPermission(
            "equinox.admin"
    )) {

        sender.sendMessage(
                MessageUtils.parse(
                        "<red>You do not have permission.</red>"
                )
        );

        return true;
    }


    /*
     * Usage:
     *
     * /equinox whistle give <player>
     */
    if (args.length < 3
            || !args[1].equalsIgnoreCase("give")) {

        sender.sendMessage(
                MessageUtils.parse(
                        "<yellow>Usage: /equinox whistle give <player></yellow>"
                )
        );

        return true;
    }


    /*
     * Find target player.
     */
    Player target =
            plugin.getServer()
                    .getPlayer(
                            args[2]
                    );


    if (target == null) {

        sender.sendMessage(
                MessageUtils.parse(
                        "<red>Player not found.</red>"
                )
        );

        return true;
    }


    /*
     * Create whistle.
     */
    ItemStack whistle =
            plugin.getWhistleManager()
                    .createWhistle();


    /*
     * Give item.
     */
    target.getInventory()
            .addItem(
                    whistle
            );


    /*
     * Admin message.
     */
    sender.sendMessage(
            MessageUtils.parse(
                    "<green>Equinox Whistle given to "
                            + target.getName()
                            + "!</green>"
            )
    );


    /*
     * Player message.
     */
    target.sendMessage(
            MessageUtils.parse(
                    "<light_purple>✦ You received an Equinox Whistle!</light_purple>"
            )
    );


    return true;
}

    /*
     * ============================================================
     * MOUNT INFO
     * ============================================================
     */

    private boolean handleMountInfo(Player player) {

        if (!plugin.getMountManager()
                .hasMount(player)) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>You do not have a registered Equinox mount.</red>"
                    )
            );

            return true;
        }

        MountData mountData =
                plugin.getMountManager()
                        .getMount(player);

        player.sendMessage(
                MessageUtils.parse(
                        "<gold><bold>✦ EQUINOX MOUNT INFO ✦</bold></gold>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>Name: <white>"
                                + mountData.getHorseName()
                                + "</white></gray>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>Status: <green>Registered</green></gray>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>Horse ID: <dark_gray>"
                                + mountData.getHorseId()
                                + "</dark_gray></gray>"
                )
        );

        return true;
    }


    /*
     * ============================================================
     * UNBIND MOUNT
     * ============================================================
     */

    private boolean handleMountUnbind(Player player) {

        if (!plugin.getMountManager()
                .hasMount(player)) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>You do not have a registered Equinox mount.</red>"
                    )
            );

            return true;
        }

        MountData mountData =
                plugin.getMountManager()
                        .getMount(player);

        boolean success =
                plugin.getMountManager()
                        .unregisterMount(
                                mountData.getHorseId()
                        );

        if (success) {

            player.sendMessage(
                    MessageUtils.parse(
                            "<green>Your Equinox mount has been unbound.</green>"
                    )
            );

        } else {

            player.sendMessage(
                    MessageUtils.parse(
                            "<red>Could not unbind your mount.</red>"
                    )
            );
        }

        return true;
    }


    /*
     * ============================================================
     * HORSE DISPLAY NAME
     * ============================================================
     */

    private String getHorseDisplayName(Horse horse) {

        if (horse.getCustomName() != null) {

            return horse.getCustomName();
        }

        return "Equinox Mount";
    }


    /*
     * ============================================================
     * GET HORSE ARMOR MATERIAL
     * ============================================================
     */

    private Material getHorseArmorMaterial(
            String input
    ) {

        return switch (
                input.toLowerCase()
        ) {

            case "leather" ->
                    Material.LEATHER_HORSE_ARMOR;

            case "iron" ->
                    Material.IRON_HORSE_ARMOR;

            case "gold",
                 "golden" ->
                    Material.GOLDEN_HORSE_ARMOR;

            case "diamond" ->
                    Material.DIAMOND_HORSE_ARMOR;

            case "netherite" ->
                    Material.NETHERITE_HORSE_ARMOR;

            default -> null;
        };
    }


    /*
     * ============================================================
     * MAIN HELP
     * ============================================================
     */

    private void sendHelp(
            CommandSender sender
    ) {

        sender.sendMessage(
                MessageUtils.plain(
                        "<gold><bold>⚡ EQUINOX ⚡</bold></gold>"
                )
        );

        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox armor give <player> <type></gray>"
                )
        );

        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox enchant <type> <level></gray>"
                )
        );

        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox disenchant <type></gray>"
                )
        );

        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox mount bind</gray>"
                )
        );

        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox mount info</gray>"
                )
        );

        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox mount unbind</gray>"
                )
        );
        sender.sendMessage(
                MessageUtils.plain(
                        "<gray>/equinox whistle give <player></gray>"
                )
        );
    }


    /*
     * ============================================================
     * MOUNT HELP
     * ============================================================
     */

    private void sendMountHelp(Player player) {

        player.sendMessage(
                MessageUtils.parse(
                        "<gold><bold>✦ EQUINOX MOUNT COMMANDS ✦</bold></gold>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>/equinox mount bind</gray>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>/equinox mount info</gray>"
                )
        );

        player.sendMessage(
                MessageUtils.parse(
                        "<gray>/equinox mount unbind</gray>"
                )
        );
    }


    /*
     * ============================================================
     * TAB COMPLETION
     * ============================================================
     */

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        /*
         * First command argument.
         */
        if (args.length == 1) {

            return filter(
                    args[0],
                    Arrays.asList(
                            "help",
                            "armor",
                            "enchant",
                            "disenchant",
                            "mount",
                            "whistle"
                    )
            );
        }

        /*
         * Second command argument.
         */
        if (args.length == 2) {

            if (args[0].equalsIgnoreCase("armor")) {

                return filter(
                        args[1],
                        Collections.singletonList("give")
                );
            }

            if (args[0].equalsIgnoreCase("enchant")
                    || args[0].equalsIgnoreCase("disenchant")) {

                List<String> enchantments =
                        new ArrayList<>();

                for (EnchantmentType type
                        : EnchantmentType.values()) {

                    enchantments.add(
                            type.getId()
                    );
                }

                return filter(
                        args[1],
                        enchantments
                );
            }

            if (args[0].equalsIgnoreCase("mount")) {

                return filter(
                        args[1],
                        Arrays.asList(
                                "bind",
                                "info",
                                "unbind"
                        )
                );
            }

            if (args[0].equalsIgnoreCase("whistle")) {

                return filter(
                        args[1],
                        Collections.singletonList("give")
                );
            }
        }

        /*
         * Armor types.
         */
        if (args.length == 4
                && args[0].equalsIgnoreCase("armor")
                && args[1].equalsIgnoreCase("give")) {

            return filter(
                    args[3],
                    Arrays.asList(
                            "leather",
                            "iron",
                            "golden",
                            "diamond",
                            "netherite"
                    )
            );
        }

        return Collections.emptyList();
    }


    /*
     * ============================================================
     * TAB FILTER
     * ============================================================
     */

    private List<String> filter(
            String input,
            List<String> values
    ) {

        String lower =
                input.toLowerCase();

        return values.stream()
                .filter(
                        value ->
                                value.startsWith(lower)
                )
                .toList();
    }
}
