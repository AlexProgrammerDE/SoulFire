package com.soulfiremc.server.protocol.netty;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

public interface SFPacketRegistry {
  Function<ByteBuf, MinecraftPacket> getPacketFactoryById(final int packetId);

  int getIdByPacket(final MinecraftPacket packetClass);

  void writePacket(final ByteBuf buf, final MinecraftPacket packet);
}
