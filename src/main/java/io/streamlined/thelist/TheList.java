package io.streamlined.thelist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.streamlined.thelist.config.MyConfig;
import io.streamlined.thelist.events.MainListener;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;

@Plugin(
        id = "thelist",
        name = "TheList",
        version = "${project.version}",
        description = "A plugin to manage the server list.",
        authors = {
                "Quaint"
        }
)
public final class TheList {
    @Getter @Setter
    private static TheList instance;
    @Getter @Setter
    private static MyConfig myConfig;
    @Getter @Setter
    private static ProxyServer proxy;
    @Getter @Setter
    private static Logger logger;
    @Getter @Setter
    private static File dataFolder;

    @Inject
    public TheList(ProxyServer s, Logger l, @DataDirectory Path dd) {
        proxy = s;
        logger = l;
        dataFolder = dd.toFile();
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        // Plugin startup logic
        instance = this;
        myConfig = new MyConfig();

        getProxy().getEventManager().register(this, new MainListener());
    }
}
