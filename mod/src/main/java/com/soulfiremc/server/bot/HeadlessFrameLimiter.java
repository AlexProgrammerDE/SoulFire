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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/// Paces one bot's frames without the display limiter's shared state or busy spinning.
public final class HeadlessFrameLimiter {
  private final LongSupplier nanoTime;
  private final LongConsumer parkNanos;
  private long lastFrameTime;

  public HeadlessFrameLimiter() {
    this(System::nanoTime, LockSupport::parkNanos);
  }

  HeadlessFrameLimiter(LongSupplier nanoTime, LongConsumer parkNanos) {
    this.nanoTime = nanoTime;
    this.parkNanos = parkNanos;
    this.lastFrameTime = nanoTime.getAsLong();
  }

  public void limitDisplayFPS(int framerateLimit) {
    var frameDuration = TimeUnit.SECONDS.toNanos(1) / framerateLimit;
    while (!Thread.currentThread().isInterrupted()) {
      var remaining = frameDuration - (nanoTime.getAsLong() - lastFrameTime);
      if (remaining <= 0) {
        break;
      }
      parkNanos.accept(remaining);
    }
    lastFrameTime = nanoTime.getAsLong();
  }
}
