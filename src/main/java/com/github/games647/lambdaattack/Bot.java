package com.github.games647.lambdaattack;

import java.net.Proxy;

import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

public class Bot implements AutoCloseable {

    private final Proxy proxy;

    private final String username;
    private final String password;

    private Session session;

    public Bot(String username, String password) {
        this(username, password, Proxy.NO_PROXY);
    }

    public Bot(String username, String password, Proxy proxy) {
        this.username = username;
        this.password = password;

        this.proxy = proxy;
    }

    public Bot(String username) {
        this(username, "");
    }

    public MinecraftProtocol authenticate() throws RequestException {
        MinecraftProtocol protocol;
        if (!password.isEmpty()) {
            protocol = new MinecraftProtocol(username, password);
            System.out.println("Successfully authenticated user.");
        } else {
            protocol = new MinecraftProtocol(username);
        }

        return protocol;
    }

    public boolean isOnline() {
        return session != null && session.isConnected();
    }

    public Session getSession() {
        return session;
    }

    public void connect(String host, int port) throws RequestException {
        MinecraftProtocol account = authenticate();

        Client client = new Client(host, port, account, new TcpSessionFactory(proxy));
        this.session = client.getSession();
        client.getSession().addListener(new SessionListener());

        client.getSession().connect();
    }

    @Override
    public void close() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
