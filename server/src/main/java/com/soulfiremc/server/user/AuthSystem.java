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
package com.soulfiremc.server.user;

import java.util.Date;

public interface AuthSystem {
  /**
   * Authenticates a user by subject and token issued at date.
   *
   * @param subject  The subject of the token
   * @param issuedAt The date the token was made, use to check if the token is valid for that user.
   *                 Use issuedAt to check if the token is valid for that user. If a user resets their password,
   *                 the token should be invalidated by raising the required issuedAt date to the current date.
   * @return The authenticated user
   */
  ServerCommandSource authenticate(String subject, Date issuedAt);
}
