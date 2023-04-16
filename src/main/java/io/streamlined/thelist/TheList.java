package io.streamlined.thelist;

import io.streamlined.thelist.config.MyConfig;
import io.streamlined.thelist.events.MainListener;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Plugin;

public final class TheList extends Plugin {
    @Getter @Setter
    private static TheList instance;
    @Getter @Setter
    private static MyConfig myConfig;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        myConfig = new MyConfig();

        getProxy().getPluginManager().registerListener(this, new MainListener());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
