package io.streamlined.thelist.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {
    public static String stringedColor(String input) {
        return input.replaceAll("&", "§");
    }

    public static Component colorize(String input) {
        return LegacyComponentSerializer.builder().character('&').extractUrls().build().deserialize(input);
    }
}
