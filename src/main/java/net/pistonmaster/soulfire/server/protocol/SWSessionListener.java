/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.protocol;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.pistonmaster.soulfire.server.api.event.bot.BotDisconnectedEvent;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketSendingEvent;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketSentEvent;
import net.pistonmaster.soulfire.server.protocol.bot.SessionDataManager;

public class SWSessionListener extends SessionAdapter {
    private final SessionDataManager bus;
    private final BotConnection botConnection;
    private final LambdaManager busInvoker;

    public SWSessionListener(SessionDataManager bus, BotConnection botConnection) {
        this.bus = bus;
        this.botConnection = botConnection;
        this.busInvoker = LambdaManager.basic(new ASMGenerator());
        busInvoker.register(bus);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        var event = new SWPacketReceiveEvent(botConnection, (MinecraftPacket) packet);
        botConnection.eventBus().call(event);
        if (event.isCancelled()) {
            return;
        }

        botConnection.logger().trace("Received packet: {}", packet.getClass().getSimpleName());

        try {
            busInvoker.call(event.packet());
        } catch (Throwable t) {
            botConnection.logger().error("Error while handling packet!", t);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        var event1 = new SWPacketSendingEvent(botConnection, event.getPacket());
        botConnection.eventBus().call(event1);
        event.setPacket(event1.packet());
        event.setCancelled(event1.isCancelled());

        if (event1.isCancelled()) {
            return;
        }

        botConnection.logger().trace("Sending packet: {}", event.getPacket().getClass().getSimpleName());
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        var event = new SWPacketSentEvent(botConnection, (MinecraftPacket) packet);
        botConnection.eventBus().call(event);

        botConnection.logger().trace("Sent packet: {}", packet.getClass().getSimpleName());
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        try {
            bus.onDisconnectEvent(event);
        } catch (Throwable t) {
            botConnection.logger().error("Error while handling disconnect event!", t);
        }

        botConnection.eventBus().call(new BotDisconnectedEvent(botConnection));
    }
}
