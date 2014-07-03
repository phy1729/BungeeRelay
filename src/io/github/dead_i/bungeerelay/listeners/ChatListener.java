package io.github.dead_i.bungeerelay.listeners;

import io.github.dead_i.bungeerelay.IRC;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class ChatListener implements Listener {
    Plugin plugin;
    public ChatListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!IRC.getInstance().sock.isConnected()) return;
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String msg = event.getMessage();
        if (!msg.startsWith("/") || (msg.startsWith("/me "))) {
            if (msg.startsWith("/me ")) msg = "\001ACTION " + msg.substring(4) + "\001";
            IRC.getInstance().out.println(":" + IRC.getInstance().uids.get(player) + " PRIVMSG " + IRC.getInstance().channel + " :" + msg);

            for (ProxiedPlayer o : plugin.getProxy().getPlayers()) {
                if (!player.getServer().getInfo().getName().equals(o.getServer().getInfo().getName())) o.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', IRC.getInstance().config.getString("formats.msg"))
                        .replace("{SENDER}", IRC.getInstance().users.get(IRC.getInstance().uids.get(player)).nick)
                        .replace("{MESSAGE}", msg)));
            }
        }
    }
}
