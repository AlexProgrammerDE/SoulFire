package net.pistonmaster.serverwrecker.protocol.tcp;


import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.exception.CancelDecoderException;
import com.viaversion.viaversion.exception.CancelEncoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ViaCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private final UserConnection info;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!ctx.channel().isActive() || !info.checkServerboundPacket()) throw CancelEncoderException.generate(null);
        if (!info.shouldTransformPacket()) {
            out.add(msg.retain());
            return;
        }
        var transformedBuf = ctx.alloc().buffer().writeBytes(msg);
        try {
            info.transformServerbound(transformedBuf, CancelEncoderException::generate);
            out.add(transformedBuf.retain());
        } finally {
            transformedBuf.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!ctx.channel().isActive()) return;
        if (!info.checkClientboundPacket()) throw CancelDecoderException.generate(null);
        if (!info.shouldTransformPacket()) {
            out.add(msg.retain());
            return;
        }
        var transformedBuf = ctx.alloc().buffer().writeBytes(msg);
        try {
            info.transformClientbound(transformedBuf, CancelDecoderException::generate);
            out.add(transformedBuf.retain());
        } finally {
            transformedBuf.release();
        }
    }
}
