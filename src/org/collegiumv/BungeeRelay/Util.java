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

    public static void handleMessage(IRC irc, String mode, String senderUID, String target, String message) {
        String sender = irc.senders.get(senderUID).name;
        String format="";
        Collection<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>();
        boolean isPM;
        if (target.charAt(0) == '#') { // PRIVMSG is for a channel
            isPM = false;
            if (target.equals(irc.channel)) {
                players = proxy.getPlayers();
            }
        } else {
            isPM = true;
            ProxiedPlayer to = irc.getPlayerByUid(target);
            if (to != null) {
                players.add(to);
                irc.replies.put(to, sender);
            }
        }
        if (message.charAt(0) == '\001') { // This is a CTCP message
            message = message.replaceAll("\001", ""); // Remove the 0x01 at beginning and end
            String subcommand = message.split(" ")[0];
            if (message.contains(" ")) message = message.split(" ",2)[1]; // Remove subcommand from message
            if (subcommand.equals("ACTION")) {
                if (isPM) {
                    format = irc.config.getString("formats.privateme");
                } else {
                    format = irc.config.getString("formats.me");
                }
            } else if (subcommand.equals("VERSION")) {
                for (ProxiedPlayer player : players) {
                    irc.write(player, "NOTICE", new String[]{senderUID, "\001VERSION Minecraft v" + player.getPendingConnection().getVersion() + " proxied by BungeeRelay v" + irc.version + "\001"});
                }
            }
        } else {
            if (isPM) {
                format = irc.config.getString("formats.privatemsg");
            } else {
                format = irc.config.getString("formats.msg");
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

    public static void disconnect(IRC irc, ProxiedPlayer player, String mode, String sender, String reason, String target) {
        player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', irc.config.getString("formats.disconnect" + mode)
                .replace("{SENDER}", sender)
                .replace("{TARGET}", target)
                .replace("{REASON}", reason))));
    }
}
