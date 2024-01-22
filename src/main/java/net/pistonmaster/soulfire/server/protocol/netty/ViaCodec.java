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
package net.pistonmaster.soulfire.server.protocol.netty;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.exception.*;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.server.viaversion.StorableSession;

import java.util.List;
import java.util.Objects;

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

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelCodecException.class)
                || PipelineUtil.containsCause(cause, CancelException.class)) {
            return;
        }

        super.exceptionCaught(ctx, cause);

        if (cause instanceof EncoderException) {
            return;
        }

        // Decoder exception
        if ((PipelineUtil.containsCause(cause, InformativeException.class)
                && info.getProtocolInfo().getServerState() != State.HANDSHAKE)
                || Via.getManager().debugHandler().enabled()) {
            Objects.requireNonNull(info.get(StorableSession.class), "Storable Session missing")
                    .session().logger().error("A ViaVersion error has occurred:", cause);
        }
    }
}
