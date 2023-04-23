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
package net.pistonmaster.serverwrecker.protocol.tcp;


import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import javax.crypto.SecretKey;
import java.util.List;

public class CryptoCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private SecretKey keyEncode;
    private SecretKey keyDecode;
    private VelocityCipher encoder;
    private VelocityCipher decoder;

    public CryptoCodec(SecretKey keyEncode, SecretKey keyDecode) {
        this.keyEncode = keyEncode;
        this.keyDecode = keyDecode;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        encoder = Natives.cipher.get().forEncryption(keyEncode);
        decoder = Natives.cipher.get().forDecryption(keyDecode);
        keyEncode = null;
        keyDecode = null;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        ByteBuf compatible = MoreByteBufUtils.ensureCompatible(ctx.alloc(), encoder, msg);
        try {
            encoder.process(compatible);
            out.add(compatible.retain());
        } finally {
            compatible.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!ctx.channel().isActive()) return;
        ByteBuf compatible = MoreByteBufUtils.ensureCompatible(ctx.alloc(), decoder, msg);
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
