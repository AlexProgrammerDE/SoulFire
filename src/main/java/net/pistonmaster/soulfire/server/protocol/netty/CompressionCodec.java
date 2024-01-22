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

import com.velocitypowered.natives.compression.JavaVelocityCompressor;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.Setter;

import java.util.List;
import java.util.zip.DataFormatException;

public class CompressionCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    // stolen from Krypton (GPL) and modified
    // https://github.com/astei/krypton/blob/master/src/main/java/me/steinborn/krypton/mod/shared/network/compression/MinecraftCompressEncoder.java
    private static final int UNCOMPRESSED_CAP = 8 * 1024 * 1024; // 8MiB
    private final int compressionLevel = 6; // TODO: make configurable
    @Setter
    private int threshold;
    private VelocityCompressor compressor;
    private VelocityCompressor candidateCompressor;

    public CompressionCodec(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        var cNative = createCompressor(true);
        if (!Natives.compress.getLoadedVariant().equalsIgnoreCase("java")) {
            // Workaround for Lilypad backend servers
            compressor = createCompressor(false);
            candidateCompressor = cNative;
        } else {
            compressor = cNative;
        }
    }

    private VelocityCompressor createCompressor(boolean allowNative) {
        if (!allowNative) return JavaVelocityCompressor.FACTORY.create(Math.min(compressionLevel, 9));
        return Natives.compress.get().create(compressionLevel);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        compressor.close();
        discardCandidate();
    }

    private void promoteCandidate() {
        compressor.close();
        compressor = candidateCompressor;
        candidateCompressor = null;
    }

    private void discardCandidate() {
        if (candidateCompressor == null) return;
        candidateCompressor.close();
        candidateCompressor = null;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!ctx.channel().isActive()) return;

        var outBuf = allocateBuffer(ctx, msg);
        try {
            var uncompressedSize = msg.readableBytes();
            if (uncompressedSize < threshold) { // Not compressed
                outBuf.writeByte(0);
                outBuf.writeBytes(msg);
            } else {
                Type.VAR_INT.writePrimitive(outBuf, uncompressedSize);
                var compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, msg);
                try {
                    compressor.deflate(compatibleIn, outBuf);
                } finally {
                    compatibleIn.release();
                }
            }
            out.add(outBuf.retain());
        } finally {
            outBuf.release();
        }
    }

    private ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg) {
        var initialBufferSize = msg.readableBytes() + 1;
        return MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) throws Exception {
        if (!input.isReadable() || !ctx.channel().isActive()) return;

        var claimedUncompressedSize = Type.VAR_INT.readPrimitive(input);
        if (claimedUncompressedSize == 0) { // Uncompressed
            out.add(input.retain());
            return;
        }

        // 1.18 clients now accept compressed packets under threshold...?
        if (claimedUncompressedSize > UNCOMPRESSED_CAP) {
            throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is larger than maximum of " + UNCOMPRESSED_CAP);
        }
        var compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, input);
        var decompressed = MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);
        try {
            var readerI = compatibleIn.readerIndex();
            try {
                compressor.inflate(compatibleIn, decompressed, claimedUncompressedSize);
            } catch (DataFormatException ex) {
                // workaround for lilypad
                if (!ex.getMessage().startsWith("Received a deflate stream that was too large, wanted ")) {
                    throw ex;
                }
            }
            out.add(decompressed.retain());

            testCandidateDecompression(compatibleIn, readerI, claimedUncompressedSize);

            input.clear();
        } finally {
            decompressed.release();
            compatibleIn.release();
        }
    }

    private void testCandidateDecompression(ByteBuf in, int readerIndex, int claimedUncompressedSize) {
        if (candidateCompressor == null) return;
        in.readerIndex(readerIndex);
        var testOut = ByteBufAllocator.DEFAULT.buffer();
        try {
            candidateCompressor.inflate(in, testOut, claimedUncompressedSize);

            if (Math.random() <= 0.001) { // Runs more tests
                promoteCandidate();
            }
        } catch (DataFormatException eTest) {
            discardCandidate(); // LilyPad
        } finally {
            testOut.release();
        }
    }
}
