package org.collegiumv.BungeeRelay;

public class Server extends Sender {
    public int distance;
    public String description;

    private Server(String name, String distance, String id, String description) {
        this.name = name;
        this.distance = Integer.parseInt(distance);
        this.id = id;
        this.description = description;
    }

    public static Server create(String name, String distance, String id, String description) {
        Server server = new Server(name, distance, id, description);
        IRC.senders.put(id, server);
        return server;
    }
}
