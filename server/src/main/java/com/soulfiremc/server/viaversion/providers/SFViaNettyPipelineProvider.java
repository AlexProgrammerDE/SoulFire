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
package com.soulfiremc.server.viaversion.providers;

import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.soulfiremc.server.viaversion.StorableSession;
import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.Objects;
import javax.crypto.SecretKey;
import net.raphimc.viabedrock.api.io.compression.ProtocolCompression;
import net.raphimc.viabedrock.netty.AesEncryptionCodec;
import net.raphimc.viabedrock.netty.CompressionCodec;
import net.raphimc.viabedrock.protocol.providers.NettyPipelineProvider;

public class SFViaNettyPipelineProvider extends NettyPipelineProvider {
  @Override
  public void enableCompression(UserConnection user, ProtocolCompression protocolCompression) {
    var clientSession = Objects.requireNonNull(user.get(StorableSession.class)).session();
    var channel = clientSession.getChannel();

    if (channel.pipeline().names().contains(ViaClientSession.COMPRESSION_NAME)) {
      throw new IllegalStateException("Compression already enabled");
    }

    try {
      channel
        .pipeline()
        .addBefore(
          ViaClientSession.SIZER_NAME,
          ViaClientSession.COMPRESSION_NAME,
          new CompressionCodec(protocolCompression));
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void enableEncryption(UserConnection user, SecretKey key) {
    var clientSession = Objects.requireNonNull(user.get(StorableSession.class)).session();
    final var channel = clientSession.getChannel();

    if (channel.pipeline().names().contains(ViaClientSession.ENCRYPTION_NAME)) {
      throw new IllegalStateException("Encryption already enabled");
    }

    try {
      channel
        .pipeline()
        .addAfter(
          "vb-frame-encapsulation",
          ViaClientSession.ENCRYPTION_NAME,
          new AesEncryptionCodec(key));
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
