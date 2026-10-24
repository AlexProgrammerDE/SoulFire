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
package com.soulfiremc.server.util;

import io.grpc.Metadata;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

final class GrpcErrorsTest {
  @Test
  void rejectedActionsKeepTheirStatusAndTrailersWithoutErrorLogging() {
    var logger = mock(Logger.class);
    var trailers = new Metadata();
    var rejection = Status.ABORTED.asRuntimeException(trailers);
    var message = "Bot action rejected";

    var result = GrpcErrors.logAndMap(logger, message, rejection);

    assertTrue(GrpcErrors.isExpected(result));
    assertSame(rejection, result);
    assertSame(trailers, result.getTrailers());
    verify(logger).debug(message, rejection);
    verifyNoMoreInteractions(logger);
  }

  @Test
  void operationalFailuresRemainVisibleEvenWithARejectionAsTheirCause() {
    for (var status : new Status[]{Status.INTERNAL, Status.UNKNOWN, Status.UNAVAILABLE, Status.DEADLINE_EXCEEDED}) {
      var logger = mock(Logger.class);
      var failure = status.withCause(Status.ABORTED.asRuntimeException()).asRuntimeException();
      var message = "Bot action failed";

      assertSame(failure, GrpcErrors.logAndMap(logger, message, failure));
      assertFalse(GrpcErrors.isExpected(failure));
      verify(logger).error(message, failure);
      verifyNoMoreInteractions(logger);
    }
  }

  @Test
  void unexpectedWrappersAreInternalFailuresRatherThanClientRejections() {
    var logger = mock(Logger.class);
    var failure = new IllegalStateException(Status.ABORTED.asRuntimeException());
    var message = "Bot action failed";

    var result = GrpcErrors.logAndMap(logger, message, failure);

    assertFalse(GrpcErrors.isExpected(failure));
    assertEquals(Status.Code.INTERNAL, result.getStatus().getCode());
    assertSame(failure, result.getStatus().getCause());
    verify(logger).error(message, failure);
    verifyNoMoreInteractions(logger);
  }

  @Test
  void failuresWithoutMessagesStillHaveADescriptionAndOriginalCause() {
    var logger = mock(Logger.class);
    var failure = new RuntimeException();
    var message = "Bot action failed";

    var result = GrpcErrors.logAndMap(logger, message, failure);

    assertEquals(Status.Code.INTERNAL, result.getStatus().getCode());
    assertEquals(failure.getClass().getSimpleName(), result.getStatus().getDescription());
    assertSame(failure, result.getStatus().getCause());
    assertFalse(GrpcErrors.isExpected(null));
    verify(logger).error(message, failure);
    verifyNoMoreInteractions(logger);
  }
}
