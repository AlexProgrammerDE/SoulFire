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
import com.soulfiremc.server.script.nodes.NodeRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Main execution engine for visual scripts.
/// Parses script graphs, resolves execution order, and executes nodes.
///
/// The engine supports both synchronous and asynchronous node execution,
/// with proper handling of async actions like pathfinding and block breaking.
@Slf4j
public final class ScriptEngine {

  /// Creates a new ScriptEngine.
  public ScriptEngine() {
    log.info("ScriptEngine initialized with {} registered node types", NodeRegistry.getRegisteredCount());
  }

  /// Gets a registered node by type.
  ///
  /// @param type the node type identifier
  /// @return the node implementation, or null if not found
  public ScriptNode getNode(String type) {
    return NodeRegistry.isRegistered(type) ? NodeRegistry.create(type) : null;
  }

  /// Executes a script graph.
  /// Note: This method is primarily used for testing or one-shot execution.
  /// For activatable scripts, use executeFromTrigger() instead.
  ///
  /// @param graph         the script graph to execute
  /// @param instance      the SoulFire instance
  /// @param eventListener listener for execution events
  /// @return a future that completes when the script finishes
  public CompletableFuture<Void> execute(
    ScriptGraph graph,
    InstanceManager instance,
    ScriptEventListener eventListener
  ) {
    var context = new ScriptContext(instance, eventListener);
    return executeGraph(graph, context);
  }

  /// Executes a script starting from a specific trigger node with event inputs.
  /// Used by ScriptTriggerService when an event fires.
  ///
  /// @param graph         the script graph
  /// @param triggerNodeId the trigger node to start from
  /// @param context       the execution context
  /// @param eventInputs   inputs from the triggering event
  /// @return a future that completes when execution finishes
  public CompletableFuture<Void> executeFromTrigger(
    ScriptGraph graph,
    String triggerNodeId,
    ScriptContext context,
    Map<String, NodeValue> eventInputs
  ) {
    return CompletableFuture.runAsync(() -> {
      if (context.isCancelled()) {
        return;
      }

      var graphNode = graph.getNode(triggerNodeId);
      if (graphNode == null) {
        log.warn("Trigger node {} not found in graph", triggerNodeId);
        return;
      }

      if (!NodeRegistry.isRegistered(graphNode.type())) {
        log.warn("No implementation found for trigger node type: {}", graphNode.type());
        context.eventListener().onNodeError(triggerNodeId, "Unknown node type: " + graphNode.type());
        return;
      }
      var nodeImpl = NodeRegistry.create(graphNode.type());

      // Merge default inputs, event inputs, and resolved inputs
      var inputs = new HashMap<>(nodeImpl.getDefaultInputs());
      if (graphNode.defaultInputs() != null) {
        for (var entry : graphNode.defaultInputs().entrySet()) {
          inputs.put(entry.getKey(), NodeValue.of(entry.getValue()));
        }
      }
      inputs.putAll(eventInputs);

      context.eventListener().onNodeStarted(triggerNodeId);

      try {
        var outputs = nodeImpl.execute(context, inputs).join();
        if (context.isCancelled()) {
          return;
        }

        context.storeNodeOutputs(triggerNodeId, outputs);
        context.eventListener().onNodeCompleted(triggerNodeId, outputs);

        // Execute downstream nodes via the execution output
        executeFromOutput(graph, triggerNodeId, StandardPorts.EXEC_OUT, context).join();
      } catch (Exception e) {
        var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.error("Error executing trigger node {}: {}", triggerNodeId, message, e);
        context.eventListener().onNodeError(triggerNodeId, message);
      }
    }, context.scheduler());
  }

  /// Executes the graph with the given context.
  private CompletableFuture<Void> executeGraph(ScriptGraph graph, ScriptContext context) {
    return CompletableFuture.runAsync(() -> {
      try {
        // Find trigger nodes (entry points)
        var triggers = graph.findTriggerNodes();
        if (triggers.isEmpty()) {
          log.warn("Script {} has no trigger nodes", graph.scriptName());
          context.eventListener().onScriptCompleted(false);
          return;
        }

        // Get execution order using topological sort
        List<String> executionOrder;
        try {
          executionOrder = graph.topologicalSort();
        } catch (IllegalStateException e) {
          log.error("Script {} contains cycles: {}", graph.scriptName(), e.getMessage());
          context.eventListener().onScriptCompleted(false);
          return;
        }

        // Execute nodes in order
        var success = executeNodes(graph, executionOrder, context).join();
        context.eventListener().onScriptCompleted(success);

      } catch (Exception e) {
        log.error("Error executing script {}", graph.scriptName(), e);
        context.eventListener().onScriptCompleted(false);
      }
    }, context.scheduler());
  }

