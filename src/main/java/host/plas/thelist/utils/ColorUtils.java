package host.plas.thelist.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class ColorUtils {
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String decolorize(String message) {
        return ChatColor.stripColor(message);
    }

    public static void sendColoredMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }
}
