package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
import org.collegiumv.BungeeRelay.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
        irc.changeNick((ProxiedPlayer)sender, args[0]);
    }
}
