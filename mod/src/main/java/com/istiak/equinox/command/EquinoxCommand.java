package com.istiak.equinox.command;

import com.istiak.equinox.EnchantScanner;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Port of EquinoxCommand (/equinox tree with aliases /eq via literal registration).
 */
public final class EquinoxCommand {

    private static final SuggestionProvider<CommandSourceStack> ENCHANTS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    List.of("swift", "titan-leap", "vitality"), builder);

    private static final SuggestionProvider<CommandSourceStack> ARMOR_TYPES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    List.of("leather", "iron", "golden", "diamond", "netherite"), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("equinox")
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("armor")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(ARMOR_TYPES)
                                                .executes(ctx -> armorGive(ctx))))))
                .then(Commands.literal("enchant")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(ENCHANTS)
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> enchant(ctx)))))
                .then(Commands.literal("disenchant")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(ENCHANTS)
                                .executes(ctx -> disenchant(ctx))))
                .then(Commands.literal("mount")
                        .then(Commands.literal("bind").executes(ctx -> mountBind(ctx)))
                        .then(Commands.literal("info").executes(ctx -> mountInfo(ctx)))
                        .then(Commands.literal("unbind").executes(ctx -> mountUnbind(ctx))))
                .then(Commands.literal("whistle")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> whistleGive(ctx)))))
        );

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
            case "leather" -> new ItemStack(net.minecraft.world.item.Items.LEATHER_HORSE_ARMOR);
            case "iron" -> new ItemStack(net.minecraft.world.item.Items.IRON_HORSE_ARMOR);
            case "gold", "golden" -> new ItemStack(net.minecraft.world.item.Items.GOLDEN_HORSE_ARMOR);
            case "diamond" -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR);
            case "netherite" -> new ItemStack(net.minecraft.world.item.Items.NETHERITE_HORSE_ARMOR);
            default -> null;
        };

        if (base == null) {
            send(source, "<red>Available types: leather, iron, golden, diamond, netherite</red>");
            return 0;
        }

        target.getInventory().add(EquinoxItems.createArmor(base));
        send(source, "<green>Equinox Horse Armor given to " + target.getGameProfile().getName() + "!</green>");
        EquinoxMod.sendPrefix(target, "<gold>You received Equinox Horse Armor!</gold>");
        return 1;
    }

    // ======================================================================
    // ENCHANT / DISENCHANT
    // ======================================================================

    private static int enchant(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        String typeId = StringArgumentType.getString(ctx, "type");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        EnchantmentType type = EnchantmentType.fromString(typeId);

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
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be used by a player."));
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

    private static int mountBind(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        // Look for a horse within ~6 blocks (plugin used getTargetEntity(6)).
        Entity picked = player.pick(6.0, 1.0f, false).getType() == net.minecraft.world.phys.HitResult.Type.ENTITY
                ? ((net.minecraft.world.phys.EntityHitResult) player.pick(6.0, 1.0f, false)).getEntity()
                : null;

        if (!(picked instanceof Horse horse)) {
            EquinoxMod.sendPrefix(player, "<red>You must look directly at your horse.</red>");
            EquinoxMod.sendPrefix(player, "<gray>Stand close to your horse and try again.</gray>");
            return 0;
        }

        if (!horse.isTamed()) {
            EquinoxMod.sendPrefix(player, "<red>This horse is not tamed.</red>");
            return 0;
        }

        if (!horse.isOwnedBy(player)) {
            EquinoxMod.sendPrefix(player, "<red>You do not own this horse.</red>");
            return 0;
        }

        if (!EquinoxItems.isEquinoxArmor(horse.getBodyArmorItem())) {
            EquinoxMod.sendPrefix(player, "<red>Your horse must wear Equinox Horse Armor.</red>");
            return 0;
        }

        MountSavedData mounts = MountSavedData.get(player.server);
        if (!mounts.register(player, horse)) {
            EquinoxMod.sendPrefix(player, "<red>Could not bind this horse.</red>");
            return 0;
        }

        EquinoxMod.sendPrefix(player, "<gold><bold>✦ EQUINOX MOUNT BOUND ✦</bold></gold>");
        EquinoxMod.sendPrefix(player, "<green>Your horse has been successfully bound!</green>");
        String name = horse.hasCustomName() ? horse.getCustomName().getString() : "Equinox Mount";
        EquinoxMod.sendPrefix(player, "<gray>Mount: <white>" + name + "</white></gray>");
        EquinoxMod.sendPrefix(player, "<dark_gray>You can now use your Equinox Mount system.</dark_gray>");
        return 1;
    }

    private static int mountInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        CommandSourceStack source = ctx.getSource();
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        MountSavedData mounts = MountSavedData.get(player.server);
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

    private static int mountUnbind(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        CommandSourceStack source = ctx.getSource();
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        MountSavedData mounts = MountSavedData.get(player.server);
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
        send(source, "<green>Equinox Whistle given to " + target.getGameProfile().getName() + "!</green>");
        EquinoxMod.sendPrefix(target, "<light_purple>✦ You received an Equinox Whistle!</light_purple>");
        return 1;
    }

    // ======================================================================
    // HELPERS
    // ======================================================================

    private static void send(CommandSourceStack source, String mini) {
        source.sendSuccess(() -> EquinoxMessages.parse(mini), false);
    }
}
