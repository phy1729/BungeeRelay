package io.github.dead_i.bungeerelay;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

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

    public static void sendChannelJoin(ProxiedPlayer p) {
        String uid = IRC.uids.get(p);
        IRC.out.println("FJOIN " + IRC.channel + " " + System.currentTimeMillis() / 1000 + " +nt :," + uid);
    }

    public static void sendAll(String message) {
        proxy.broadcast(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    public static void sendError(CommandSender player, String message) {
        player.sendMessage(new TextComponent(ChatColor.RED + message));
    }

    public static void updateTS(String ts) {
        long timestamp = stringToTS(ts);
        if (timestamp < IRC.channelTS) {
            IRC.channelTS = timestamp;
        }
    }

    public static long stringToTS(String ts) {
        Long LongTimestamp = Long.parseLong(ts);
        return LongTimestamp.longValue();
    }

    public static String getUidByNick(String nick) {
        for (Map.Entry<String, String> entry : IRC.users.entrySet()) {
            if (nick.equalsIgnoreCase(entry.getValue())) return entry.getKey();
        }
        return null;
    }

    public static ProxiedPlayer getPlayerByUid(String uid) {
        for (Map.Entry<ProxiedPlayer, String> entry : IRC.uids.entrySet()) {
            if (uid.equalsIgnoreCase(entry.getValue())) return entry.getKey();
        }
        return null;
    }
}
