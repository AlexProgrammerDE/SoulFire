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
package com.soulfiremc.server.grpc;

import io.grpc.Status;

import java.time.Duration;

/// Gives native rendering a bounded startup period before requiring client input.
final class PovSessionHeartbeat {
  static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);
  private static final long INPUT_TIMEOUT_NANOS = Duration.ofSeconds(5).toNanos();

  private long deadline;
  private boolean streaming;

  PovSessionHeartbeat(long now) {
    deadline = now + STARTUP_TIMEOUT.toNanos();
  }

  synchronized void check(long now) {
    if (now - deadline >= 0) {
      throw Status.DEADLINE_EXCEEDED.withDescription(streaming
        ? "POV heartbeat timed out" : "POV startup timed out").asRuntimeException();
    }
  }

  synchronized void frameSent(long now) {
    if (!streaming) {
      streaming = true;
      deadline = now + INPUT_TIMEOUT_NANOS;
    }
  }

  synchronized void inputReceived(long now) {
    if (streaming) deadline = now + INPUT_TIMEOUT_NANOS;
  }
}
