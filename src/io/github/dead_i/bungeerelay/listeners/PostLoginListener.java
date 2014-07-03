package io.github.dead_i.bungeerelay.listeners;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
import io.github.dead_i.bungeerelay.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PostLoginListener implements Listener {
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (!IRC.getInstance().sock.isConnected()) return;
        ProxiedPlayer player = event.getPlayer();

        IRC.getInstance().addPlayer(player);

        Util.sendUserConnect(player);
        Util.sendChannelJoin(player);
    }
}
