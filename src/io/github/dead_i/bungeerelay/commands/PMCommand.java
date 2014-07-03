package io.github.dead_i.bungeerelay.commands;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
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
        String uid = Util.getUidByNick(args[0]);
        if (uid == null) {
            Util.sendError(sender, args[0] + " is not on IRC right now.");
            return;
        }

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(args));
        list.remove(0);
        StringBuilder msg = new StringBuilder();
        for (String a : list) msg.append(a).append(" ");
        IRC.out.println(":" + IRC.uids.get(sender) + " PRIVMSG " + uid + " :" + msg);
        IRC.replies.put((ProxiedPlayer) sender, args[0]);
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', IRC.config.getString("formats.privatemsg")
                .replace("{SENDER}", sender.getName())
                .replace("{MESSAGE}", msg))));
    }
}
