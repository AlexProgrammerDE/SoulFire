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
package net.pistonmaster.serverwrecker.version.v1_19;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.common.IPacketWrapper;
import net.pistonmaster.serverwrecker.common.SessionEventBus;

@RequiredArgsConstructor
public class SessionListener1_19 extends SessionAdapter {
    private final SessionEventBus bus;
    private final IPacketWrapper wrapper;

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
            Component message = chatPacket.getSignedContent();
            bus.onChat(PlainTextComponentSerializer.plainText().serialize(message));
        } else if (packet instanceof ClientboundPlayerPositionPacket posPacket) {
            double posX = posPacket.getX();
            double posY = posPacket.getY();
            double posZ = posPacket.getZ();
            float pitch = posPacket.getPitch();
            float yaw = posPacket.getYaw();
            bus.onPosition(posX, posY, posZ, pitch, yaw);
        } else if (packet instanceof ClientboundSetHealthPacket healthPacket) {
            bus.onHealth(healthPacket.getHealth(), healthPacket.getFood());
        } else if (packet instanceof ClientboundLoginPacket) {
            bus.onJoin();
        } else if (packet instanceof ClientboundDisconnectPacket disconnectPacket) {
            bus.onDisconnectPacket(PlainTextComponentSerializer.plainText().serialize(disconnectPacket.getReason()));
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        bus.onDisconnectEvent(event.getReason(), event.getCause());
    }
}