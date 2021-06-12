package org.collegiumv.BungeeRelay.listeners;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnectListener implements Listener {
    private IRC irc;

    public PlayerDisconnectListener(IRC irc) {
        this.irc = irc;
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        User user = irc.players.get(event.getPlayer());
        irc.write(user, "QUIT", new String[]{irc.config.getString("formats.mcquit").replace("{SENDER}", user.name)});
        user.delete();
    }
}
