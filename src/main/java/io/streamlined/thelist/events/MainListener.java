package io.streamlined.thelist.events;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import io.streamlined.thelist.TheList;
import io.streamlined.thelist.config.bits.ServerTunnel;
import io.streamlined.thelist.utils.ColorUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import tv.quaint.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MainListener {
    @Getter @Setter
    private static ConcurrentSkipListMap<ServerTunnel, ServerPing> serverPingCache = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static Component alternateMotd = ColorUtils.colorize("&c&lLoading&8... &e(&bPlease refresh&e)");
    @Getter @Setter
    private static ServerPing.SamplePlayer alternatePlayerInfo = new ServerPing.SamplePlayer(
            "§c§lLoading§8... §e(§bPlease refresh§e)"
            , UUID.fromString("00000000-0000-0000-0000-000000000000"));

    public MainListener() {
        TheList.getInstance().getLogger().info("MainListener has been registered!");
    }

    @Subscribe
    public void onPingServer(ProxyPingEvent event) {
        String hostName;
        try {
            if (event.getConnection().getVirtualHost().isPresent()) hostName = event.getConnection().getVirtualHost().get().getHostName();
            else {
                event.setPing(getDefaultFrom(event.getPing()));
                return;
            }
        } catch (Exception e) {
            event.setPing(getDefaultFrom(event.getPing()));
            return;
        }

        TheList.getMyConfig().getServerTunnels().forEach(serverTunnel -> {
            if (! serverTunnel.isPossibleHost(hostName)) return;

            RegisteredServer srv = null;

            for (RegisteredServer s : TheList.getProxy().getAllServers()) {
                ServerInfo server = s.getServerInfo();
                if (server.getName().equalsIgnoreCase(serverTunnel.getServerActualName())) {
                    srv = s;
                    break;
                }
            }

            if (srv != null) {
                if (serverTunnel.getLastPinged() != null && MathUtils.isDateOlderThan(serverTunnel.getLastPinged(), 5, ChronoUnit.SECONDS)) {
                    ServerPing serverPing = serverPingCache.get(serverTunnel);
                    if (serverPing != null) {
                        event.setPing(serverPing);
                        return;
                    }
                }

                ServerPing original = event.getPing();
                ServerPing ping = onPing(srv).completeOnTimeout(null, TheList.getMyConfig().getPingWaitMillis(), TimeUnit.MILLISECONDS).join();

                if (ping != null) {
                    event.setPing(ping);
                    serverTunnel.setLastPinged(new Date());
                    serverPingCache.put(serverTunnel, ping);
                } else {
                    event.setPing(getDefaultFrom(original));
                }
            }
        });
    }

    public CompletableFuture<ServerPing> onPing(RegisteredServer serverInfo) {
        return CompletableFuture.supplyAsync(() -> {
            ServerPing.Builder response = ServerPing.builder();

            CompletableFuture<ServerPing> future = new CompletableFuture<>();

            ServerPing result = serverInfo.ping().join();

            Optional<ServerPing.Players> players = result.getPlayers();
            if (players.isPresent()) {
                ServerPing.Players players1 = players.get();
                response.onlinePlayers(players1.getOnline());
                response.maximumPlayers(players1.getMax());
                response.samplePlayers(players1.getSample().toArray(new ServerPing.SamplePlayer[0]));
            }

            Component motd = result.getDescriptionComponent();
            response.description(motd);

            if (result.getModinfo().isPresent()) {
                response.mods(result.getModinfo().get().getMods().toArray(new ModInfo.Mod[0]));
                response.modType(result.getModinfo().get().getType());
            }

            Optional<Favicon> favicon = result.getFavicon();
            favicon.ifPresent(response::favicon);

            ServerPing.Version version = result.getVersion();
            response.version(version);

            return response.build();
        });
    }

    @Subscribe
    public void onConnect(ServerConnectedEvent event) {
        Player connection = event.getPlayer();

        if (connection.getVirtualHost().isEmpty()) return;
        String hostName = connection.getVirtualHost().get().getHostString();

        TheList.getLogger().info("Host: " + hostName);

        TheList.getMyConfig().getServerTunnels().forEach(serverTunnel -> {
            if (! serverTunnel.isPossibleHost(hostName)) return;

            RegisteredServer srv = null;

            for (RegisteredServer s : TheList.getProxy().getAllServers()) {
                ServerInfo server = s.getServerInfo();
                if (server.getName().equalsIgnoreCase(serverTunnel.getServerActualName())) {
                    srv = s;
                    break;
                }
            }

            if (srv != null) connection.createConnectionRequest(srv).connect();
            else TheList.getLogger().warn("ServerInfo for " + serverTunnel.getServerActualName() + " is null!");
        });
    }

    public static ServerPing getDefaultFrom(ServerPing original) {
        ServerPing.Builder response = ServerPing.builder();
        response.version(original.getVersion());

        ServerPing.SamplePlayer[] playerInfo = new ServerPing.SamplePlayer[1];
        playerInfo[0] = getAlternatePlayerInfo();
        response.samplePlayers(playerInfo);

        if (original.getPlayers().isPresent()) {
            response.maximumPlayers(original.getPlayers().get().getMax());
            response.onlinePlayers(original.getPlayers().get().getOnline());
        }

        response.description(getAlternateMotd());

        if (original.getModinfo().isPresent()) {
            response.mods(original.getModinfo().get().getMods().toArray(new ModInfo.Mod[0]));
            response.modType(original.getModinfo().get().getType());
        }

        return response.build();
    }
}
