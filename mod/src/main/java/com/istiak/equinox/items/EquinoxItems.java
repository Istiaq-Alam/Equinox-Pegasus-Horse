package com.istiak.equinox.items;

import com.istiak.equinox.EquinoxMod;
import com.istiak.equinox.enchant.EnchantmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Port of HorseArmorManager + WhistleManager, extended with the
 * survival-progression items:
 *
 * - Five tiered Equinox armors (leather -> netherite) that are REAL items:
 *   found in loot chests, sold by villagers, upgradable to netherite via
 *   smithing, present in the creative inventory.
 * - Fifteen Equinox enchantment books (Swift / Titan Leap / Vitality, levels
 *   1-5): applied to Equinox armor in an anvil, found in loot chests and
 *   villager trades. They are NOT vanilla enchantments, so they can never
 *   appear on an enchanting table.
 *
 * The enchantment storage stays in the CUSTOM_DATA component
 * ("equinox_armor" flag + per-type level int), exactly like before, so
 * existing bound mounts stay compatible.
 */
public final class EquinoxItems {

    private EquinoxItems() {
    }

    // ======================================================================
    // REGISTERED ITEMS
    // ======================================================================

    public static Item LEATHER_EQUINOX_ARMOR;
    public static Item IRON_EQUINOX_ARMOR;
    public static Item GOLDEN_EQUINOX_ARMOR;
    public static Item DIAMOND_EQUINOX_ARMOR;
    public static Item NETHERITE_EQUINOX_ARMOR;

    /** Book items keyed by "<type>-<level>", e.g. "swift-3" (createBook lookup). */
    public static final Map<String, Item> ENCHANT_BOOKS = new java.util.HashMap<>();
    /** Reverse lookup: book item -> what it grants (no string parsing). */
    public static final Map<Item, BookEnchant> BOOK_ENCHANTS = new java.util.HashMap<>();

