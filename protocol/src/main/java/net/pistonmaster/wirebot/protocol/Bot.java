package net.pistonmaster.wirebot.protocol;

import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.pistonmaster.wirebot.common.*;
import net.pistonmaster.wirebot.version.v1_11.ChatPacket1_11;
import net.pistonmaster.wirebot.version.v1_11.SessionListener1_11;
import net.pistonmaster.wirebot.version.v1_12.ChatPacket1_12;
import net.pistonmaster.wirebot.version.v1_12.SessionListener1_12;
import net.pistonmaster.wirebot.version.v1_13.ChatPacket1_13;
import net.pistonmaster.wirebot.version.v1_13.SessionListener1_13;
import net.pistonmaster.wirebot.version.v1_14.ChatPacket1_14;
import net.pistonmaster.wirebot.version.v1_14.SessionListener1_14;
import net.pistonmaster.wirebot.version.v1_15.ChatPacket1_15;
import net.pistonmaster.wirebot.version.v1_15.SessionListener1_15;
import net.pistonmaster.wirebot.version.v1_16.ChatPacket1_16;
import net.pistonmaster.wirebot.version.v1_16.SessionListener1_16;
import net.pistonmaster.wirebot.version.v1_17.ChatPacket1_17;
import net.pistonmaster.wirebot.version.v1_17.SessionListener1_17;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class Bot implements IBot {
    private final Options options;
    private final ProxyInfo proxyInfo;
    private final Logger logger;
    private final IPacketWrapper account;

    private Session session;
    private EntitiyLocation location;
    private float health = -1;
    private float food = -1;
    private final ServiceServer serviceServer;

    public Bot(Options options, IPacketWrapper account, Logger log, ServiceServer serviceServer) {
        this(options, account, null, log, serviceServer);
    }

    public Bot(Options options, IPacketWrapper account, InetSocketAddress proxyInfo, Logger log, ServiceServer serviceServer) {
        this.options = options;
        this.account = account;
        this.proxyInfo = new ProxyInfo(ProxyInfo.Type.SOCKS5, proxyInfo);
        this.serviceServer = serviceServer;

        this.logger = Logger.getLogger(account.getProfileName());
        this.logger.setParent(log);
    }

    public void connect(String host, int port) {
        Client client;
        if (proxyInfo == null) {
            client = new Client(host, port, (PacketProtocol) account, new TcpSessionFactory());
        } else {
            client = new Client(host, port, (PacketProtocol) account, new TcpSessionFactory(proxyInfo));
        }
        this.session = client.getSession();

        SessionService sessionService = new SessionService();
        sessionService.setBaseUri(serviceServer.getSession());
        // session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService); // TODO

        SessionEventBus bus = new SessionEventBus(options, logger, this);

        switch (options.gameVersion) {
            case VERSION_1_11 -> session.addListener(new SessionListener1_11(bus));
            case VERSION_1_12 -> session.addListener(new SessionListener1_12(bus));
            case VERSION_1_13 -> session.addListener(new SessionListener1_13(bus));
            case VERSION_1_14 -> session.addListener(new SessionListener1_14(bus));
            case VERSION_1_15 -> session.addListener(new SessionListener1_15(bus));
            case VERSION_1_16 -> session.addListener(new SessionListener1_16(bus));
            case VERSION_1_17 -> session.addListener(new SessionListener1_17(bus));
            default -> throw new IllegalStateException("Unknown session listener");
        }

        session.connect();
    }

    public void sendMessage(String message) {
        switch (options.gameVersion) {
            case VERSION_1_11 -> session.send(new ChatPacket1_11(message));
            case VERSION_1_12 -> session.send(new ChatPacket1_12(message));
            case VERSION_1_13 -> session.send(new ChatPacket1_13(message));
            case VERSION_1_14 -> session.send(new ChatPacket1_14(message));
            case VERSION_1_15 -> session.send(new ChatPacket1_15(message));
            case VERSION_1_16 -> session.send(new ChatPacket1_16(message));
            case VERSION_1_17 -> session.send(new ChatPacket1_17(message));
            default -> throw new IllegalArgumentException("Invalid game version");
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

    public void setLocation(EntitiyLocation location) {
        this.location = location;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getFood() {
        return food;
    }

    public void setFood(float food) {
        this.food = food;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyInfo getProxy() {
        return proxyInfo;
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
