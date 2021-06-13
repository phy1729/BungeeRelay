package org.collegiumv.BungeeRelay;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.collegiumv.BungeeRelay.commands.*;
import org.collegiumv.BungeeRelay.listeners.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class Main extends Plugin {
    Configuration config;
    public void onEnable() {
        // Save the default configuration
        // replace saveDefaultConfig();

        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
                Files.copy(getResourceAsStream("config.yml"), file.toPath());
            } catch(IOException e) {
                this.getLogger().severe("Unable to write default config");
            }
        }

        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch(IOException e) {
            this.getLogger().severe("Unable to load log file");
        }

        Socket socket;
        try {
            socket = new Socket(this.config.getString("server.host"), this.config.getInt("server.port"));
        } catch (IOException e) {
            this.getLogger().severe("Unable to connect.");
            throw new RuntimeException(e);
        }
        final IRC irc = new InspIRCd(socket, this.config, this);

        // Register listeners
        getProxy().getPluginManager().registerListener(this, new ChatListener(this, irc));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener(irc));
        getProxy().getPluginManager().registerListener(this, new PostLoginListener(irc));

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new SayCommand(irc));
        getProxy().getPluginManager().registerCommand(this, new PMCommand(irc));
        getProxy().getPluginManager().registerCommand(this, new PMReplyCommand(irc));
        getProxy().getPluginManager().registerCommand(this, new IRCNickCommand(irc));

        // Register aliases
        getProxy().getPluginManager().registerCommand(this, new PMRCommand(irc));

        // Initiate the connection, which will, in turn, pass the socket to the IRC class
        getProxy().getScheduler().runAsync(this, new Runnable() {
            public void run() {
                connect(irc);
            }
        });
    }

    public void connect(IRC irc) {
        getLogger().info("Attempting connection...");
        try {
            irc.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleDisconnect(final IRC irc) {
        getLogger().info("Disconnected from server.");
        int reconnect = this.config.getInt("server.reconnect");
        if (reconnect >= 0) {
            getLogger().info("Reconnecting in " + reconnect / 1000 + " seconds...");
            getProxy().getScheduler().schedule(this, new Runnable() {
                public void run() {
                    connect(irc);
                }
            }, reconnect, TimeUnit.MILLISECONDS);
        }
    }
}
