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

import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.proxy.SFProxy;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.InetSocketAddress;

public final class NettyHelper {
  public static final String PROXY_NAME = "sf_proxy";

  private NettyHelper() {
  }

  public static void addProxy(SFProxy proxy, ChannelPipeline pipeline, boolean isBedrock) {
    switch (proxy.type()) {
      case HTTP -> {
        if (proxy.username() != null && proxy.password() != null) {
          pipeline.addLast(PROXY_NAME, new HttpProxyHandler(proxy.address(), proxy.username(), proxy.password()));
        } else {
          pipeline.addLast(PROXY_NAME, new HttpProxyHandler(proxy.address()));
        }
      }
      case SOCKS4 -> {
        if (proxy.username() != null) {
          pipeline.addLast(PROXY_NAME, new Socks4ProxyHandler(proxy.address(), proxy.username()));
        } else {
          pipeline.addLast(PROXY_NAME, new Socks4ProxyHandler(proxy.address()));
        }
      }
      case SOCKS5 -> {
        if (isBedrock) {
          pipeline.addLast(PROXY_NAME, new Socks5UdpRelayHandler(
            (InetSocketAddress) proxy.address(), proxy.username(), proxy.password()));
        } else if (proxy.username() != null && proxy.password() != null) {
          pipeline.addLast(PROXY_NAME, new Socks5ProxyHandler(proxy.address(), proxy.username(), proxy.password()));
        } else {
          pipeline.addLast(PROXY_NAME, new Socks5ProxyHandler(proxy.address()));
        }
      }
      default -> throw new UnsupportedOperationException("Unsupported proxy type: " + proxy.type());
    }
  }

  public static EventLoopGroup createEventLoopGroup(String name, SoulFireScheduler.RunnableWrapper runnableWrapper) {
    var group =
      TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory().apply(
        r ->
          Thread.ofPlatform().name(name).daemon().priority(Thread.MAX_PRIORITY).unstarted(runnableWrapper.wrap(r)));

    Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(group::shutdownGracefully));

    return group;
  }
}
