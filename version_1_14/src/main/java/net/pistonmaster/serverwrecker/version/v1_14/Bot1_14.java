package net.pistonmaster.serverwrecker.version.v1_14;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.pistonmaster.serverwrecker.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Bot1_14 extends AbstractBot {
    private final Options options;
    private final Proxy proxyInfo;
    private final Logger logger;
    private final IPacketWrapper account;
    private final ServiceServer serviceServer;
    private Session session;

    public Bot1_14(Options options, IPacketWrapper account, InetSocketAddress address, Logger log, ServiceServer serviceServer, ProxyType proxyType, String username, String password) {
        this.options = options;
        this.account = account;
        if (address == null) {
            this.proxyInfo = null;
        } else {
            this.proxyInfo = new Proxy(convertType(proxyType), address);
        }

        this.serviceServer = serviceServer;

        this.logger = LoggerFactory.getLogger(account.getProfileName());
    }

    public void connect(String host, int port) {
        Client client;
        if (proxyInfo == null) {
            client = new Client(host, port, (PacketProtocol) account, new TcpSessionFactory());
        } else {
            client = new Client(host, port, (PacketProtocol) account, new TcpSessionFactory(proxyInfo));
        }
        this.session = client.getSession();

        // SessionService sessionService = new SessionService();
        // sessionService.setBaseUri(serviceServer.getSession());
        // session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService); // TODO

        SessionEventBus bus = new SessionEventBus(options, logger, this);

        session.addListener(new SessionListener1_14(bus));

        session.connect();
    }

    public void sendMessage(String message) {
        session.send(new ClientChatPacket(message));
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

    public Proxy getProxy() {
        return proxyInfo;
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }

    public Proxy.Type convertType(ProxyType type) {
        return switch (type) {
            case HTTP -> Proxy.Type.HTTP;
            case SOCKS4, SOCKS5 -> Proxy.Type.SOCKS;
        };
    }
}
