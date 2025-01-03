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

import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.viaversion.SFVLPipeline;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.server.viaversion.StorableSession;
import com.viaversion.vialoader.netty.VLPipeline;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.haproxy.*;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import lombok.Getter;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.storage.AuthChainData;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;
import org.geysermc.mcprotocollib.network.tcp.*;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class ViaClientSession extends TcpSession {
  public static final String SIZER_NAME = "sizer";
  public static final String COMPRESSION_NAME = "compression";
  public static final String ENCRYPTION_NAME = "encryption";
  public static final String CODEC_NAME = "codec";

  @Getter
  private final Logger logger;
  private final SocketAddress targetAddress;
  private final String bindAddress;
  private final int bindPort;
  private final SFProxy proxy;
  private final PacketCodecHelper codecHelper;
  @Getter
  private final EventLoopGroup eventLoopGroup;
  @Getter
  private final BotConnection botConnection;
  private final Queue<Runnable> packetTickQueue;
  private boolean delimiterBlockProcessing = false;

  public ViaClientSession(
    SocketAddress targetAddress,
    Logger logger,
    PacketProtocol protocol,
    SFProxy proxy,
    EventLoopGroup eventLoopGroup,
    BotConnection botConnection) {
    this(targetAddress, logger, protocol, proxy, eventLoopGroup, botConnection, new ConcurrentLinkedQueue<>());
  }

  private ViaClientSession(
    SocketAddress targetAddress,
    Logger logger,
    PacketProtocol protocol,
    SFProxy proxy,
    EventLoopGroup eventLoopGroup,
    BotConnection botConnection,
    Queue<Runnable> packetTickQueue) {
    super(null, -1, protocol, packetTickQueue::add);
    this.logger = logger;
    this.targetAddress = targetAddress;
    this.bindAddress = "0.0.0.0";
    this.bindPort = 0;
    this.proxy = proxy;
    this.codecHelper = protocol.createHelper();
    this.eventLoopGroup = eventLoopGroup;
    this.botConnection = botConnection;
    this.packetTickQueue = packetTickQueue;
  }

  public boolean isDisconnected() {
    return this.disconnected;
  }

  @Override
  public void connect(boolean wait) {
    if (this.disconnected) {
      throw new IllegalStateException("Session has already been disconnected.");
    }

    var version = botConnection.protocolVersion();
    var isBedrock = SFVersionConstants.isBedrock(version);
    var bootstrap = new Bootstrap();

    bootstrap.group(eventLoopGroup);
    if (isBedrock) {
      if (proxy != null && !proxy.type().udpSupport()) {
        throw new IllegalStateException("Proxy must support UDP! (Only SOCKS5 is supported)");
      }

      bootstrap.channelFactory(
        RakChannelFactory.client(TransportHelper.TRANSPORT_TYPE.datagramChannelClass()));
    } else {
      bootstrap.channel(TransportHelper.TRANSPORT_TYPE.socketChannelClass());
    }

    bootstrap
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getFlag(BuiltinFlags.CLIENT_CONNECT_TIMEOUT, 30) * 1000)
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

      if (TransportHelper.TRANSPORT_TYPE.supportsTcpFastOpenClient()) {
        bootstrap.option(ChannelOption.TCP_FASTOPEN_CONNECT, true);
      }
    }

    bootstrap.handler(
      new ChannelInitializer<>() {
        @Override
        public void initChannel(@NotNull Channel channel) {
          var protocol = getPacketProtocol();
          protocol.newClientSession(ViaClientSession.this, false);

          var pipeline = channel.pipeline();

          if (proxy != null) {
            SFNettyHelper.addProxy(pipeline, proxy);
          }

          initializeHAProxySupport(channel);

          // This monitors the traffic
          var trafficHandler = new GlobalTrafficShapingHandler(channel.eventLoop(), 0, 0, 1000);
          pipeline.addLast("traffic", trafficHandler);
          setFlag(SFProtocolConstants.TRAFFIC_HANDLER, trafficHandler);

          pipeline.addLast("read-timeout", new ReadTimeoutHandler(getFlag(BuiltinFlags.READ_TIMEOUT, 30)));
          pipeline.addLast("write-timeout", new WriteTimeoutHandler(getFlag(BuiltinFlags.WRITE_TIMEOUT, 0)));

          // This does the extra magic
          var userConnection = new UserConnectionImpl(channel, true);
          new ProtocolPipelineImpl(userConnection);

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

          pipeline.addLast(ENCRYPTION_NAME, new TcpPacketEncryptor());
          pipeline.addLast(SIZER_NAME, new TcpPacketSizer(protocol.getPacketHeader(), getCodecHelper()));
          pipeline.addLast(COMPRESSION_NAME, new TcpPacketCompression(getCodecHelper()));
          pipeline.addLast(CODEC_NAME, new TcpPacketCodec(ViaClientSession.this, true));
          pipeline.addLast("flush-handler", new FlushHandler());
          pipeline.addLast("manager", ViaClientSession.this);

          pipeline.addLast(new SFVLPipeline(userConnection));
          if (isBedrock) {
            pipeline.remove(COMPRESSION_NAME);
            pipeline.remove(ENCRYPTION_NAME);
          }

          pipeline.addBefore(CODEC_NAME, "flow-control", new FlowControlHandler());
          pipeline.addBefore(VLPipeline.VIA_CODEC_NAME, "via-flow-control", new FlowControlHandler());
        }
      });

    bootstrap.remoteAddress(targetAddress);
    bootstrap.localAddress(bindAddress, bindPort);

    var handleFuture = new CompletableFuture<Void>();
    bootstrap.connect().addListener((futureListener) -> {
      if (!futureListener.isSuccess()) {
        exceptionCaught(null, futureListener.cause());
      }

      handleFuture.complete(null);
    });

    if (wait) {
      handleFuture.join();
    }
  }

  @Override
  public MinecraftCodecHelper getCodecHelper() {
    return (MinecraftCodecHelper) this.codecHelper;
  }

  private void initializeHAProxySupport(Channel channel) {
    var clientAddress = getFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS);
    if (clientAddress == null) {
      return;
    }

    channel.pipeline().addLast("proxy-protocol-encoder", HAProxyMessageEncoder.INSTANCE);
    var proxiedProtocol = clientAddress.getAddress() instanceof Inet4Address ? HAProxyProxiedProtocol.TCP4 : HAProxyProxiedProtocol.TCP6;
    var remoteAddress = (InetSocketAddress) channel.remoteAddress();
    channel.writeAndFlush(new HAProxyMessage(
      HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, proxiedProtocol,
      clientAddress.getAddress().getHostAddress(), remoteAddress.getAddress().getHostAddress(),
      clientAddress.getPort(), remoteAddress.getPort()
    )).addListener(future -> channel.pipeline().remove("proxy-protocol-encoder"));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (PipelineUtil.containsCause(cause, CancelCodecException.class)) {
      logger.debug("Packet was cancelled.", cause);
      return;
    }

    super.exceptionCaught(ctx, cause);

    logger.debug("Exception caught in Netty session.", cause);
  }

  @Override
  public void callPacketReceived(Packet packet) {
    if (packet instanceof ClientboundDelimiterPacket) {
      // Block or unlock packets for processing
      delimiterBlockProcessing = !delimiterBlockProcessing;
    }

    super.callPacketReceived(packet);
  }

  public void tick() {
    // The server said we should block packets for processing until we get another delimiter packet
    if (delimiterBlockProcessing) {
      return;
    }

    Runnable packetHandler;
    while ((packetHandler = packetTickQueue.poll()) != null) {
      packetHandler.run();
    }
  }

  @Override
  public void send(@NotNull Packet packet, Runnable onSent) {
    var channel = getChannel();
    if (channel == null || !channel.isActive() || eventLoopGroup.isShutdown()) {
      logger.debug("Channel is not active, dropping packet {}", packet.getClass().getSimpleName());
      return;
    }

    if (!channel.eventLoop().inEventLoop()) {
      channel.eventLoop().execute(() -> this.send(packet, onSent));
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
              if (onSent != null) {
                onSent.run();
              }

              callPacketSent(toSend);
            } else {
              packetExceptionCaught(null, future.cause(), packet);
            }
          });
  }

  public void packetExceptionCaught(ChannelHandlerContext ctx, Throwable cause, Packet packet) {
    if (PipelineUtil.containsCause(cause, CancelCodecException.class)) {
      logger.debug("Packet was cancelled.", cause);
      callPacketSent(packet);
      return;
    }

    super.exceptionCaught(ctx, cause);

    logger.debug("Exception caught in Netty session.", cause);
  }
}
