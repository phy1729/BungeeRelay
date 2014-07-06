package org.collegiumv.BungeeRelay.listeners;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
import org.collegiumv.BungeeRelay.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PostLoginListener implements Listener {
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (!IRC.sock.isConnected()) return;
        ProxiedPlayer player = event.getPlayer();
        User.create(player);
        Util.sendUserConnect(player);
        Util.sendChannelJoin(player);
    }
}
