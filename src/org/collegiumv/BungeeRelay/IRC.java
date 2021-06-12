package org.collegiumv.BungeeRelay;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

abstract public class IRC {
    public Configuration config;
    public String version;
    public String SID;
    public Sender sender;
    public String channel;
    public long channelTS;
    public HashMap<String, Sender> senders = new HashMap<String, Sender>();
    public HashMap<ProxiedPlayer, User> players = new HashMap<ProxiedPlayer, User>();
    public HashMap<String, User> users = new HashMap<String, User>();
    public HashMap<ProxiedPlayer, String> replies = new HashMap<ProxiedPlayer, String>();
    Plugin plugin;

    Socket sock;
    private BufferedReader in;
    private PrintWriter out;

    public IRC(Socket sock, Configuration config, Plugin plugin) {
        this.sock = sock;
        this.config = config;
        this.plugin = plugin;

        version = plugin.getDescription().getVersion();
        SID = generateSID();
        sender = Server.create(this, config.getString("server.servername"), "0", SID, config.getString("server.realname"));
        channel = config.getString("server.channel");
    }

    abstract void doConnect();

    public void connect() throws IOException {
        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);
        doConnect();
        while (sock.isConnected()) handleData(in.readLine());
    }

    public boolean isConnected() {
        return sock.isConnected();
    }

    public void write(Sender sender, String command, String[] params) {
        if (!this.sock.isConnected()) {
            return;
        }

        ArrayList<String> parts = new ArrayList<String>();
        if (sender != null) {
            parts.add(":" + sender.id);
        }

        parts.add(command);

        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            if (i == params.length - 1) {
                parts.add(":" + param);
            } else {
                if (param.startsWith(":")) {
                    throw new RuntimeException("Non-terminal param cannot start with :");
                }
                if (param.contains(" ")) {
                    throw new RuntimeException("Non-terminal param cannot contain a space");
                }
                parts.add(param);
            }
        }

        out.println(String.join(" ", parts));
    }

    public void write(ProxiedPlayer player, String command, String[] data) {
        write(players.get(player), command, data);
    }

    public void write(String command, String[] data) {
        write((Sender)null, command, data);
    }

    abstract public void handleData(String data) throws IOException;

    abstract String generateSID();

    abstract public String getNextUid();

    abstract public boolean isValidNick(String nick);

    abstract public void sendUserConnect(ProxiedPlayer player);

    abstract public void sendChannelJoin(ProxiedPlayer player);

    public void sendQuit(User user) {
        write(user, "QUIT", new String[]{config.getString("formats.mcquit").replace("{SENDER}", user.name)});
    }

    public void sendPrivmsg(ProxiedPlayer player, String target, String message) {
        write(player, "PRIVMSG", new String[]{target, message});
    }

    public void sendPrivmsg(Sender sender, String target, String message) {
        write(sender, "PRIVMSG", new String[]{target, message});
    }

    void updateTS(String ts) {
        long timestamp = Long.parseLong(ts);
        if (timestamp < channelTS) {
            channelTS = timestamp;
        }
    }

    public String getUidByNick(String nick) {
        for (Map.Entry<String, User> entry : users.entrySet()) {
            if (nick.equalsIgnoreCase(entry.getValue().name)) return entry.getKey();
        }
        return null;
    }

    public ProxiedPlayer getPlayerByUid(String uid) {
        for (Map.Entry<ProxiedPlayer, User> entry : players.entrySet()) {
            if (uid.equalsIgnoreCase(entry.getValue().id)) return entry.getKey();
        }
        return null;
    }
}
