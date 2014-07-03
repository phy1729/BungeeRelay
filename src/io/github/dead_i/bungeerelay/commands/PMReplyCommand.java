package io.github.dead_i.bungeerelay.commands;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class PMReplyCommand extends Command {
    public PMReplyCommand() {
        super("pmreply");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Util.sendError(sender, "Usage: /pmreply <message ...>");
            return;
        }
        if (!IRC.getInstance().sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        if (!IRC.getInstance().replies.containsKey(sender)) {
            Util.sendError(sender, "You must be engaged within a conversation to use this.");
            return;
        }

        String[] newargs = new String[args.length + 1];
        newargs[0] = IRC.getInstance().replies.get(sender);
        for (int i = 0; i < args.length; i++) {
            newargs[i+1] = args[i];
        }
        new PMCommand().execute(sender, newargs);
    }
}
