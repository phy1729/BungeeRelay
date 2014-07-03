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

    public static void sendUserConnect(ProxiedPlayer player) {
        String playerUID = IRC.uids.get(player);
        User user = IRC.users.get(playerUID);
        IRC.out.println(":" + IRC.SID + " UID " + playerUID + " " + user.nickTime + " " + user.nick + " " + player.getAddress().getHostName() + " " + player.getAddress().getHostName() + " " + IRC.config.getString("formats.ident").replace("{IDENT}", player.getName()) + " " + player.getAddress().toString() + " " + user.connectTime + " +r :Minecraft Player");
    }

    public static void sendChannelJoin(ProxiedPlayer player) {
        IRC.out.println(":" + IRC.SID + " FJOIN " + IRC.channel + " " + IRC.channelTS + " + :," + IRC.uids.get(player));
    }

    public static void handleKickKill(String mode, String senderUID, String targetUID, String reason) {
        String target = IRC.users.get(targetUID).nick;
        String sender = IRC.users.get(senderUID).nick;
        sendAll(IRC.config.getString("formats." + mode)
                .replace("{SENDER}", sender)
                .replace("{TARGET}", target)
                .replace("{REASON}", reason));
        ProxiedPlayer player = getPlayerByUid(targetUID);
        if (player != null) {
            if (IRC.config.getBoolean("server.reconnect" + mode)) {
                if (mode.equals("kill")) sendUserConnect(player);
                sendChannelJoin(player);
            } else {
                player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', IRC.config.getString("formats.disconnect" + mode)
                        .replace("{SENDER}", sender)
                        .replace("{TARGET}", target)
                        .replace("{REASON}", reason))));
                IRC.users.remove(targetUID);
                IRC.uids.remove(player);
            }
        }
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
        for (Map.Entry<String, User> entry : IRC.users.entrySet()) {
            if (nick.equalsIgnoreCase(entry.getValue().nick)) return entry.getKey();
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
