public class User {
    public String nick;
    public long connectTime;
    public long nickTime;

    public User(String nick) {
        this.nick = nick;
        connectTime = System.currentTimeMillis() / 1000;
        nickTime = connectTime;
    }
}
