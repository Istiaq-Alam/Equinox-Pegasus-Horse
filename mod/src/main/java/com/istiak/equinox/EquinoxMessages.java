package com.istiak.equinox;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * MiniMessage-like parser used by the plugin, reimplemented for vanilla
 * text components. Supports the subset of tags this mod uses:
 * gold, bold, gray, dark_gray, green, red, aqua, yellow, white,
 * light_purple, dark_purple and the closing tags.
 */
public final class EquinoxMessages {

    private EquinoxMessages() {
    }

    public static Component parse(String message) {
        MutableComponent result = Component.empty();
        int i = 0;
        ChatFormatting color = null;
        boolean bold = false;

        while (i < message.length()) {
            int open = message.indexOf('<', i);
            if (open == -1) {
                result.append(lit(message.substring(i), color, bold));
                break;
            }

            if (open > i) {
                result.append(lit(message.substring(i, open), color, bold));
            }

            int close = message.indexOf('>', open);
            if (close == -1) {
                result.append(lit(message.substring(open), color, bold));
                break;
            }

            String tag = message.substring(open + 1, close);
            i = close + 1;

            if (tag.startsWith("/")) {
                String name = tag.substring(1);
                if (name.equals("bold")) {
                    bold = false;
                } else if (isColor(name)) {
                    color = null;
                }
            } else if (tag.equals("bold")) {
                bold = true;
            } else if (isColor(tag)) {
                color = fromTag(tag);
            } else {
                // Unknown tag, keep it literally.
                result.append(lit("<" + tag + ">", color, bold));
            }
        }

        return result;
    }

    private static MutableComponent lit(String text, ChatFormatting color, boolean bold) {
        MutableComponent c = Component.literal(text);
        if (color != null) c = c.withStyle(color);
        if (bold) c = c.withStyle(ChatFormatting.BOLD);
        return c;
    }

    private static boolean isColor(String tag) {
        return fromTag(tag) != null;
    }

    private static ChatFormatting fromTag(String tag) {
        return switch (tag) {
            case "gold" -> ChatFormatting.GOLD;
            case "gray" -> ChatFormatting.GRAY;
            case "dark_gray" -> ChatFormatting.DARK_GRAY;
            case "green" -> ChatFormatting.GREEN;
            case "red" -> ChatFormatting.RED;
            case "aqua" -> ChatFormatting.AQUA;
            case "yellow" -> ChatFormatting.YELLOW;
            case "white" -> ChatFormatting.WHITE;
            case "light_purple" -> ChatFormatting.LIGHT_PURPLE;
            case "dark_purple" -> ChatFormatting.DARK_PURPLE;
            case "dark_green" -> ChatFormatting.DARK_GREEN;
            case "blue" -> ChatFormatting.BLUE;
            case "black" -> ChatFormatting.BLACK;
            case "dark_red" -> ChatFormatting.DARK_RED;
            case "dark_aqua" -> ChatFormatting.DARK_AQUA;
            case "dark_blue" -> ChatFormatting.DARK_BLUE;
            default -> null;
        };
    }
}
