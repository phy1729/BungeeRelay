BungeeRelay
===========
BungeeRelay is a proxy plugin for BungeeCord that aims to take IRC to a whole new level, by providing seamless integration with your IRC network. The majority of pre-existing solutions consist of a single bot connecting to a network and relaying any chat information. This plugin takes this a step further by connecting to your InspIRCd network as a server so that each player appears as a separate user from IRC. This opens up many possibilities, as you can interact with players as normal IRC users, rather than doing it through bot commands. It also means that your IRC channel user list will be populated with your players, rather than a single relay bot.

Setting up
----------
To use this plugin on your own server, you simply need to drop this plugin and the BungeeYAML plugin into your BungeeCord installation's "plugins" folder. When you first run the plugin, a default config.yml will be created there, which contains some tips on configuring the plugin.

Compiling
---------
The plugin makes use of Maven to enable fast and easy compiling. If you wish to compile the plugin, simply run `mvn package`.

Licence
-------
This plugin and its source code is licenced under the ISC License.
