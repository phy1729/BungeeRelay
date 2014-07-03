package org.collegiumv.BungeeRelay.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class PMRCommand extends Command {
    public PMRCommand() {
        super("pmr");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        new PMReplyCommand().execute(sender, args);
    }
}
