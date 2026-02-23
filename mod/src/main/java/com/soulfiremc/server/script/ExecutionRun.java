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
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/// Per-invocation execution state.
/// Each trigger invocation gets its own ExecutionRun so that concurrent triggers
/// don't corrupt each other's node output sinks.
@Slf4j
public final class ExecutionRun {
  static final long DEFAULT_MAX_EXECUTION_COUNT = 100_000;
  static final Duration DEFAULT_DATA_EDGE_TIMEOUT = Duration.ofSeconds(10);

  private final ConcurrentHashMap<String, Sinks.Many<Map<String, NodeValue>>> nodeOutputSinks =
    new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Map<String, NodeValue>> publishedOutputs =
    new ConcurrentHashMap<>();
  private final Set<String> triggeredDataNodes = ConcurrentHashMap.newKeySet();
  private final AtomicLong executionCount = new AtomicLong(0);

  /// Stack-based check result for nested loop support.
  private final Deque<boolean[]> checkResultStack = new ConcurrentLinkedDeque<>();

  /// Whether this execution is running synchronously on the tick thread.
  private final boolean tickSynchronous;
  private final Duration dataEdgeTimeout;
  private final long maxExecutionCount;
  private final ScriptEventListener listener;

  public ExecutionRun() {
    this(false);
  }

  public ExecutionRun(boolean tickSynchronous) {
    this(tickSynchronous, DEFAULT_DATA_EDGE_TIMEOUT, DEFAULT_MAX_EXECUTION_COUNT, null);
  }

  public ExecutionRun(boolean tickSynchronous, Duration dataEdgeTimeout, long maxExecutionCount, ScriptEventListener listener) {
    this.tickSynchronous = tickSynchronous;
    this.dataEdgeTimeout = dataEdgeTimeout;
    this.maxExecutionCount = maxExecutionCount;
    this.listener = listener;
    // Initialize with one context for the top-level execution
    pushCheckContext();
  }

  /// Returns whether this execution is running synchronously on the tick thread.
  public boolean isTickSynchronous() {
    return tickSynchronous;
  }

  /// Waits for a node to produce outputs within this run.
  ///
  /// @param nodeId   the node to wait for
  /// @param nodeDesc human-readable node descriptor for log messages
  /// @return a Mono that completes with the node's outputs
  public Mono<Map<String, NodeValue>> awaitNodeOutputs(String nodeId, String nodeDesc) {
    return nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.many().replay().latest())
      .asFlux()
      .next()
      .timeout(dataEdgeTimeout)
      .doOnError(_ -> {
        log.error("Timeout waiting for node {} outputs - "
          + "DATA edge may point to a node not on the execution path", nodeDesc);
        if (listener != null) {
          listener.onLog("warn", "Data edge timeout (" + dataEdgeTimeout.getSeconds()
            + "s) for node " + nodeDesc);
        }
      })
      .onErrorReturn(Map.of());
  }

  /// Publishes that a node has failed, allowing consumers waiting via awaitNodeOutputs
  /// to receive an empty result immediately instead of timing out.
  ///
  /// @param nodeId the failed node identifier
  public void publishNodeFailure(String nodeId) {
    publishNodeOutputs(nodeId, Map.of());
  }

  /// Publishes node outputs, allowing consumers to receive the latest value.
  /// Uses replay().latest() so that producers can emit multiple times (loops work)
  /// and consumers always get the most recent value.
  ///
  /// @param nodeId   the node identifier
  /// @param outputs  the output values
  public void publishNodeOutputs(String nodeId, Map<String, NodeValue> outputs) {
    publishedOutputs.put(nodeId, outputs);
    var result = nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.many().replay().latest())
      .tryEmitNext(outputs);
    if (result.isFailure()) {
      log.warn("Failed to publish outputs for node {}: {}", nodeId, result);
    }
  }

  /// Returns the most recently published outputs for a node, or null if none published yet.
  /// Used for non-blocking synchronous resolution of on-path DATA edges.
  ///
  /// @param nodeId the node identifier
  /// @return the published outputs, or null if not yet available
  public @Nullable Map<String, NodeValue> getPublishedOutputs(String nodeId) {
    return publishedOutputs.get(nodeId);
  }

  /// Marks a data-only node as triggered for eager execution within this run.
  /// Returns true if this is the first time the node is marked (caller should execute it),
  /// false if already triggered (caller should skip, another path is handling it).
  ///
  /// @param nodeId the data-only node identifier
  /// @return true if newly marked, false if already triggered
  public boolean markDataNodeTriggered(String nodeId) {
    return triggeredDataNodes.add(nodeId);
  }

  /// Pushes a new check context onto the stack for nested loop support.
  public void pushCheckContext() {
    checkResultStack.push(new boolean[]{false, false});
  }

  /// Pops the current check context from the stack.
  public void popCheckContext() {
    checkResultStack.poll();
  }

  /// Sets the check result flag, used by ResultNode to communicate
  /// a boolean condition back to loop nodes (e.g., RepeatUntilNode).
  ///
  /// @param value the boolean result
  public void setCheckResult(boolean value) {
    var top = checkResultStack.peek();
    if (top != null) {
      top[0] = true; // wasSet
      top[1] = value; // result
    }
  }

  /// Returns whether setCheckResult was called since the last reset.
  ///
  /// @return true if a ResultNode set the check result
  public boolean wasCheckResultSet() {
    var top = checkResultStack.peek();
    return top != null && top[0];
  }

  /// Gets and resets the check result flag.
  /// Returns the current value and resets it to false.
  ///
  /// @return the check result before reset
  public boolean getAndResetCheckResult() {
    var top = checkResultStack.peek();
    if (top != null) {
      var result = top[1];
      top[0] = false;
      top[1] = false;
      return result;
    }
    return false;
  }

  /// Resets all data-only node trigger flags, allowing them to re-execute.
  /// Used by loop nodes before evaluating check chains so that data-only
  /// nodes (e.g., CompareNode) can re-evaluate with fresh upstream values.
  public void resetDataNodeTriggers() {
    triggeredDataNodes.clear();
  }

  /// Increments the execution count and checks if the limit has been exceeded.
  ///
  /// @return true if execution is allowed, false if the limit has been exceeded
  public boolean incrementAndCheckLimit() {
    return executionCount.incrementAndGet() <= maxExecutionCount;
  }

  /// Returns the current execution count for stats reporting.
  public long getExecutionCount() {
    return executionCount.get();
  }

  /// Returns the maximum execution count limit.
  public long getMaxExecutionCount() {
    return maxExecutionCount;
  }
}
