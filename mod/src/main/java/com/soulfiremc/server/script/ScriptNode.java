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
import java.util.concurrent.CompletableFuture;

/// Base interface for all script nodes in the visual scripting system.
/// Each node type implements this interface to define its execution behavior.
///
/// Nodes are functional and pure - they receive inputs and produce outputs.
/// They have access to a minimal runtime API (instance, scheduler, pending ops)
/// but not to execution machinery like output storage or cancellation state.
public interface ScriptNode {
  /// Returns the unique type identifier for this node.
  /// This is used to match node definitions from the proto with their implementations.
  ///
  /// @return the node type identifier (e.g., "trigger.on_start", "action.pathfind")
  String getType();

  /// Returns the complete metadata for this node type.
  /// Used by clients to render the node without hardcoded knowledge.
  ///
  /// @return the node metadata including ports, display name, category, etc.
  NodeMetadata getMetadata();

  /// Executes this node with the given runtime and inputs.
  /// The execution can be asynchronous for operations like pathfinding or block breaking.
  ///
  /// @param runtime the node runtime providing access to instance and scheduler
  /// @param inputs  the resolved input values from connected nodes or default values
  /// @return a future that completes with the node's output values
  CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs);

  /// Returns whether this node is a trigger node (entry point for script execution).
  /// Trigger nodes have no execution input and start script flows.
  ///
  /// @return true if this is a trigger node
  default boolean isTrigger() {
    return getMetadata().isTrigger();
  }

  /// Returns the default values for this node's inputs.
  /// These are used when an input is not connected to another node.
  ///
  /// @return a map of input names to their default values
  default Map<String, NodeValue> getDefaultInputs() {
    return Map.of();
  }
}
