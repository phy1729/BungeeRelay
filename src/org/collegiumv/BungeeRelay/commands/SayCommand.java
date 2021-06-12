package org.collegiumv.BungeeRelay.commands;

import org.collegiumv.BungeeRelay.IRC;
import org.collegiumv.BungeeRelay.Util;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class SayCommand extends Command {
    private IRC irc;

    public SayCommand(IRC irc) {
        super("say", "irc.say");
        this.irc = irc;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Util.sendError(sender,"Usage: /say <message ...>");
            return;
        }
        StringBuilder msg = new StringBuilder();
        for (String a : args) msg.append(a);
        Util.sendAll(irc.config.getString("formats.saycommand").replace("{MESSAGE}", msg.toString()));
        if (!irc.sock.isConnected()) {
            Util.sendError(sender, "The proxy is not connected to IRC.");
            return;
        }
        irc.write(irc.Sender, "PRIVMSG", new String[]{irc.channel, msg.toString()});
    }
}
