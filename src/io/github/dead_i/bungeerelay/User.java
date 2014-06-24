package io.github.dead_i.bungeerelay;

public class User {
    public String nick;
    public long connectTime;
    public long nickTime;
    public boolean local;
    public String server; // User's server's SID if local = false

    public User(String nick) {
        local = true;
        this.nick = nick;
        connectTime = System.currentTimeMillis() / 1000;
        nickTime = connectTime;
    }

    public User(String server, String nickTime, String nick, String connectTime) {
        local = false;
        this.server = server;
        this.nick = nick;
        this.nickTime = Util.stringToTS(nickTime);
        this.connectTime = Util.stringToTS(connectTime);
    }
}
