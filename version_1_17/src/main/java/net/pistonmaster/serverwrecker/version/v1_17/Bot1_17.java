/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
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
package net.pistonmaster.serverwrecker.version.v1_17;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.common.*;
import org.slf4j.Logger;

@Getter
@RequiredArgsConstructor
public class Bot1_17 extends AbstractBot {
    private final Options options;
    private final Logger logger;
    private final IPacketWrapper account;
    private final ServiceServer serviceServer;
    private final ProxyBotData proxyBotData;
    private Session session;

    @Override
    public void connect(String host, int port, SessionEventBus bus) {
        session = new TcpClientSession(host, port, (PacketProtocol) account,
                NullHelper.nullOrConvert(proxyBotData,
                        data -> new ProxyInfo(ProxyInfo.Type.valueOf(data.getType().name()), data.getAddress(), data.getUsername(), data.getPassword())));

        session.setFlag(BuiltinFlags.PRINT_DEBUG, options.debug());

        session.setConnectTimeout(options.connectTimeout());
        session.setCompressionThreshold(options.compressionThreshold());
        session.setReadTimeout(options.readTimeout());
        session.setWriteTimeout(options.writeTimeout());

        session.addListener(new SessionListener1_17(bus, account));

        session.connect(options.waitEstablished());
    }

    @Override
    public void sendMessage(String message) {
        session.send(new ClientChatPacket(message));
    }

    @Override
    public boolean isOnline() {
        return session != null && session.isConnected();
    }

    @Override
    public void sendPositionRotation(boolean onGround, double x, double y, double z, float yaw, float pitch) {
        session.send(new ClientPlayerPositionRotationPacket(onGround, x, y, z, yaw, pitch));
    }

    @Override
    public void sendPosition(boolean onGround, double x, double y, double z) {
        session.send(new ClientPlayerPositionPacket(onGround, x, y, z));
    }

    @Override
    public void sendRotation(boolean onGround, float yaw, float pitch) {
        session.send(new ClientPlayerRotationPacket(onGround, yaw, pitch));
    }

    @Override
    public void sendGround(boolean onGround) {
        session.send(new ClientPlayerMovementPacket(onGround));
    }

    @Override
    public void sendClientCommand(int actionId) {
        session.send(new ClientRequestPacket(ClientRequest.values()[actionId]));
    }

    @Override
    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
