package host.plas.thelist.commands;

import host.plas.thelist.TheList;
import host.plas.thelist.config.bits.ServerTunnel;
import host.plas.thelist.utils.ColorUtils;
import host.plas.thelist.utils.TunnelManager;
import host.plas.thelist.utils.UuidUtils;
import net.md_5.bungee.api.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ManageCMD extends FunCommand {
    public ManageCMD() {
        super(TheList.getInstance(), "manageservers", "thelist.commands.manage.base", "thelist");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            ColorUtils.sendColoredMessage(sender, "&cUsage: /managetunnels <action> [args]");
            return;
        }

        String action = args[0];
        String uuid = UuidUtils.getUuid(sender);

        switch (action) {
            case "hostname":
                if (args.length < 4) {
                    ColorUtils.sendColoredMessage(sender, "&cUsage: /managetunnels hostname <server> <add/remove> <hostname>");
                    return;
                }

                String server = args[1];
                String addRemove = args[2];
                String hostname = args[3];

                if (! TunnelManager.hasTunnel(server)) {
                    ColorUtils.sendColoredMessage(sender, "&cServer &e" + server + " &cdoes not have a tunnel.");
                    return;
                }

                ServerTunnel tunnel = TunnelManager.getTunnel(server).get();

                if (! tunnel.isCanEdit(uuid) && ! uuid.equals("%")) {
                    ColorUtils.sendColoredMessage(sender, "&cYou do not have permission to edit this tunnel.");
                    return;
                }

                if (addRemove.equalsIgnoreCase("add")) {
                    tunnel.getPossibleHosts().add(hostname);

                    ColorUtils.sendColoredMessage(sender, "&aAdded &e" + hostname + " &ato the possible hosts for &e" + server + "&a.");
                } else if (addRemove.equalsIgnoreCase("remove")) {
                    tunnel.getPossibleHosts().remove(hostname);

                    ColorUtils.sendColoredMessage(sender, "&aRemoved &e" + hostname + " &afrom the possible hosts for &e" + server + "&a.");
                } else {
                    ColorUtils.sendColoredMessage(sender, "&cUsage: /managetunnels hostname <server> <add/remove> <hostname>");
                }
                break;
            case "list":
                if (args.length < 2) {
                    ColorUtils.sendColoredMessage(sender, "&cUsage: /managetunnels list <server>");
                    return;
                }

                server = args[1];

                if (! TunnelManager.hasTunnel(server)) {
                    ColorUtils.sendColoredMessage(sender, "&cServer &e" + server + " &cdoes not have a tunnel.");
                    return;
                }

                tunnel = TunnelManager.getTunnel(server).get();
                if (! tunnel.isCanEdit(uuid) && ! uuid.equals("%")) {
                    ColorUtils.sendColoredMessage(sender, "&cYou do not have permission to edit this tunnel.");
                    return;
                }

                ColorUtils.sendColoredMessage(sender, "&ePossible Hosts for &a" + server + "&e:");
                for (String host : tunnel.getPossibleHosts()) {
                    ColorUtils.sendColoredMessage(sender, "&a- &e" + host);
                }
                break;
            case "mytunnels":
                ColorUtils.sendColoredMessage(sender, "&eManageable tunnels&8:");
                String finalUuid = uuid;
                TunnelManager.collectAndDo(tunnel1 -> {
                    if (! tunnel1.isCanEdit(finalUuid) || ! finalUuid.equals("%")) return;

                    ColorUtils.sendColoredMessage(sender, "&a- &e" + tunnel1.getServerActualName() + " &8(&7" + tunnel1.getServerDisplayName() + "&8)");
                });
                break;
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        String uuid = UuidUtils.getUuid(sender);

        if (args.length == 1) {
            return new ArrayList<>(List.of("hostname", "list", "mytunnels"));
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("hostname") || args[0].equalsIgnoreCase("list")) {
                return TunnelManager.getLoadedTunnels().stream()
                        .filter(tunnel -> tunnel.isCanEdit(uuid) || uuid.equals("%"))
                        .map(ServerTunnel::getServerActualName)
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("hostname")) {
                return new ArrayList<>(List.of("add", "remove"));
            }
        }

        return new ArrayList<>();
    }
}
