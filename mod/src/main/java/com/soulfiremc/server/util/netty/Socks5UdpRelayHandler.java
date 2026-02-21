/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/// Implements SOCKS5 UDP ASSOCIATE (RFC 1928, command 0x03) for proxying
/// UDP traffic through a SOCKS5 proxy. Used for Bedrock Edition connections
/// which use RakNet over UDP instead of TCP.
///
/// Opens a TCP control connection to the SOCKS5 proxy for the handshake,
/// then wraps/unwraps UDP datagrams with the SOCKS5 UDP relay header.
@Slf4j
public class Socks5UdpRelayHandler extends ChannelDuplexHandler {
  private final InetSocketAddress proxyAddress;
  private final @Nullable String username;
  private final @Nullable String password;
  private Channel tcpControlChannel;
  private InetSocketAddress relayAddress;
  private InetSocketAddress originalDestination;

  public Socks5UdpRelayHandler(InetSocketAddress proxyAddress, @Nullable String username, @Nullable String password) {
    this.proxyAddress = proxyAddress;
    this.username = username;
    this.password = password;
  }

  @Override
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                      SocketAddress localAddress, ChannelPromise promise) {
    originalDestination = (InetSocketAddress) remoteAddress;

    new Bootstrap()
      .group(ctx.channel().eventLoop())
      .channelFactory(TransportHelper.TRANSPORT_TYPE.socketChannelFactory())
      .handler(new ChannelInitializer<>() {
        @Override
        protected void initChannel(Channel ch) {
          ch.pipeline().addLast(new Socks5TcpControlHandler(ctx, promise));
        }
      })
      .connect(proxyAddress)
      .addListener((ChannelFutureListener) future -> {
        if (future.isSuccess()) {
          tcpControlChannel = future.channel();
        } else {
          promise.tryFailure(future.cause());
        }
      });
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ByteBuf buf) {
      var header = buildSocks5UdpHeader(ctx.alloc(), originalDestination);
      ctx.write(Unpooled.wrappedBuffer(header, buf), promise);
    } else if (msg instanceof DatagramPacket packet) {
      var header = buildSocks5UdpHeader(ctx.alloc(), originalDestination);
      var content = Unpooled.wrappedBuffer(header, packet.content().retain());
      ctx.write(new DatagramPacket(content, relayAddress), promise);
      packet.release();
    } else {
      super.write(ctx, msg, promise);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf buf) {
      skipSocks5UdpHeader(buf);
      ctx.fireChannelRead(buf);
    } else if (msg instanceof DatagramPacket packet) {
      skipSocks5UdpHeader(packet.content());
      ctx.fireChannelRead(packet);
    } else {
      super.channelRead(ctx, msg);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (tcpControlChannel != null) {
      tcpControlChannel.close();
    }
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("SOCKS5 UDP relay error", cause);
    if (tcpControlChannel != null) {
      tcpControlChannel.close();
    }
    ctx.close();
  }

  private static ByteBuf buildSocks5UdpHeader(ByteBufAllocator alloc, InetSocketAddress destination) {
    var addr = destination.getAddress().getAddress();
    var buf = alloc.buffer(4 + addr.length + 2);
    buf.writeShort(0x0000); // RSV
    buf.writeByte(0x00); // FRAG
    buf.writeByte(addr.length == 4 ? 0x01 : 0x04); // ATYP: IPv4 or IPv6
    buf.writeBytes(addr);
    buf.writeShort(destination.getPort());
    return buf;
  }

  private static void skipSocks5UdpHeader(ByteBuf buf) {
    buf.skipBytes(2); // RSV
    buf.skipBytes(1); // FRAG
    var atyp = buf.readByte();
    switch (atyp) {
      case 0x01 -> buf.skipBytes(4); // IPv4
      case 0x03 -> buf.skipBytes(buf.readUnsignedByte()); // Domain name
      case 0x04 -> buf.skipBytes(16); // IPv6
      default -> throw new IllegalStateException("Unknown SOCKS5 address type: " + atyp);
    }
    buf.skipBytes(2); // DST.PORT
  }

  /// Handles the SOCKS5 handshake over the TCP control connection.
  /// State machine: GREETING -> AUTH (optional) -> UDP_ASSOCIATE -> DONE
  private class Socks5TcpControlHandler extends ByteToMessageDecoder {
    private enum State {
      GREETING, AUTH, UDP_ASSOCIATE, DONE
    }

    private final ChannelHandlerContext datagramCtx;
    private final ChannelPromise connectPromise;
    private State state = State.GREETING;

    Socks5TcpControlHandler(ChannelHandlerContext datagramCtx, ChannelPromise connectPromise) {
      this.datagramCtx = datagramCtx;
      this.connectPromise = connectPromise;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      var greeting = ctx.alloc().buffer();
      greeting.writeByte(0x05); // VER
      if (username != null && password != null) {
        greeting.writeByte(0x02); // NMETHODS
        greeting.writeByte(0x00); // NO_AUTH
        greeting.writeByte(0x02); // USERNAME_PASSWORD
      } else {
        greeting.writeByte(0x01); // NMETHODS
        greeting.writeByte(0x00); // NO_AUTH
      }
      ctx.writeAndFlush(greeting);
      super.channelActive(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      switch (state) {
        case GREETING -> decodeGreeting(ctx, in);
        case AUTH -> decodeAuth(ctx, in);
        case UDP_ASSOCIATE -> decodeUdpAssociate(ctx, in);
        case DONE -> in.skipBytes(in.readableBytes());
      }
    }

    private void decodeGreeting(ChannelHandlerContext ctx, ByteBuf in) {
      if (in.readableBytes() < 2) {
        return;
      }

      var ver = in.readByte();
      var method = in.readByte();

      if (ver != 0x05) {
        fail(ctx, new Exception("Invalid SOCKS5 version: " + ver));
        return;
      }

      if (method == 0x02 && username != null && password != null) {
        sendAuth(ctx);
        state = State.AUTH;
      } else if (method == 0x00) {
        sendUdpAssociate(ctx);
        state = State.UDP_ASSOCIATE;
      } else {
        fail(ctx, new Exception("Unsupported SOCKS5 auth method: " + method));
      }
    }

    private void decodeAuth(ChannelHandlerContext ctx, ByteBuf in) {
      if (in.readableBytes() < 2) {
        return;
      }

      in.readByte(); // VER
      var status = in.readByte();

      if (status != 0x00) {
        fail(ctx, new Exception("SOCKS5 authentication failed, status: " + status));
        return;
      }

      sendUdpAssociate(ctx);
      state = State.UDP_ASSOCIATE;
    }

    private void decodeUdpAssociate(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
      if (in.readableBytes() < 4) {
        return;
      }

      in.markReaderIndex();
      in.readByte(); // VER
      var rep = in.readByte();
      in.readByte(); // RSV
      var atyp = in.readByte();

      if (rep != 0x00) {
        fail(ctx, new Exception("SOCKS5 UDP ASSOCIATE failed, reply code: " + rep));
        return;
      }

      var addrLen = switch (atyp) {
        case 0x01 -> 4; // IPv4
        case 0x04 -> 16; // IPv6
        case 0x03 -> {
          if (in.readableBytes() < 1) {
            in.resetReaderIndex();
            yield -1;
          }
          yield in.readUnsignedByte(); // Domain name length
        }
        default -> {
          fail(ctx, new Exception("Unknown SOCKS5 address type: " + atyp));
          yield -1;
        }
      };

      if (addrLen < 0) {
        return;
      }

      if (in.readableBytes() < addrLen + 2) {
        in.resetReaderIndex();
        return;
      }

      var addrBytes = new byte[addrLen];
      in.readBytes(addrBytes);
      var port = in.readUnsignedShort();

      InetSocketAddress relay;
      if (atyp == 0x03) {
        relay = new InetSocketAddress(new String(addrBytes, StandardCharsets.US_ASCII), port);
      } else {
        relay = new InetSocketAddress(InetAddress.getByAddress(addrBytes), port);
      }

      // Proxies commonly return 0.0.0.0 as the relay address, use proxy host instead
      if (relay.getAddress() != null && relay.getAddress().isAnyLocalAddress()) {
        relay = new InetSocketAddress(proxyAddress.getAddress(), relay.getPort());
      }

      relayAddress = relay;
      log.debug("SOCKS5 UDP relay established at {}", relayAddress);

      datagramCtx.connect(relayAddress, null, connectPromise);
      state = State.DONE;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      connectPromise.tryFailure(new Exception("SOCKS5 TCP control connection closed"));
      datagramCtx.close();
      super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      fail(ctx, cause);
    }

    private void fail(ChannelHandlerContext ctx, Throwable cause) {
      connectPromise.tryFailure(cause);
      ctx.close();
    }

    private void sendAuth(ChannelHandlerContext ctx) {
      var usernameBytes = username.getBytes(StandardCharsets.UTF_8);
      var passwordBytes = password.getBytes(StandardCharsets.UTF_8);
      var auth = ctx.alloc().buffer(3 + usernameBytes.length + passwordBytes.length);
      auth.writeByte(0x01); // VER
      auth.writeByte(usernameBytes.length);
      auth.writeBytes(usernameBytes);
      auth.writeByte(passwordBytes.length);
      auth.writeBytes(passwordBytes);
      ctx.writeAndFlush(auth);
    }

    private void sendUdpAssociate(ChannelHandlerContext ctx) {
      var request = ctx.alloc().buffer(10);
      request.writeByte(0x05); // VER
      request.writeByte(0x03); // CMD: UDP ASSOCIATE
      request.writeByte(0x00); // RSV
      request.writeByte(0x01); // ATYP: IPv4
      request.writeInt(0); // DST.ADDR: 0.0.0.0
      request.writeShort(0); // DST.PORT: 0
      ctx.writeAndFlush(request);
    }
  }
}
