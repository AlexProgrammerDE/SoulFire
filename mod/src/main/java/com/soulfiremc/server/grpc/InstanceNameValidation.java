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

import java.util.regex.Pattern;

final class InstanceNameValidation {
  // Match JavaScript trim so client and server normalize pasted names consistently.
  private static final Pattern OUTER_WHITESPACE = Pattern.compile("^[\\s\\p{Z}\\x{FEFF}]+|[\\s\\p{Z}\\x{FEFF}]+$");
  private static final Pattern CONTROL_OR_LINE_BREAK = Pattern.compile("[\\p{Cc}\\p{Zl}\\p{Zp}]");

  private InstanceNameValidation() {
  }

  static String requireName(String name) {
    var normalized = OUTER_WHITESPACE.matcher(name).replaceAll("");
    if (normalized.isEmpty() || normalized.length() > 32 || CONTROL_OR_LINE_BREAK.matcher(normalized).find()) {
      throw Status.INVALID_ARGUMENT.withDescription("Instance names must contain 1 to 32 characters without control characters or line breaks.").asRuntimeException();
    }
    return normalized;
  }
}
