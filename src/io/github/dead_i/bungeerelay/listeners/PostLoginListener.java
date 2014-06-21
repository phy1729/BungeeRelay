package io.github.dead_i.bungeerelay.listeners;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class PostLoginListener implements Listener {
    Plugin plugin;
    public PostLoginListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (!IRC.sock.isConnected()) return;
        ProxiedPlayer p = event.getPlayer();
        String name = IRC.config.getString("server.userprefix") + player.getName() + IRC.config.getString("server.usersuffix");
        IRC.times.put(player, System.currentTimeMillis() / 1000);
        IRC.nickTimes.put(player, IRC.times.get(player));
        IRC.users.put(IRC.currentUid, name);
        IRC.uids.put(player, IRC.currentUid);
        Util.incrementUid();
        Util.sendUserConnect(p);
        String chan = IRC.config.getString("server.channel");
        if (!chan.isEmpty()) Util.sendChannelJoin(p, chan);
    }
}
