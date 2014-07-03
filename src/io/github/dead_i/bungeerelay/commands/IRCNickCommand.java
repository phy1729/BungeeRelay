package io.github.dead_i.bungeerelay.commands;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
import io.github.dead_i.bungeerelay.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class IRCNickCommand extends Command {
    public IRCNickCommand() {
        super("ircnick");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            Util.sendError(sender, "Usage: /ircnick <nick>");
            return;
        }
        if (!IRC.sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        if (!Util.isValidNick(args[0])) {
            Util.sendError(sender, "The nick " + args[0] + " is invalid.");
            return;
        }
        if (Util.getUidByNick(args[0]) != null) {
            Util.sendError(sender, "The nick " + args[0] + " is already in use.");
            return;
        }

        User user = IRC.users.get(IRC.uids.get(sender));
        user.nick = args[0];
        user.nickTime = System.currentTimeMillis() / 1000;
        IRC.out.println(":" + IRC.uids.get(sender) + " NICK " + user.nick + " " + user.nickTime);
    }
}
