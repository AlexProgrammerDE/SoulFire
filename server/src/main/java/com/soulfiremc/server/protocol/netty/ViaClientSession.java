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

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket;
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import com.github.steveice10.packetlib.crypt.PacketEncryption;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpSession;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.viaversion.FrameCodec;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.server.viaversion.StorableSession;
import com.soulfiremc.settings.account.service.BedrockData;
import com.soulfiremc.settings.proxy.SFProxy;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.SecretKey;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.raphimc.viabedrock.api.protocol.BedrockBaseProtocol;
import net.raphimc.viabedrock.netty.BatchLengthCodec;
import net.raphimc.viabedrock.netty.PacketEncapsulationCodec;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.storage.AuthChainData;
import net.raphimc.vialegacy.api.protocol.PreNettyBaseProtocol;
import net.raphimc.vialegacy.netty.PreNettyLengthCodec;
import net.raphimc.vialoader.netty.viabedrock.DisconnectHandler;
import net.raphimc.vialoader.netty.viabedrock.RakMessageEncapsulationCodec;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.slf4j.Logger;

public class ViaClientSession extends TcpSession {
  public static final String SIZER_NAME = "sizer";
  public static final String COMPRESSION_NAME = "compression";
  public static final String ENCRYPTION_NAME = "encryption";

  @Getter
  private final Logger logger;
  private final InetSocketAddress targetAddress;
  private final String bindAddress;
  private final int bindPort;
  private final SFProxy proxy;
  private final PacketCodecHelper codecHelper;
  @Getter
  private final EventLoopGroup eventLoopGroup;
  @Getter
  private final BotConnection botConnection;
  private final Queue<Packet> packetTickQueue = new ConcurrentLinkedQueue<>();
  private boolean delimiterBlockProcessing = false;

  public ViaClientSession(
    InetSocketAddress targetAddress,
    Logger logger,
    PacketProtocol protocol,
    SFProxy proxy,
    EventLoopGroup eventLoopGroup,
    BotConnection botConnection) {
    super(null, -1, protocol);
    this.logger = logger;
    this.targetAddress = targetAddress;
    this.bindAddress = "0.0.0.0";
    this.bindPort = 0;
    this.proxy = proxy;
    this.codecHelper = protocol.createHelper();
    this.eventLoopGroup = eventLoopGroup;
    this.botConnection = botConnection;
  }

  @Override
  public void connect(boolean wait) {
    if (this.disconnected) {
      throw new IllegalStateException("Session has already been disconnected.");
    }

    try {
      var version = botConnection.protocolVersion();
      var isBedrock = SFVersionConstants.isBedrock(version);
      var bootstrap = new Bootstrap();

      bootstrap.group(eventLoopGroup);
      if (isBedrock) {
        if (proxy != null && !proxy.type().udpSupport()) {
          throw new IllegalStateException("Proxy must support UDP! (Only SOCKS5 is supported)");
        }

        bootstrap.channelFactory(
          RakChannelFactory.client(SFNettyHelper.TRANSPORT_METHOD.datagramChannelClass()));
      } else {
        bootstrap.channel(SFNettyHelper.TRANSPORT_METHOD.channelClass());
      }

      bootstrap
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout() * 1000)
        .option(ChannelOption.IP_TOS, 0x18);

      if (isBedrock) {
        bootstrap
          .option(
            RakChannelOption.RAK_PROTOCOL_VERSION,
            ProtocolConstants.BEDROCK_RAKNET_PROTOCOL_VERSION)
          .option(RakChannelOption.RAK_CONNECT_TIMEOUT, 4_000L)
          .option(RakChannelOption.RAK_SESSION_TIMEOUT, 30_000L)
          .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong());
      } else {
        bootstrap.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true);

