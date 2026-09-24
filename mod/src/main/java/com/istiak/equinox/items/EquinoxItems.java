package com.istiak.equinox.items;

import com.istiak.equinox.EquinoxMod;
import com.istiak.equinox.enchant.EnchantmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of HorseArmorManager and WhistleManager.
 *
 * Uses vanilla custom_data components instead of the Bukkit
 * PersistentDataContainer:
 *   equinox_armor   = 1b
 *   equinox_whistle = 1b
 *   swift / titan-leap / vitality = level (int)
 */
public final class EquinoxItems {

    private EquinoxItems() {
    }

    public static ItemStack createArmor(ItemStack base) {
        ItemStack item = base.copy();
        item.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ Equinox Horse Armor ✦")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("No Equinox enchantments").withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("✦ Legendary Mount Equipment").withStyle(ChatFormatting.GOLD));
        item.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));

        updateLore(item);
        return item;
    }

    public static ItemStack createWhistle() {
        ItemStack whistle = new ItemStack(Items.GOAT_HORN);
        whistle.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ Equinox Whistle")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("A mystical horn bound to your mount.").withStyle(ChatFormatting.GRAY));
        lore.add(Component.empty());
        lore.add(Component.literal("Right-click to call your").withStyle(ChatFormatting.WHITE));
        lore.add(Component.literal("Equinox Mount.").withStyle(ChatFormatting.LIGHT_PURPLE));
        lore.add(Component.empty());
        lore.add(Component.literal("✦ Bound to Equinox").withStyle(ChatFormatting.DARK_PURPLE));
        whistle.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));

        CompoundTag data = new CompoundTag();
        data.putBoolean("equinox_whistle", true);
        whistle.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        return whistle;
    }

    public static boolean isWhistle(ItemStack item) {
        if (item == null || !item.is(Items.GOAT_HORN)) return false;
        CompoundTag data = customData(item);
        return data.contains("equinox_whistle") && data.getBoolean("equinox_whistle");
    }

    public static boolean isEquinoxArmor(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        if (!isHorseArmor(item)) return false;
        CompoundTag data = customData(item);
        return data.contains("equinox_armor") && data.getBoolean("equinox_armor");
    }

    public static boolean isHorseArmor(ItemStack item) {
        return item != null && (
                item.is(Items.LEATHER_HORSE_ARMOR)
                        || item.is(Items.IRON_HORSE_ARMOR)
                        || item.is(Items.GOLDEN_HORSE_ARMOR)
                        || item.is(Items.DIAMOND_HORSE_ARMOR)
                        || item.is(Items.NETHERITE_HORSE_ARMOR)
        );
    }

    public static int getLevel(ItemStack item, EnchantmentType type) {
        if (!isEquinoxArmor(item)) return 0;
        CompoundTag data = customData(item);
        if (!data.contains(type.getId())) return 0;
        return data.getInt(type.getId());
    }

    public static boolean addEnchantment(ItemStack item, EnchantmentType type, int level) {
        if (!isEquinoxArmor(item)) return false;
        if (level < 1 || level > type.getMaxLevel()) return false;

        CompoundTag data = customData(item);
        data.putBoolean("equinox_armor", true);
        data.putInt(type.getId(), level);
        item.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        updateLore(item);
        return true;
    }

    public static boolean removeEnchantment(ItemStack item, EnchantmentType type) {
        if (!isEquinoxArmor(item)) return false;
        CompoundTag data = customData(item);
        data.remove(type.getId());
        item.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        updateLore(item);
        return true;
    }

    public static void updateLore(ItemStack item) {
        if (!isEquinoxArmor(item)) return;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        boolean any = false;
        for (EnchantmentType type : EnchantmentType.values()) {
            int level = getLevel(item, type);
            if (level > 0) {
                any = true;
                lore.add(Component.literal(type.getDisplayName() + " " + toRoman(level))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        if (!any) {
            lore.add(Component.literal("No Equinox enchantments").withStyle(ChatFormatting.GRAY));
        }

        lore.add(Component.literal("━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));
        lore.add(Component.literal("✦ Legendary Mount Equipment").withStyle(ChatFormatting.GOLD));
        item.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
    }

    private static String toRoman(int number) {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number >= 1 && number <= roman.length) return roman[number - 1];
        return String.valueOf(number);
    }

    public static CompoundTag customData(ItemStack item) {
        net.minecraft.world.item.component.CustomData cd = item.get(DataComponents.CUSTOM_DATA);
        return cd == null ? new CompoundTag() : cd.copyTag();
    }
}
