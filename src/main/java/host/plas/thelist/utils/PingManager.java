package host.plas.thelist.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import host.plas.thelist.TheList;
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
            .expireAfterWrite(10, TimeUnit.SECONDS)
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
        AtomicReference<ServerPing> response = new AtomicReference<>(null);

        TunnelManager.getTunnelByHosts(hostName).ifPresent(tunnel -> {
            ServerPing gotten = getCache().get(hostName.toLowerCase(), key -> buildPing(original, tunnel));

            if (gotten != null) {
                response.set(gotten);
            }
        });

        if (response.get() == null) {
            response.set(getDefaultFrom(original, GrabPingType.NULL_SERVER));
        }

        return response.get();
    }

    public static ServerPing buildPing(ServerPing original, ServerTunnel tunnel) {
        return buildPing(original, tunnel, false);
    }

    public static ServerPing buildPing(ServerPing original, ServerTunnel tunnel, boolean async) {
        AtomicReference<ServerPing> response = new AtomicReference<>(null);

        tunnel.getServerInfo().ifPresent(serverInfo -> {
            if (async) {
                onPing(serverInfo).thenAccept(ping -> {
                    try {
                        getCache().invalidate(tunnel.getServerActualName().toLowerCase());
                    } catch (Exception e) {
                        // Ignore
                    }
                    getCache().put(tunnel.getServerActualName().toLowerCase(), ping);
                });
            } else {
                onPing(serverInfo).thenAccept(response::set).join();
            }
        });

        if (response.get() == null) {
            response.set(getDefaultFrom(original, GrabPingType.NULL_SERVER));
        }

        return response.get();
    }

    public static CompletableFuture<ServerPing> onPing(ServerInfo serverInfo) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<ServerPing> atomicReference = new AtomicReference<>(null);

            serverInfo.ping((callback, error) -> {
                ServerPing response = new ServerPing();

                if (error != null) {
                    Logger.logInfo("Error pinging server: " + serverInfo.getName());
                    Logger.logWarning(error);

                    atomicReference.set(getDefaultFrom(response, GrabPingType.NULL_SERVER));
                    return;
                }

                ServerPing.Players players = callback.getPlayers();
                response.setPlayers(players);

                BaseComponent motd = callback.getDescriptionComponent();
                response.setDescriptionComponent(motd);

                ServerPing.ModInfo modInfo = callback.getModinfo();
                response.getModinfo().setModList(modInfo.getModList());
                response.getModinfo().setType(modInfo.getType());

                Favicon favicon = callback.getFaviconObject();
                response.setFavicon(favicon);

                ServerPing.Protocol version = callback.getVersion();
                response.setVersion(version);

                atomicReference.set(response);
            });

            while (atomicReference.get() == null) {
                Thread.onSpinWait();
            }

            return atomicReference.get();
        });
    }
}
