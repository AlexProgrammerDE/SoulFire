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
package com.soulfiremc.server.renderer;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PovEncoderProbesTest {
  @Test
  void retriesFromTheEndOfTheFailedProbeAndKeepsSuccessfulHardwareAvailable() {
    var clock = new AtomicLong(Long.MAX_VALUE - Duration.ofSeconds(1).toNanos());
    var probes = new PovEncoderProbes(clock::get);
    var attempts = new AtomicInteger();
    var unavailable = new IllegalStateException();
    assertSame(unavailable, assertThrows(IllegalStateException.class, () -> probes.open("nvenc", () -> {
      attempts.incrementAndGet();
      clock.addAndGet(Duration.ofSeconds(3).toNanos());
      throw unavailable;
    })));
    clock.addAndGet(PovEncoderProbes.RETRY_DELAY.toNanos() - 1);
    assertTrue(probes.open("nvenc", attempts::incrementAndGet).isEmpty());
    assertEquals(1, attempts.get());

    clock.incrementAndGet();
    assertEquals(2, probes.open("nvenc", attempts::incrementAndGet).orElseThrow());
    assertEquals(3, probes.open("nvenc", attempts::incrementAndGet).orElseThrow());
  }

  @Test
  void concurrentViewersShareFailureWhileOtherEncodersRemainIndependent() throws Exception {
    var probes = new PovEncoderProbes(() -> 0);
    var attempts = new AtomicInteger();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var secondStarted = new CountDownLatch(1);
    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = workers.submit(() -> assertThrows(IllegalStateException.class, () -> probes.open("nvenc", () -> {
        attempts.incrementAndGet();
        entered.countDown();
        try {
          if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Probe was not released");
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new AssertionError(interrupted);
        }
        throw new IllegalStateException();
      })));
      try {
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var second = workers.submit(() -> {
          secondStarted.countDown();
          return probes.open("nvenc", attempts::incrementAndGet);
        });
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        assertEquals(42, probes.open("qsv", () -> 42).orElseThrow());
        release.countDown();
        first.get(5, TimeUnit.SECONDS);
        assertTrue(second.get(5, TimeUnit.SECONDS).isEmpty());
        assertEquals(1, attempts.get());
      } finally {
        release.countDown();
      }
    }
  }
}
