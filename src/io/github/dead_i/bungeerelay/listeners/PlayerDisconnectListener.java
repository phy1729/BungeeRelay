package io.github.dead_i.bungeerelay.listeners;

import io.github.dead_i.bungeerelay.IRC;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnectListener implements Listener {
    Plugin plugin;
    public PlayerDisconnectListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (IRC.sock.isConnected()) IRC.out.println(":" + IRC.uids.get(player) + " QUIT :" + IRC.config.getString("formats.mcquit")
            .replace("{SENDER}", IRC.users.get(player).nick));
        IRC.users.remove(IRC.uids.get(player));
        IRC.uids.remove(player);
    }
}
