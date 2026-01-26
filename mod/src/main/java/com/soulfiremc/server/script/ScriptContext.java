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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/// Execution context for scripts.
/// Implements NodeRuntime to provide the minimal API surface for nodes.
/// Additional methods (output storage, cancellation) are for engine use only.
///
/// This class is thread-safe for use in async node execution.
/// Scripts run at instance level. Bot-specific operations receive the bot as an
/// explicit input parameter - nodes should be stateless and pure.
@Getter
public final class ScriptContext implements NodeRuntime {
  private final InstanceManager instance;
  private final Map<String, Map<String, NodeValue>> nodeOutputs;
  private final ScriptEventListener eventListener;
  private final Set<Future<?>> pendingOperations;
  private volatile boolean cancelled;

  /// Creates a new script context.
  ///
  /// @param instance      the SoulFire instance
  /// @param eventListener listener for script execution events
  public ScriptContext(InstanceManager instance, ScriptEventListener eventListener) {
    this.instance = instance;
    this.nodeOutputs = new ConcurrentHashMap<>();
    this.eventListener = eventListener;
    this.pendingOperations = ConcurrentHashMap.newKeySet();
  }

  @Override
  public SoulFireScheduler scheduler() {
    return instance.scheduler();
  }

  @Override
  public void addPendingOperation(Future<?> future) {
    pendingOperations.add(future);
    // Auto-remove when complete
    if (future instanceof CompletableFuture<?> cf) {
      cf.whenComplete((_, _) -> pendingOperations.remove(future));
    }
  }

  public void log(String level, String message) {
    eventListener.onLog(level, message);
  }

  /// Cancels all pending async operations.
  public void cancelPendingOperations() {
    for (var future : pendingOperations) {
      future.cancel(true);
    }
    pendingOperations.clear();
  }

  /// Stores the outputs of a node for later retrieval by connected nodes.
  ///
  /// @param nodeId  the node identifier
  /// @param outputs the output values
  public void storeNodeOutputs(String nodeId, Map<String, NodeValue> outputs) {
    nodeOutputs.put(nodeId, new ConcurrentHashMap<>(outputs));
  }

  /// Gets the outputs from a previously executed node.
  ///
  /// @param nodeId the node identifier
  /// @return the outputs, or empty map if not found
  public Map<String, NodeValue> getNodeOutputs(String nodeId) {
    return nodeOutputs.getOrDefault(nodeId, Map.of());
  }

  /// Gets a specific output value from a previously executed node.
  ///
  /// @param nodeId     the node identifier
  /// @param outputName the name of the output port
  /// @return the output value wrapped in Optional
  public Optional<NodeValue> getNodeOutput(String nodeId, String outputName) {
    var outputs = nodeOutputs.get(nodeId);
    if (outputs == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(outputs.get(outputName));
  }

  /// Checks if execution has been cancelled.
  ///
  /// @return true if cancelled
  public boolean isCancelled() {
    return cancelled;
  }

  /// Cancels the script execution and all pending operations.
  public void cancel() {
    this.cancelled = true;
    cancelPendingOperations();
    eventListener.onScriptCancelled();
  }
}
