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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
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
          Thread.sleep(1);
          return System.nanoTime() >= endTime;
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void waitCondition(BooleanSupplier condition) {
    try {
      ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
        public boolean isReleasable() {
          return !condition.getAsBoolean();
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

  public static void acquireYielding(Semaphore semaphore) {
    try {
      ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
        public boolean isReleasable() {
          return semaphore.tryAcquire();
        }

        public boolean block() throws InterruptedException {
          semaphore.acquire();
          return true;
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void lockYielding(Lock lock) {
    try {
      ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
        public boolean isReleasable() {
          return lock.tryLock();
        }

        public boolean block() {
          lock.lock();
          return true;
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
