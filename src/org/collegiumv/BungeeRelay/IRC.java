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

public class IRC {
    public static Socket sock;
    public static BufferedReader in;
    public static Configuration config;
    public static String version;
    public static String SID;
    public static Sender Sender;
    public static String currentUid;
    public static String prefixModes;
    public static String chanModes;
    public static String argModes;
    public static int nickMax;
    public static long startTime;
    public static boolean authenticated;
    public static boolean capabState;
    public static String channel;
    public static long channelTS;
    public static HashMap<String, Sender> senders = new HashMap<String, Sender>();
    public static HashMap<ProxiedPlayer, User> players = new HashMap<ProxiedPlayer, User>();
    public static HashMap<String, User> users = new HashMap<String, User>();
    public static HashMap<ProxiedPlayer, String> replies = new HashMap<ProxiedPlayer, String>();
    Plugin plugin;

    private static PrintWriter out;

    public IRC(Socket sock, Configuration config, Plugin plugin) throws IOException {
        this.sock = sock;
        this.config = config;
        this.plugin = plugin;

        version = plugin.getDescription().getVersion();
        SID = generateSID();
        Sender = Server.create(config.getString("server.servername"), "0", SID, config.getString("server.realname"));
        currentUid = SID + "AAAAAA";
        authenticated = false;
        capabState = false;
        startTime = System.currentTimeMillis() / 1000;
        channelTS = startTime;
        channel = config.getString("server.channel");

        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);

