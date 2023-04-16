package io.streamlined.thelist.events;

import io.streamlined.thelist.TheList;
import io.streamlined.thelist.config.bits.ServerTunnel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import tv.quaint.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MainListener implements Listener {
    @Getter @Setter
    private static ConcurrentSkipListMap<ServerTunnel, ServerPing> serverPingCache = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static TextComponent alternateMotd = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&c&lLoading&8... &e(&bPlease refresh&e)"));
    @Getter @Setter
    private static ServerPing.PlayerInfo alternatePlayerInfo = new ServerPing.PlayerInfo(
            ChatColor.translateAlternateColorCodes('&', "&c&lLoading&8... &e(&bPlease refresh&e)")
            , "00000000-0000-0000-0000-000000000000");

    public MainListener() {
        TheList.getInstance().getLogger().info("MainListener has been registered!");
    }

    @EventHandler
    public void onPingServer(ProxyPingEvent event) {
        String hostName;
        try {
            hostName = event.getConnection().getVirtualHost().getHostString();
        } catch (Exception e) {
            event.setResponse(getDefaultFrom(event.getResponse()));
            return;
        }

        TheList.getMyConfig().getServerTunnels().forEach(serverTunnel -> {
            if (! serverTunnel.isPossibleHost(hostName)) return;

            ServerInfo serverInfo = null;

            for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
                if (server.getName().equalsIgnoreCase(serverTunnel.getServerActualName())) {
                    serverInfo = server;
                    break;
                }
            }

            if (serverInfo != null) {
                if (serverTunnel.getLastPinged() != null && MathUtils.isDateOlderThan(serverTunnel.getLastPinged(), 5, ChronoUnit.SECONDS)) {
                    ServerPing serverPing = serverPingCache.get(serverTunnel);
                    if (serverPing != null) {
                        event.setResponse(serverPing);
                        return;
                    }
                }

                ServerPing original = event.getResponse();
                ServerPing ping = onPing(serverInfo).completeOnTimeout(null, TheList.getMyConfig().getPingWaitMillis(), TimeUnit.MILLISECONDS).join();

                if (ping != null) {
                    event.setResponse(ping);
                    serverTunnel.setLastPinged(new Date());
                    serverPingCache.put(serverTunnel, ping);
                } else {
                    event.setResponse(getDefaultFrom(original));
                }
            }
        });
    }

    public CompletableFuture<ServerPing> onPing(ServerInfo serverInfo) {
        return CompletableFuture.supplyAsync(() -> {
            ServerPing response = new ServerPing();

            CompletableFuture<ServerPing> future = new CompletableFuture<>();

            serverInfo.ping((result, error) -> {
                if (error != null) {
                    // Handle the error.
                    return;
                }

                ServerPing.Players players = result.getPlayers();
                response.setPlayers(players);

                BaseComponent motd = result.getDescriptionComponent();
                response.setDescriptionComponent(motd);

                ServerPing.ModInfo modInfo = result.getModinfo();
                response.getModinfo().setModList(modInfo.getModList());
                response.getModinfo().setType(modInfo.getType());

                Favicon favicon = result.getFaviconObject();
                response.setFavicon(favicon);

                ServerPing.Protocol version = result.getVersion();
                response.setVersion(version);

                future.complete(response);
            });

            return future.join();
        });
    }

    @EventHandler
    public void onConnect(ServerConnectEvent event) {
        if (! event.getReason().equals(ServerConnectEvent.Reason.JOIN_PROXY)) return;

        ProxiedPlayer player = event.getPlayer();
        PendingConnection connection = player.getPendingConnection();

        String hostName = connection.getVirtualHost().getHostString();

        TheList.getInstance().getLogger().info("Host: " + hostName);

        TheList.getMyConfig().getServerTunnels().forEach(serverTunnel -> {
            if (! serverTunnel.isPossibleHost(hostName)) return;

            ServerInfo serverInfo = null;

            for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
                if (server.getName().equalsIgnoreCase(serverTunnel.getServerActualName())) {
                    serverInfo = server;
                    break;
                }
            }

            if (serverInfo != null) event.setTarget(serverInfo);
            else TheList.getInstance().getLogger().warning("ServerInfo for " + serverTunnel.getServerActualName() + " is null!");
        });
    }

    public static ServerPing getDefaultFrom(ServerPing original) {
        ServerPing response = new ServerPing();
        response.setVersion(original.getVersion());

        ServerPing.PlayerInfo[] playerInfo = new ServerPing.PlayerInfo[1];
        playerInfo[0] = getAlternatePlayerInfo();

        ServerPing.Players players = new ServerPing.Players(original.getPlayers().getMax(), original.getPlayers().getOnline(), playerInfo);

        response.setPlayers(players);
        response.setDescriptionComponent(getAlternateMotd());
        response.getModinfo().setModList(original.getModinfo().getModList());
        response.getModinfo().setType(original.getModinfo().getType());
        return response;
    }
}
