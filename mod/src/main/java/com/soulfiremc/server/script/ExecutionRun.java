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
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Per-invocation execution state.
/// Each trigger invocation gets its own ExecutionRun so that concurrent triggers
/// don't corrupt each other's node output sinks.
public final class ExecutionRun {
  private final ConcurrentHashMap<String, Sinks.One<Map<String, NodeValue>>> nodeOutputSinks =
    new ConcurrentHashMap<>();

  /// Waits for a node to produce outputs within this run.
  ///
  /// @param nodeId the node to wait for
  /// @return a Mono that completes with the node's outputs
  public Mono<Map<String, NodeValue>> awaitNodeOutputs(String nodeId) {
    return nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.one())
      .asMono()
      .timeout(Duration.ofMinutes(5))
      .onErrorReturn(Map.of());
  }

  /// Publishes node outputs, completing the Sink for that node within this run.
  ///
  /// @param nodeId  the node identifier
  /// @param outputs the output values
  public void publishNodeOutputs(String nodeId, Map<String, NodeValue> outputs) {
    nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.one())
      .tryEmitValue(outputs);
  }
}
