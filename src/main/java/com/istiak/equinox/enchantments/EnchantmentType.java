package com.istiak.equinox.enchantments;

import java.util.Locale;

public enum EnchantmentType {

    SWIFT(
            "swift",
            "⚡ Swift",
            "enchantments.swift"
    ),

    TITAN_LEAP(
            "titan-leap",
            "🦘 Titan Leap",
            "enchantments.titan-leap"
    ),

    VITALITY(
            "vitality",
            "❤ Vitality",
            "enchantments.vitality"
    );

    private final String id;
    private final String displayName;
    private final String configPath;

    EnchantmentType(
            String id,
            String displayName,
            String configPath
    ) {

        this.id = id;
        this.displayName = displayName;
        this.configPath = configPath;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getConfigPath() {
        return configPath;
    }

    public static EnchantmentType fromString(String input) {

        if (input == null) {
            return null;
        }

        String normalized =
                input.toLowerCase(Locale.ROOT)
                        .replace("_", "-");

        for (EnchantmentType type : values()) {

            if (type.id.equals(normalized)) {
                return type;
            }

        }

        if (normalized.equals("leap")
                || normalized.equals("jump")
                || normalized.equals("jumpboost")) {

            return TITAN_LEAP;
        }

        return null;
    }
}
