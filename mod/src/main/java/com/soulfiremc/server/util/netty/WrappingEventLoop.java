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
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;

public class WrappingEventLoop extends WrappingEventLoopGroup implements EventLoop {
  private final EventLoop delegate;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;

  public WrappingEventLoop(EventLoop delegate, SoulFireScheduler.RunnableWrapper runnableWrapper) {
    super(delegate, runnableWrapper);
    this.delegate = delegate;
    this.runnableWrapper = runnableWrapper;
  }

  @Override
  public EventLoopGroup parent() {
    return new WrappingEventLoopGroup(delegate.parent(), runnableWrapper);
  }

  @Override
  public boolean inEventLoop(Thread thread) {
    return delegate.inEventLoop(thread);
  }
}