        // Send our capabilities where we pretend we can do everything
        write("CAPAB", new String[]{"START", "1202"});
        write("CAPAB", new String[]{"CAPABILITIES", "PROTOCOL=1202"});
        write("CAPAB", new String[]{"END"});
        while (sock.isConnected()) handleData(in.readLine());
    }

    public static void write(Sender sender, String command, String[] params) {
        if (!IRC.sock.isConnected()) {
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

    public static void write(ProxiedPlayer player, String command, String[] data) {
        write(players.get(player), command, data);
    }

    public static void write(String command, String[] data) {
        write((Sender)null, command, data);
    }

    public void handleData(String data) throws IOException {
        if (data == null) throw new IOException();
        if (data.isEmpty()) return;

        if (config.getBoolean("server.debug")) plugin.getLogger().info("Received: "+data);

        // Normalize input so sender if and is in sender, command is in command,
        // and the arguments are in args and are 1 indexed
        String[] ex = data.trim().split(" ");
        String command, sender;
        int offset;
        if (ex[0].charAt(0) == ':') { // We have a sender
            sender = ex[0].substring(1);
            command = ex[1];
            if (!senders.containsKey(sender)) {
                if (command.equals("FMODE") || command.equals("MODE") || command.equals("KICK") || command.equals("KILL") || command.equals("TOPIC") || command.equals("ADDLINE") || command.equals("DELLINE")) {
                    // Dropping these commands would cause a de-sync c.f. treesocket2.cpp:251
                    sender = sender.substring(0,3);
                    if (!senders.containsKey(sender)) sender = SID; // If the server was split, fall back to our SID
                } else {
                    return; // Drop the command
                }
            }
            offset = 1;
        } else {
            sender = "";
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

        if (command.equals("ERROR")) {
            sock.close();
            plugin.getLogger().warning("Remote ERROR'd with message: " + args[1]);
            authenticated = false;
            capabState = false;
            throw new IOException(); // This will make us reconnect

        } else if (command.equals("CAPAB")) {
            if (args[1].equals("START")) {
                capabState = true;

            } else if (!capabState && authenticated) {
                plugin.getLogger().warning("CAPAB *MUST* start with CAPAB START after authentication");
                write("ERROR", new String[]{"Received CAPAB command without CAPAB START"});

            } else if (args[1].equals("CAPABILITIES")) {
                // Dynamically find which modes require arguments
                for (String s:args[2].split(" ")) {
                    if (s.contains("CHANMODES=")) {
                        chanModes = s.split("=")[1];
                        String[] chanmodeSets = chanModes.split(",",-1);
                        argModes = "";
                        // The first three sets take arguments
                        for (int i = 0; i < 3; ++i) {
                            argModes += chanmodeSets[i];
                        }
                    } else if (s.contains("NICKMAX=")) {
                        nickMax = Integer.parseInt(s.split("=")[1]);
                    } else if (s.contains("PREFIX=")) {
                        // Grab the modes inside the parens after the "="
                        prefixModes = s.split("=")[1].split("\\(")[1].split("\\)")[0];
                    }
                }

            } else if (args[1].equals("END")) {
                capabState = false;
                if (!authenticated) {
                    plugin.getLogger().info("Authenticating with server...");
                    write("SERVER", new String[]{config.getString("server.servername"), config.getString("server.sendpass"), "0", SID, config.getString("server.realname")});
                }
            }

        } else if (!authenticated) {
            if (command.equals("SERVER")) {
                // <servername> <password> <hopcount> <id> <description>
                if (!args[2].equals(config.getString("server.recvpass"))) {
                    plugin.getLogger().warning("The server "+args[1]+" presented the wrong password.");
                    plugin.getLogger().warning("Remember that the recvpass and sendpass are opposite to the ones in your links.conf");
                    write("ERROR", new String[]{"Password received was incorrect"});
                    sock.close();
                }
                authenticated = true;
                Server.create(args[1], args[3], args[4], args[5]);
                plugin.getLogger().info("Authentication successful");
                plugin.getLogger().info("Bursting");
                out.println(":" + SID + " BURST " + startTime);
                out.println(":" + SID + " VERSION :BungeeRelay-" + version);
                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                    sendUserConnect(player);
                    sendChannelJoin(player);
                }
                out.println(":" + SID + " ENDBURST");

            } else {
                plugin.getLogger().warning("Unrecognized command during authentication: " + data);
                write("ERROR", new String[]{"Unrecognized command during authentication " + command});
                sock.close();
            }

        } else { // We have already authenticated
            if (command.equals("ENDBURST")) {
                plugin.getLogger().info("Bursting done");

            } else if (command.equals("FJOIN")) {
                // <channel> <timestamp> +[<modes> {mode params}] [<[statusmodes],uuid> {<[statusmodes],uuid>}]
                if (args[1].equals(channel)) {
                    updateTS(args[2]);
                    String modes = args[3];
                    int countArgModes = 0;
                    for (Character c : argModes.toCharArray()) {
                        for (Character mode : modes.toCharArray()) {
                            if (mode.equals(c)) {
                                ++countArgModes;
                            }
                        }
                    }
                    for (String user : args[4+countArgModes].split(" ")) {
                        if (!user.contains(",") || !users.containsKey(user.split(",")[1])) continue; // If the user was KILLed while joining don't error
                        Util.sendAll(config.getString("formats.join")
                                .replace("{SENDER}", users.get(user.split(",")[1]).name));
                    }
                }

            } else if (command.equals("FMODE")) {
                // <target> <timestamp> <modes and parameters>
                if (args[1].equals(channel)) {
                    updateTS(args[2]);
                    String modes = "";
                    for (int i=3; i<args.length; i++) {
                        modes = modes + args[i] + " ";
                    }
                    Util.sendAll(config.getString("formats.mode")
                            .replace("{SENDER}", senders.get(sender).name)
                            .replace("{MODE}", modes));
                }

            } else if (command.equals("KICK")) {
                // <channel> <user>{,<user>} [<reason>]
                if (args[1].equals(channel)) {
                    for (String target : args[2].split(",")) {
                        handleKickKill("kick", sender, target, args[3]);
                    }
                }

            } else if (command.equals("KILL")) {
                // <user> <reason>
                handleKickKill("kill", sender, args[1], args[2]);

            } else if (command.equals("NOTICE")) {
                // <msgtarget> <text to be sent>
                Util.handleMessage("notice", sender, args[1], args[2]);

            } else if (command.equals("NICK")) {
                // <new_nick>
                Util.sendAll(config.getString("formats.nick")
                        .replace("{OLD_NICK}", users.get(sender).name)
                        .replace("{NEW_NICK}", args[1]));
                users.get(sender).name = args[1];

            } else if (command.equals("PART")) {
                String reason;
                if (args.length > 2) {
                    reason = args[2];
                } else {
                    reason = "";
                }
                Util.sendAll(config.getString("formats.part")
                        .replace("{SENDER}", users.get(sender).name)
                        .replace("{REASON}", reason));

            } else if (command.equals("PING")) {
                out.println(":" + SID + " PONG " + SID + " "+args[1]);

            } else if (command.equals("PRIVMSG")) {
                // <msgtarget> <text to be sent>
                Util.handleMessage("privmsg", sender, args[1], args[2]);

            } else if (command.equals("QUIT")) {
                // <reason>
                String reason;
                if (args.length > 1) {
                    reason = args[1];
                } else {
                    reason = "";
                }
                Util.sendAll(config.getString("formats.ircquit")
                        .replace("{SENDER}", users.get(sender).name)
                        .replace("{REASON}", reason));
                users.remove(sender);

            } else if (command.equals("SERVER")) {
                // <servername> <password> <hopcount> <id> <description>
                Server.create(args[1], args[3], args[4], args[5]);

            } else if (command.equals("UID")) {
                // <uid> <timestamp> <nick> <hostname> <displayed-hostname> <ident> <ip> <signon time> +<modes {mode params}> <gecos>
                User.create(sender, args[1], args[2], args[3], args[8]);
            }
        }
    }

    public static String generateSID() {
        // Yes it's slower to do % every time but Java doesn't have unsigned and this is run only once
        int SID = 0;
        for (char c : IRC.config.getString("server.servername").toCharArray()) {
            SID = (5 * SID + (int) c) % 1000;
        }
        for (char c : IRC.config.getString("server.realname").toCharArray()) {
            SID = (5 * SID + (int) c) % 1000;
        }
        return String.format("%03d", SID);
    }

    public static void incrementUid(int pos) {
        StringBuilder sb = new StringBuilder(IRC.currentUid);
        if (IRC.currentUid.charAt(pos) == 'Z') {
            sb.setCharAt(pos, '0');
            IRC.currentUid = sb.toString();
        } else if (IRC.currentUid.charAt(pos) == '9') {
            sb.setCharAt(pos, 'A');
            IRC.currentUid = sb.toString();
            if (pos == 3) return;
            incrementUid(pos - 1);
        } else {
            sb.setCharAt(pos, (char) (IRC.currentUid.charAt(pos) + 1));
            IRC.currentUid = sb.toString();
        }
    }

    public static void incrementUid() {
        do {
            incrementUid(8);
        } while (IRC.players.containsValue(IRC.currentUid));
    }

    public static boolean isValidNick(String nick) {
        if (nick.isEmpty() || nick.length() > IRC.nickMax)
            return false;

        char firstChar = nick.charAt(0);
        if ((firstChar >= '0' && firstChar <= '9') || firstChar == '-')
            return false;

        for (char c : nick.toCharArray()) {
            if (c >= 'A' && c <= '}') {
                // "A"-"}" can occur anywhere in a nickname
                continue;
            }
            if ((c >= '0' && c <= '9') || c == '-') {
                // "0"-"9", "-" can occur anywhere BUT the first char of a nickname
                continue;
            }
            return false;
        }
        return true;
    }

    public static void sendUserConnect(ProxiedPlayer player) {
        User user = IRC.players.get(player);
        IRC.write(IRC.Sender, "UID", new String[]{user.id, Long.toString(user.nickTime), user.name, player.getAddress().getHostName(), player.getAddress().getHostName(), IRC.config.getString("formats.ident").replace("{IDENT}", player.getName()), player.getAddress().getAddress().getHostAddress(), Long.toString(user.connectTime), "+r", "Minecraft Player"});
    }

    public static void sendChannelJoin(ProxiedPlayer player) {
        IRC.write(IRC.Sender, "FJOIN", new String[]{IRC.channel, Long.toString(IRC.channelTS), "+", "," + IRC.players.get(player).id});
    }

    public static void handleKickKill(String mode, String senderUID, String targetUID, String reason) {
        if (!(IRC.users.containsKey(targetUID) && IRC.senders.containsKey(senderUID))) return;
        String target = IRC.users.get(targetUID).name;
        String sender = IRC.senders.get(senderUID).name;
        Util.sendAll(IRC.config.getString("formats." + mode)
                .replace("{SENDER}", sender)
                .replace("{TARGET}", target)
                .replace("{REASON}", reason));
        ProxiedPlayer player = getPlayerByUid(targetUID);
        if (player != null) {
            if (IRC.config.getBoolean("server.reconnect" + mode)) {
                if (mode.equals("kill")) sendUserConnect(player);
                sendChannelJoin(player);
            } else {
                Util.disconnect(player, mode, sender, reason, target);
                IRC.players.get(player).delete();
            }
        }
    }

    public static void updateTS(String ts) {
        long timestamp = Long.parseLong(ts);
        if (timestamp < IRC.channelTS) {
            IRC.channelTS = timestamp;
        }
    }

    public static String getUidByNick(String nick) {
        for (Map.Entry<String, User> entry : IRC.users.entrySet()) {
            if (nick.equalsIgnoreCase(entry.getValue().name)) return entry.getKey();
        }
        return null;
    }

    public static ProxiedPlayer getPlayerByUid(String uid) {
        for (Map.Entry<ProxiedPlayer, User> entry : IRC.players.entrySet()) {
            if (uid.equalsIgnoreCase(entry.getValue().id)) return entry.getKey();
        }
        return null;
    }
}
