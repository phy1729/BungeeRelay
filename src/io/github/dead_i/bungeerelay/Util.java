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

    public static boolean isValidNick(String nick) {
        if (nick.isEmpty() || nick.length() > IRC.nickMax)
            return false;

        char firstChar = nick.charAt(0);
        if ((firstChar >= '0' && firstChar <= '9') || firstChar == '-')
            return false;

        for (char c : nick.toCharArray()) {
            if (c >= 'A' && c <= '}') {
                // "A"-"}" can occur anywhere in a nickname
                continue;
            }
            if ((c >= '0' && c <= '9') || c == '-') {
                // "0"-"9", "-" can occur anywhere BUT the first char of a nickname
                continue;
            }
            return false;
        }
        return true;
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

    public static void handleMessage(String mode, String senderUID, String target, String message) {
        String sender = IRC.users.get(senderUID).nick;
        String format="";
        Collection<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>();
        boolean isPM;
        if (target.charAt(0) == '#') { // PRIVMSG is for a channel
            isPM = false;
            if (target.equals(IRC.channel)) {
                players = proxy.getPlayers();
            }
        } else {
            isPM = true;
            ProxiedPlayer to = getPlayerByUid(target);
            if (to != null) {
                players.add(to);
                IRC.replies.put(to, sender);
            }
        }
        if (message.charAt(0) == '\001') { // This is a CTCP message
            message = message.replaceAll("\001", ""); // Remove the 0x01 at beginning and end
            String subcommand = message.split(" ")[0];
            if (message.contains(" ")) message = message.split(" ",2)[1]; // Remove subcommand from message
            if (subcommand.equals("ACTION")) {
                if (isPM) {
                    format = IRC.config.getString("formats.privateme");
                } else {
                    format = IRC.config.getString("formats.me");
                }
            } else if (subcommand.equals("VERSION")) {
                for (ProxiedPlayer player : players) {
                    IRC.out.println(":" + IRC.uids.get(player) + " NOTICE " + senderUID + " :\001VERSION Minecraft v" + player.getPendingConnection().getVersion() + " proxied by BungeeRelay v0.1\001");
                }
            }
        } else {
            if (isPM) {
                format = IRC.config.getString("formats.privatemsg");
            } else {
                format = IRC.config.getString("formats.msg");
            }
        }
        if (format != "") {
            for (ProxiedPlayer p : players) {
                p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', format)
                        .replace("{SENDER}", sender)
                        .replace("{MESSAGE}", message)));
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
