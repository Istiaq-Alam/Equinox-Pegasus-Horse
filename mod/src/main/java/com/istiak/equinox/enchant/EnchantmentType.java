package com.istiak.equinox.enchant;

/**
 * Port of com.istiak.equinox.enchantments.EnchantmentType.
 */
public enum EnchantmentType {
    SWIFT("swift", "⚡ Swift", 10, 0.10),
    TITAN_LEAP("titan-leap", "🦘 Titan Leap", 5, 0.10),
    VITALITY("vitality", "❤ Vitality", 5, 0.10);

    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final double bonusPerLevel;

    EnchantmentType(String id, String displayName, int maxLevel, double bonusPerLevel) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.bonusPerLevel = bonusPerLevel;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getBonusPerLevel() {
        return bonusPerLevel;
    }

    public static EnchantmentType fromString(String input) {
        if (input == null) return null;
        String normalized = input.toLowerCase().replace("_", "-");
        for (EnchantmentType type : values()) {
            if (type.id.equals(normalized)) return type;
        }
        if (normalized.equals("leap") || normalized.equals("jump") || normalized.equals("jumpboost")) {
            return TITAN_LEAP;
        }
        return null;
    }
}
