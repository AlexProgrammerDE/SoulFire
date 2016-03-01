package com.github.games647.minecraftstresstester;

import java.net.Proxy;

import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

public class Bot {

    private static final Proxy PROXY = Proxy.NO_PROXY;

    private final String username;
    private final String password;

    private Session session;

    public Bot(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Bot(String username) {
        this(username, "");
    }

    public MinecraftProtocol authenticate() throws RequestException {
        MinecraftProtocol protocol;
        if (!password.isEmpty()) {
            protocol = new MinecraftProtocol(username, password, false);
            System.out.println("Successfully authenticated user.");
        } else {
            protocol = new MinecraftProtocol(username);
        }

        return protocol;
    }

    public void query(String host, int port) {
//        MinecraftProtocol protocol = new MinecraftProtocol(SubProtocol.STATUS);
//        Client client = new Client(host, port, protocol, new TcpSessionFactory());
//        client.getSession().setFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY, new ServerInfoHandler() {
//            @Override
//            public void handle(Session session, ServerStatusInfo info) {
//                System.out.println("Version: " + info.getVersionInfo().getVersionName() + ", " + info.getVersionInfo().getProtocolVersion());
//                System.out.println("Player Count: " + info.getPlayerInfo().getOnlinePlayers() + " / " + info.getPlayerInfo().getMaxPlayers());
//                System.out.println("Players: " + Arrays.toString(info.getPlayerInfo().getPlayers()));
//                System.out.println("Description: " + info.getDescription().getFullText());
//                System.out.println("Icon: " + info.getIcon());
//            }
//        });
//
//        client.getSession().setFlag(MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY, new ServerPingTimeHandler() {
//            @Override
//            public void handle(Session session, long pingTime) {
//                System.out.println("Server ping took " + pingTime + "ms");
//            }
//        });
//
//        client.getSession().connect();
//        while(client.getSession().isConnected()) {
//            try {
//                Thread.sleep(5);
//            } catch(InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public Session getSession() {
        return session;
    }

    public void connect(String host, int port) throws RequestException {
        MinecraftProtocol account = authenticate();

        Client client = new Client(host, port, account, new TcpSessionFactory(PROXY));
        this.session = client.getSession();
        client.getSession().addListener(new SessionListener());

        client.getSession().connect();
    }
}
