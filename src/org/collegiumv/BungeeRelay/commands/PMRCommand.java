package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class PMRCommand extends Command {
    private IRC irc;

    public PMRCommand(IRC irc) {
        super("pmr");
        this.irc = irc;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        new PMReplyCommand(irc).execute(sender, args);
    }
}
