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
package com.soulfiremc.server.task;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SmeltTaskProviderTest {
  @Test
  void loadsTheRequestedOperationsAsOneBatch() {
    assertEquals(
      8,
      SmeltTaskProvider.batchOperationCount(8, 64, 64, 1)
    );
    assertEquals(
      32,
      SmeltTaskProvider.batchOperationCount(64, 64, 64, 2)
    );
    assertEquals(
      12,
      SmeltTaskProvider.batchOperationCount(64, 12, 64, 1)
    );
  }

  @Test
  void loadsEnoughFuelForTheWholeBatch() {
    assertEquals(
      1,
      SmeltTaskProvider.additionalFuelItems(8, 200, 0, 0, 1_600)
    );
    assertEquals(
      2,
      SmeltTaskProvider.additionalFuelItems(9, 200, 0, 0, 1_600)
    );
    assertEquals(
      0,
      SmeltTaskProvider.additionalFuelItems(8, 200, 0, 1, 1_600)
    );
  }

  @Test
  void consumesResidualBurnTimeBeforeQueuingMoreFuel() {
    assertEquals(
      0,
      SmeltTaskProvider.additionalFuelItems(7, 200, 1_400, 0, 300)
    );
    assertEquals(
      1,
      SmeltTaskProvider.additionalFuelItems(8, 200, 1_399, 0, 300)
    );
  }

  @Test
  void productiveFullStackCanExceedTheOriginalWaitingLimit() {
    var watchdog = new SmeltTaskProvider.CookingProgressWatchdog(12_000);
    assertDoesNotThrow(() -> {
      for (var output = 0; output < 64; output++) {
        for (var cookingTick = 0; cookingTick < 200; cookingTick++) {
          watchdog.observe(cookingTick / 200.0F, output);
        }
      }
    });
  }

  @Test
  void unchangedCookingAndOutputStillTimeOut() {
    var watchdog = new SmeltTaskProvider.CookingProgressWatchdog(3);
    watchdog.observe(0.5F, 12);
    for (var tick = 0; tick < 3; tick++) {
      watchdog.observe(0.5F, 12);
    }
    var error = assertThrows(StatusRuntimeException.class, () -> watchdog.observe(0.5F, 12));
    assertEquals(Status.Code.DEADLINE_EXCEEDED, error.getStatus().getCode());
  }

  @Test
  void newOutputRestartsTheDeadlineWhenTheCookingBarWraps() {
    var watchdog = new SmeltTaskProvider.CookingProgressWatchdog(2);
    watchdog.observe(0.75F, 4);
    watchdog.observe(0.75F, 4);
    watchdog.observe(0.75F, 4);
    watchdog.observe(0.0F, 5);
    watchdog.observe(0.0F, 5);
    assertDoesNotThrow(() -> watchdog.observe(0.0F, 5));
    var error = assertThrows(StatusRuntimeException.class, () -> watchdog.observe(0.0F, 5));
    assertEquals(Status.Code.DEADLINE_EXCEEDED, error.getStatus().getCode());
  }

  @Test
  void regressingCookingAndRemovedOutputDoNotHideAStall() {
    var watchdog = new SmeltTaskProvider.CookingProgressWatchdog(2);
    watchdog.observe(0.75F, 3);
    watchdog.observe(0.5F, 2);
    watchdog.observe(0.25F, 1);
    var error = assertThrows(StatusRuntimeException.class, () -> watchdog.observe(0.0F, 0));
    assertEquals(Status.Code.DEADLINE_EXCEEDED, error.getStatus().getCode());
  }

}
