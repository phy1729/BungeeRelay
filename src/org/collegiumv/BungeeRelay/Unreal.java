package org.collegiumv.BungeeRelay;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Unreal extends IRC {
    private String currentUid;
    private String prefixModes;
    private String chanModes;
    private String argModes;
    private int nickMax;
    private long startTime;
    private boolean authenticated;
    private boolean capabState;

    public Unreal(Socket sock, Configuration config, Plugin plugin) {
        super(sock, config, plugin);

        currentUid = SID + "AAAAAA";
        authenticated = false;
        capabState = false;
        startTime = System.currentTimeMillis() / 1000;
        channelTS = startTime;
    }

    void doConnect() {
        // Send our capabilities where we pretend we can do everything
        plugin.getLogger().info("Authenticating with server...");
        write("PASS", new String[]{config.getString("server.sendpass")});
        write("PROTOCTL", new String[]{"EAUTH=" + config.getString("server.servername"), "SID=" + SID});
        write("PROTOCTL", new String[]{"NOQUIT", "NICKv2", "SJOIN", "SJ3", "NICKIP", "TKLEXT2"});
        write("SERVER", new String[]{config.getString("server.servername"), "1", config.getString("server.realname")});
        write("EOS", new String[]{});
    }

    public void handleCommand(String sender, String command, String[] args) throws IOException {
        if (sender != null && !senders.containsKey(sender)) {
            if (command.equals("FMODE") || command.equals("MODE") || command.equals("KICK") || command.equals("KILL") || command.equals("TOPIC") || command.equals("ADDLINE") || command.equals("DELLINE")) {
                // Dropping these commands would cause a de-sync c.f. treesocket2.cpp:251
                sender = sender.substring(0,3);
                if (!senders.containsKey(sender)) sender = SID; // If the server was split, fall back to our SID
            } else {
                return; // Drop the command
            }
        }

        if (command.equals("ERROR")) {
            sock.close();
            plugin.getLogger().warning("Remote ERROR'd with message: " + args[1]);
            authenticated = false;
            capabState = false;
            throw new IOException(); // This will make us reconnect

        } else if (!authenticated) {
            if (command.equals("PASS")) {
                // <password>
                if (!args[1].equals(config.getString("server.recvpass"))) {
                    plugin.getLogger().warning("The server presented the wrong password.");
                    plugin.getLogger().warning("Remember that the recvpass and sendpass are opposite to the ones in your links.conf");
                    write("ERROR", new String[]{"Password received was incorrect"});
                    sock.close();
                }
                authenticated = true;

            } else {
                plugin.getLogger().warning("Unrecognized command during authentication: " + command);
                write("ERROR", new String[]{"Unrecognized command during authentication " + command});
                sock.close();
            }

        } else { // We have already authenticated
            if (command.equals("FMODE")) {
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
                Util.handleMessage(this, "notice", sender, args[1], args[2]);

            } else if (command.equals("NICK")) {
                // <new_nick> <timestamp>
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
                write(this.sender, "PONG", new String[]{args[1]});

            } else if (command.equals("PRIVMSG")) {
                // <msgtarget> <text to be sent>
                Util.handleMessage(this, "privmsg", sender, args[1], args[2]);

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
                Server.create(this, args[1], args[3], args[4], args[5]);

            } else if (command.equals("SJOIN")) {
                // <timestamp> <channel> +[<modes> {mode params}] :buffer
                if (args[2].equals(channel)) {
                    updateTS(args[1]);
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

            } else if (command.equals("UID")) {
                // <nickname> <hopcount> <timestamp> <username> <hostname> <uid> <servicestamp> <umodes> <virthost> <cloakedhost> <ip> <gecos>
                User.create(this, sender, args[6], args[3], args[1], args[3]);
            }
        }
    }

    public String generateSID() {
        // Yes it's slower to do % every time but Java doesn't have unsigned and this is run only once
        int SID = 0;
        for (char c : config.getString("server.servername").toCharArray()) {
            SID = (5 * SID + (int) c) % 1000;
        }
        for (char c : config.getString("server.realname").toCharArray()) {
            SID = (5 * SID + (int) c) % 1000;
        }
        return String.format("%03d", SID);
    }

    private void incrementUid(int pos) {
        StringBuilder sb = new StringBuilder(currentUid);
        if (currentUid.charAt(pos) == 'Z') {
            sb.setCharAt(pos, '0');
            currentUid = sb.toString();
        } else if (currentUid.charAt(pos) == '9') {
            sb.setCharAt(pos, 'A');
            currentUid = sb.toString();
            if (pos == 3) return;
            incrementUid(pos - 1);
        } else {
            sb.setCharAt(pos, (char) (currentUid.charAt(pos) + 1));
            currentUid = sb.toString();
        }
    }

    public String getNextUid() {
        String uid = currentUid;
        do {
            incrementUid(8);
        } while (players.containsValue(currentUid));
        return uid;
    }

    public boolean isValidNick(String nick) {
        if (nick.isEmpty() || nick.length() > nickMax)
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

    void doChangeNick(User user, String newNick) {
        user.nickTime = System.currentTimeMillis() / 1000;
        write(user, "NICK", new String[]{newNick, Long.toString(user.nickTime)});
    }

    public void sendUserConnect(ProxiedPlayer player) {
        User user = players.get(player);
        write(sender, "UID", new String[]{user.id, Long.toString(user.nickTime), user.name, player.getAddress().getHostName(), player.getAddress().getHostName(), config.getString("formats.ident").replace("{IDENT}", player.getName()), player.getAddress().getAddress().getHostAddress(), Long.toString(user.connectTime), "+r", "Minecraft Player"});
    }

    public void sendChannelJoin(ProxiedPlayer player) {
        write(sender, "FJOIN", new String[]{channel, Long.toString(channelTS), "+", "," + players.get(player).id});
    }

    public void handleKickKill(String mode, String senderUID, String targetUID, String reason) {
        if (!(users.containsKey(targetUID) && senders.containsKey(senderUID))) return;
        String target = users.get(targetUID).name;
        String sender = senders.get(senderUID).name;
        Util.sendAll(config.getString("formats." + mode)
                .replace("{SENDER}", sender)
                .replace("{TARGET}", target)
                .replace("{REASON}", reason));
        ProxiedPlayer player = getPlayerByUid(targetUID);
        if (player != null) {
            if (config.getBoolean("server.reconnect" + mode)) {
                if (mode.equals("kill")) sendUserConnect(player);
                sendChannelJoin(player);
            } else {
                Util.disconnect(this, player, mode, sender, reason, target);
                players.get(player).delete();
            }
        }
    }
}
