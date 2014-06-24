package io.github.dead_i.bungeerelay;

import net.craftminecraft.bungee.bungeeyaml.bukkitapi.file.FileConfiguration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IRC {
    public final Socket sock;
    public final BufferedReader in;
    public final PrintWriter out;
    public final FileConfiguration config;
    public final String SID;
    public String currentUid;
    public String prefixModes;
    public String chanModes;
    public String argModes;
    public final long startTime;
    public boolean authenticated;
    public boolean capabState;
    public String channel;
    public long channelTS;
    public HashMap<ProxiedPlayer, String> uids = new HashMap<ProxiedPlayer, String>();
    public HashMap<ProxiedPlayer, String> replies = new HashMap<ProxiedPlayer, String>();
    public HashMap<String, User> users = new HashMap<String, User>();
    Plugin final plugin;


    public IRC(Socket sock, FileConfiguration config, Plugin plugin) throws IOException {
        this.sock = sock;
        this.config = config;
        this.plugin = plugin;

        SID = config.getString("server.id");
        currentUid = SID + "AAAAAA";
        authenticated = false;
        capabState = false;
        startTime = System.currentTimeMillis() / 1000;
        channelTS = startTime;
        channel = config.getString("server.channel");

        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);

        // Send our capabilities which we pretend we can do everything
        out.println("CAPAB START 1202");
        out.println("CAPAB CAPABILITIES :PROTOCOL=1202");
        out.println("CAPAB END");
        while (sock.isConnected()) handleData(in.readLine());
    }

    private int countChar(String s, Character c)
    {
        int count = 0;
        for (Character charInString:s.toCharArray()) {
            if (charInString.equals(c)) {
                ++count;
            }
        }
        return count;
    }

    public void handleData(String data) throws IOException {
        if (data == null) throw new IOException();
        if (data.isEmpty()) return;

        if (config.getBoolean("server.debug")) plugin.getLogger().info("Received: "+data);

        // Normalize input so sender if and is in sender, command is in command,
        // and the arguments are in args and are 1 indexed
        String[] args, ex = data.trim().split(" ");
        String command, sender;
        if (ex[0].charAt(0) == ':') { // We have a sender
            sender = ex[0].substring(1);
            command = ex[1];
            args = new String[ ex.length-1 ];
            for (int i = 0; i < ex.length-1; i++) {
                args[i] = ex[i+1];
            }
        } else {
            sender = "";
            command = ex[0];
            args = ex;
        }

        if (command.equals("ERROR")) {
            sock.close();
            authenticated = false;
            capabState = false;
            throw new IOException(); // This will make us reconnect

        } else if (command.equals("CAPAB")) {
            if (args[1].equals("START")) {
                capabState = true;

            } else if (!capabState && authenticated) {
                plugin.getLogger().warning("CAPAB *MUST* start with CAPAB START after authentication");
                out.println("ERROR :Received CAPAB command without CAPAB START" + command);

            } else if (args[1].equals("CAPABILITIES")) {
                // Dynamically find which modes require arguments
                for (String s:args) {
                    if (s.contains("CHANMODES=")) {
                        chanModes = s.split("=")[1];
                        String[] chanmodeSets = chanModes.split(",");
                        argModes = "";
                        // The first three sets take arguments
                        for (int i = 0; i < 3; ++i) {
                            argModes += chanmodeSets[i];
                        }
                    }
                    if (s.contains("PREFIX=")) {
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
                out.println(":" + SID + " VERSION :BungeeRelay-0.1");
                out.println(":" + SID + " ENDBURST");
                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                    Util.sendUserConnect(player);
                    Util.sendChannelJoin(player);
                }

            } else {
                plugin.getLogger().warning("Unrecognized command during authentication: " + data);
                out.println("ERROR :Unrecognized command during authentication " + command);
                sock.close();
            }

        } else { // We have already authenticated
            if (command.equals("ADDLINE")) {
            } else if (command.equals("AWAY")) {
            } else if (command.equals("BURST")) {
            } else if (command.equals("ENDBURST")) {
                plugin.getLogger().info("Bursting done");

            } else if (command.equals("FJOIN")) {
                // <channel> <timestamp> +[<modes> {mode params}] [:<[statusmodes],uuid> {<[statusmodes],uuid>}]
                if (args[1] == channel) {
                    Util.updateTS(args[2]);
                }
                String modes = args[3];
                int countArgModes = 0;
                for (Character c:argModes.toCharArray()) {
                    countArgModes += countChar (modes, c);
                }
                for (ProxiedPlayer p : Util.getPlayersByChannel(args[1])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.join")
                            .replace("{SENDER}", users.get(args[5].split(",")[1]).nick))));
                }

            } else if (command.equals("FMODE")) {
                // <target> <timestamp> <modes and parameters>
                if (args[1] == channel) {
                    Util.updateTS(args[2]);
                    String modes;
                    for (int i=3; i<args.length; i++) {
                        modes = modes + args[i] + " ";
                    }
                    Util.sendAll(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.mode")
                                .replace("{SENDER}", users.get(sender).nick)
                                .replace("{MODE}", modes))));
                }
            } else if (command.equals("FTOPIC")) {

            } else if (command.equals("KICK")) {
                // <channel>{,<channel>} <user>{,<user>} [:<comment>]
                String reason = Util.sliceStringArray(args, 3).substring(1);
                String target = users.get(args[2]).nick;
                String senderNick = users.get(sender).nick;
                for (ProxiedPlayer p : Util.getPlayersByChannel(args[1])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.kick")
                            .replace("{SENDER}", sender)
                            .replace("{TARGET}", target)
                            .replace("{REASON}", reason))));
                }
                String full = users.get(args[2]).nick;
                int prefixlen = config.getString("server.userprefix").length();
                int suffixlen = config.getString("server.usersuffix").length();
                if (config.getBoolean("server.kick") && prefixlen < full.length() && suffixlen < full.length()) {
                    ProxiedPlayer player = plugin.getProxy().getPlayer(full.substring(prefixlen, full.length() - suffixlen));
                    if (player != null) {
                        player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.disconnectkick")
                                .replace("{SENDER}", sender)
                                .replace("{TARGET}", target)
                                .replace("{REASON}", reason))));
                    }
                }
                users.remove(args[2]);

            } else if (command.equals("METADATA")) {
            } else if (command.equals("NOTICE")) {
            } else if (command.equals("NICK")) {
                // <new_nick>
                // FIXME
                // Util.sendAll(users.get(sender).nick + " is now known as " + args[1]);
                users.get(sender).nick = args[1];
            } else if (command.equals("OPERTYPE")) {
            } else if (command.equals("PART")) {
                String reason;
                if (args.length > 2) {
                    reason = Util.sliceStringArray(args, 2).substring(1);
                } else {
                    reason = "";
                }
                for (ProxiedPlayer p : Util.getPlayersByChannel(args[1])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.part")
                            .replace("{SENDER}", users.get(sender).nick)
                            .replace("{REASON}", reason))));
                }

            } else if (command.equals("PING")) {
                out.println(":" + SID + " PONG " + SID + " "+args[1]);

            } else if (command.equals("PRIVMSG")) {
                String from = users.get(sender).nick;
                String player = users.get(args[1]).nick;
                int prefixlen = config.getString("server.userprefix").length();
                int suffixlen = config.getString("server.usersuffix").length();
                Collection<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>();
                boolean isPM;
                if (player != null && prefixlen + suffixlen < player.length()) {
                    ProxiedPlayer to = plugin.getProxy().getPlayer(player.substring(prefixlen, player.length() - suffixlen));
                    isPM = (users.containsKey(args[1]) && to != null);
                    if (isPM) {
                        players.add(to);
                        replies.put(to, from);
                    }
                } else {
                    isPM = false;
                }
                if (!isPM) players = Util.getPlayersByChannel(args[1]);
                for (ProxiedPlayer p : players) {
                    int len;
                    if (args[2].equals(":" + (char) 1 + "ACTION")) {
                        len = 3;
                    } else {
                        len = 2;
                    }
                    String s = Util.sliceStringArray(args, len);
                    String out;
                    if (len == 4) {
                        if (isPM) {
                            out = config.getString("formats.privateme");
                        } else {
                            out = config.getString("formats.me");
                        }
                        s = s.replaceAll(Character.toString((char) 1), "");
                    } else {
                        if (isPM) {
                            out = config.getString("formats.privatemsg");
                        } else {
                            out = config.getString("formats.msg");
                        }
                        s = s.substring(1);
                    }
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', out)
                            .replace("{SENDER}", from)
                            .replace("{MESSAGE}", s)));
                }

            } else if (command.equals("QUIT")) {
                String reason;
                if (args.length > 2) {
                    reason = Util.sliceStringArray(args, 1).substring(1);
                } else {
                    reason = "";
                }
                Util.sendAll(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.quit")
                            .replace("{SENDER}", users.get(sender).nick)
                            .replace("{REASON}", reason))));
                users.remove(sender);

            } else if (command.equals("SERVER")) {
            } else if (command.equals("SNONOTICE")) {
            } else if (command.equals("UID")) {
                // FIXME
                // users.put(args[1], args[3]);
            } else if (command.equals("VERSION")) {
            } else {
                plugin.getLogger().warning("Unrecognized command: " + data);
            }
        }
    }
}
