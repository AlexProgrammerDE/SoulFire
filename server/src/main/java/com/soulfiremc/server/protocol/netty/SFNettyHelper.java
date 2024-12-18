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

import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.SFHelpers;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;

public class SFNettyHelper {
  private SFNettyHelper() {}

  public static EventLoopGroup createEventLoopGroup(String name) {
    var group =
      TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory().apply(
        r ->
          Thread.ofPlatform().name(name).daemon().priority(Thread.MAX_PRIORITY).unstarted(r));

    Runtime.getRuntime().addShutdownHook(new Thread(group::shutdownGracefully));

    return group;
  }

  public static void addProxy(ChannelPipeline pipeline, SFProxy proxy) {
    var address = proxy.getSocketAddress();
    SFHelpers.mustSupply(() -> switch (proxy.type()) {
      case HTTP -> () -> {
        if (proxy.username() != null && proxy.password() != null) {
          pipeline.addLast("proxy", new HttpProxyHandler(address, proxy.username(), proxy.password()));
        } else {
          pipeline.addLast("proxy", new HttpProxyHandler(address));
        }
      };
      case SOCKS4 -> () -> pipeline.addLast("proxy", new Socks4ProxyHandler(address, proxy.username()));
      case SOCKS5 -> () -> pipeline.addLast("proxy", new Socks5ProxyHandler(address, proxy.username(), proxy.password()));
    });
  }
}
