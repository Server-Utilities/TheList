package host.plas.thelist;

import host.plas.thelist.config.MyConfig;
import host.plas.thelist.config.TunnelConfig;
import host.plas.thelist.events.MainListener;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import tv.quaint.objects.handling.derived.IPluginEventable;

import java.util.concurrent.TimeUnit;

public final class TheList extends Plugin implements IPluginEventable {
    @Getter @Setter
    private static TheList instance;

    @Getter @Setter
    private static MyConfig mainConfig;
    @Getter @Setter
    private static TunnelConfig tunnelConfig;

    @Getter @Setter
    private static MainListener mainListener;

    @Getter @Setter
    private ScheduledTask reloadTask;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        mainConfig = new MyConfig();
        tunnelConfig = new TunnelConfig();

        mainListener = new MainListener();

        reloadTask = ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            mainConfig.reloadResource(true);

            tunnelConfig.reloadConfig();
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public String getIdentifier() {
        return getDescription().getName();
    }
}
