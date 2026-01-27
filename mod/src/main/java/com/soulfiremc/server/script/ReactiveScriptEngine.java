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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/// Reactive execution engine for visual scripts.
/// Uses Project Reactor for proper fan-in synchronization and parallel branch execution.
///
/// Key improvements over the legacy engine:
/// - Fan-in: Waits for all upstream DATA edges before executing a node
/// - Parallelism: Independent branches execute concurrently via Flux.flatMap
/// - Backpressure: Configurable concurrency limits prevent resource exhaustion
/// - Cancellation: Proper cleanup via Disposable
@Slf4j
public final class ReactiveScriptEngine {
  /// Maximum number of parallel branches to execute simultaneously.
  private static final int MAX_PARALLEL_BRANCHES = 8;

  /// Extracts the simple name from a port ID.
  /// Port IDs have format "type-name" (e.g., "vector3-position" -> "position").
  /// Execution ports use "exec-in"/"exec-out" which return "in"/"out".
  private static String extractPortName(String portId) {
    var dashIndex = portId.indexOf('-');
    return dashIndex >= 0 ? portId.substring(dashIndex + 1) : portId;
  }

  public ReactiveScriptEngine() {
    log.info("ReactiveScriptEngine initialized with {} registered node types", NodeRegistry.getRegisteredCount());
  }

  /// Executes a script starting from a specific trigger node.
  /// Used by ScriptTriggerService when an event fires.
  ///
  /// @param graph         the script graph
  /// @param triggerNodeId the trigger node to start from
  /// @param context       the reactive execution context
  /// @param eventInputs   inputs from the triggering event
  /// @return a Mono that completes when execution finishes
  public Mono<Void> executeFromTrigger(
    ScriptGraph graph,
    String triggerNodeId,
    ReactiveScriptContext context,
    Map<String, NodeValue> eventInputs
  ) {
    return Mono.defer(() -> {
      if (context.isCancelled()) {
        return Mono.empty();
      }

      var graphNode = graph.getNode(triggerNodeId);
      if (graphNode == null) {
        log.warn("Trigger node {} not found in graph", triggerNodeId);
        return Mono.empty();
      }

      if (!NodeRegistry.isRegistered(graphNode.type())) {
        log.warn("No implementation found for trigger node type: {}", graphNode.type());
        context.eventListener().onNodeError(triggerNodeId, "Unknown node type: " + graphNode.type());
        return Mono.empty();
      }

      var nodeImpl = NodeRegistry.create(graphNode.type());

      // Build inputs: defaults + graph defaults + event inputs
      var inputs = new HashMap<>(nodeImpl.getDefaultInputs());
      if (graphNode.defaultInputs() != null) {
        for (var entry : graphNode.defaultInputs().entrySet()) {
          inputs.put(entry.getKey(), NodeValue.of(entry.getValue()));
        }
      }
      inputs.putAll(eventInputs);

      context.eventListener().onNodeStarted(triggerNodeId);

      return nodeImpl.executeReactive(context, inputs)
        .doOnNext(outputs -> {
          context.publishNodeOutputs(triggerNodeId, outputs);
          context.eventListener().onNodeCompleted(triggerNodeId, outputs);
        })
        .doOnError(e -> {
          var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
          log.error("Error executing trigger node {}: {}", triggerNodeId, message, e);
          context.eventListener().onNodeError(triggerNodeId, message);
        })
        .onErrorResume(e -> Mono.just(Map.of()))
        .flatMap(outputs -> executeDownstream(graph, triggerNodeId, "exec-out", context));
    }).subscribeOn(context.getReactorScheduler());
  }

  /// Executes all nodes connected to an execution output handle.
  /// Uses Flux.flatMap for parallel execution with bounded concurrency.
  ///
  /// @param graph        the script graph
  /// @param fromNodeId   the source node ID
  /// @param outputHandle the execution output handle to follow
  /// @param context      the execution context
  /// @return a Mono that completes when all downstream nodes finish
  private Mono<Void> executeDownstream(
    ScriptGraph graph,
    String fromNodeId,
    String outputHandle,
    ReactiveScriptContext context
  ) {
    var nextNodes = graph.getNextExecutionNodes(fromNodeId, outputHandle);
    if (nextNodes.isEmpty()) {
      return Mono.empty();
    }

    // Execute downstream nodes in parallel (respecting max concurrency)
    return Flux.fromIterable(nextNodes)
      .flatMap(
        nodeId -> executeNodeWithDependencies(graph, nodeId, context),
        MAX_PARALLEL_BRANCHES
      )
      .then();
  }

