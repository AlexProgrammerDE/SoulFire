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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/// Shares failed automatic encoder probes without permanently disabling hardware.
final class PovEncoderProbes {
  static final Duration RETRY_DELAY = Duration.ofSeconds(30);

  private final ConcurrentHashMap<String, Probe> probes = new ConcurrentHashMap<>();
  private final LongSupplier clock;

  PovEncoderProbes() {
    this(System::nanoTime);
  }

  PovEncoderProbes(LongSupplier clock) {
    this.clock = clock;
  }

  <T> Optional<T> open(String name, Supplier<T> initialize) {
    var probe = probes.computeIfAbsent(name, _ -> new Probe());
    synchronized (probe) {
      if (probe.failed && clock.getAsLong() - probe.failedAt < RETRY_DELAY.toNanos()) return Optional.empty();
      try {
        var encoder = initialize.get();
        probe.failed = false;
        return Optional.of(encoder);
      } catch (RuntimeException failure) {
        probe.failedAt = clock.getAsLong();
        probe.failed = true;
        throw failure;
      }
    }
  }

  private static final class Probe {
    private boolean failed;
    private long failedAt;
  }
}
