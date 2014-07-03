package io.github.dead_i.bungeerelay;

import io.github.dead_i.bungeerelay.commands.*;
import io.github.dead_i.bungeerelay.listeners.*;
import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Main extends ConfigurablePlugin {
    public void onEnable() {
        // Save the default configuration
        saveDefaultConfig();

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
            IRC.initialize(new Socket(getConfig().getString("server.host"), getConfig().getInt("server.port")), getConfig(), this);
        } catch (UnknownHostException e) {
            handleDisconnect();
        } catch (IOException e) {
            handleDisconnect();
        }
    }

    public void handleDisconnect() {
        getLogger().info("Disconnected from server.");
        int reconnect = getConfig().getInt("server.reconnect");
        if (reconnect >= 0) {
            getLogger().info("Reconnecting in " + reconnect / 1000 + " seconds...");
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }, reconnect, TimeUnit.MILLISECONDS);
        }
    }
}
