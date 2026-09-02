package com.istiak.equinox.items;

import com.istiak.equinox.EquinoxPlugin;
import com.istiak.equinox.enchantments.EnchantmentType;
import com.istiak.equinox.utils.MessageUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;




public final class HorseArmorManager {

    private final EquinoxPlugin plugin;

    private final NamespacedKey armorKey;

    public HorseArmorManager(EquinoxPlugin plugin) {

        this.plugin = plugin;

        this.armorKey =
                new NamespacedKey(
                        plugin,
                        "equinox_horse_armor"
                );
    }

    public ItemStack createArmor(Material material) {

        if (!isHorseArmorMaterial(material)) {
            throw new IllegalArgumentException(
                    "Material is not horse armor: "
                            + material
            );
        }

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(
                armorKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.displayName(
                MessageUtils.plain(
                        "<gold><bold>✦ Equinox Horse Armor ✦</bold></gold>"
                )
        );

        item.setItemMeta(meta);

        updateLore(item);

        return item;
    }

    public boolean isEquinoxArmor(ItemStack item) {

        if (item == null
                || item.getType().isAir()) {

            return false;
        }

        if (!isHorseArmorMaterial(item.getType())) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return meta.getPersistentDataContainer().has(
                armorKey,
                PersistentDataType.BYTE
        );
    }

    public boolean isHorseArmorMaterial(Material material) {

    return material == Material.LEATHER_HORSE_ARMOR
            || material == Material.IRON_HORSE_ARMOR
            || material == Material.GOLDEN_HORSE_ARMOR
            || material == Material.DIAMOND_HORSE_ARMOR
            || material == Material.NETHERITE_HORSE_ARMOR;
}

    public NamespacedKey getEnchantmentKey(
            EnchantmentType type
    ) {

        return new NamespacedKey(
                plugin,
                "enchantment_" + type.getId()
        );
    }

    public int getLevel(
            ItemStack item,
            EnchantmentType type
    ) {

        if (!isEquinoxArmor(item)) {
            return 0;
        }

        PersistentDataContainer pdc =
                item.getItemMeta()
                        .getPersistentDataContainer();

        Integer level = pdc.get(
                getEnchantmentKey(type),
                PersistentDataType.INTEGER
        );

        return level == null ? 0 : level;
    }

    public boolean addEnchantment(
            ItemStack item,
            EnchantmentType type,
            int level
    ) {

        if (!isEquinoxArmor(item)) {
            return false;
        }

        int maxLevel = plugin.getConfig().getInt(
                type.getConfigPath() + ".max-level",
                1
        );

        if (level < 1 || level > maxLevel) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(
                getEnchantmentKey(type),
                PersistentDataType.INTEGER,
                level
        );

        item.setItemMeta(meta);

        updateLore(item);

        return true;
    }

    public boolean removeEnchantment(
            ItemStack item,
            EnchantmentType type
    ) {

        if (!isEquinoxArmor(item)) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().remove(
                getEnchantmentKey(type)
        );

        item.setItemMeta(meta);

        updateLore(item);

        return true;
    }

    public List<EnchantmentType> getEnchantments(
            ItemStack item
    ) {

        List<EnchantmentType> result =
                new ArrayList<>();

        for (EnchantmentType type
                : EnchantmentType.values()) {

            if (getLevel(item, type) > 0) {
                result.add(type);
            }
        }

        return result;
    }

    public void updateLore(ItemStack item) {

        if (!isEquinoxArmor(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        List<Component> lore =
                new ArrayList<>();

        lore.add(
                MessageUtils.plain(
                        "<dark_gray>━━━━━━━━━━━━━━━━━━</dark_gray>"
                )
        );

        boolean hasEnchantments = false;

        for (EnchantmentType type
                : EnchantmentType.values()) {

            int level = getLevel(item, type);

            if (level > 0) {

                hasEnchantments = true;

                lore.add(
                        MessageUtils.plain(
                                "<aqua>"
                                        + type.getDisplayName()
                                        + " "
                                        + toRoman(level)
                                        + "</aqua>"
                        )
                );
            }
        }

        if (!hasEnchantments) {

            lore.add(
                    MessageUtils.plain(
                            "<gray>No Equinox enchantments</gray>"
                    )
            );
        }

        lore.add(
                MessageUtils.plain(
                        "<dark_gray>━━━━━━━━━━━━━━━━━━</dark_gray>"
                )
        );

        lore.add(
                MessageUtils.plain(
                        "<gold>✦ Legendary Mount Equipment</gold>"
                )
        );

        meta.lore(lore);

        item.setItemMeta(meta);
    }

    private String toRoman(int number) {

        String[] roman = {
                "I",
                "II",
                "III",
                "IV",
                "V",
                "VI",
                "VII",
                "VIII",
                "IX",
                "X"
        };

        if (number >= 1
                && number <= roman.length) {

            return roman[number - 1];
        }

        return String.valueOf(number);
    }
}
