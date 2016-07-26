package com.github.games647.lambdaattack;

import java.net.Proxy;
import java.util.logging.Logger;

import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

public class Bot implements AutoCloseable {

    public static final char COMMAND_IDENTIFIER = '/';

    private final Proxy proxy;
    private final Logger logger;
    private final MinecraftProtocol account;

    private Session session;
    private EntitiyLocation location;
    private float health = -1;
    private float food = -1;

    public Bot(MinecraftProtocol account) {
        this(account, Proxy.NO_PROXY);
    }
    
    public Bot(MinecraftProtocol account, Proxy proxy) {
        this.account = account;
        this.proxy = proxy;

        this.logger = Logger.getLogger(account.getProfile().getName());
        this.logger.setParent(LambdaAttack.getLogger());
    }

    public void connect(String host, int port) throws RequestException {
        Client client = new Client(host, port, account, new TcpSessionFactory(proxy));
        this.session = client.getSession();
        client.getSession().addListener(new SessionListener(this));

        client.getSession().connect();
    }

    public void sendMessage(String message) {
        if (session != null) {
            ClientChatPacket chatPacket = new ClientChatPacket(message);
            session.send(chatPacket);
        }
    }

    public boolean isOnline() {
        return session != null && session.isConnected();
    }

    public Session getSession() {
        return session;
    }

    public EntitiyLocation getLocation() {
        return location;
    }

    protected void setLocation(EntitiyLocation location) {
        this.location = location;
    }

    public double getHealth() {
        return health;
    }

    protected void setHealth(float health) {
        this.health = health;
    }

    public float getFood() {
        return food;
    }

    protected void setFood(float food) {
        this.food = food;
    }

    public Logger getLogger() {
        return logger;
    }

    public GameProfile getGameProfile() {
        return account.getProfile();
    }

    @Override
    public void close() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