  /// Executes nodes in the given order.
  private CompletableFuture<Boolean> executeNodes(
    ScriptGraph graph,
    List<String> executionOrder,
    ScriptContext context
  ) {
    return executeNodesRecursive(graph, executionOrder, 0, context);
  }

  /// Recursively executes nodes, handling async operations.
  private CompletableFuture<Boolean> executeNodesRecursive(
    ScriptGraph graph,
    List<String> executionOrder,
    int index,
    ScriptContext context
  ) {
    if (index >= executionOrder.size() || context.isCancelled()) {
      return CompletableFuture.completedFuture(!context.isCancelled());
    }

    var nodeId = executionOrder.get(index);
    var graphNode = graph.getNode(nodeId);
    if (graphNode == null) {
      log.warn("Node {} not found in graph", nodeId);
      return executeNodesRecursive(graph, executionOrder, index + 1, context);
    }

    if (!NodeRegistry.isRegistered(graphNode.type())) {
      log.warn("No implementation found for node type: {}", graphNode.type());
      context.eventListener().onNodeError(nodeId, "Unknown node type: " + graphNode.type());
      return executeNodesRecursive(graph, executionOrder, index + 1, context);
    }
    var nodeImpl = NodeRegistry.create(graphNode.type());

    // Resolve inputs from connected nodes
    var inputs = graph.resolveInputs(nodeId, context);

    // Merge with node's default inputs
    var mergedInputs = new HashMap<>(nodeImpl.getDefaultInputs());
    mergedInputs.putAll(inputs);

    // Execute the node
    context.eventListener().onNodeStarted(nodeId);

    return nodeImpl.execute(context, mergedInputs)
      .thenCompose(outputs -> {
        if (context.isCancelled()) {
          return CompletableFuture.completedFuture(false);
        }

        // Store outputs for downstream nodes
        context.storeNodeOutputs(nodeId, outputs);
        context.eventListener().onNodeCompleted(nodeId, outputs);

        // Continue with next node
        return executeNodesRecursive(graph, executionOrder, index + 1, context);
      })
      .exceptionally(error -> {
        var message = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
        log.error("Error executing node {}: {}", nodeId, message, error);
        context.eventListener().onNodeError(nodeId, message);
        return false;
      });
  }

  /// Executes nodes following execution edges from a specific node.
  /// This is used for conditional execution (e.g., if/else branches).
  ///
  /// @param graph        the script graph
  /// @param fromNodeId   the source node ID
  /// @param outputHandle the execution output handle to follow
  /// @param context      the execution context
  /// @return a future that completes when all downstream nodes finish
  public CompletableFuture<Void> executeFromOutput(
    ScriptGraph graph,
    String fromNodeId,
    String outputHandle,
    ScriptContext context
  ) {
    var nextNodes = graph.getNextExecutionNodes(fromNodeId, outputHandle);
    if (nextNodes.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    // Execute all connected nodes in parallel
    var futures = nextNodes.stream()
      .map(nodeId -> executeSingleNode(graph, nodeId, context))
      .toArray(CompletableFuture[]::new);

    return CompletableFuture.allOf(futures);
  }

  /// Executes a single node and its downstream nodes.
  private CompletableFuture<Void> executeSingleNode(
    ScriptGraph graph,
    String nodeId,
    ScriptContext context
  ) {
    if (context.isCancelled()) {
      return CompletableFuture.completedFuture(null);
    }

    var graphNode = graph.getNode(nodeId);
    if (graphNode == null) {
      return CompletableFuture.completedFuture(null);
    }

    if (!NodeRegistry.isRegistered(graphNode.type())) {
      context.eventListener().onNodeError(nodeId, "Unknown node type: " + graphNode.type());
      return CompletableFuture.completedFuture(null);
    }
    var nodeImpl = NodeRegistry.create(graphNode.type());

    var inputs = graph.resolveInputs(nodeId, context);
    var mergedInputs = new HashMap<>(nodeImpl.getDefaultInputs());
    mergedInputs.putAll(inputs);

    context.eventListener().onNodeStarted(nodeId);

    return nodeImpl.execute(context, mergedInputs)
      .thenCompose(outputs -> {
        context.storeNodeOutputs(nodeId, outputs);
        context.eventListener().onNodeCompleted(nodeId, outputs);

        // Follow the default execution output
        return executeFromOutput(graph, nodeId, StandardPorts.EXEC_OUT, context);
      })
      .exceptionally(error -> {
        var message = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
        context.eventListener().onNodeError(nodeId, message);
        return null;
      });
  }

  /// Returns all registered node types.
  ///
  /// @return list of registered node type identifiers
  public List<String> getRegisteredNodeTypes() {
    return List.copyOf(NodeRegistry.getRegisteredTypes());
  }
}
