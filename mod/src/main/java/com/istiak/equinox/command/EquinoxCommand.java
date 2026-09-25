package com.istiak.equinox.command;

import com.istiak.equinox.EquinoxMessages;
import com.istiak.equinox.EquinoxMod;
import com.istiak.equinox.enchant.EnchantmentType;
import com.istiak.equinox.items.EquinoxItems;
import com.istiak.equinox.mount.MountData;
import com.istiak.equinox.mount.MountSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Port of EquinoxCommand (/equinox tree, /eq alias).
 *
 * Permission model (26.x has no per-node plugin permissions):
 *   - admin subcommands (armor/enchant/disenchant/whistle) require the
 *     COMMANDS_ADMIN permission (op level 2 equivalent).
 *   - mount bind/info/unbind are available to every player, like the plugin's
 *     equinox.use default-true permission.
 */
public final class EquinoxCommand {

    private static final SuggestionProvider<CommandSourceStack> ENCHANTS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    List.of("swift", "titan-leap", "vitality"), builder);

    private static final SuggestionProvider<CommandSourceStack> ARMOR_TYPES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    List.of("leather", "iron", "golden", "diamond", "netherite"), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = net.minecraft.commands.Commands.literal("equinox")
                .then(net.minecraft.commands.Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource())))
                .then(net.minecraft.commands.Commands.literal("armor")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(net.minecraft.commands.Commands.literal("give")
                                .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                                        .then(net.minecraft.commands.Commands.argument("type", StringArgumentType.word())
                                                .suggests(ARMOR_TYPES)
                                                .executes(EquinoxCommand::armorGive)))))
                .then(net.minecraft.commands.Commands.literal("enchant")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(net.minecraft.commands.Commands.argument("type", StringArgumentType.word())
                                .suggests(ENCHANTS)
                                .then(net.minecraft.commands.Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(EquinoxCommand::enchant))))
                .then(net.minecraft.commands.Commands.literal("disenchant")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(net.minecraft.commands.Commands.argument("type", StringArgumentType.word())
                                .suggests(ENCHANTS)
                                .executes(EquinoxCommand::disenchant)))
                .then(net.minecraft.commands.Commands.literal("mount")
                        .then(net.minecraft.commands.Commands.literal("bind")
                                .executes(ctx -> mountBind(ctx.getSource())))
                        .then(net.minecraft.commands.Commands.literal("info")
                                .executes(ctx -> mountInfo(ctx.getSource())))
                        .then(net.minecraft.commands.Commands.literal("unbind")
                                .executes(ctx -> mountUnbind(ctx.getSource()))))
                .then(net.minecraft.commands.Commands.literal("whistle")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                        .then(net.minecraft.commands.Commands.literal("give")
                                .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                                        .executes(EquinoxCommand::whistleGive))));

        dispatcher.register(root);

        // /eq alias
        dispatcher.register(net.minecraft.commands.Commands.literal("eq")
                .redirect(dispatcher.getRoot().getChild("equinox")));
    }

    private static int help(CommandSourceStack source) {
        send(source, "<gold><bold>⚡ EQUINOX ⚡</bold></gold>");
        send(source, "<gray>/equinox armor give <player> <type></gray>");
        send(source, "<gray>/equinox enchant <type> <level></gray>");
        send(source, "<gray>/equinox disenchant <type></gray>");
        send(source, "<gray>/equinox mount bind</gray>");
        send(source, "<gray>/equinox mount info</gray>");
        send(source, "<gray>/equinox mount unbind</gray>");
        send(source, "<gray>/equinox whistle give <player></gray>");
        return 1;
    }

    // ======================================================================
    // ARMOR GIVE
    // ======================================================================

    private static int armorGive(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String type = StringArgumentType.getString(ctx, "type").toLowerCase();

        ItemStack base = switch (type) {
            case "leather" -> EquinoxItems.createArmor(EquinoxItems.LEATHER_EQUINOX_ARMOR);
            case "iron" -> EquinoxItems.createArmor(EquinoxItems.IRON_EQUINOX_ARMOR);
            case "gold", "golden" -> EquinoxItems.createArmor(EquinoxItems.GOLDEN_EQUINOX_ARMOR);
            case "diamond" -> EquinoxItems.createArmor(EquinoxItems.DIAMOND_EQUINOX_ARMOR);
            case "netherite" -> EquinoxItems.createArmor(EquinoxItems.NETHERITE_EQUINOX_ARMOR);
            default -> null;
        };

        if (base == null) {
            send(source, "<red>Available types: leather, iron, golden, diamond, netherite</red>");
            return 0;
        }

        target.getInventory().add(base);
        send(source, "<green>Equinox Horse Armor given to " + target.getName().getString() + "!</green>");
        EquinoxMod.sendPrefix(target, "<gold>You received Equinox Horse Armor!</gold>");
        return 1;
    }

    // ======================================================================
    // ENCHANT / DISENCHANT
    // ======================================================================

    private static int enchant(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        EnchantmentType type = EnchantmentType.fromString(StringArgumentType.getString(ctx, "type"));
        int level = IntegerArgumentType.getInteger(ctx, "level");

        if (type == null) {
            EquinoxMod.sendPrefix(player, "<red>Unknown enchantment.</red>");
            return 0;
        }

        ItemStack item = player.getMainHandItem();
        if (!EquinoxItems.isEquinoxArmor(item)) {
            EquinoxMod.sendPrefix(player, "<red>Hold Equinox Horse Armor in your main hand.</red>");
            return 0;
        }

        if (!EquinoxItems.addEnchantment(item, type, level)) {
            EquinoxMod.sendPrefix(player, "<red>Level must be between 1 and " + type.getMaxLevel() + ".</red>");
            return 0;
        }

        EquinoxMod.sendPrefix(player, "<green>" + type.getDisplayName() + " " + level + " added!</green>");
        return 1;
    }

    private static int disenchant(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        EnchantmentType type = EnchantmentType.fromString(StringArgumentType.getString(ctx, "type"));
        if (type == null) {
            EquinoxMod.sendPrefix(player, "<red>Unknown enchantment.</red>");
            return 0;
        }

        ItemStack item = player.getMainHandItem();
        if (!EquinoxItems.isEquinoxArmor(item)) {
            EquinoxMod.sendPrefix(player, "<red>Hold Equinox Horse Armor first.</red>");
            return 0;
        }

        EquinoxItems.removeEnchantment(item, type);
        EquinoxMod.sendPrefix(player, "<green>" + type.getDisplayName() + " removed.</green>");
        return 1;
    }

    // ======================================================================
    // MOUNT BIND / INFO / UNBIND
    // ======================================================================

    private static int mountBind(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        // Entity raycast within 6 blocks (port of Bukkit's getTargetEntity(6)).
        // Entity.pick() only raycasts BLOCKS, so we clip the look ray against
        // nearby horses' bounding boxes ourselves.
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 view = player.getViewVector(1.0f);
        net.minecraft.world.phys.Vec3 end = eye.add(view.scale(6.0));
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox()
                .expandTowards(view.scale(6.0))
                .inflate(1.0);

        Entity picked = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity candidate : player.level().getEntities(player, searchBox,
                e -> e instanceof Horse && e.isAlive())) {
            var hit = candidate.getBoundingBox().clip(eye, end);
            if (hit.isPresent()) {
                double dist = eye.distanceToSqr(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    picked = candidate;
                }
            }
        }

        if (!(picked instanceof Horse horse)) {
            EquinoxMod.sendPrefix(player, "<red>You must look directly at your horse.</red>");
            EquinoxMod.sendPrefix(player, "<gray>Stand close to your horse and try again.</gray>");
            return 0;
        }

        if (!horse.isTamed()) {
            EquinoxMod.sendPrefix(player, "<red>This horse is not tamed.</red>");
            return 0;
        }

        // Ownership: the tamed horse's owner must be this player.
        var ownerRef = horse.getOwnerReference();
        if (ownerRef == null || !player.getUUID().equals(ownerRef.getUUID())) {
            EquinoxMod.sendPrefix(player, "<red>You do not own this horse.</red>");
            return 0;
        }

        if (!EquinoxItems.isEquinoxArmor(horse.getItemBySlot(EquipmentSlot.BODY))) {
            EquinoxMod.sendPrefix(player, "<red>Your horse must wear Equinox Horse Armor.</red>");
            return 0;
        }

        MountSavedData mounts = MountSavedData.get(player.level().getServer());
        if (!mounts.register(player, horse)) {
            EquinoxMod.sendPrefix(player, "<red>Could not bind this horse.</red>");
            return 0;
        }

        boolean gaveWhistle = giveWhistleIfMissing(player);

        EquinoxMod.sendPrefix(player, "<gold><bold>✦ EQUINOX MOUNT BOUND ✦</bold></gold>");
        EquinoxMod.sendPrefix(player, "<green>Your horse has been successfully bound!</green>");
        String name = horse.hasCustomName() ? horse.getCustomName().getString() : "Equinox Mount";
        EquinoxMod.sendPrefix(player, "<gray>Mount: <white>" + name + "</white></gray>");
        if (gaveWhistle) {
            EquinoxMod.sendPrefix(player, "<light_purple>✦ You received an Equinox Whistle!</light_purple>");
        }
        EquinoxMod.sendPrefix(player, "<dark_gray>You can now use your Equinox Mount system.</dark_gray>");
        return 1;
    }

    /**
     * Binding a mount automatically provides the whistle needed to call it.
     * Players who already carry one are not given duplicates.
     */
    private static boolean giveWhistleIfMissing(ServerPlayer player) {
        if (player.getInventory().contains(EquinoxItems::isWhistle)) {
            return false;
        }
        player.getInventory().add(EquinoxItems.createWhistle());
        return true;
    }

    private static int mountInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        MountSavedData mounts = MountSavedData.get(player.level().getServer());
        MountData data = mounts.get(player.getUUID());
        if (data == null) {
            EquinoxMod.sendPrefix(player, "<red>You do not have a registered Equinox mount.</red>");
            return 0;
        }

        EquinoxMod.sendPrefix(player, "<gold><bold>✦ EQUINOX MOUNT INFO ✦</bold></gold>");
        EquinoxMod.sendPrefix(player, "<gray>Name: <white>" + data.getHorseName() + "</white></gray>");
        EquinoxMod.sendPrefix(player, "<gray>Status: <green>Registered</green></gray>");
        EquinoxMod.sendPrefix(player, "<gray>Horse ID: <dark_gray>" + data.getHorseId() + "</dark_gray></gray>");
        return 1;
    }

    private static int mountUnbind(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        MountSavedData mounts = MountSavedData.get(player.level().getServer());
        MountData data = mounts.get(player.getUUID());
        if (data == null) {
            EquinoxMod.sendPrefix(player, "<red>You do not have a registered Equinox mount.</red>");
            return 0;
        }

        if (mounts.unregister(data.getHorseId())) {
            EquinoxMod.sendPrefix(player, "<green>Your Equinox mount has been unbound.</green>");
        } else {
            EquinoxMod.sendPrefix(player, "<red>Could not unbind your mount.</red>");
        }
        return 1;
    }

    // ======================================================================
    // WHISTLE GIVE
    // ======================================================================

    private static int whistleGive(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        target.getInventory().add(EquinoxItems.createWhistle());
        send(source, "<green>Equinox Whistle given to " + target.getName().getString() + "!</green>");
        EquinoxMod.sendPrefix(target, "<light_purple>✦ You received an Equinox Whistle!</light_purple>");
        return 1;
    }

    // ======================================================================
    // HELPERS
    // ======================================================================

    private static void send(CommandSourceStack source, String mini) {
        source.sendSuccess(() -> EquinoxMessages.parse(mini), false);
    }

    private EquinoxCommand() {
    }
}
