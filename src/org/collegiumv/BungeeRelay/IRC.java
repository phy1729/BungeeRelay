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

    public boolean isConnected() {
        return sock.isConnected();
    }

    abstract void doConnect();

    public void connect() throws IOException {
        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);
        doConnect();
        while (sock.isConnected()) handleLine(in.readLine());
    }

    abstract public void handleCommand(String sender, String command, String[] params) throws IOException;

    private void handleLine(String data) throws IOException {
        if (data == null) throw new IOException();
        if (data.isEmpty()) return;

        if (config.getBoolean("server.debug")) plugin.getLogger().info("Received: " + data);

        // Normalize input so sender if and is in sender, command is in command,
        // and the arguments are in args and are 1 indexed
        String[] ex = data.trim().split(" ");
        String command, sender;
        int offset;
        if (ex[0].charAt(0) == ':') { // We have a sender
            sender = ex[0].substring(1);
            command = ex[1];
            offset = 1;
        } else {
            sender = null;
            command = ex[0];
            offset = 0;
        }

        // If any arg aside from sender starts with a colon the rest of the args are considered one arg
        ArrayList<String> tempArgs = new ArrayList<String>();
        for (int i = offset; i < ex.length; i++) {
            if (ex[i].charAt(0) == ':') {
                String last = ex[i].substring(1); // remove the colon from the first token
                for (i++ ; i < ex.length; i++) {
                    last += " " + ex[i];
                }
                tempArgs.add(last);
                break;
            }
            tempArgs.add(ex[i]);
        }
        String[] args = new String[tempArgs.size()];
        args = tempArgs.toArray(args);

        handleCommand(sender, command, args);
    }

    void write(Sender sender, String command, String[] params) {
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

        String line = String.join(" ", parts);
        if (config.getBoolean("server.debug")) plugin.getLogger().info("Sending: " + line);
        out.println(line);
    }

    void write(ProxiedPlayer player, String command, String[] data) {
        write(players.get(player), command, data);
    }

    void write(String command, String[] data) {
        write((Sender)null, command, data);
    }

    abstract String generateSID();

    abstract public String getNextUid();

    abstract public boolean isValidNick(String nick);

    abstract void doChangeNick(User user, String newNick);

    public void changeNick(ProxiedPlayer player, String newNick) {
        if (!this.isConnected()) {
            Util.sendError(player, "The proxy is not connected to IRC.");
            return;
        }
        if (!this.isValidNick(newNick)) {
            Util.sendError(player, "The nick " + newNick + " is invalid.");
            return;
        }
        if (this.getUidByNick(newNick) != null) {
            Util.sendError(player, "The nick " + newNick + " is already in use.");
            return;
        }

        User user = players.get(player);
        this.doChangeNick(user, newNick);
        user.name = newNick;
    }

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
