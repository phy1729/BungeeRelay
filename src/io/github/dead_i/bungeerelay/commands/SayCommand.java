package io.github.dead_i.bungeerelay.commands;

import io.github.dead_i.bungeerelay.IRC;
import io.github.dead_i.bungeerelay.Util;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class SayCommand extends Command {
    public SayCommand() {
        super("say", "irc.say");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Util.sendError(sender,"Usage: /say <message ...>");
            return;
        }
        StringBuilder msg = new StringBuilder();
        for (String a : args) msg.append(a);
        Util.sendAll(IRC.getInstance().config.getString("formats.saycommand").replace("{MESSAGE}", msg.toString()));
        if (!IRC.getInstance().sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }

        IRC.getInstance().printChannelMsg(msg.toString());


    }
}
