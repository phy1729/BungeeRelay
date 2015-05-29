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
import java.util.concurrent.TimeUnit;

public class Main extends Plugin {
    Configuration config;
    public void onEnable() {
        // Save the default configuration
        // replace saveDefaultConfig();
        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch(IOException e) {
            this.getLogger().severe("Unable to load log file");
        }

        // Register listeners
        getProxy().getPluginManager().registerListener(this, new ChatListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener());
        getProxy().getPluginManager().registerListener(this, new PostLoginListener());

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new SayCommand());
        getProxy().getPluginManager().registerCommand(this, new PMCommand());
        getProxy().getPluginManager().registerCommand(this, new PMReplyCommand());
        getProxy().getPluginManager().registerCommand(this, new IRCNickCommand());

        // Register aliases
        getProxy().getPluginManager().registerCommand(this, new PMRCommand());

        // Initiate the connection, which will, in turn, pass the socket to the IRC class
        getProxy().getScheduler().runAsync(this, new Runnable() {
            public void run() {
                connect();
            }
        });
    }

    public void connect() {
        getLogger().info("Attempting connection...");
        try {
            new IRC(new Socket(this.config.getString("server.host"), this.config.getInt("server.port")), this.config, this);
        } catch (UnknownHostException e) {
            handleDisconnect();
        } catch (IOException e) {
            handleDisconnect();
        }
    }

    public void handleDisconnect() {
        getLogger().info("Disconnected from server.");
        int reconnect = this.config.getInt("server.reconnect");
        if (reconnect >= 0) {
            getLogger().info("Reconnecting in " + reconnect / 1000 + " seconds...");
            getProxy().getScheduler().schedule(this, new Runnable() {
                public void run() {
                    connect();
                }
            }, reconnect, TimeUnit.MILLISECONDS);
        }
    }
}
