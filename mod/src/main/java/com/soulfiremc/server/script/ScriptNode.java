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

import reactor.core.publisher.Mono;

import java.util.Map;

/// Base interface for all script nodes in the visual scripting system.
/// Each node type implements this interface to define its execution behavior.
///
/// Nodes are functional and pure - they receive inputs and produce outputs.
/// They have access to a minimal runtime API (instance, scheduler, pending ops)
/// but not to execution machinery like output storage or cancellation state.
///
/// Metadata (type, ports, category, etc.) is stored in the NodeRegistry,
/// not on the node instance itself.
public interface ScriptNode {
  /// Executes this node reactively with the given runtime and inputs.
  ///
  /// @param runtime the node runtime providing access to instance and scheduler
  /// @param inputs  the resolved input values from connected nodes or default values
  /// @return a Mono that completes with the node's output values
  Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs);
}
