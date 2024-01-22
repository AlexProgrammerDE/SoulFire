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
package net.pistonmaster.soulfire.server.viaversion.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.pistonmaster.soulfire.server.protocol.netty.ViaClientSession;
import net.pistonmaster.soulfire.server.viaversion.StorableSession;
import net.raphimc.viabedrock.netty.AesEncryption;
import net.raphimc.viabedrock.netty.SnappyCompression;
import net.raphimc.viabedrock.netty.ZLibCompression;
import net.raphimc.viabedrock.protocol.providers.NettyPipelineProvider;

import javax.crypto.SecretKey;
import java.util.Objects;

public class SWViaNettyPipelineProvider extends NettyPipelineProvider {
    @Override
    public void enableCompression(UserConnection user, int threshold, int algorithm) {
        var clientSession = Objects.requireNonNull(user.get(StorableSession.class)).session();
        var channel = clientSession.getChannel();

        try {
            if (channel.pipeline().get(ViaClientSession.COMPRESSION_NAME) != null) {
                channel.pipeline().remove(ViaClientSession.COMPRESSION_NAME);
            }

            switch (algorithm) {
                case 0 ->
                        channel.pipeline().addBefore(ViaClientSession.SIZER_NAME, ViaClientSession.COMPRESSION_NAME, new ZLibCompression());
                case 1 ->
                        channel.pipeline().addBefore(ViaClientSession.SIZER_NAME, ViaClientSession.COMPRESSION_NAME, new SnappyCompression());
                default -> throw new IllegalStateException("Invalid compression algorithm: " + algorithm);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enableEncryption(UserConnection user, SecretKey key) {
        var clientSession = Objects.requireNonNull(user.get(StorableSession.class)).session();
        final var channel = clientSession.getChannel();

        try {
            if (channel.pipeline().get(ViaClientSession.ENCRYPTION_NAME) != null) {
                channel.pipeline().remove(ViaClientSession.ENCRYPTION_NAME);
            }

            channel.pipeline().addAfter("vb-frame-encapsulation", ViaClientSession.ENCRYPTION_NAME, new AesEncryption(key));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
