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
package com.soulfiremc.server.bot;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/// Tracks every network channel created for a bot and closes late arrivals once shutdown begins.
final class NetworkChannelTracker {
  private final Set<Channel> channels = ConcurrentHashMap.newKeySet();
  private final AtomicReference<Channel> currentChannel = new AtomicReference<>();
  private final AtomicBoolean closing = new AtomicBoolean();

  public void track(Channel channel) {
    channels.add(channel);
    currentChannel.set(channel);
    channel.closeFuture().addListener(_ -> {
      channels.remove(channel);
      currentChannel.compareAndSet(channel, null);
    });

    if (closing.get()) {
      channel.close();
    }
  }

  public Optional<Channel> activeChannel() {
    return Optional.ofNullable(currentChannel.get()).filter(Channel::isActive);
  }

  public void closeAll(Duration timeout) throws InterruptedException, TimeoutException {
    closing.set(true);

    var closeFutures = channels.stream()
      .filter(Channel::isOpen)
      .map(Channel::close)
      .toList();
    var deadline = System.nanoTime() + timeout.toNanos();
    for (var closeFuture : closeFutures) {
      awaitClose(closeFuture, deadline);
    }
  }

  public boolean hasOpenChannels() {
    return channels.stream().anyMatch(Channel::isOpen);
  }

  private static void awaitClose(ChannelFuture closeFuture, long deadline) throws InterruptedException, TimeoutException {
    if (!closeFuture.isDone()) {
      var remainingNanos = deadline - System.nanoTime();
      if (remainingNanos <= 0 || !closeFuture.await(remainingNanos, TimeUnit.NANOSECONDS)) {
        throw new TimeoutException("Timed out while closing a bot network channel");
      }
    }

    if (!closeFuture.isSuccess()) {
      throw new IllegalStateException("Failed to close a bot network channel", closeFuture.cause());
    }
  }
}
