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
package com.soulfiremc.server.protocol;

import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotDisconnectedEvent;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.bot.SFPacketSendingEvent;
import com.soulfiremc.server.api.event.bot.SFPacketSentEvent;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

public class SFSessionListener extends SessionAdapter {
  private final BotConnection botConnection;
  private final LambdaManager busInvoker;

  public SFSessionListener(BotConnection botConnection) {
    this.botConnection = botConnection;
    this.busInvoker = LambdaManager.basic(new ASMGenerator());
    busInvoker.register(botConnection.dataManager());
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    var event = new SFPacketReceiveEvent(botConnection, (MinecraftPacket) packet);
    SoulFireAPI.postEvent(event);
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
    var event1 = new SFPacketSendingEvent(botConnection, event.getPacket());
    SoulFireAPI.postEvent(event1);
    event.setPacket(event1.packet());
    event.setCancelled(event1.isCancelled());

    if (event1.isCancelled()) {
      return;
    }

    botConnection
      .logger()
      .trace("Sending packet: {}", event.getPacket().getClass().getSimpleName());
  }

  @Override
  public void packetSent(Session session, Packet packet) {
    var event = new SFPacketSentEvent(botConnection, (MinecraftPacket) packet);
    SoulFireAPI.postEvent(event);

    botConnection.logger().trace("Sent packet: {}", packet.getClass().getSimpleName());
  }

  @Override
  public void disconnected(DisconnectedEvent event) {
    try {
      botConnection.dataManager().onDisconnectEvent(event);
    } catch (Throwable t) {
      botConnection.logger().error("Error while handling disconnect event!", t);
    }

    SoulFireAPI.postEvent(new BotDisconnectedEvent(botConnection));
  }
}
