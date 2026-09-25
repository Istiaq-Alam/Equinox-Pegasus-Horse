package com.istiak.equinox.items;

import com.istiak.equinox.enchant.EnchantmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pure logic for anvil-based Equinox enchanting (no menu access here -
 * AnvilMenuMixin supplies the slot containers and cost DataSlot).
 *
 * Left slot:  Equinox Horse Armor
 * Right slot: Equinox Enchant Book (Swift / Titan Leap / Vitality, level 1-5)
 * Result:     the armor carrying that enchantment.
 *
 * Rules (vanilla enchanted-book merge semantics):
 *  - A lower level than current is rejected.
 *  - The same level re-applies (no-op cost-wise cheaper); a higher level
 *    upgrades and costs a little extra.
 *  - The book is consumed by vanilla onTake (1 item from each input slot)
 *    and the XP cost is taken from the cost DataSlot the mixin sets.
 */
public final class EquinoxAnvilHandler {

    private EquinoxAnvilHandler() {
    }

    /** Base XP cost for applying a book. */
    public static final int ENCHANT_COST = 5;
    /** Extra XP cost when upgrading an existing lower level. */
    public static final int UPGRADE_EXTRA_COST = 2;

    /**
     * XP cost for this combination, or -1 when it is not an Equinox
     * enchant operation (vanilla logic should run).
     */
    public static int resultCost(ItemStack armor, ItemStack book) {
        EquinoxItems.BookEnchant be = EquinoxItems.getBookEnchant(book);
        if (be == null || be.type() == null) return -1;
        if (!EquinoxItems.isEquinoxArmor(armor)) return -1;

        int current = EquinoxItems.getLevel(armor, be.type());
        if (be.level() < current) return -1; // downgrades not allowed

        int cost = ENCHANT_COST;
        if (current > 0 && be.level() > current) {
            cost += UPGRADE_EXTRA_COST;
        }
        return cost;
    }

    /**
     * Builds the enchanted armor result, or null when the combination is not
     * applicable.
     */
    public static ItemStack buildResult(ItemStack armor, ItemStack book) {
        EquinoxItems.BookEnchant be = EquinoxItems.getBookEnchant(book);
        if (be == null || be.type() == null) return null;
        if (!EquinoxItems.isEquinoxArmor(armor)) return null;

        int current = EquinoxItems.getLevel(armor, be.type());
        if (be.level() < current) return null;

        ItemStack result = armor.copy();
        EquinoxItems.addEnchantment(result, be.type(), be.level());
        return result;
    }

    /** Sound + action-bar feedback when an enchantment is applied. */
    public static void playEnchantSound(Player player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.4f);
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(
                    "✦ The Equinox armor absorbs the enchantment.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }
}
