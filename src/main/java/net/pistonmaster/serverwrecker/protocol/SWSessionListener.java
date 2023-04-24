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

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.SWPacketSendingEvent;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.util.BusHelper;

@RequiredArgsConstructor
public class SWSessionListener extends SessionAdapter {
    private final SessionDataManager bus;
    private final Bot bot;

    @Override
    public void packetReceived(Session session, Packet packet) {
        ServerWreckerAPI.postEvent(new SWPacketReceiveEvent(bot, (MinecraftPacket) packet));
        BusHelper.handlePacket(packet, bus);
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        ServerWreckerAPI.postEvent(new SWPacketSendingEvent(bot, event.getPacket()));
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        try {
            bus.onDisconnectEvent(event);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Make sure bus scheduler shuts down and resources become available again
        bus.getScheduler().shutdown();
    }
}
