package org.collegiumv.BungeeRelay.listeners;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnectListener implements Listener {
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        User user = IRC.players.get(event.getPlayer());
        IRC.write(user, "QUIT", new String[]{IRC.config.getString("formats.mcquit").replace("{SENDER}", user.name)});
        user.delete();
    }
}
