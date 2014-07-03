package io.github.dead_i.bungeerelay.listeners;

import io.github.dead_i.bungeerelay.IRC;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnectListener implements Listener {
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String playerUID = IRC.getInstance().uids.get(player);
        if (IRC.getInstance().sock.isConnected()) IRC.getInstance().out.println(":" + playerUID + " QUIT :" + IRC.getInstance().config.getString("formats.mcquit")
            .replace("{SENDER}", IRC.getInstance().users.get(playerUID).nick));
        IRC.getInstance().users.remove(playerUID);
        IRC.getInstance().uids.remove(player);
    }
}
