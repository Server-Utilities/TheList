package host.plas.thelist.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import host.plas.thelist.config.bits.ServerTunnel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PingManager {
    public enum GrabPingType {
        GET,
        NULL_SERVER,
        TIMEOUT,
        ;
    }

    @Getter @Setter
    private static TextComponent alternateMotd = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&c&lLoading&8... &e(&bPlease refresh&e)"));
    @Getter @Setter
    private static ServerPing.PlayerInfo alternatePlayerInfo = new ServerPing.PlayerInfo(
            ChatColor.translateAlternateColorCodes('&', "&c&lLoading&8... &e(&bPlease refresh&e)")
            , "00000000-0000-0000-0000-000000000000");

    @Getter @Setter
    private static Cache<String, ServerPing> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public static ServerPing getDefaultFrom(ServerPing original, GrabPingType type) {
        if (type == GrabPingType.NULL_SERVER || type == GrabPingType.TIMEOUT) return original;

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

    public static ServerPing getDefaultFrom(ServerPing original) {
        return getDefaultFrom(original, GrabPingType.GET);
    }

    public static ServerPing getFromCache(ServerPing original, String hostName) {
        AtomicReference<ServerPing> response = new AtomicReference<>(getDefaultFrom(original, GrabPingType.NULL_SERVER));

        getTunnel(hostName).ifPresent(tunnel -> {
            ServerPing ping = getCache().getIfPresent(tunnel.getServerActualName());
            if (ping == null) {
                response.set(getDefaultFrom(original, GrabPingType.TIMEOUT));

                CompletableFuture.runAsync(() -> {
                    ServerPing p = buildPing(original, tunnel);
                    getCache().put(tunnel.getServerActualName(), p);
                });
                return;
            }
            response.set(ping);
        });
        return response.get();
    }

    public static Optional<ServerTunnel> getTunnel(String hostName) {
        return TunnelManager.getLoadedTunnels().stream().filter(tunnel -> tunnel.isPossibleHost(hostName)).findFirst();
    }

    public static ServerPing buildPing(ServerPing original, ServerTunnel tunnel) {
        ServerInfo serverInfo = null;

        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (server.getName().equalsIgnoreCase(tunnel.getServerActualName())) {
                serverInfo = server;
                break;
            }
        }

        if (serverInfo == null) return getDefaultFrom(original, GrabPingType.NULL_SERVER);
        return getDefaultFrom(onPing(serverInfo).join());
    }

    public static CompletableFuture<ServerPing> onPing(ServerInfo serverInfo) {
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
}
