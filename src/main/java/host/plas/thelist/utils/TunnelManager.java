package host.plas.thelist.utils;

import host.plas.thelist.TheList;
import host.plas.thelist.config.bits.ServerTunnel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TunnelManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<ServerTunnel> loadedTunnels = new ConcurrentSkipListSet<>();

    public static void loadTunnel(ServerTunnel tunnel) {
        if (hasTunnel(tunnel.getServerActualName())) {
            unloadTunnel(tunnel);
        }

        getLoadedTunnels().add(tunnel);
    }

    public static void unloadTunnel(ServerTunnel tunnel) {
        getLoadedTunnels().removeIf(t -> t.is(tunnel.getServerActualName()));
    }

    public static Optional<ServerTunnel> getTunnel(String serverName) {
        return getLoadedTunnels().stream().filter(tunnel -> tunnel.is(serverName)).findFirst();
    }

    public static Optional<ServerTunnel> getTunnelByHosts(String hostname) {
        return getLoadedTunnels().stream().filter(tunnel -> tunnel.isPossibleHost(hostname)).findFirst();
    }

    public static boolean hasTunnel(String serverName) {
        return getTunnel(serverName).isPresent();
    }

    public static void clearServerTunnels() {
        getLoadedTunnels().clear();
    }

    public static void loadAllServerTunnels(ConcurrentSkipListSet<ServerTunnel> serverTunnels) {
        getLoadedTunnels().addAll(serverTunnels);
    }

    public static void pollTunnels() {
        getLoadedTunnels().forEach(ServerTunnel::poll);
    }

    public static void deleteTunnel(ServerTunnel serverTunnel) {
        if (! TheList.getMainConfig().getDeleteOnNoServer()) return;

        TheList.getTunnelConfig().deleteServerTunnel(serverTunnel);
        unloadTunnel(serverTunnel);
    }

    public static void collectAndDo(Consumer<ServerTunnel> consumer) {
        getLoadedTunnels().forEach(consumer);
    }

    public static Optional<ServerInfo> getServerInfo(String serverActualName) {
        AtomicReference<ServerInfo> serverInfo = new AtomicReference<>(null);

        ProxyServer.getInstance().getServers().forEach((string, info) -> {
            if (serverInfo.get() != null) return;

            if (info.getName().equals(serverActualName)) serverInfo.set(info);
        });

        return Optional.ofNullable(serverInfo.get());
    }

    public static Optional<ServerInfo> getServerInfo(ServerTunnel tunnel) {
        return getServerInfo(tunnel.getServerActualName());
    }
}
