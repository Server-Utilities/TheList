package host.plas.thelist.config;

import host.plas.thelist.TheList;
import host.plas.thelist.config.bits.ServerTunnel;
import host.plas.thelist.utils.TunnelManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class TunnelConfig extends SimpleConfiguration {
    public TunnelConfig() {
        super("tunnels.yml", TheList.getInstance(), false);
    }

    @Override
    public void init() {
        reloadConfig();
    }

    public void reloadConfig() {
        reloadResource(true);

        TunnelManager.clearServerTunnels();
        TunnelManager.loadAllServerTunnels(getServerTunnels());

        TunnelManager.pollTunnels();
    }

    public ConcurrentSkipListSet<ServerTunnel> getServerTunnels() {
        ConcurrentSkipListSet<ServerTunnel> serverTunnels = new ConcurrentSkipListSet<>();

        for (String serverName : singleLayerKeySet()) {
            try {
                String serverDisplayName = getOrSetDefault(serverName + ".server-display-name", serverName);
                ConcurrentSkipListSet<String> possibleHosts = new ConcurrentSkipListSet<>(getOrSetDefault(serverName + ".possible-hosts", new ArrayList<>()));
                boolean blockBungeeMessages = getOrSetDefault(serverName + ".block-bungee-messages", false);
                ConcurrentSkipListSet<String> canEdit = new ConcurrentSkipListSet<>(getOrSetDefault(serverName + ".can-edit", new ArrayList<>()));

                serverTunnels.add(ServerTunnel.of(serverName, serverDisplayName, possibleHosts, blockBungeeMessages, canEdit));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (ServerInfo serverInfo : ProxyServer.getInstance().getServers().values()) {
            if (! serverTunnels.stream().map(ServerTunnel::getServerActualName).collect(Collectors.toList()).contains(serverInfo.getName())) {
                serverTunnels.add(createServerTunnelByServerName(serverInfo.getName()));
            }
        }

        return serverTunnels;
    }

    public void saveServerTunnel(ServerTunnel serverTunnel) {
        getResource().set(serverTunnel.getServerActualName() + ".server-display-name", serverTunnel.getServerDisplayName());
        getResource().set(serverTunnel.getServerActualName() + ".possible-hosts", new ArrayList<>(serverTunnel.getPossibleHosts()));
        getResource().set(serverTunnel.getServerActualName() + ".block-bungee-messages", serverTunnel.isBlockBungeeMessages());
        getResource().set(serverTunnel.getServerActualName() + ".can-edit", new ArrayList<>(serverTunnel.getCanEdit()));
    }

    public void deleteServerTunnel(ServerTunnel serverTunnel) {
        getResource().set(serverTunnel.getServerActualName(), null);
    }

    public ServerTunnel createServerTunnelByServerName(String serverName) {
        String serverHost = serverName + TheList.getMainConfig().getDefaultServerHostSuffix();
        ServerTunnel tunnel = new ServerTunnel(serverName, serverName, new ConcurrentSkipListSet<>(), false, new ConcurrentSkipListSet<>());
        tunnel.getPossibleHosts().add(serverHost);

        saveServerTunnel(tunnel);

        TheList.getInstance().getLogger().info("Created server tunnel for " + serverName + " with host " + serverHost);

        return tunnel;
    }

    public boolean isServerTunnel(String serverName) {
        return getServerTunnels().stream().anyMatch(serverTunnel -> serverTunnel.getServerActualName().equalsIgnoreCase(serverName));
    }
}
