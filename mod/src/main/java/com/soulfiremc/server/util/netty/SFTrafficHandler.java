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

import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.metrics.InstanceMetricsCollector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/// Counts raw wire-level bytes in both directions and feeds them into the metrics collector.
public class SFTrafficHandler extends ChannelDuplexHandler {
  private InstanceMetricsCollector collector;

  private InstanceMetricsCollector collector(ChannelHandlerContext ctx) {
    if (collector == null) {
      var botConnection = ctx.channel().attr(SFConstants.NETTY_BOT_CONNECTION).get();
      if (botConnection != null) {
        collector = botConnection.instanceManager().metricsCollector();
      }
    }
    return collector;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf buf) {
      var c = collector(ctx);
      if (c != null) {
        c.addBytesReceived(buf.readableBytes());
      }
    }
    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ByteBuf buf) {
      var c = collector(ctx);
      if (c != null) {
        c.addBytesSent(buf.readableBytes());
      }
    }
    super.write(ctx, msg, promise);
  }
}
