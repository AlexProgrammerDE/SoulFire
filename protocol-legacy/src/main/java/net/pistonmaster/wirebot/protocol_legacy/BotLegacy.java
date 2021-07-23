package net.pistonmaster.wirebot.protocol_legacy;

import net.pistonmaster.wirebot.common.*;
import net.pistonmaster.wirebot.version.v1_10.ChatPacket1_10;
import net.pistonmaster.wirebot.version.v1_10.SessionListener1_10;
import net.pistonmaster.wirebot.version.v1_8.ChatPacket1_8;
import net.pistonmaster.wirebot.version.v1_8.SessionListener1_8;
import net.pistonmaster.wirebot.version.v1_9.ChatPacket1_9;
import net.pistonmaster.wirebot.version.v1_9.SessionListener1_9;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.packet.PacketProtocol;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class BotLegacy implements IBot {
    private final Options options;
    private final Logger logger;
    private final IPacketWrapper account;

    private Session session;
    private EntitiyLocation location;
    private float health = -1;
    private float food = -1;
    private final ServiceServer serviceServer;

    public BotLegacy(Options options, IPacketWrapper account, Logger log, ServiceServer serviceServer) {
        this(options, account, null, log, serviceServer);
    }

    public BotLegacy(Options options, IPacketWrapper account, InetSocketAddress proxyInfo, Logger log, ServiceServer serviceServer) {
        this.options = options;
        this.account = account;
        this.serviceServer = serviceServer;
        // TODO reverse implement proxy system

        this.logger = Logger.getLogger(account.getProfileName());
        this.logger.setParent(log);
    }

    public void connect(String host, int port) {
        Client client = new Client(host, port, (PacketProtocol) account, new TcpSessionFactory());
        this.session = client.getSession();

        // SessionService sessionService = new SessionService();
        // sessionService.setBaseUri(serviceServer.getSession());
        // session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService); // TODO

        SessionEventBus bus = new SessionEventBus(options, logger, this);

        switch (options.gameVersion) {
            case VERSION_1_8 -> session.addListener(new SessionListener1_8(bus));
            case VERSION_1_9 -> session.addListener(new SessionListener1_9(bus));
            case VERSION_1_10 -> session.addListener(new SessionListener1_10(bus));
            default -> throw new IllegalStateException("Unknown session listener");
        }

        session.connect();
    }

    public void sendMessage(String message) {
        switch (options.gameVersion) {
            case VERSION_1_8 -> session.send(new ChatPacket1_8(message));
            case VERSION_1_9 -> session.send(new ChatPacket1_9(message));
            case VERSION_1_10 -> session.send(new ChatPacket1_10(message));
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

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
