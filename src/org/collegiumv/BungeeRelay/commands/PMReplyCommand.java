package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class PMReplyCommand extends Command {
    private IRC irc;

    public PMReplyCommand(IRC irc) {
        super("pmreply");
        this.irc = irc;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Util.sendError(sender, "Usage: /pmreply <message ...>");
            return;
        }
        if (!irc.sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        if (!irc.replies.containsKey(sender)) {
            Util.sendError(sender, "You must be engaged within a conversation to use this.");
            return;
        }

        String[] newargs = new String[args.length + 1];
        newargs[0] = irc.replies.get(sender);
        for (int i = 0; i < args.length; i++) {
            newargs[i+1] = args[i];
        }
        new PMCommand(irc).execute(sender, newargs);
    }
}
