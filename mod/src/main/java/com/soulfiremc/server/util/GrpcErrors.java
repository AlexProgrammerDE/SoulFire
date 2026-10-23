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

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

public final class GrpcErrors {
  private GrpcErrors() {}

  public static boolean isExpected(@Nullable Throwable throwable) {
    return throwable instanceof StatusRuntimeException error && switch (error.getStatus().getCode()) {
      case ABORTED, ALREADY_EXISTS, CANCELLED, FAILED_PRECONDITION, INVALID_ARGUMENT,
           NOT_FOUND, OUT_OF_RANGE, PERMISSION_DENIED, UNAUTHENTICATED -> true;
      default -> false;
    };
  }

  public static StatusRuntimeException logAndMap(Logger logger, String message, Throwable throwable) {
    if (isExpected(throwable)) {
      logger.debug(message, throwable);
    } else {
      logger.error(message, throwable);
    }
    if (throwable instanceof StatusRuntimeException error) {
      return error;
    }
    return Status.INTERNAL
      .withDescription(Objects.requireNonNullElse(
        throwable.getMessage(), throwable.getClass().getSimpleName()))
      .withCause(throwable)
      .asRuntimeException();
  }
}
