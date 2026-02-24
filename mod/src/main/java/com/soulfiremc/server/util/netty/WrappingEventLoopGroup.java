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
import io.netty.channel.*;

@SuppressWarnings("deprecation")
public class WrappingEventLoopGroup extends WrappingEventExecutorGroup implements EventLoopGroup {
  private final EventLoopGroup delegate;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;

  public WrappingEventLoopGroup(EventLoopGroup delegate, SoulFireScheduler.RunnableWrapper runnableWrapper) {
    super(delegate, runnableWrapper);
    this.delegate = delegate;
    this.runnableWrapper = runnableWrapper;
  }

  @Override
  public EventLoop next() {
    return new WrappingEventLoop(delegate.next(), runnableWrapper);
  }

  @Override
  public ChannelFuture register(Channel channel) {
    return delegate.register(channel);
  }

  @Override
  public ChannelFuture register(ChannelPromise promise) {
    return delegate.register(promise);
  }

  @Override
  public ChannelFuture register(Channel channel, ChannelPromise promise) {
    return delegate.register(channel, promise);
  }
}
