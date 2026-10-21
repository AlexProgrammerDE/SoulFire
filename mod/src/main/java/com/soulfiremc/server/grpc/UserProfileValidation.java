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
import org.apache.commons.validator.routines.EmailValidator;
import org.jooq.exception.DataAccessException;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.SQLException;
import java.util.regex.Pattern;

final class UserProfileValidation {
  private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9-]{1,30}[a-z0-9]");

  private UserProfileValidation() {
  }

  static void requireUsername(String username) {
    if (!USERNAME_PATTERN.matcher(username).matches()) {
      throw Status.INVALID_ARGUMENT.withDescription("Username must be 3 to 32 characters. Use lowercase letters, digits, and internal hyphens.").asRuntimeException();
    }
  }

  static void requireEmail(String email) {
    if (email.length() > 255 || !EmailValidator.getInstance().isValid(email)) {
      throw Status.INVALID_ARGUMENT.withDescription("Enter a valid email address with at most 255 characters.").asRuntimeException();
    }
  }

  static void rethrowConflict(Throwable failure) {
    if (!(failure instanceof DataAccessException exception)) {
      return;
    }
    var duplicate = switch (exception.getCause(SQLException.class)) {
      case SQLiteException sqlite -> sqlite.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;
      case SQLException sql -> "23505".equals(sql.getSQLState())
        || ("23000".equals(sql.getSQLState()) && sql.getErrorCode() == 1062);
      case null -> false;
    };
    if (duplicate) {
      throw Status.ALREADY_EXISTS.withDescription("Username or email is already in use.").asRuntimeException();
    }
  }

}
