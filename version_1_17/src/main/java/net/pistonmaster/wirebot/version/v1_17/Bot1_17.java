package net.pistonmaster.wirebot.version.v1_17;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import net.pistonmaster.wirebot.common.*;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class Bot1_17 extends AbstractBot {
    private final Options options;
    private final ProxyInfo proxyInfo;
    private final Logger logger;
    private final IPacketWrapper account;
    private final ServiceServer serviceServer;
    private Session session;

    public Bot1_17(Options options, IPacketWrapper account, InetSocketAddress address, Logger log, ServiceServer serviceServer, ProxyType proxyType) {
        this.options = options;
        this.account = account;
        if (address == null) {
            this.proxyInfo = null;
        } else {
            this.proxyInfo = new ProxyInfo(ProxyInfo.Type.valueOf(proxyType.name()), address);
        }

        this.serviceServer = serviceServer;

        this.logger = Logger.getLogger(account.getProfileName());
        this.logger.setParent(log);
    }

    public void connect(String host, int port) {
        if (proxyInfo == null) {
            session = new TcpClientSession(host, port, (PacketProtocol) account);
        } else {
            session = new TcpClientSession(host, port, (PacketProtocol) account, proxyInfo);
        }

        // SessionService sessionService = new SessionService();
        // sessionService.setBaseUri(serviceServer.getSession());
        // session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService); // TODO

        SessionEventBus bus = new SessionEventBus(options, logger, this);

        session.addListener(new SessionListener1_17(bus));

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

    public ProxyInfo getProxy() {
        return proxyInfo;
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
