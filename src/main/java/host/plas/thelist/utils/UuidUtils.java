package host.plas.thelist.utils;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class UuidUtils {
    public static String getUuid(CommandSender sender) {
        if (sender.equals(ProxyServer.getInstance().getConsole())) {
            return "%";
        } else {
            if (sender instanceof ProxiedPlayer) {
                return ((ProxiedPlayer) sender).getUniqueId().toString();
            } else {
                return "%";
            }
        }
    }
}
