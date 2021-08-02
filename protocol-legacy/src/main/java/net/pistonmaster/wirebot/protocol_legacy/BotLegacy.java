package net.pistonmaster.wirebot.protocol_legacy;

import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.pistonmaster.wirebot.common.*;
import net.pistonmaster.wirebot.version.v1_10.ChatPacket1_10;
import net.pistonmaster.wirebot.version.v1_10.SessionListener1_10;
import net.pistonmaster.wirebot.version.v1_8.ChatPacket1_8;
import net.pistonmaster.wirebot.version.v1_8.SessionListener1_8;
import net.pistonmaster.wirebot.version.v1_9.ChatPacket1_9;
import net.pistonmaster.wirebot.version.v1_9.SessionListener1_9;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class BotLegacy extends AbstractBot {
    private final Options options;
    private final Logger logger;
    private final IPacketWrapper account;

    private Session session;

    public BotLegacy(Options options, IPacketWrapper account, Logger log) {
        this(options, account, null, log);
    }

    public BotLegacy(Options options, IPacketWrapper account, InetSocketAddress proxyInfo, Logger log) {
        this.options = options;
        this.account = account;
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

    public Logger getLogger() {
        return logger;
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