        if (SFNettyHelper.TRANSPORT_METHOD.tcpFastOpenClientSideAvailable()) {
          bootstrap.option(ChannelOption.TCP_FASTOPEN_CONNECT, true);
        }
      }

      bootstrap.handler(
        new ChannelInitializer<>() {
          @Override
          public void initChannel(Channel channel) {
            var protocol = getPacketProtocol();
            protocol.newClientSession(ViaClientSession.this);

            var pipeline = channel.pipeline();

            refreshReadTimeoutHandler(channel);
            refreshWriteTimeoutHandler(channel);

            if (proxy != null) {
              SFNettyHelper.addProxy(pipeline, proxy);
            }

            // This monitors the traffic
            var trafficHandler = new GlobalTrafficShapingHandler(channel.eventLoop(), 0, 0, 1000);
            pipeline.addLast("traffic", trafficHandler);
            setFlag(SFProtocolConstants.TRAFFIC_HANDLER, trafficHandler);

            // This does the extra magic
            var userConnection = new UserConnectionImpl(channel, true);
            userConnection.put(new StorableSession(ViaClientSession.this));

            if (isBedrock && botConnection.minecraftAccount().isPremiumBedrock()) {
              var bedrockData = (BedrockData) botConnection.minecraftAccount().accountData();
              userConnection.put(
                new AuthChainData(
                  bedrockData.mojangJwt(),
                  bedrockData.identityJwt(),
                  bedrockData.publicKey(),
                  bedrockData.privateKey(),
                  bedrockData.deviceId(),
                  bedrockData.playFabId()));
            }

            setFlag(SFProtocolConstants.VIA_USER_CONNECTION, userConnection);

            var protocolPipeline = new ProtocolPipelineImpl(userConnection);

            if (SFVersionConstants.isLegacy(version)) {
              protocolPipeline.add(PreNettyBaseProtocol.INSTANCE);
              pipeline.addLast("vl-prenetty", new PreNettyLengthCodec(userConnection));
            } else if (isBedrock) {
              protocolPipeline.add(BedrockBaseProtocol.INSTANCE);
              pipeline.addLast("vb-disconnect", new DisconnectHandler());
              pipeline.addLast("vb-frame-encapsulation", new RakMessageEncapsulationCodec());
            }

            if (isBedrock) {
              pipeline.addLast(SIZER_NAME, new BatchLengthCodec());
              pipeline.addLast("vb-packet-encapsulation", new PacketEncapsulationCodec());
            } else {
              pipeline.addLast(SIZER_NAME, new FrameCodec());
            }

            // Inject Via codec
            pipeline.addLast("via-codec", new ViaCodec(userConnection));

            pipeline.addLast("codec", new SFTcpPacketCodec(ViaClientSession.this));
            pipeline.addLast("manager", ViaClientSession.this);

            addHAProxySupport(pipeline);
          }
        });

      bootstrap.remoteAddress(targetAddress);
      bootstrap.localAddress(bindAddress, bindPort);

      var future = bootstrap.connect();
      if (wait) {
        future.sync();
      }

      future.addListener(
        (futureListener) -> {
          if (!futureListener.isSuccess()) {
            exceptionCaught(null, futureListener.cause());
          }
        });
    } catch (Throwable t) {
      exceptionCaught(null, t);
    }
  }

  @Override
  public int getCompressionThreshold() {
    throw new UnsupportedOperationException("Not supported method.");
  }

  public void setCompressionThreshold(int threshold) {
    logger.debug("Enabling compression with threshold {}", threshold);

    var channel = getChannel();
    if (channel == null) {
      throw new IllegalStateException("Channel is not initialized.");
    }

    if (threshold >= 0) {
      var handler = channel.pipeline().get(COMPRESSION_NAME);
      if (handler == null) {
        channel
          .pipeline()
          .addBefore("via-codec", COMPRESSION_NAME, new CompressionCodec(threshold));
      } else {
        ((CompressionCodec) handler).threshold(threshold);
      }
    } else if (channel.pipeline().get(COMPRESSION_NAME) != null) {
      channel.pipeline().remove(COMPRESSION_NAME);
    }
  }

  @Override
  public void setCompressionThreshold(int threshold, boolean validateDecompression) {
    throw new UnsupportedOperationException("Not supported method.");
  }

  @Override
  public void enableEncryption(PacketEncryption encryption) {
    throw new UnsupportedOperationException("Not supported method.");
  }

  @Override
  public MinecraftCodecHelper getCodecHelper() {
    return (MinecraftCodecHelper) this.codecHelper;
  }

  private void addHAProxySupport(ChannelPipeline pipeline) {
    InetSocketAddress clientAddress = getFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS);
    if (getFlag(BuiltinFlags.ENABLE_CLIENT_PROXY_PROTOCOL, false) && clientAddress != null) {
      pipeline.addFirst(
        "proxy-protocol-packet-sender",
        new ChannelInboundHandlerAdapter() {
          @Override
          public void channelActive(ChannelHandlerContext ctx) throws Exception {
            var proxiedProtocol =
              clientAddress.getAddress() instanceof Inet4Address
                ? HAProxyProxiedProtocol.TCP4
                : HAProxyProxiedProtocol.TCP6;
            var remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            ctx.channel()
              .writeAndFlush(
                new HAProxyMessage(
                  HAProxyProtocolVersion.V2,
                  HAProxyCommand.PROXY,
                  proxiedProtocol,
                  clientAddress.getAddress().getHostAddress(),
                  remoteAddress.getAddress().getHostAddress(),
                  clientAddress.getPort(),
                  remoteAddress.getPort()));
            ctx.pipeline().remove(this);
            ctx.pipeline().remove("proxy-protocol-encoder");
            super.channelActive(ctx);
          }
        });
      pipeline.addFirst("proxy-protocol-encoder", HAProxyMessageEncoder.INSTANCE);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof CancelCodecException) {
      return;
    }

    super.exceptionCaught(ctx, cause);

    logger.debug("Exception caught in Netty session.", cause);
  }

  @Override
  public void callPacketReceived(Packet packet) {
    if (packet.isPriority()) {
      super.callPacketReceived(packet);
      return;
    }

    if (packet instanceof ClientboundDelimiterPacket) {
      // Block or unlock packets for processing
      delimiterBlockProcessing = !delimiterBlockProcessing;
    } else {
      packetTickQueue.add(packet);
    }
  }

  public void tick() {
    // The server said we should block packets for processing until we get another delimiter packet
    if (delimiterBlockProcessing) {
      return;
    }

    Packet packet;
    while ((packet = packetTickQueue.poll()) != null) {
      super.callPacketReceived(packet);
    }
  }

  @Override
  public void send(Packet packet) {
    var channel = getChannel();
    if (channel == null || !channel.isActive() || eventLoopGroup.isShutdown()) {
      return;
    }

    var sendingEvent = new PacketSendingEvent(this, packet);
    this.callEvent(sendingEvent);

    if (sendingEvent.isCancelled()) {
      logger.debug("Packet {} was cancelled.", packet.getClass().getSimpleName());
      return;
    }

    final var toSend = sendingEvent.getPacket();
    channel
      .writeAndFlush(toSend)
      .addListener(
        (ChannelFutureListener)
          future -> {
            if (future.isSuccess()) {
              callPacketSent(toSend);
            } else {
              packetExceptionCaught(null, future.cause(), packet);
            }
          });
  }

  @Override
  public void disconnect(Component reason, Throwable cause) {
    super.disconnect(reason, cause);
  }

  public void packetExceptionCaught(ChannelHandlerContext ctx, Throwable cause, Packet packet) {
    if (cause instanceof CancelCodecException) {
      callPacketSent(packet);
      return;
    }

    super.exceptionCaught(ctx, cause);

    logger.debug("Exception caught in Netty session.", cause);
  }

  public void enableJavaEncryption(SecretKey key) {
    var codec = new CryptoCodec(key, key);
    var pipeline = getChannel().pipeline();

    if (pipeline.get("vl-prenetty") != null) {
      logger.debug("Enabling legacy decryption");
      pipeline.addBefore("vl-prenetty", ENCRYPTION_NAME, codec);
    } else {
      logger.debug("Enabling decryption");
      pipeline.addBefore(SIZER_NAME, ENCRYPTION_NAME, codec);
    }
  }
}
