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
package com.soulfiremc.server.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public interface AuthenticatedUser {
  String getUsername();

  boolean canAccess(Resource resource);

  default void canAccessOrThrow(Resource resource) {
    if (!canAccess(resource)) {
      throw new StatusRuntimeException(
        Status.PERMISSION_DENIED.withDescription("You do not have permission to access this resource"));
    }
  }
}
