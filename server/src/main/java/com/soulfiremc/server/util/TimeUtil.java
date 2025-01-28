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
package com.soulfiremc.server.util;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Simple class to make waiting easier and less verbose.
 */
public class TimeUtil {
  public static final long NANOSECONDS_PER_SECOND = TimeUnit.SECONDS.toNanos(1L);
  public static final long NANOSECONDS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1L);

  private TimeUtil() {}

  public static void waitTime(long time, TimeUnit unit) {
    try {
      ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
        private final long endTime = System.nanoTime() + unit.toNanos(time);

        public boolean isReleasable() {
          return System.nanoTime() >= endTime;
        }

        public boolean block() throws InterruptedException {
          var remainingTime = endTime - System.nanoTime();
          if (remainingTime > 0) {
            Thread.sleep(remainingTime / NANOSECONDS_PER_MILLISECOND, (int) (remainingTime % NANOSECONDS_PER_MILLISECOND));
          }

          return true;
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void waitCondition(BooleanSupplier condition) {
    waitCondition(condition, null);
  }

  public static void waitCondition(BooleanSupplier condition, @Nullable Duration timeout) {
    var startTime = System.nanoTime();
    try {
      ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
        public boolean isReleasable() {
          return !condition.getAsBoolean() || (timeout != null && System.nanoTime() - startTime >= timeout.toNanos());
        }

        public boolean block() throws InterruptedException {
          Thread.sleep(1);
          return !condition.getAsBoolean();
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
