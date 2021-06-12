package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
import org.collegiumv.BungeeRelay.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class IRCNickCommand extends Command {
    private IRC irc;

    public IRCNickCommand(IRC irc) {
        super("ircnick");
        this.irc = irc;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            Util.sendError(sender, "Usage: /ircnick <nick>");
            return;
        }
        if (!irc.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        if (!irc.isValidNick(args[0])) {
            Util.sendError(sender, "The nick " + args[0] + " is invalid.");
            return;
        }
        if (irc.getUidByNick(args[0]) != null) {
            Util.sendError(sender, "The nick " + args[0] + " is already in use.");
            return;
        }

        User user = irc.players.get(sender);
        user.name = args[0];
        user.nickTime = System.currentTimeMillis() / 1000;
        irc.write(user, "NICK", new String[]{user.name, Long.toString(user.nickTime)});
    }
}
