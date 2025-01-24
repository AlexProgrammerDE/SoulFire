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

import com.soulfiremc.server.SoulFireScheduler;
import io.netty.channel.EventLoopGroup;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;

public class SFNettyHelper {
  private SFNettyHelper() {}

  public static EventLoopGroup createEventLoopGroup(String name, SoulFireScheduler.RunnableWrapper runnableWrapper) {
    var group =
      TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory().apply(
        r ->
          Thread.ofPlatform().name(name).daemon().priority(Thread.MAX_PRIORITY).unstarted(runnableWrapper.wrap(r)));

    Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(group::shutdownGracefully));

    return group;
  }
}
