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
        if (!IRC.sock.isConnected()) return;
        ProxiedPlayer player = event.getPlayer();
        String playerUID = IRC.currentUid;
        Util.incrementUid();
        IRC.uids.put(player, playerUID);
        String nick;
        if (Util.getUidByNick(player.getName()) == null) { // No collison, use their nick
            nick = player.getName();
        } else {
            nick = IRC.config.getString("server.userprefix") + player.getName() + IRC.config.getString("server.usersuffix");
        }
        IRC.users.put(playerUID, new User(nick));
        Util.sendUserConnect(player);
        Util.sendChannelJoin(player);
    }
}
