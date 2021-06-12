package org.collegiumv.BungeeRelay;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class User extends Sender {
    public long connectTime;
    public long nickTime;
    public String server;
    public boolean local;
    public ProxiedPlayer player;

    private User(ProxiedPlayer player) {
        this.player = player;
        id = IRC.currentUid;
        IRC.incrementUid();
        if (IRC.getUidByNick(player.getName()) == null) { // No collison, use their nick
            name = player.getName();
        } else {
            name = IRC.config.getString("server.userprefix") + player.getName() + IRC.config.getString("server.usersuffix");
        }
        local = true;
        connectTime = System.currentTimeMillis() / 1000;
        nickTime = connectTime;
        server = IRC.SID;
    }

    public static User create(ProxiedPlayer player) {
        User user = new User(player);
        IRC.senders.put(user.id, user);
        IRC.players.put(player, user);
        IRC.users.put(user.id, user);
        return user;
    }

    private User(String server, String id, String nickTime, String name, String connectTime) {
        local = false;
        this.server = server;
        this.id = id;
        this.name = name;
        this.nickTime = Long.parseLong(nickTime);
        this.connectTime = Long.parseLong(connectTime);
    }

    public static User create(String server, String id, String nickTime, String name, String connectTime) {
        User user = new User(server, id, nickTime, name, connectTime);
        IRC.senders.put(user.id, user);
        IRC.users.put(user.id, user);
        return user;
    }

    public void delete() {
        IRC.users.remove(id);
        IRC.senders.remove(id);
        if (local) {
            IRC.players.remove(player);
            IRC.replies.remove(player);
        }
    }
}
