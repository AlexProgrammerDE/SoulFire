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

import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.viaversion.vialoader.netty.VLPipeline;
import com.viaversion.vialoader.netty.ViaCodec;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.ChannelHandler;

public class SFVLPipeline extends VLPipeline {
  public SFVLPipeline(UserConnection user) {
    super(user);
  }

  @Override
  public ChannelHandler createViaCodec() {
    return new ViaCodec(this.user);
  }

  @Override
  protected String compressionCodecName() {
    return ViaClientSession.COMPRESSION_NAME;
  }

  @Override
  protected String packetCodecName() {
    return ViaClientSession.CODEC_NAME;
  }

  @Override
  protected String lengthCodecName() {
    return ViaClientSession.SIZER_NAME;
  }
}
