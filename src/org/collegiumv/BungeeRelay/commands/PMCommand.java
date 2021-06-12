package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.Arrays;

public class PMCommand extends Command {
    public PMCommand() {
        super("pm");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Util.sendError(sender, "Usage: /pm <user> <message ...>");
            return;
        }
        if (!IRC.sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        String uid = IRC.getUidByNick(args[0]);
        if (uid == null) {
            Util.sendError(sender, args[0] + " is not on IRC right now.");
            return;
        }

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(args));
        list.remove(0);
        String msg = String.join(" ", list);
        ProxiedPlayer player = (ProxiedPlayer) sender;
        IRC.write(player, "PRIVMSG", new String[]{uid, msg});
        IRC.replies.put(player, args[0]);
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', IRC.config.getString("formats.privatemsg")
                .replace("{SENDER}", sender.getName())
                .replace("{MESSAGE}", msg))));
    }
}
