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

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.kqueue.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.*;

import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

public final class TransportHelper {
  public static final TransportHelper.TransportType TRANSPORT_TYPE = TransportHelper.determineTransportMethod();

  private TransportHelper() {
  }

  private static TransportType determineTransportMethod() {
    if (IoUring.isAvailable()) {
      return new TransportType(
        TransportMethod.IO_URING,
        IoUringServerSocketChannel.class,
        IoUringServerSocketChannel::new,
        IoUringSocketChannel.class,
        IoUringSocketChannel::new,
        IoUringDatagramChannel.class,
        IoUringDatagramChannel::new,
        factory -> new MultiThreadIoEventLoopGroup(0, factory, IoUringIoHandler.newFactory()),
        IoUring.isTcpFastOpenServerSideAvailable(),
        IoUring.isTcpFastOpenClientSideAvailable()
      );
    }

    if (Epoll.isAvailable()) {
      return new TransportType(
        TransportMethod.EPOLL,
        EpollServerSocketChannel.class,
        EpollServerSocketChannel::new,
        EpollSocketChannel.class,
        EpollSocketChannel::new,
        EpollDatagramChannel.class,
        EpollDatagramChannel::new,
        factory -> new MultiThreadIoEventLoopGroup(0, factory, EpollIoHandler.newFactory()),
        Epoll.isTcpFastOpenServerSideAvailable(),
        Epoll.isTcpFastOpenClientSideAvailable()
      );
    }

    if (KQueue.isAvailable()) {
      return new TransportType(
        TransportMethod.KQUEUE,
        KQueueServerSocketChannel.class,
        KQueueServerSocketChannel::new,
        KQueueSocketChannel.class,
        KQueueSocketChannel::new,
        KQueueDatagramChannel.class,
        KQueueDatagramChannel::new,
        factory -> new MultiThreadIoEventLoopGroup(0, factory, KQueueIoHandler.newFactory()),
        KQueue.isTcpFastOpenServerSideAvailable(),
        KQueue.isTcpFastOpenClientSideAvailable()
      );
    }

    return new TransportType(
      TransportMethod.NIO,
      NioServerSocketChannel.class,
      NioServerSocketChannel::new,
      NioSocketChannel.class,
      NioSocketChannel::new,
      NioDatagramChannel.class,
      NioDatagramChannel::new,
      factory -> new MultiThreadIoEventLoopGroup(0, factory, NioIoHandler.newFactory()),
      false,
      false
    );
  }

  public enum TransportMethod {
    NIO, EPOLL, KQUEUE, IO_URING
  }

  public record TransportType(TransportMethod method,
                              Class<? extends ServerSocketChannel> serverSocketChannelClass,
                              ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory,
                              Class<? extends SocketChannel> socketChannelClass,
                              ChannelFactory<? extends SocketChannel> socketChannelFactory,
                              Class<? extends DatagramChannel> datagramChannelClass,
                              ChannelFactory<? extends DatagramChannel> datagramChannelFactory,
                              Function<ThreadFactory, EventLoopGroup> eventLoopGroupFactory,
                              boolean supportsTcpFastOpenServer,
                              boolean supportsTcpFastOpenClient) {
  }
}
