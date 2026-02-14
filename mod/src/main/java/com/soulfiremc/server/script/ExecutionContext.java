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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Immutable execution context that flows along execution edges.
/// Contains accumulated outputs from all upstream nodes in the current execution chain.
/// Each branch of execution gets its own context snapshot.
///
/// This is the Node-RED/n8n-style "message" that flows through the execution chain.
/// Trigger outputs (bot, tickCount, message, etc.) become the initial context.
/// Each node's outputs are merged into the context for downstream nodes.
public record ExecutionContext(Map<String, NodeValue> values) {
  private static final ExecutionContext EMPTY = new ExecutionContext(Map.of());

  /// Creates an empty execution context.
  public static ExecutionContext empty() {
    return EMPTY;
  }

  /// Creates an execution context from initial trigger outputs.
  public static ExecutionContext from(Map<String, NodeValue> initial) {
    return new ExecutionContext(Collections.unmodifiableMap(new HashMap<>(initial)));
  }

  /// Creates a new context with the given node outputs merged in.
  /// New values override existing ones with the same key.
  /// Returns a new instance — this context is not modified.
  public ExecutionContext mergeWith(Map<String, NodeValue> nodeOutputs) {
    if (nodeOutputs.isEmpty()) {
      return this;
    }
    var merged = new HashMap<>(values);
    merged.putAll(nodeOutputs);
    return new ExecutionContext(Collections.unmodifiableMap(merged));
  }
}
