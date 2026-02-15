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

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// Per-invocation execution state.
/// Each trigger invocation gets its own ExecutionRun so that concurrent triggers
/// don't corrupt each other's node output sinks.
@Slf4j
public final class ExecutionRun {
  private static final long MAX_EXECUTION_COUNT = 100_000;

  private final ConcurrentHashMap<String, Sinks.Many<Map<String, NodeValue>>> nodeOutputSinks =
    new ConcurrentHashMap<>();
  private final AtomicLong executionCount = new AtomicLong(0);

  /// Whether this execution is running synchronously on the tick thread.
  private final boolean tickSynchronous;

  public ExecutionRun() {
    this(false);
  }

  public ExecutionRun(boolean tickSynchronous) {
    this.tickSynchronous = tickSynchronous;
  }

  /// Returns whether this execution is running synchronously on the tick thread.
  public boolean isTickSynchronous() {
    return tickSynchronous;
  }

  /// Waits for a node to produce outputs within this run.
  ///
  /// @param nodeId the node to wait for
  /// @return a Mono that completes with the node's outputs
  public Mono<Map<String, NodeValue>> awaitNodeOutputs(String nodeId) {
    return nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.many().replay().latest())
      .asFlux()
      .next()
      .timeout(Duration.ofSeconds(30))
      .doOnError(_ -> log.warn("Timeout waiting for node {} outputs - "
        + "DATA edge may point to a node not on the execution path", nodeId))
      .onErrorReturn(Map.of());
  }

  /// Publishes node outputs, allowing consumers to receive the latest value.
  /// Uses replay().latest() so that producers can emit multiple times (loops work)
  /// and consumers always get the most recent value.
  ///
  /// @param nodeId  the node identifier
  /// @param outputs the output values
  public void publishNodeOutputs(String nodeId, Map<String, NodeValue> outputs) {
    var result = nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.many().replay().latest())
      .tryEmitNext(outputs);
    if (result.isFailure()) {
      log.warn("Failed to publish outputs for node {}: {}", nodeId, result);
    }
  }

  /// Increments the execution count and checks if the limit has been exceeded.
  ///
  /// @return true if execution is allowed, false if the limit has been exceeded
  public boolean incrementAndCheckLimit() {
    return executionCount.incrementAndGet() <= MAX_EXECUTION_COUNT;
  }
}