    private static Item book(EnchantmentType type, int level) {
        // Registry path uses underscores; EnchantmentType.getId() may contain '-'.
        String path = "equinox_" + type.getId().replace('-', '_') + "_book_" + level;
        Item.Properties props = new Item.Properties()
                .setId(key(path))
                .stacksTo(16)
                .rarity(Rarity.RARE)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .component(DataComponents.CUSTOM_NAME,
                        Component.literal("✦ Equinox Enchant Book ✦")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal(type.getDisplayName() + " " + toRoman(level))
                                .withStyle(ChatFormatting.AQUA),
                        Component.literal("Apply to Equinox Horse Armor in an anvil")
                                .withStyle(ChatFormatting.GRAY))));
        Item item = Registry.register(BuiltInRegistries.ITEM, key(path), new Item(props));
        ENCHANT_BOOKS.put(type.getId() + "-" + level, item);
        BOOK_ENCHANTS.put(item, new BookEnchant(type, level));
        return item;
    }

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, EquinoxMod.id(path));
    }

    // ======================================================================
    // REGISTRATION
    // ======================================================================

    public static void register() {
        LEATHER_EQUINOX_ARMOR = registerArmor("leather_equinox_armor",
                ArmorMaterials.LEATHER, Rarity.COMMON, false);
        IRON_EQUINOX_ARMOR = registerArmor("iron_equinox_armor",
                ArmorMaterials.IRON, Rarity.COMMON, false);
        GOLDEN_EQUINOX_ARMOR = registerArmor("golden_equinox_armor",
                ArmorMaterials.GOLD, Rarity.UNCOMMON, false);
        DIAMOND_EQUINOX_ARMOR = registerArmor("diamond_equinox_armor",
                ArmorMaterials.DIAMOND, Rarity.RARE, false);
        NETHERITE_EQUINOX_ARMOR = registerArmor("netherite_equinox_armor",
                ArmorMaterials.NETHERITE, Rarity.EPIC, true);

        // Spec: Swift / Titan Leap / Vitality books exist in levels 1-5 only
        // (Swift's attribute max level of 10 stays for the admin command).
        for (EnchantmentType type : EnchantmentType.values()) {
            int bookMax = Math.min(type.getMaxLevel(), 5);
            for (int level = 1; level <= bookMax; level++) {
                book(type, level);
            }
        }

        EquinoxMod.log("Registered Equinox items: 5 armors, "
                + ENCHANT_BOOKS.size() + " enchantment books.");
    }

    private static Item registerArmor(String path, ArmorMaterial material,
                                      Rarity rarity, boolean fireResistant) {
        // setId is required before construction: the Item constructor resolves
        // its description id from the properties ("Item id not set" otherwise).
        Item.Properties props = new Item.Properties()
                .setId(key(path))
                .horseArmor(material)
                .stacksTo(1)
                .rarity(rarity)
                .component(DataComponents.CUSTOM_NAME,
                        Component.literal("✦ Equinox Horse Armor ✦")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (fireResistant) {
            props.fireResistant();
        }
        return Registry.register(BuiltInRegistries.ITEM, key(path), new Item(props));
    }

    // ======================================================================
    // ARMOR FACTORY / IDENTITY
    // ======================================================================

    /** Wraps any registered Equinox armor item with its identity component + lore. */
    public static ItemStack createArmor(Item baseItem) {
        ItemStack item = new ItemStack(baseItem);

        CompoundTag data = customData(item);
        data.putBoolean("equinox_armor", true);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

        item.set(DataComponents.CUSTOM_NAME,
                Component.literal("✦ Equinox Horse Armor ✦")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        updateLore(item);
        return item;
    }

    /** Backwards-compatible overload for the /equinox armor give command. */
    public static ItemStack createArmorFromVanilla(ItemStack base) {
        Item equinox = vanillaToEquinox(base.getItem());
        if (equinox == null) {
            return ItemStack.EMPTY;
        }
        return createArmor(equinox);
    }

    public static Item vanillaToEquinox(Item vanilla) {
        if (vanilla == Items.LEATHER_HORSE_ARMOR) return LEATHER_EQUINOX_ARMOR;
        if (vanilla == Items.IRON_HORSE_ARMOR) return IRON_EQUINOX_ARMOR;
        if (vanilla == Items.GOLDEN_HORSE_ARMOR) return GOLDEN_EQUINOX_ARMOR;
        if (vanilla == Items.DIAMOND_HORSE_ARMOR) return DIAMOND_EQUINOX_ARMOR;
        if (vanilla == Items.NETHERITE_HORSE_ARMOR) return NETHERITE_EQUINOX_ARMOR;
        return null;
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
        whistle.set(DataComponents.LORE, new ItemLore(lore));

        CompoundTag data = new CompoundTag();
        data.putBoolean("equinox_whistle", true);
        whistle.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return whistle;
    }

    // ======================================================================
    // IDENTITY CHECKS
    // ======================================================================

    public static boolean isWhistle(ItemStack item) {
        if (item == null || !item.is(Items.GOAT_HORN)) return false;
        return customData(item).getBooleanOr("equinox_whistle", false);
    }

    /**
     * A registered Equinox armor item carrying the identity flag.
     * (The flag alone is the source of truth - loot, trades and the admin
     * command all produce flagged stacks.)
     */
    public static boolean isEquinoxArmor(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        return isEquinoxArmorItem(item.getItem());
    }

    /** Any stack of one of the five registered armor items (flag or not). */
    public static boolean isEquinoxArmorItem(Item item) {
        return item == LEATHER_EQUINOX_ARMOR || item == IRON_EQUINOX_ARMOR
                || item == GOLDEN_EQUINOX_ARMOR || item == DIAMOND_EQUINOX_ARMOR
                || item == NETHERITE_EQUINOX_ARMOR;
    }

    /** Legacy vanilla horse armor items (accepted only by the admin give command). */
    public static boolean isHorseArmor(ItemStack item) {
        return item != null && (
                item.is(Items.LEATHER_HORSE_ARMOR)
                        || item.is(Items.IRON_HORSE_ARMOR)
                        || item.is(Items.GOLDEN_HORSE_ARMOR)
                        || item.is(Items.DIAMOND_HORSE_ARMOR)
                        || item.is(Items.NETHERITE_HORSE_ARMOR)
        );
    }

    // ======================================================================
    // ENCHANTMENT BOOKS
    // ======================================================================

    /** The enchantment type + level a book grants, or null if not an Equinox book. */
    public static BookEnchant getBookEnchant(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        return BOOK_ENCHANTS.get(item.getItem());
    }

    public static ItemStack createBook(EnchantmentType type, int level) {
        Item item = ENCHANT_BOOKS.get(type.getId() + "-" + level);
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    public record BookEnchant(EnchantmentType type, int level) {
    }

    // ======================================================================
    // ENCHANTMENT STORAGE (unchanged format)
    // ======================================================================

    public static int getLevel(ItemStack item, EnchantmentType type) {
        if (!isEquinoxArmor(item)) return 0;
        return customData(item).getIntOr(type.getId(), 0);
    }

    public static boolean addEnchantment(ItemStack item, EnchantmentType type, int level) {
        if (!isEquinoxArmor(item)) return false;
        if (level < 1 || level > type.getMaxLevel()) return false;

        CompoundTag data = customData(item);
        data.putBoolean("equinox_armor", true);
        data.putInt(type.getId(), level);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        updateLore(item);
        return true;
    }

    public static boolean removeEnchantment(ItemStack item, EnchantmentType type) {
        if (!isEquinoxArmor(item)) return false;
        CompoundTag data = customData(item);
        data.remove(type.getId());
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
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
        item.set(DataComponents.LORE, new ItemLore(lore));
    }

    private static String toRoman(int number) {
        String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number >= 1 && number <= roman.length) return roman[number - 1];
        return String.valueOf(number);
    }

    public static CompoundTag customData(ItemStack item) {
        CustomData cd = item.get(DataComponents.CUSTOM_DATA);
        return cd == null ? new CompoundTag() : cd.copyTag();
    }
}
