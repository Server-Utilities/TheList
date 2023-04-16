package io.streamlined.thelist.config;

import io.streamlined.thelist.TheList;
import io.streamlined.thelist.config.bits.ServerTunnel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;
import tv.quaint.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class MyConfig extends SimpleConfiguration {
    @Getter @Setter
    private static ConcurrentSkipListSet<ServerTunnel> loadedServerTunnels = new ConcurrentSkipListSet<>();
    @Getter @Setter
    private static Date lastLoaded = null;

    public MyConfig() {
        super("config.yml", TheList.getInstance().getDataFolder(), false);
    }

    @Override
    public void init() {
        getDefaultServerHostSuffix();

        getPingWaitMillis();

        getServerTunnels();
    }

    public ConcurrentSkipListSet<ServerTunnel> getServerTunnels() {
        if (lastLoaded != null && MathUtils.isDateOlderThan(lastLoaded, 30, ChronoUnit.SECONDS)) {
            return loadedServerTunnels;
        } else {
            lastLoaded = new Date();
            loadedServerTunnels = new ConcurrentSkipListSet<>();
        }

        ConcurrentSkipListSet<ServerTunnel> serverTunnels = new ConcurrentSkipListSet<>();

        for (String serverName : singleLayerKeySet("server-tunnels")) {
            try {
                String serverDisplayName = getResource().getString("server-tunnels." + serverName + ".server-display-name");
                ConcurrentSkipListSet<String> possibleHosts = new ConcurrentSkipListSet<>(getResource().getStringList("server-tunnels." + serverName + ".possible-hosts"));
                boolean blockBungeeMessages = getResource().getBoolean("server-tunnels." + serverName + ".block-bungee-messages");
                ConcurrentSkipListSet<String> canEdit = new ConcurrentSkipListSet<>(getResource().getStringList("server-tunnels." + serverName + ".can-edit"));

                serverTunnels.add(ServerTunnel.of(serverName, serverDisplayName, possibleHosts, blockBungeeMessages, canEdit));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        loadedServerTunnels = serverTunnels;

        for (ServerInfo serverInfo : ProxyServer.getInstance().getServers().values()) {
            if (! serverTunnels.stream().map(ServerTunnel::getServerActualName).collect(Collectors.toList()).contains(serverInfo.getName())) {
                createServerTunnelByServerName(serverInfo.getName());
            }
        }

        return loadedServerTunnels;
    }

    public void saveServerTunnel(ServerTunnel serverTunnel) {
        getResource().set("server-tunnels." + serverTunnel.getServerActualName() + ".server-display-name", serverTunnel.getServerDisplayName());
        getResource().set("server-tunnels." + serverTunnel.getServerActualName() + ".possible-hosts", new ArrayList<>(serverTunnel.getPossibleHosts()));
        getResource().set("server-tunnels." + serverTunnel.getServerActualName() + ".block-bungee-messages", serverTunnel.isBlockBungeeMessages());
        getResource().set("server-tunnels." + serverTunnel.getServerActualName() + ".can-edit", new ArrayList<>(serverTunnel.getCanEdit()));
    }

    public void deleteServerTunnel(ServerTunnel serverTunnel) {
        getResource().set("server-tunnels." + serverTunnel.getServerActualName(), null);
    }

    public void createServerTunnelByServerName(String serverName) {
        String serverHost = serverName + getDefaultServerHostSuffix();
        ServerTunnel tunnel = new ServerTunnel(serverName, serverName, new ConcurrentSkipListSet<>(), false, new ConcurrentSkipListSet<>());
        tunnel.getPossibleHosts().add(serverHost);

        saveServerTunnel(tunnel);

        loadedServerTunnels.add(tunnel);

        TheList.getInstance().getLogger().info("Created server tunnel for " + serverName + " with host " + serverHost);
    }

    public boolean isServerTunnel(String serverName) {
        return getServerTunnels().stream().anyMatch(serverTunnel -> serverTunnel.getServerActualName().equalsIgnoreCase(serverName));
    }

    public long getPingWaitMillis() {
        return getResource().getOrSetDefault("ping-wait-millis", 2777L);
    }

    public String getDefaultServerHostSuffix() {
        return getResource().getOrSetDefault("default-server-host-suffix", ".plasmere.net");
    }
}
