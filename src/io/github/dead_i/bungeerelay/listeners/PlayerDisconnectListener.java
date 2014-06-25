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
        String playerUID = IRC.uids.get(player);
        if (IRC.sock.isConnected()) IRC.out.println(":" + playerUID + " QUIT :" + IRC.config.getString("formats.mcquit")
            .replace("{SENDER}", IRC.users.get(playerUID).nick));
        IRC.users.remove(playerUID);
        IRC.uids.remove(player);
    }
}
