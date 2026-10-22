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
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PovSessionHeartbeatTest {
  @Test
  void givesAColdRendererTimeToPublishTheInputCapability() {
    var heartbeat = new PovSessionHeartbeat(0);
    var firstFrame = Duration.ofSeconds(10).toNanos();
    assertDoesNotThrow(() -> heartbeat.check(firstFrame));
    heartbeat.frameSent(firstFrame);
    assertDoesNotThrow(() -> heartbeat.check(firstFrame + Duration.ofSeconds(4).toNanos()));
    assertExpired(heartbeat, firstFrame + Duration.ofSeconds(5).toNanos());
  }

  @Test
  void videoAloneDoesNotKeepAnAbandonedSessionAlive() {
    var heartbeat = new PovSessionHeartbeat(0);
    heartbeat.frameSent(Duration.ofSeconds(1).toNanos());
    heartbeat.frameSent(Duration.ofSeconds(5).toNanos());
    assertExpired(heartbeat, Duration.ofSeconds(6).toNanos());
  }

  @Test
  void inputRenewsTheStreamingDeadlineButCannotExtendStartup() {
    var heartbeat = new PovSessionHeartbeat(0);
    heartbeat.inputReceived(Duration.ofSeconds(29).toNanos());
    assertExpired(heartbeat, PovSessionHeartbeat.STARTUP_TIMEOUT.toNanos());

    heartbeat = new PovSessionHeartbeat(0);
    heartbeat.frameSent(Duration.ofSeconds(10).toNanos());
    heartbeat.inputReceived(Duration.ofSeconds(14).toNanos());
    var streaming = heartbeat;
    assertDoesNotThrow(() -> streaming.check(Duration.ofSeconds(18).toNanos()));
    assertExpired(streaming, Duration.ofSeconds(19).toNanos());
  }

  @Test
  void handlesTheMonotonicClockWrapping() {
    var start = Long.MAX_VALUE - Duration.ofSeconds(1).toNanos();
    var heartbeat = new PovSessionHeartbeat(start);
    assertDoesNotThrow(() -> heartbeat.check(start + Duration.ofSeconds(10).toNanos()));
    assertExpired(heartbeat, start + PovSessionHeartbeat.STARTUP_TIMEOUT.toNanos());
  }

  private static void assertExpired(PovSessionHeartbeat heartbeat, long now) {
    assertEquals(Status.Code.DEADLINE_EXCEEDED,
      assertThrows(StatusRuntimeException.class, () -> heartbeat.check(now)).getStatus().getCode());
  }
}