  /// Executes a node after waiting for all its DATA dependencies.
  /// This is the key improvement: proper fan-in synchronization.
  ///
  /// @param graph   the script graph
  /// @param nodeId  the node to execute
  /// @param context the execution context
  /// @return a Mono that completes when the node and its downstream finish
  private Mono<Void> executeNodeWithDependencies(
    ScriptGraph graph,
    String nodeId,
    ReactiveScriptContext context
  ) {
    if (context.isCancelled()) {
      return Mono.empty();
    }

    var graphNode = graph.getNode(nodeId);
    if (graphNode == null) {
      return Mono.empty();
    }

    if (!NodeRegistry.isRegistered(graphNode.type())) {
      context.eventListener().onNodeError(nodeId, "Unknown node type: " + graphNode.type());
      return Mono.empty();
    }

    var nodeImpl = NodeRegistry.create(graphNode.type());

    // Collect all DATA edges targeting this node
    var incomingDataEdges = graph.getIncomingDataEdges(nodeId);

    // Build the base inputs from node defaults
    var baseInputs = new HashMap<>(nodeImpl.getDefaultInputs());
    if (graphNode.defaultInputs() != null) {
      for (var entry : graphNode.defaultInputs().entrySet()) {
        baseInputs.put(entry.getKey(), NodeValue.of(entry.getValue()));
      }
    }

    // If no DATA edges, execute immediately with base inputs
    if (incomingDataEdges.isEmpty()) {
      return executeNode(nodeImpl, nodeId, baseInputs, graph, context);
    }

    // Wait for all upstream nodes to complete and merge their outputs
    // Port IDs have format "type-name" but nodes store outputs/lookup inputs by simple name
    var upstreamMonos = incomingDataEdges.stream()
      .map(edge -> context.awaitNodeOutputs(edge.sourceNodeId())
        .map(outputs -> {
          var sourceKey = extractPortName(edge.sourceHandle());
          var targetKey = extractPortName(edge.targetHandle());
          var value = outputs.get(sourceKey);
          return value != null ? Map.entry(targetKey, value) : null;
        })
        .filter(entry -> entry != null))
      .toList();

    return Flux.merge(upstreamMonos)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b))
      .map(resolvedInputs -> {
        var merged = new HashMap<>(baseInputs);
        merged.putAll(resolvedInputs);
        return merged;
      })
      .flatMap(inputs -> executeNode(nodeImpl, nodeId, inputs, graph, context));
  }

  /// Executes a single node with resolved inputs.
  ///
  /// @param nodeImpl the node implementation
  /// @param nodeId   the node identifier
  /// @param inputs   the resolved inputs
  /// @param graph    the script graph
  /// @param context  the execution context
  /// @return a Mono that completes when the node and its downstream finish
  private Mono<Void> executeNode(
    ScriptNode nodeImpl,
    String nodeId,
    Map<String, NodeValue> inputs,
    ScriptGraph graph,
    ReactiveScriptContext context
  ) {
    context.eventListener().onNodeStarted(nodeId);

    return nodeImpl.executeReactive(context, inputs)
      .doOnNext(outputs -> {
        context.publishNodeOutputs(nodeId, outputs);
        context.eventListener().onNodeCompleted(nodeId, outputs);
      })
      .doOnError(e -> {
        var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.error("Error executing node {}: {}", nodeId, message, e);
        context.eventListener().onNodeError(nodeId, message);
      })
      .onErrorResume(e -> Mono.just(Map.of()))
      .flatMap(outputs -> executeDownstream(graph, nodeId, "exec-out", context));
  }

  /// Executes the full graph (for testing/one-shot execution).
  ///
  /// @param graph         the script graph to execute
  /// @param instance      the SoulFire instance
  /// @param eventListener listener for execution events
  /// @return a Mono that completes when the script finishes
  public Mono<Void> execute(
    ScriptGraph graph,
    InstanceManager instance,
    ScriptEventListener eventListener
  ) {
    var context = new ReactiveScriptContext(instance, eventListener);

    var triggers = graph.findTriggerNodes();
    if (triggers.isEmpty()) {
      log.warn("Script {} has no trigger nodes", graph.scriptName());
      eventListener.onScriptCompleted(false);
      return Mono.empty();
    }

    // Validate no cycles
    try {
      graph.topologicalSort();
    } catch (IllegalStateException e) {
      log.error("Script {} contains cycles: {}", graph.scriptName(), e.getMessage());
      eventListener.onScriptCompleted(false);
      return Mono.empty();
    }

    // Execute all triggers in parallel
    return Flux.fromIterable(triggers)
      .flatMap(triggerId -> executeFromTrigger(graph, triggerId, context, Map.of()))
      .then()
      .doOnSuccess(v -> eventListener.onScriptCompleted(true))
      .doOnError(e -> eventListener.onScriptCompleted(false));
  }

  /// Gets a registered node by type.
  ///
  /// @param type the node type identifier
  /// @return the node implementation, or null if not found
  public ScriptNode getNode(String type) {
    return NodeRegistry.isRegistered(type) ? NodeRegistry.create(type) : null;
  }

  /// Returns all registered node types.
  ///
  /// @return list of registered node type identifiers
  public List<String> getRegisteredNodeTypes() {
    return List.copyOf(NodeRegistry.getRegisteredTypes());
  }
}
