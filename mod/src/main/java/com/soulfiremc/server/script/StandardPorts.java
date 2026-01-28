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

  // Branch execution outputs
  public static final String EXEC_TRUE = "true";
  public static final String EXEC_FALSE = "false";

  // Loop execution outputs
  public static final String EXEC_LOOP = "loop";
  public static final String EXEC_DONE = "done";

  // Sequence outputs (exec_0, exec_1, etc. generated dynamically)
  public static final String EXEC_PREFIX = "exec_";

  private StandardPorts() {
    // Utility class - no instantiation
  }
}
