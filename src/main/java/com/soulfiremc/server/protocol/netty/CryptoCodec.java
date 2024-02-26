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

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.security.GeneralSecurityException;
import java.util.List;
import javax.crypto.SecretKey;

public class CryptoCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
  private final VelocityCipher encoder;
  private final VelocityCipher decoder;

  public CryptoCodec(SecretKey keyEncode, SecretKey keyDecode) {
    try {
      this.encoder = Natives.cipher.get().forEncryption(keyEncode);
      this.decoder = Natives.cipher.get().forDecryption(keyDecode);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Unable to initialize cipher", e);
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
    var compatible = MoreByteBufUtils.ensureCompatible(ctx.alloc(), encoder, msg);
    try {
      encoder.process(compatible);
      out.add(compatible.retain());
    } finally {
      compatible.release();
    }
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
    if (!ctx.channel().isActive()) {
      return;
    }

    var compatible = MoreByteBufUtils.ensureCompatible(ctx.alloc(), decoder, msg);
    try {
      decoder.process(compatible);
      out.add(compatible.retain());
    } finally {
      compatible.release();
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    encoder.close();
    decoder.close();
  }
}
