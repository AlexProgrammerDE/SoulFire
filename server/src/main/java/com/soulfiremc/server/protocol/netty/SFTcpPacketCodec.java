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
package com.soulfiremc.server.protocol.netty;

import com.soulfiremc.server.protocol.SFProtocolHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.raphimc.netminecraft.packet.PacketTypes;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

@RequiredArgsConstructor
public class SFTcpPacketCodec extends ByteToMessageCodec<MinecraftPacket> {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (in.readableBytes() != 0) {
      final var registry = ctx.channel().attr(SFProtocolHelper.SF_PACKET_REGISTRY_ATTRIBUTE_KEY).get();
      final var packetId = PacketTypes.readVarInt(in);
      final var factory = registry.getPacketFactoryById(packetId);
      if (factory == null) {
        throw new IllegalStateException("Tried to read not registered packet: " + packetId);
      }

      out.add(factory.apply(in));
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, MinecraftPacket in, ByteBuf out) {
    final var registry = ctx.channel().attr(SFProtocolHelper.SF_PACKET_REGISTRY_ATTRIBUTE_KEY).get();
    final var packetId = registry.getIdByPacket(in);
    if (packetId == -1) {
      throw new IllegalStateException("Tried to write not registered packet: " + in.getClass().getName());
    }

    PacketTypes.writeVarInt(out, packetId);
    registry.writePacket(out, in);
  }
}
