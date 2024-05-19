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
package com.soulfiremc.server.viaversion;

import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.exception.CancelDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameCodec extends ByteToMessageCodec<ByteBuf> {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      // Netty throws an exception when there's no output
      throw CancelDecoderException.CACHED;
    }
    // Ignore, should prevent DoS https://github.com/SpigotMC/BungeeCord/pull/2908

    var index = in.readerIndex();
    var nByte = new AtomicInteger();
    var result =
      in.forEachByte(
        it -> {
          nByte.getAndIncrement();
          var hasNext = (it & 0x10000000) != 0;
          if (nByte.get() > 3) {
            throw getBadLength();
          }
          return hasNext;
        });
    in.readerIndex(index);
    if (result == -1) {
      return; // not readable
    }

    var length = Types.VAR_INT.readPrimitive(in);

    if (length >= 2097152 || length < 0) {
      throw getBadLength();
    }
    if (!in.isReadable(length)) {
      in.readerIndex(index);
      return;
    }

    out.add(in.readRetainedSlice(length));
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
    if (msg.readableBytes() >= 2097152) {
      throw getBadLength();
    }
    Types.VAR_INT.writePrimitive(out, msg.readableBytes());
    out.writeBytes(msg);
  }

  private DecoderException getBadLength() {
    return new DecoderException("Invalid length!");
  }
}
