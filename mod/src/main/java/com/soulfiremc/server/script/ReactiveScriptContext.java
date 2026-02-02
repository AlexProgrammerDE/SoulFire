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
import lombok.Getter;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/// Reactive execution context for scripts.
/// Implements NodeRuntime to provide the API surface for nodes.
/// Uses Reactor Sinks to coordinate node output availability across the graph.
///
/// Each node gets a Sink that completes when the node produces outputs.
/// Downstream nodes waiting on DATA edges can await these sinks.
@Getter
public final class ReactiveScriptContext implements NodeRuntime {
  private final InstanceManager instance;
  private final Scheduler reactorScheduler;
  private final ScriptEventListener eventListener;

  /// Each node gets a Sink that completes when the node produces outputs.
  /// Key: nodeId, Value: Sink that will emit the node's outputs.
  private final ConcurrentHashMap<String, Sinks.One<Map<String, NodeValue>>> nodeOutputSinks;

  /// For cancellation via Disposable.
  private volatile Disposable execution;
  private volatile boolean cancelled;

  /// Creates a new reactive script context.
  ///
  /// @param instance      the SoulFire instance
  /// @param eventListener listener for script execution events
  public ReactiveScriptContext(InstanceManager instance, ScriptEventListener eventListener) {
    this.instance = instance;
    this.reactorScheduler = new SoulFireReactorScheduler(instance.scheduler());
    this.eventListener = eventListener;
    this.nodeOutputSinks = new ConcurrentHashMap<>();
  }

  @Override
  public SoulFireScheduler scheduler() {
    return instance.scheduler();
  }

  @Override
  public void addPendingOperation(Future<?> future) {
    // For reactive execution, we track via Disposable instead
    // This method exists for backward compatibility with CompletableFuture-based nodes
  }

  @Override
  public void log(String level, String message) {
    eventListener.onLog(level, message);
  }

  /// Gets the Reactor scheduler for reactive operations.
  ///
  /// @return the Reactor scheduler
  public Scheduler getReactorScheduler() {
    return reactorScheduler;
  }

  /// Waits for a node to produce outputs.
  /// Used by downstream nodes waiting on DATA edges.
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

  /// Gets outputs if already available (non-blocking).
  /// Returns empty map if the node hasn't completed yet.
  ///
  /// @param nodeId the node identifier
  /// @return the outputs, or empty map if not available
  public Map<String, NodeValue> getNodeOutputs(String nodeId) {
    var sink = nodeOutputSinks.get(nodeId);
    if (sink == null) {
      return Map.of();
    }
    // Try to get the value if already emitted (non-blocking)
    try {
      var result = sink.asMono().block(Duration.ZERO);
      return result != null ? result : Map.of();
    } catch (Exception _) {
      return Map.of();
    }
  }

  /// Publishes node outputs, completing the Sink for that node.
  /// This unblocks any downstream nodes waiting on awaitNodeOutputs().
  ///
  /// @param nodeId  the node identifier
  /// @param outputs the output values
  public void publishNodeOutputs(String nodeId, Map<String, NodeValue> outputs) {
    nodeOutputSinks
      .computeIfAbsent(nodeId, _ -> Sinks.one())
      .tryEmitValue(outputs);
  }

  /// Checks if execution has been cancelled.
  ///
  /// @return true if cancelled
  public boolean isCancelled() {
    return cancelled;
  }

  /// Cancels the script execution.
  public void cancel() {
    this.cancelled = true;
    if (execution != null) {
      execution.dispose();
    }
    eventListener.onScriptCancelled();
  }

  /// Sets the main execution disposable for cancellation.
  ///
  /// @param execution the disposable to set
  public void setExecution(Disposable execution) {
    this.execution = execution;
  }

  /// Resets context for a new execution (clears previous outputs).
  public void reset() {
    nodeOutputSinks.clear();
    cancelled = false;
  }
}
