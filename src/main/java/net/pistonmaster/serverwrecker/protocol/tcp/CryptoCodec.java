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
