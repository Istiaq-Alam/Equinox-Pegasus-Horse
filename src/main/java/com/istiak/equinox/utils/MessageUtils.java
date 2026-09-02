package com.istiak.equinox.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private MessageUtils() {
    }

    public static Component parse(String message) {

        return MINI_MESSAGE.deserialize(
                "<gold><bold>Equinox</bold></gold> <dark_gray>»</dark_gray> "
                        + message
        );
    }

    public static Component plain(String message) {

        return MINI_MESSAGE.deserialize(message);
    }
}
