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

import com.soulfiremc.settings.proxy.SFProxy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;

public class SFNettyHelper {
  public static final TransportMethod TRANSPORT_METHOD =
    switch (TransportHelper.determineTransportMethod().method()) {
      case IO_URING -> new TransportMethod(
        IOUring.isTcpFastOpenClientSideAvailable(),
        IOUringSocketChannel.class,
        IOUringDatagramChannel.class,
        IOUringEventLoopGroup::new);
      case EPOLL -> new TransportMethod(
        Epoll.isTcpFastOpenClientSideAvailable(),
        EpollSocketChannel.class,
        EpollDatagramChannel.class,
        EpollEventLoopGroup::new);
      case NIO, KQUEUE -> new TransportMethod(
        false, NioSocketChannel.class, NioDatagramChannel.class, NioEventLoopGroup::new);
    };

  private SFNettyHelper() {}

  public static EventLoopGroup createEventLoopGroup(int threads, String name) {
    var group =
      TRANSPORT_METHOD.eventLoopFactory.apply(
        threads,
        r ->
          Thread.ofPlatform().name(name).daemon().priority(Thread.MAX_PRIORITY).unstarted(r));

    Runtime.getRuntime().addShutdownHook(new Thread(group::shutdownGracefully));

    return group;
  }

  public static void addProxy(ChannelPipeline pipeline, SFProxy proxy) {
    var address = proxy.getSocketAddress();
    switch (proxy.type()) {
      case HTTP -> {
        if (proxy.username() != null && proxy.password() != null) {
          pipeline.addFirst("proxy", new HttpProxyHandler(address, proxy.username(), proxy.password()));
        } else {
          pipeline.addFirst("proxy", new HttpProxyHandler(address));
        }
      }
      case SOCKS4 -> pipeline.addFirst("proxy", new Socks4ProxyHandler(address, proxy.username()));
      case SOCKS5 -> pipeline.addFirst("proxy", new Socks5ProxyHandler(address, proxy.username(), proxy.password()));
    }
  }

  public record TransportMethod(
    boolean tcpFastOpenClientSideAvailable,
    Class<? extends Channel> channelClass,
    Class<? extends DatagramChannel> datagramChannelClass,
    BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopFactory) {}
}
