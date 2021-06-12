package org.collegiumv.BungeeRelay.listeners;

import org.collegiumv.BungeeRelay.IRC;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class ChatListener implements Listener {
    Plugin plugin;
    private IRC irc;

    public ChatListener(Plugin plugin, IRC irc) {
        this.plugin = plugin;
        this.irc = irc;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!irc.sock.isConnected()) return;
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String msg = event.getMessage();
        if (!msg.startsWith("/") || (msg.startsWith("/me "))) {
            if (msg.startsWith("/me ")) msg = "\001ACTION " + msg.substring(4) + "\001";
            irc.write(player, "PRIVMSG", new String[]{irc.channel, msg});

            for (ProxiedPlayer o : plugin.getProxy().getPlayers()) {
                if (!player.getServer().getInfo().getName().equals(o.getServer().getInfo().getName())) o.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', irc.config.getString("formats.msg"))
                        .replace("{SENDER}", player.getName())
                        .replace("{MESSAGE}", msg)));
            }
        }
    }
}
