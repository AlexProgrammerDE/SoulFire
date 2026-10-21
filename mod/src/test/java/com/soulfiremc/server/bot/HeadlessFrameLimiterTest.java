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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeadlessFrameLimiterTest {
  private static long millis(long millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }

  @Test
  void frameWorkCountsTowardsTheConfiguredLimit() {
    var now = new AtomicLong();
    var waits = new ArrayList<Long>();
    var limiter = new HeadlessFrameLimiter(now::get, nanos -> {
      waits.add(nanos);
      now.addAndGet(nanos);
    });
    now.addAndGet(millis(6));

    limiter.limitDisplayFPS(100);

    assertEquals(List.of(millis(4)), waits);
    assertEquals(millis(10), now.get());
  }

  @Test
  void anotherBotCannotResetTheFrameBudget() {
    var now = new AtomicLong();
    var waits = new ArrayList<Long>();
    LongConsumer park = nanos -> {
      waits.add(nanos);
      now.addAndGet(nanos);
    };
    var first = new HeadlessFrameLimiter(now::get, park);
    var second = new HeadlessFrameLimiter(now::get, park);
    now.addAndGet(millis(6));

    first.limitDisplayFPS(100);
    second.limitDisplayFPS(100);

    assertEquals(List.of(millis(4)), waits);
    assertEquals(millis(10), now.get());
  }

  @Test
  void earlyWakeupsParkAgainForTheRemainingBudget() {
    var now = new AtomicLong();
    var waits = new ArrayList<Long>();
    var limiter = new HeadlessFrameLimiter(now::get, nanos -> {
      waits.add(nanos);
      now.addAndGet(waits.size() == 1 ? millis(5) : nanos);
    });

    limiter.limitDisplayFPS(50);

    assertEquals(List.of(millis(20), millis(15)), waits);
    assertEquals(millis(20), now.get());
  }

  @Test
  void overBudgetFramesDoNotAccumulateSleepDebtAndLimitsCanChange() {
    var now = new AtomicLong();
    var waits = new ArrayList<Long>();
    var limiter = new HeadlessFrameLimiter(now::get, nanos -> {
      waits.add(nanos);
      now.addAndGet(nanos);
    });
    now.addAndGet(millis(15));
    limiter.limitDisplayFPS(100);
    now.addAndGet(millis(5));

    limiter.limitDisplayFPS(50);

    assertEquals(List.of(millis(15)), waits);
    assertEquals(millis(35), now.get());
  }

  @Test
  void interruptionEndsTheWaitWithoutClearingTheInterruptFlag() {
    var now = new AtomicLong();
    var waits = new ArrayList<Long>();
    var limiter = new HeadlessFrameLimiter(now::get, nanos -> {
      waits.add(nanos);
      Thread.currentThread().interrupt();
    });
    try {
      limiter.limitDisplayFPS(100);

      assertTrue(Thread.currentThread().isInterrupted());
      assertEquals(List.of(millis(10)), waits);
    } finally {
      Thread.interrupted();
    }
  }
}
