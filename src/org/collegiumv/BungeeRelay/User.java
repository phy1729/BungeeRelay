package org.collegiumv.BungeeRelay;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class User extends Sender {
    public long connectTime;
    public long nickTime;
    public String server;
    public boolean local;
    public ProxiedPlayer player;

    private IRC irc;

    private User(IRC irc, ProxiedPlayer player) {
        this.irc = irc;
        this.player = player;
        id = irc.currentUid;
        irc.incrementUid();
        if (irc.getUidByNick(player.getName()) == null) { // No collison, use their nick
            name = player.getName();
        } else {
            name = irc.config.getString("server.userprefix") + player.getName() + irc.config.getString("server.usersuffix");
        }
        local = true;
        connectTime = System.currentTimeMillis() / 1000;
        nickTime = connectTime;
        server = irc.SID;
    }

    public static User create(IRC irc, ProxiedPlayer player) {
        User user = new User(irc, player);
        irc.senders.put(user.id, user);
        irc.players.put(player, user);
        irc.users.put(user.id, user);
        return user;
    }

    private User(IRC irc, String server, String id, String nickTime, String name, String connectTime) {
        local = false;
        this.irc = irc;
        this.server = server;
        this.id = id;
        this.name = name;
        this.nickTime = Long.parseLong(nickTime);
        this.connectTime = Long.parseLong(connectTime);
    }

    public static User create(IRC irc, String server, String id, String nickTime, String name, String connectTime) {
        User user = new User(irc, server, id, nickTime, name, connectTime);
        irc.senders.put(user.id, user);
        irc.users.put(user.id, user);
        return user;
    }

    public void delete() {
        irc.users.remove(id);
        irc.senders.remove(id);
        if (local) {
            irc.players.remove(player);
            irc.replies.remove(player);
        }
    }
}
