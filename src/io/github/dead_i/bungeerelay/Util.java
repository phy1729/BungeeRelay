package io.github.dead_i.bungeerelay;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

public class Util {
    private static ProxyServer proxy = ProxyServer.getInstance();

    public static void incrementUid(int pos) {
        StringBuilder sb = new StringBuilder(IRC.currentUid);
        if (IRC.currentUid.charAt(pos) == 'Z') {
            sb.setCharAt(pos, '0');
            IRC.currentUid = sb.toString();
        } else if (IRC.currentUid.charAt(pos) == '9') {
            sb.setCharAt(pos, 'A');
            IRC.currentUid = sb.toString();
            if (pos == 3) return;
            incrementUid(pos - 1);
        } else {
            sb.setCharAt(pos, (char) (IRC.currentUid.charAt(pos) + 1));
            IRC.currentUid = sb.toString();
        }
    }

    public static void incrementUid() {
        do {
            incrementUid(8);
        } while (IRC.uids.containsValue(IRC.currentUid));
    }

    public static void sendUserConnect(ProxiedPlayer p) {
        String name = IRC.config.getString("server.userprefix") + p.getName() + IRC.config.getString("server.usersuffix");
        IRC.times.put(p, System.currentTimeMillis() / 1000);
        IRC.uids.put(p, IRC.currentUid);
        IRC.users.put(IRC.currentUid, name);
        IRC.out.println("UID " + IRC.currentUid + " " + System.currentTimeMillis() / 1000 + " " + name + " " + p.getAddress().getHostName() + " " + p.getAddress().getHostName() + " " + p.getName() + " " + p.getAddress().getHostString() + " " + IRC.times.get(p) + " +r :Minecraft Player");
    }

    public static void sendChannelJoin(ProxiedPlayer p, String c) {
        String uid = IRC.uids.get(p);
        IRC.out.println("FJOIN " + c + " " + System.currentTimeMillis() / 1000 + " +nt :," + uid);
    }

    public static Collection<ProxiedPlayer> getPlayersByChannel(String c) {
        return proxy.getPlayers();
    }

    public static String getUidByNick(String nick) {
        for (Map.Entry<String, String> entry : IRC.users.entrySet()) {
            if (nick.equalsIgnoreCase(entry.getValue())) return entry.getKey();
        }
        return null;
    }

    public static Long getChanTS(String c) {
        if (!IRC.chans.containsKey(c)) IRC.chans.put(c, new Channel(System.currentTimeMillis() / 1000));
        return IRC.chans.get(c).ts;
    }
}
