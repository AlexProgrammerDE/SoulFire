/*
 * ServerWrecker
 *
 * Copyright (C) 2021 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.version.v1_18;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import net.pistonmaster.serverwrecker.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Bot1_18 extends AbstractBot {
    private final Options options;
    private final ProxyInfo proxyInfo;
    private final Logger logger;
    private final IPacketWrapper account;
    private final ServiceServer serviceServer;
    private Session session;

    public Bot1_18(Options options, IPacketWrapper account, InetSocketAddress address, ServiceServer serviceServer, ProxyType proxyType, String username, String password) {
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

        SessionEventBus bus = new SessionEventBus(options, logger, this);

        session.addListener(new SessionListener1_18(bus, (ProtocolWrapper1_18) account));

        session.connect();
    }

    public void sendMessage(String message) {
        session.send(new ServerboundChatPacket(message));
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
        session.send(new ServerboundMovePlayerPosPacket(true, x, y, z));
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
