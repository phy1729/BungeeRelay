package io.github.dead_i.bungeerelay.commands;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class IRCNickCommand extends Command {
    Plugin plugin;
    public IRCNickCommand(Plugin plugin) {
        super("ircnick", "irc.nick");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Util.sendError(sender, "Usage: /ircnick <nick>");
            return;
        }
        if (!IRC.sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        if (Util.getUidByNick(args[0]) != null) {
            Util.sendError(sender, "The nick " + args[0] + " is already in use.");
            return;
        }
	IRC.out.println(":" + IRC.uids.get(sender) + " NICK " + args[0] + " " + System.currentTimeMillis() / 1000);
    }
}
