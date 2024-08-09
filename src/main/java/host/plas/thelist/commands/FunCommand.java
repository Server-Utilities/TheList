package host.plas.thelist.commands;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

@Getter @Setter
public abstract class FunCommand extends Command implements TabExecutor {
    private Plugin plugin;

    public FunCommand(Plugin plugin, boolean register, String name, String permission, String... aliases) {
        super(name, permission, aliases);

        this.plugin = plugin;

        if (register) {
            register();
        }
    }

    public FunCommand(Plugin plugin, String name, String permission, String... aliases) {
        this(plugin, true, name, permission, aliases);
    }

    public void register() {
        ProxyServer.getInstance().getPluginManager().registerCommand(getPlugin(), this);
    }
}
