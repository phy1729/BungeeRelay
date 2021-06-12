package org.collegiumv.BungeeRelay;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Collection;

public class Util {
    private static ProxyServer proxy = ProxyServer.getInstance();

    public static void handleMessage(String mode, String senderUID, String target, String message) {
        String sender = IRC.senders.get(senderUID).name;
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
            ProxiedPlayer to = IRC.getPlayerByUid(target);
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
                    IRC.write(player, "NOTICE", new String[]{senderUID, "\001VERSION Minecraft v" + player.getPendingConnection().getVersion() + " proxied by BungeeRelay v" + IRC.version + "\001"});
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

    public static void disconnect(ProxiedPlayer player, String mode, String sender, String reason, String target) {
        player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', IRC.config.getString("formats.disconnect" + mode)
                .replace("{SENDER}", sender)
                .replace("{TARGET}", target)
                .replace("{REASON}", reason))));
    }
}
