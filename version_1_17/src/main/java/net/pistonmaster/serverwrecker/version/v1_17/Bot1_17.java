package net.pistonmaster.serverwrecker.version.v1_17;

import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import net.pistonmaster.serverwrecker.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Bot1_17 extends AbstractBot {
    private final Options options;
    private final ProxyInfo proxyInfo;
    private final Logger logger;
    private final IPacketWrapper account;
    private final ServiceServer serviceServer;
    private Session session;

    public Bot1_17(Options options, IPacketWrapper account, InetSocketAddress address, Logger log, ServiceServer serviceServer, ProxyType proxyType, String username, String password) {
        this.options = options;
        this.account = account;
        if (address == null) {
            this.proxyInfo = null;
        } else if (username != null && password != null) {
            this.proxyInfo = new ProxyInfo(ProxyInfo.Type.valueOf(proxyType.name()), address, username, password);
        } else {
            this.proxyInfo = new ProxyInfo(ProxyInfo.Type.valueOf(proxyType.name()), address);
        }

        this.serviceServer = serviceServer;

        this.logger = LoggerFactory.getLogger(account.getProfileName());
    }

    public void connect(String host, int port) {
        if (proxyInfo == null) {
            session = new TcpClientSession(host, port, (PacketProtocol) account);
        } else {
            session = new TcpClientSession(host, port, (PacketProtocol) account, proxyInfo);
        }

        SessionService sessionService = new SessionService();
        sessionService.setBaseUri(serviceServer.getSession());
        session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);

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

    public void sendPosition(double x, double y, double z) {
        session.send(new ClientPlayerPositionPacket(true, x, y, z));
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
