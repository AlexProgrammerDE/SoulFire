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
import reactor.core.publisher.Mono;

import java.util.Map;

/// Runtime environment for script node execution.
/// Provides minimal API surface for nodes - just what they need to execute their logic.
/// Execution machinery (output storage, cancellation, event listeners) is hidden.
public interface NodeRuntime {
  /// Gets the per-script state store for stateful nodes.
  /// State is scoped to the script execution and cleaned up on deactivation.
  ///
  /// @return the script state store
  ScriptStateStore stateStore();

  /// Gets the SoulFire instance for accessing bots and game state.
  ///
  /// @return the instance manager
  InstanceManager instance();

  /// Gets the scheduler for async operations.
  ///
  /// @return the instance scheduler
  SoulFireScheduler scheduler();

  /// Logs a message from script execution.
  /// Used by Print node and other debugging nodes.
  ///
  /// @param level   the log level (debug, info, warn, error)
  /// @param message the message to log
  void log(String level, String message);

  /// Triggers downstream execution along a named exec handle.
  /// Only functional during reactive engine execution; self-driving nodes
  /// (LoopNode, ForEachNode, etc.) use this to iterate.
  ///
  /// @param handle  the exec output port ID to follow (e.g., "exec_loop")
  /// @param outputs the outputs to merge into the execution context
  /// @return a Mono that completes when downstream execution finishes
  default Mono<Void> executeDownstream(String handle, Map<String, NodeValue> outputs) {
    throw new UnsupportedOperationException("Only available during reactive engine execution");
  }
}
