package com.soulfiremc.server.viaversion;

import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.ChannelHandler;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.vialoader.netty.ViaCodec;

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
