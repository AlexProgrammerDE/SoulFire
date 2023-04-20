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

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginDisconnectPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pistonmaster.serverwrecker.common.GameMode;
import net.pistonmaster.serverwrecker.common.IPacketWrapper;
import net.pistonmaster.serverwrecker.common.SessionEventBus;

@RequiredArgsConstructor
public class SessionListener1_17 extends SessionAdapter {
    private final SessionEventBus bus;
    private final IPacketWrapper wrapper;

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerChatPacket chatPacket) {
            // Message API was replaced in version 1.16
            Component message = chatPacket.getMessage();
            bus.onChat(toPlainText(message));
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket posPacket) {
            bus.onPosition(posPacket.getX(), posPacket.getY(), posPacket.getZ(), posPacket.getYaw(), posPacket.getPitch());
        } else if (receiveEvent.getPacket() instanceof ServerPlayerHealthPacket healthPacket) {
            bus.onHealth(healthPacket.getHealth(), healthPacket.getFood(), healthPacket.getSaturation());
        } else if (receiveEvent.getPacket() instanceof ServerJoinGamePacket playLoginPacket) {
            bus.onJoin(playLoginPacket.getEntityId(),
                    playLoginPacket.isHardcore(),
                    GameMode.valueOf(playLoginPacket.getGameMode().name()),
                    playLoginPacket.getMaxPlayers());
        } else if (receiveEvent.getPacket() instanceof ServerDisconnectPacket disconnectPacket) {
            bus.onDisconnectPacket(toPlainText(disconnectPacket.getReason()));
        } else if (receiveEvent.getPacket() instanceof LoginDisconnectPacket loginDisconnectPacket) {
            bus.onLoginDisconnectPacket(toPlainText(loginDisconnectPacket.getReason()));
        } else if (receiveEvent.getPacket() instanceof ServerEntityVelocityPacket motionPacket) {
            bus.onEntityMotion(motionPacket.getEntityId(),
                    motionPacket.getMotionX(),
                    motionPacket.getMotionY(),
                    motionPacket.getMotionZ());
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        bus.onDisconnectEvent(event.getReason(), event.getCause());
    }

    private String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
