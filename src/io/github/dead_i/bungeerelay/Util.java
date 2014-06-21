package io.github.dead_i.bungeerelay;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

public class Util {
    private static ProxyServer proxy = ProxyServer.getInstance();

    public static void incrementUid(int pos) {
        StringBuilder sb = new StringBuilder(IRC.currentUid);
        if (IRC.currentUid.charAt(pos) == 'Z') {
            sb.setCharAt(pos, '0');
            IRC.currentUid = sb.toString();
        }else if (IRC.currentUid.charAt(pos) == '9') {
            sb.setCharAt(pos, 'A');
            IRC.currentUid = sb.toString();
            if (pos == 3) return;
            incrementUid(pos - 1);
        }else{
            sb.setCharAt(pos, (char) (IRC.currentUid.charAt(pos) + 1));
            IRC.currentUid = sb.toString();
        }
    }

    public static void incrementUid() {
        incrementUid(8);
    }

    public static void sendUserConnect(ProxiedPlayer player) {
        String name = IRC.config.getString("server.userprefix") + player.getName() + IRC.config.getString("server.usersuffix");
        IRC.times.put(player, System.currentTimeMillis() / 1000);
        IRC.uids.put(player, IRC.currentUid);
        IRC.users.put(IRC.currentUid, name);
        IRC.out.println("UID " + IRC.currentUid + " " + System.currentTimeMillis() / 1000 + " " + name + " " + player.getAddress().getHostName() + " " + player.getAddress().getHostName() + " " + player.getName() + " " + player.getAddress().getHostString() + " " + IRC.times.get(player) + " +r :Minecraft Player");
    }

    public static void sendChannelJoin(ProxiedPlayer player, String channel) {
        String prefix = "";
        if (player.hasPermission("irc.owner")) prefix += "q";
        if (player.hasPermission("irc.protect")) prefix += "a";
        if (player.hasPermission("irc.op")) prefix += "o";
        if (player.hasPermission("irc.halfop")) prefix += "h";
        if (player.hasPermission("irc.voice")) prefix += "v";
        prefix = verifyPrefix(prefix);
        IRC.out.println("FJOIN " + channel + " " + System.currentTimeMillis() / 1000 + " :" + prefix + "," + IRC.uids.get(player));
    }

    public static void sendBotJoin(String channel) {
    String prefix = verifyPrefix(IRC.config.getString("bot.modes"));
        IRC.out.println("FJOIN " + channel + " " + getChanTS(channel) + " :" + prefix + "," + IRC.botUID);
    }

    private static String verifyPrefix(String prefix) {
    // Gracefully degrade to the next highest mode if the server doesn't support one
    // +o and +v are built-in so no need to check for those
    String finalPrefix = "";
    switch (true)
        case prefix.contains("q"):
            if (IRC.prefixModes.contains("q") finalPrefix += "q" && break;
        case prefix.contains("a"):
            if (IRC.prefixModes.contains("a") finalPrefix += "a" && break;
        case prefix.contains("o"):
            finalPrefix += "o" && break;
        case prefix.contains("h"):
            if (IRC.prefixModes.contains("h") finalPrefix += "h" && break;
        case prefix.contains("v"):
            finalPrefix += "v" && break;
    return finalPrefix;
    }

    public static boolean giveChannelModes(String channel, String m) {
        String modes = m.split(" ")[0];
        for (int i=0; i<modes.length(); i++) {
            String mode = Character.toString(modes.charAt(i));
            if (!IRC.prefixModes.contains(mode) && !IRC.chanModes.contains(mode) && !mode.equals("+") && !mode.equals("-")) {
                proxy.getLogger().warning("Tried to set the +" + mode + " mode, but the IRC server stated earlier that it wasn't compatible with this mode.");
                proxy.getLogger().warning("If you want to use " + mode + ", enable appropriate module in your IRC server's configuration files.");
                proxy.getLogger().warning("Skipping...");
                return false;
            }
        }
        IRC.out.println(":" + IRC.SID + " FMODE " + channel + " " + getChanTS(channel) + " " + m);
        return true;
    }

    public static void setChannelTopic(String channel, String topic) {
    if (!t.isEmpty()) IRC.out.println(":" + SID + " TOPIC " + channel + " :" + topic);
    }

    public static List<String> getChannels() {
        List<String> out = new ArrayList<String>();
        String channel = IRC.config.getString("server.channel");
        if (channel.isEmpty()) {
            for (ServerInfo si : proxy.getServers().values()) {
                out.add(IRC.config.getString("server.chanprefix") + si.getName());
            }
        }else{
            out.add(channel);
        }
        return out;
    }

    public static Collection<ProxiedPlayer> getPlayersByChannel(String c) {
        if (IRC.config.getString("server.staff").equalsIgnoreCase(c)) {
            return Collections.emptyList();
        }else if (IRC.config.getString("server.channel").isEmpty()) {
            String pref = IRC.config.getString("server.chanprefix");
            if (c.startsWith(pref)) c = c.substring(pref.length());
            ServerInfo si = proxy.getServerInfo(c);
            if (si == null) return Collections.emptyList();
            return si.getPlayers();
        }else{
            return proxy.getPlayers();
        }
    }

    public static String getUidByNick(String nick) {
        for (Map.Entry<String, String> entry : IRC.users.entrySet()) {
            if (nick.equalsIgnoreCase(entry.getValue())) return entry.getKey();
        }
        return null;
    }

    public static Long getChanTS(String c) {
        if (!IRC.chans.containsKey(c)) IRC.chans.put(c, new Channel(System.currentTimeMillis() / 1000));
        return IRC.chans.get(c).ts;
    }

    public static String sliceStringArray(String[] a, Integer l) {
        String s = "";
        for (int i=l; i<a.length; i++) {
            s += a[i] + " ";
        }
        return s.trim();
    }
}
