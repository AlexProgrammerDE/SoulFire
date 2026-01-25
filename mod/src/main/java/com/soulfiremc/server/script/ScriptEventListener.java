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

import java.util.Map;

/// Interface for receiving script execution events.
/// Implementations can use these events to update the UI or log execution progress.
public interface ScriptEventListener {
  /// Called when a node starts executing.
  ///
  /// @param nodeId the unique identifier of the node
  void onNodeStarted(String nodeId);

  /// Called when a node completes execution successfully.
  ///
  /// @param nodeId  the unique identifier of the node
  /// @param outputs the output values produced by the node
  void onNodeCompleted(String nodeId, Map<String, Object> outputs);

  /// Called when a node encounters an error during execution.
  ///
  /// @param nodeId the unique identifier of the node
  /// @param error  description of the error that occurred
  void onNodeError(String nodeId, String error);

  /// Called when the entire script completes execution.
  ///
  /// @param success true if the script completed without errors
  void onScriptCompleted(boolean success);

  /// Called when the script execution is cancelled.
  void onScriptCancelled();

  /// Called when a variable value changes during execution.
  ///
  /// @param name  the name of the variable
  /// @param value the new value of the variable
  default void onVariableChanged(String name, Object value) {
    // Default no-op implementation
  }

  /// Called when a script logs a message (via Print node).
  ///
  /// @param level   the log level (debug, info, warn, error)
  /// @param message the log message
  default void onLog(String level, String message) {
    // Default no-op implementation
  }

  /// A no-op implementation of ScriptEventListener.
  ScriptEventListener NOOP = new ScriptEventListener() {
    @Override
    public void onNodeStarted(String nodeId) {}

    @Override
    public void onNodeCompleted(String nodeId, Map<String, Object> outputs) {}

    @Override
    public void onNodeError(String nodeId, String error) {}

    @Override
    public void onScriptCompleted(boolean success) {}

    @Override
    public void onScriptCancelled() {}
  };
}
