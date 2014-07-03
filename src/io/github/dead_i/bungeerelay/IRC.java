package io.github.dead_i.bungeerelay;

import net.craftminecraft.bungee.bungeeyaml.bukkitapi.file.FileConfiguration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class IRC {
    public static Socket sock;
    public static BufferedReader in;
    public static PrintWriter out;
    public static FileConfiguration config;
    public static String version;
    public static String SID;
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
    public static HashMap<ProxiedPlayer, String> uids = new HashMap<ProxiedPlayer, String>();
    public static HashMap<ProxiedPlayer, String> replies = new HashMap<ProxiedPlayer, String>();
    public static HashMap<String, User> users = new HashMap<String, User>();
    Plugin plugin;

    public IRC(Socket sock, FileConfiguration config, Plugin plugin) throws IOException {
        this.sock = sock;
        this.config = config;
        this.plugin = plugin;

        version = plugin.getDescription().getVersion();
        SID = Util.generateSID();
        currentUid = SID + "AAAAAA";
        authenticated = false;
        capabState = false;
        startTime = System.currentTimeMillis() / 1000;
        channelTS = startTime;
        channel = config.getString("server.channel");

        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);

        // Send our capabilities where we pretend we can do everything
        out.println("CAPAB START 1202");
        out.println("CAPAB CAPABILITIES :PROTOCOL=1202");
        out.println("CAPAB END");
        while (sock.isConnected()) handleData(in.readLine());
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
                out.println("ERROR :Received CAPAB command without CAPAB START");

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
                    out.println("SERVER " + config.getString("server.servername") + " " + config.getString("server.sendpass") + " 0 " + SID + " :" + config.getString("server.realname"));
                }
            }

        } else if (!authenticated) {
            if (command.equals("SERVER")) {
                // <servername> <password> <hopcount> <id> <description>
                if (!args[2].equals(config.getString("server.recvpass"))) {
                    plugin.getLogger().warning("The server "+args[1]+" presented the wrong password.");
                    plugin.getLogger().warning("Remember that the recvpass and sendpass are opposite to the ones in your links.conf");
                    out.println("ERROR :Password received was incorrect");
                    sock.close();
                }
                authenticated = true;
                plugin.getLogger().info("Authentication successful");
                plugin.getLogger().info("Bursting");
                out.println(":" + SID + " BURST " + startTime);
                out.println(":" + SID + " VERSION :BungeeRelay-" + version);
                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                    Util.sendUserConnect(player);
                    Util.sendChannelJoin(player);
                }
                out.println(":" + SID + " ENDBURST");

            } else {
                plugin.getLogger().warning("Unrecognized command during authentication: " + data);
                out.println("ERROR :Unrecognized command during authentication " + command);
                sock.close();
            }

        } else { // We have already authenticated
            if (command.equals("ENDBURST")) {
                plugin.getLogger().info("Bursting done");

            } else if (command.equals("FJOIN")) {
                // <channel> <timestamp> +[<modes> {mode params}] [<[statusmodes],uuid> {<[statusmodes],uuid>}]
                if (args[1].equals(channel)) {
                    Util.updateTS(args[2]);
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
                        Util.sendAll(config.getString("formats.join")
                                .replace("{SENDER}", users.get(user.split(",")[1]).nick));
                    }
                }

            } else if (command.equals("FMODE")) {
                // <target> <timestamp> <modes and parameters>
                if (args[1].equals(channel)) {
                    Util.updateTS(args[2]);
                    String modes = "";
                    for (int i=3; i<args.length; i++) {
                        modes = modes + args[i] + " ";
                    }
                    Util.sendAll(config.getString("formats.mode")
                            .replace("{SENDER}", users.get(sender).nick)
                            .replace("{MODE}", modes));
                }

            } else if (command.equals("KICK")) {
                // <channel> <user>{,<user>} [<reason>]
                if (args[1].equals(channel)) {
                    for (String target : args[2].split(",")) {
                        Util.handleKickKill("kick", sender, target, args[3]);
                    }
                }

            } else if (command.equals("KILL")) {
                // <user> <reason>
                Util.handleKickKill("kill", sender, args[1], args[2]);

            } else if (command.equals("NOTICE")) {
                // <msgtarget> <text to be sent>
                Util.handleMessage("notice", sender, args[1], args[2]);

            } else if (command.equals("NICK")) {
                // <new_nick>
                Util.sendAll(config.getString("formats.nick")
                        .replace("{OLD_NICK}", users.get(sender).nick)
                        .replace("{NEW_NICK}", args[1]));
                users.get(sender).nick = args[1];

            } else if (command.equals("PART")) {
                String reason;
                if (args.length > 2) {
                    reason = args[2];
                } else {
                    reason = "";
                }
                Util.sendAll(config.getString("formats.part")
                        .replace("{SENDER}", users.get(sender).nick)
                        .replace("{REASON}", reason));

            } else if (command.equals("PING")) {
                out.println(":" + SID + " PONG " + SID + " "+args[1]);

            } else if (command.equals("PRIVMSG")) {
                // <msgtarget> <text to be sent>
                Util.handleMessage("privmsg", sender, args[1], args[2]);

            } else if (command.equals("QUIT")) {
                // <reason>
                String reason;
                if (args.length > 2) {
                    reason = args[1];
                } else {
                    reason = "";
                }
                Util.sendAll(config.getString("formats.ircquit")
                        .replace("{SENDER}", users.get(sender).nick)
                        .replace("{REASON}", reason));
                users.remove(sender);

            } else if (command.equals("UID")) {
                // <uid> <timestamp> <nick> <hostname> <displayed-hostname> <ident> <ip> <signon time> +<modes {mode params}> <gecos>
                users.put(args[1], new User(sender, args[2], args[3], args[8]));
            }
        }
    }
}
