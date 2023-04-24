/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.ProxyInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.NullHelper;
import net.pistonmaster.serverwrecker.common.ProxyBotData;
import net.pistonmaster.serverwrecker.common.SWOptions;
import net.pistonmaster.serverwrecker.common.ServiceServer;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.tcp.ViaTcpClientSession;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.BitSet;
import java.util.Collections;

@Getter
@Setter
@RequiredArgsConstructor
public class Bot {
    private final ServerWrecker serverWrecker;
    private final SWOptions options;
    private final Logger logger;
    private final MinecraftProtocol protocol;
    private final ServiceServer serviceServer;
    private final ProxyBotData proxyBotData;
    private ViaTcpClientSession session;

    public void connect(String host, int port, SessionDataManager bus) {
        ViaTcpClientSession session = new ViaTcpClientSession(host, port, protocol,
                NullHelper.nullOrConvert(proxyBotData,
                        data -> new ProxyInfo(ProxyInfo.Type.valueOf(data.getType().name()), data.getAddress(), data.getUsername(), data.getPassword())),
                options);

        session.setFlag(BuiltinFlags.PRINT_DEBUG, options.debug());

        session.setConnectTimeout(options.connectTimeout());
        session.setReadTimeout(options.readTimeout());
        session.setWriteTimeout(options.writeTimeout());

        session.addListener(new SWBaseListener(logger, ProtocolState.LOGIN));
        session.addListener(new SWSessionListener(bus, this));

        session.connect(options.waitEstablished());
        this.session = session;
    }

    public void sendMessage(String message) {
        if (message.startsWith("/")) {
            session.send(new ServerboundChatCommandPacket(message.substring(1), Instant.now().toEpochMilli(), 0, Collections.emptyList(), 0, new BitSet()));
        } else {
            session.send(new ServerboundChatPacket(message, Instant.now().toEpochMilli(), 0, new byte[0], 0, new BitSet()));
        }
    }

    public boolean isOnline() {
        return session != null && session.isConnected();
    }

    public void sendPositionRotation(boolean onGround, double x, double y, double z, float yaw, float pitch) {
        session.send(new ServerboundMovePlayerPosRotPacket(onGround, x, y, z, yaw, pitch));
    }

    public void sendPosition(boolean onGround, double x, double y, double z) {
        session.send(new ServerboundMovePlayerPosPacket(onGround, x, y, z));
    }

    public void sendRotation(boolean onGround, float yaw, float pitch) {
        session.send(new ServerboundMovePlayerRotPacket(onGround, yaw, pitch));
    }

    public void sendGround(boolean onGround) {
        session.send(new ServerboundMovePlayerStatusOnlyPacket(onGround));
    }

    public void sendClientCommand(int actionId) {
        session.send(new ServerboundClientCommandPacket(ClientCommand.values()[actionId]));
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect("Disconnect");
        }
    }
}
