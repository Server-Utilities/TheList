package host.plas.thelist.config;

import host.plas.thelist.TheList;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

public class MyConfig extends SimpleConfiguration {
    public MyConfig() {
        super("config.yml", TheList.getInstance().getDataFolder(), false);
    }

    @Override
    public void init() {
        TheList.setMainConfig(this);

        getDefaultServerHostSuffix();

        getPingWaitMillis();

        getDeleteOnNoServer();
    }

    public long getPingWaitMillis() {
        reloadResource();

        return getOrSetDefault("ping-wait-millis", 500L);
    }

    public String getDefaultServerHostSuffix() {
        reloadResource();

        return getOrSetDefault("default-server-host-suffix", ".plas.host");
    }

    public boolean getDeleteOnNoServer() {
        reloadResource();

        return getOrSetDefault("delete-on-no-server", false);
    }
}
