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
package com.soulfiremc.server.script;

/// Constants for standard port names used across the scripting system.
/// These provide a single source of truth for port identifiers.
public final class StandardPorts {
  // Execution port names
  public static final String EXEC_IN = "in";
  public static final String EXEC_OUT = "out";

  // Bot input port name
  public static final String BOT_IN = "bot";

  // Branch execution outputs
  public static final String EXEC_TRUE = "exec_true";
  public static final String EXEC_FALSE = "exec_false";

  // Loop execution outputs
  public static final String EXEC_LOOP = "exec_loop";
  public static final String EXEC_DONE = "exec_done";

  // Success/error execution outputs
  public static final String EXEC_SUCCESS = "exec_success";
  public static final String EXEC_ERROR = "exec_error";

  // Gate/rate-limit execution outputs
  public static final String EXEC_ALLOWED = "exec_allowed";
  public static final String EXEC_BLOCKED = "exec_blocked";
  public static final String EXEC_DENIED = "exec_denied";

  // Switch execution outputs
  public static final String EXEC_DEFAULT = "exec_default";

  /// Builds a dynamic exec port name (e.g., exec("0") → "exec_0", exec("case3") → "exec_case3").
  public static String exec(String suffix) {
    return "exec_" + suffix;
  }

  private StandardPorts() {
    // Utility class - no instantiation
  }
}
