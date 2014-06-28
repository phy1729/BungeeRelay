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
    public static Socket sock;
    public static BufferedReader in;
    public static PrintWriter out;
    public static FileConfiguration config;
    public static String SID;
    public static String botUID;
    public static String currentUid;
    public static String prefixModes;
    public static String chanModes;
    public static long startTime = System.currentTimeMillis() / 1000;
    public static boolean authenticated;
    public static HashMap<ProxiedPlayer, Long> times = new HashMap<ProxiedPlayer, Long>();
    public static boolean capabState;
    public static HashMap<ProxiedPlayer, String> uids = new HashMap<ProxiedPlayer, String>();
    public static HashMap<ProxiedPlayer, String> replies = new HashMap<ProxiedPlayer, String>();
    public static HashMap<String, String> users = new HashMap<String, String>();
    public static HashMap<String, Channel> chans = new HashMap<String, Channel>();
    Plugin plugin;

    private static String argModes = "";

    public IRC(Socket s, FileConfiguration c, Plugin p) throws IOException {
        sock = s;
        config = c;
        plugin = p;

        SID = config.getString("server.id");
        botUID = SID + "AAAAAA";
        currentUid = SID + "AAAAAB";
        authenticated = false;
        capabState = false;

        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(sock.getOutputStream(), true);

        // Send our capabilities where we pretend we can do everything
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
                out.println("BURST " + startTime);
                out.println("VERSION :0.1");
                out.println("UID " + botUID + " " + startTime + " " + config.getString("bot.nick") + " BungeeRelay " + config.getString("bot.host") + " " + config.getString("bot.ident") + " BungeeRelay " + startTime + " +o :" + config.getString("bot.realname"));
                out.println(":" + botUID + " OPERTYPE " + config.getString("bot.opertype"));
                String chan = config.getString("server.channel");
                String botmodes = config.getString("bot.modes");
                Util.sendMainJoin(chan, botmodes);
                for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    Util.sendUserConnect(p);
                    Util.sendChannelJoin(p, chan);
                    Util.incrementUid();
                }
                out.println("ENDBURST");

            } else {
                plugin.getLogger().warning("Unrecognized command during authentication: " + data);
                out.println("ERROR :Unrecognized command during authentication " + command);
                sock.close();
            }

        } else { // We have already authenticated
            if (command.equals("ENDBURST")) {
                plugin.getLogger().info("Bursting done");

            } else if (command.equals("FJOIN")) {
                if (!chans.containsKey(args[1])) {
                    Long ts = Long.parseLong(args[2]);
                    if (!ts.equals(Util.getChanTS(args[1]))) chans.get(args[1]).ts = ts;
                }
                String modes = args[3];
                int countArgModes = 0;
                for (Character c:argModes.toCharArray()) {
                    countArgModes += countChar (modes, c);
                }
                chans.get(args[1]).users.add(args[4+countArgModes].split(",")[1]);
                for (ProxiedPlayer p : Util.getPlayersByChannel(args[1])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.join")
                            .replace("{SENDER}", users.get(args[5].split(",")[1])))));
                }

            } else if (command.equals("FMODE")) {
                String s = "";
                String d = "+";
                int v = 4;
                for (int i=0; i<args[3].length(); i++) {
                    String m = Character.toString(args[3].charAt(i));
                    String[] cm = chanModes.split(",");
                    if (m.equals("b") && chans.containsKey(args[1])) chans.get(ex[1]).bans.add(args[v]);
                    if (m.equals("+") || m.equals("-")) {
                        d = m;
                    }else if (cm[0].contains(m) || cm[1].contains(m) || (cm[2].contains(m) && d.equals("+"))) {
                        s = s + ex[v] + " ";
                        v++;
                    }else if (args.length > v && users.containsKey(args[v])) {
                        s = s + users.get(ex[v]) + " ";
                        v++;
                    }
                }
                for (ProxiedPlayer p : Util.getPlayersByChannel(args[1])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.mode")
                            .replace("{SENDER}", users.get(sender))
                            .replace("{MODE}", args[3] + " " + s))));
                }

            } else if (command.equals("KICK")) {
                String reason = args[3];
                String target = users.get(args[2]);
                String senderNick = users.get(sender);
                for (ProxiedPlayer p : Util.getPlayersByChannel(args[1])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.kick")
                            .replace("{SENDER}", senderNick)
                            .replace("{TARGET}", target)
                            .replace("{REASON}", reason))));
                }
                String full = users.get(args[2]);
                int prefixlen = config.getString("server.userprefix").length();
                int suffixlen = config.getString("server.usersuffix").length();
                if (config.getBoolean("server.kick") && prefixlen < full.length() && suffixlen < full.length()) {
                    ProxiedPlayer player = plugin.getProxy().getPlayer(full.substring(prefixlen, full.length() - suffixlen));
                    if (player != null) {
                        player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.disconnectkick")
                                .replace("{SENDER}", senderNick)
                                .replace("{TARGET}", target)
                                .replace("{REASON}", reason))));
                    }
                }
                users.remove(args[2]);

            } else if (command.equals("PART")) {
                String reason;
                if (args.length > 2) {
                    reason = args[2];
                } else {
                    reason = "";
                }
                for (ProxiedPlayer p : Util.getPlayersByChannel(ex[2])) {
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.part")
                            .replace("{SENDER}", users.get(sender))
                            .replace("{REASON}", reason))));
                }

            } else if (command.equals("PING")) {
                out.println("PONG " + SID + " "+args[1]);

            } else if (command.equals("PRIVMSG")) {
                String from = users.get(sender);
                String player = users.get(args[1]);
                int prefixlen = config.getString("server.userprefix").length();
                int suffixlen = config.getString("server.usersuffix").length();
                Collection<ProxiedPlayer> players = new ArrayList<ProxiedPlayer>();
                boolean isPM;
                if (player != null && prefixlen < player.length() && suffixlen < player.length()) {
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
                    String s = args[2];
                    String ch = Character.toString((char) 1);
                    String out;
                    if (s.charAt(0) == (char) 1) {
                        if (isPM) {
                            out = config.getString("formats.privateme");
                        } else {
                            out = config.getString("formats.me");
                        }
                        s = s.replaceAll(ch, "");
                    } else {
                        if (isPM) {
                            out = config.getString("formats.privatemsg");
                        } else {
                            out = config.getString("formats.msg");
                        }
                    }
                    p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', out)
                            .replace("{SENDER}", from)
                            .replace("{MESSAGE}", s)));
                }

            } else if (command.equals("QUIT")) {
                String reason;
                if (args.length > 1) {
                    reason = args[1];
                } else {
                    reason = "";
                }
                for (Map.Entry<String, Channel> ch : chans.entrySet()) {
                    String chan = IRC.config.getString("server.channel");
                    if (chan.isEmpty()) {
                        chan = ch.getKey();
                    }else if (!ch.getKey().equals(chan)) {
                        continue;
                    }
                    if (!ch.getValue().users.contains(sender)) continue;
                    for (ProxiedPlayer p : Util.getPlayersByChannel(chan)) {
                        p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', config.getString("formats.quit")
                                .replace("{SENDER}", users.get(sender))
                                .replace("{REASON}", reason))));
                    }
                }
                users.remove(sender);

            } else if (command.equals("UID")) {
                users.put(args[1], args[3]);
            }
        }
    }
}
