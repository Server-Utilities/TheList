package host.plas.thelist.events;

import host.plas.thelist.TheList;
import host.plas.thelist.utils.Logger;
import host.plas.thelist.utils.PingManager;
import host.plas.thelist.utils.TunnelManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Date;

public class MainListener implements Listener {
    public MainListener() {
        ProxyServer.getInstance().getPluginManager().registerListener(TheList.getInstance(), this);

        Logger.logInfo("MainListener has been registered!");
    }

    @EventHandler
    public void onPingServer(ProxyPingEvent event) {
        String hostName;
        try {
            hostName = event.getConnection().getVirtualHost().getHostString();
        } catch (Throwable e) {
            event.setResponse(PingManager.getDefaultFrom(event.getResponse()));
            return;
        }

        TunnelManager.collectAndDo(serverTunnel -> {
            if (! serverTunnel.isPossibleHost(hostName)) return;

            event.setResponse(PingManager.getFromCache(event.getResponse(), hostName));

            serverTunnel.setLastPinged(new Date());
        });
    }

    @EventHandler
    public void onConnect(ServerConnectEvent event) {
        if (! event.getReason().equals(ServerConnectEvent.Reason.JOIN_PROXY)) return;

        ProxiedPlayer player = event.getPlayer();
        PendingConnection connection = player.getPendingConnection();

        String hostName = connection.getVirtualHost().getHostString();

        Logger.logInfo(event.getPlayer().getName() + " is using host address: " + hostName);

        TunnelManager.collectAndDo(tunnel -> {
            if (! tunnel.isPossibleHost(hostName)) return;

            ServerInfo serverInfo = null;

            for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
                if (server.getName().equalsIgnoreCase(tunnel.getServerActualName())) {
                    serverInfo = server;
                    break;
                }
            }

            if (serverInfo != null) event.setTarget(serverInfo);
            else Logger.logWarning("ServerInfo for " + tunnel.getServerActualName() + " is null!");
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.isCancelled()) return;

        if (! (event.getReceiver() instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
        String serverName = player.getServer().getInfo().getName();

        TunnelManager.collectAndDo(tunnel -> {
            if (! tunnel.is(serverName)) return;

            if (tunnel.isBlockBungeeMessages()) event.setCancelled(true);
        });
    }
}
