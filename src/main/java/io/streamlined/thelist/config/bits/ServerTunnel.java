package io.streamlined.thelist.config.bits;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.initialization.qual.FBCBottom;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerTunnel implements Comparable<ServerTunnel> {
    @Getter @Setter
    private String serverActualName;
    @Getter @Setter
    private String serverDisplayName;
    @Getter @Setter
    private ConcurrentSkipListSet<String> possibleHosts;
    @Getter @Setter
    private boolean blockBungeeMessages;
    @Getter @Setter
    private ConcurrentSkipListSet<UUID> canEdit;
    @Getter @Setter
    private Date lastPinged;

    public ServerTunnel(String serverActualName, String serverDisplayName, ConcurrentSkipListSet<String> possibleHosts, boolean blockBungeeMessages, ConcurrentSkipListSet<UUID> canEdit) {
        this.serverActualName = serverActualName;
        this.serverDisplayName = serverDisplayName;
        this.possibleHosts = possibleHosts;
        this.blockBungeeMessages = blockBungeeMessages;
        this.canEdit = canEdit;
        this.lastPinged = null;
    }

    public static ServerTunnel of(String serverActualName, String serverDisplayName, ConcurrentSkipListSet<String> possibleHosts, boolean blockBungeeMessages, ConcurrentSkipListSet<String> canEdit) {
        return new ServerTunnel(serverActualName, serverDisplayName, possibleHosts, blockBungeeMessages, ServerTunnel.toUUIDSet(canEdit));
    }

    public void addPossibleHost(String host) {
        possibleHosts.add(host);
    }

    public void removePossibleHost(String host) {
        possibleHosts.remove(host);
    }

    public boolean isPossibleHost(String host) {
        return isPossibleHost(host, true);
    }

    public boolean isPossibleHost(String host, boolean ignoreCase) {
        if (ignoreCase) {
            AtomicBoolean isPossible = new AtomicBoolean(false);
            possibleHosts.forEach(possibleHost -> {
                if (possibleHost.equalsIgnoreCase(host)) {
                    isPossible.set(true);
                }
            });
            return isPossible.get();
        }

        return possibleHosts.contains(host);
    }

    public void addCanEdit(String uuid) {
        try {
            canEdit.add(UUID.fromString(uuid));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeCanEdit(String uuid) {
        try {
            canEdit.remove(UUID.fromString(uuid));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCanEdit(String uuid) {
        try {
            return canEdit.contains(UUID.fromString(uuid));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ConcurrentSkipListSet<UUID> toUUIDSet(ConcurrentSkipListSet<String> stringSet) {
        ConcurrentSkipListSet<UUID> uuidSet = new ConcurrentSkipListSet<>();
        stringSet.forEach(uuid -> {
            try {
                uuidSet.add(UUID.fromString(uuid));
            } catch (Exception e) {
                e.printStackTrace();
                stringSet.remove(uuid);
            }
        });
        return uuidSet;
    }

    public static ConcurrentSkipListSet<String> toStringSet(ConcurrentSkipListSet<UUID> uuidSet) {
        ConcurrentSkipListSet<String> stringSet = new ConcurrentSkipListSet<>();
        uuidSet.forEach(uuid -> {
            try {
                stringSet.add(uuid.toString());
            } catch (Exception e) {
                e.printStackTrace();
                uuidSet.remove(uuid);
            }
        });
        return stringSet;
    }

    @Override
    public int compareTo(@NotNull ServerTunnel o) {
        return serverActualName.compareTo(o.serverActualName);
    }
}
