package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
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
        Util.sendAll(IRC.config.getString("formats.saycommand").replace("{MESSAGE}", msg.toString()));
        if (!IRC.sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        IRC.write(IRC.Sender, "PRIVMSG", new String[]{IRC.channel, msg.toString()});
    }
}
