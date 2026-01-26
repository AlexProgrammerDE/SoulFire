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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;

import java.util.concurrent.Future;

/// Runtime environment for script node execution.
/// Provides minimal API surface for nodes - just what they need to execute their logic.
/// Execution machinery (output storage, cancellation, event listeners) is hidden.
public interface NodeRuntime {
  /// Gets the SoulFire instance for accessing bots and game state.
  ///
  /// @return the instance manager
  InstanceManager instance();

  /// Gets the scheduler for async operations.
  ///
  /// @return the instance scheduler
  SoulFireScheduler scheduler();

  /// Registers a pending async operation for cleanup on script deactivation.
  /// Call this for any long-running futures that should be cancelled when the script stops.
  ///
  /// @param future the future to track
  void addPendingOperation(Future<?> future);

  /// Logs a message from script execution.
  /// Used by Print node and other debugging nodes.
  ///
  /// @param level   the log level (debug, info, warn, error)
  /// @param message the message to log
  void log(String level, String message);
}
