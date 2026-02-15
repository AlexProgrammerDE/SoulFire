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
/// Key features:
/// - Fan-in: Waits for all upstream DATA edges before executing a node
/// - Parallelism: Independent branches execute concurrently via Flux.flatMap
/// - Dynamic routing: Follows exec output ports whose IDs appear in node outputs
/// - Self-driving loops: Loop nodes call back into the engine via NodeRuntime.executeDownstream
/// - Error routing: Nodes with exec_error ports route errors; others stop the branch
/// - Muted bypass: Muted nodes pass execution context through unchanged
/// - Per-invocation isolation: Each trigger creates its own ExecutionRun
@Slf4j
public final class ReactiveScriptEngine {
  /// Maximum number of parallel branches to execute simultaneously.
  private static final int MAX_PARALLEL_BRANCHES = 8;

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
    return executeFromTriggerInternal(graph, triggerNodeId, context, eventInputs, false);
  }

  /// Executes a script starting from a specific trigger node, synchronously on the calling thread.
  /// Used by ScriptTriggerService for tick-based triggers that need to execute
  /// on the tick thread without scheduler switching.
  ///
  /// @param graph         the script graph
  /// @param triggerNodeId the trigger node to start from
  /// @param context       the reactive execution context
  /// @param eventInputs   inputs from the triggering event
  /// @return a Mono that completes when execution finishes (no scheduler switching)
  public Mono<Void> executeFromTriggerSync(
    ScriptGraph graph,
    String triggerNodeId,
    ReactiveScriptContext context,
    Map<String, NodeValue> eventInputs
  ) {
    return executeFromTriggerInternal(graph, triggerNodeId, context, eventInputs, true);
  }

  /// Internal implementation for trigger execution.
  ///
  /// @param graph             the script graph
  /// @param triggerNodeId     the trigger node to start from
  /// @param context           the reactive execution context
  /// @param eventInputs       inputs from the triggering event
  /// @param tickSynchronous   if true, executes on the calling thread (tick thread) without switching schedulers
  /// @return a Mono that completes when execution finishes
  private Mono<Void> executeFromTriggerInternal(
    ScriptGraph graph,
    String triggerNodeId,
    ReactiveScriptContext context,
    Map<String, NodeValue> eventInputs,
    boolean tickSynchronous
  ) {
    var mono = Mono.<Void>defer(() -> {
      if (context.isCancelled()) {
        return Mono.empty();
      }

      var run = new ExecutionRun(tickSynchronous);

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
      var metadata = NodeRegistry.getMetadata(graphNode.type());

      // Build inputs: defaults + graph defaults + event inputs
      var inputs = new HashMap<>(NodeRegistry.computeDefaultInputs(metadata));
      if (graphNode.defaultInputs() != null) {
        for (var entry : graphNode.defaultInputs().entrySet()) {
          inputs.put(entry.getKey(), NodeValue.of(entry.getValue()));
        }
      }
      inputs.putAll(eventInputs);

      context.eventListener().onNodeStarted(triggerNodeId);

      var nodeRuntime = createNodeRuntime(graph, triggerNodeId, context, run, ExecutionContext.empty());

      return nodeImpl.executeReactive(nodeRuntime, inputs)
        .doOnNext(outputs -> {
          run.publishNodeOutputs(triggerNodeId, outputs);
          context.eventListener().onNodeCompleted(triggerNodeId, outputs);
        })
        .doOnError(e -> {
          var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
          log.error("Error executing trigger node {}: {}", triggerNodeId, message, e);
          context.eventListener().onNodeError(triggerNodeId, message);
        })
        .onErrorResume(_ -> Mono.empty())
        .flatMap(outputs -> {
          var execPortIds = metadata.outputs().stream()
            .filter(p -> p.type() == PortType.EXEC)
            .map(PortDefinition::id)
            .collect(Collectors.toSet());
          var contextOutputs = new HashMap<>(outputs);
          execPortIds.forEach(contextOutputs::remove);
          var execContext = ExecutionContext.from(contextOutputs);
          return followExecOutputs(graph, triggerNodeId, metadata, outputs, context, run, execContext);
        });
    });

    // For synchronous tick execution, don't switch schedulers — run on the calling thread
    return tickSynchronous ? mono : mono.subscribeOn(context.getReactorScheduler());
  }

  /// Determines which exec output ports to follow based on node outputs,
  /// then executes downstream for each active port.
  ///
  /// Convention: if any exec output port IDs appear as keys in the outputs map,
  /// follow those. Otherwise fall back to "out".
  private Mono<Void> followExecOutputs(
    ScriptGraph graph,
    String nodeId,
    NodeMetadata metadata,
    Map<String, NodeValue> outputs,
    ReactiveScriptContext context,
    ExecutionRun run,
    ExecutionContext execContext
  ) {
    var execPorts = metadata.outputs().stream()
      .filter(p -> p.type() == PortType.EXEC)
      .map(PortDefinition::id)
      .toList();

    if (execPorts.isEmpty()) {
      // Node has no exec output ports — terminal node
      return Mono.empty();
    }

    var active = execPorts.stream()
      .filter(outputs::containsKey)
      .toList();

    if (active.isEmpty()) {
      if (execPorts.size() == 1 && StandardPorts.EXEC_OUT.equals(execPorts.getFirst())) {
        // Single "out" port — always follow it
        active = List.of(StandardPorts.EXEC_OUT);
      } else {
        // Multiple exec ports but none matched — self-driving node already handled it
        return Mono.empty();
      }
    }

    return Flux.fromIterable(active)
      .flatMap(handle -> executeDownstream(graph, nodeId, handle, context, run, execContext), MAX_PARALLEL_BRANCHES)
      .then();
  }

  /// Executes all nodes connected to an execution output handle.
  /// Uses Flux.flatMap for parallel execution with bounded concurrency.
  ///
  /// @param graph        the script graph
  /// @param fromNodeId   the source node ID
  /// @param outputHandle the execution output handle to follow
  /// @param context      the script execution context
  /// @param run          the per-invocation execution run
  /// @param execContext  the accumulated execution context flowing along execution edges
  /// @return a Mono that completes when all downstream nodes finish
  private Mono<Void> executeDownstream(
    ScriptGraph graph,
    String fromNodeId,
    String outputHandle,
    ReactiveScriptContext context,
    ExecutionRun run,
    ExecutionContext execContext
  ) {
    var nextNodes = graph.getNextExecutionNodes(fromNodeId, outputHandle);
    if (nextNodes.isEmpty()) {
      return Mono.empty();
    }

    return Flux.fromIterable(nextNodes)
      .flatMap(
        nodeId -> executeNodeWithDependencies(graph, nodeId, context, run, execContext),
        MAX_PARALLEL_BRANCHES
      )
      .then();
  }

  /// Executes a node after waiting for all its DATA dependencies.
  /// This is the key improvement: proper fan-in synchronization.
  ///
  /// Input resolution priority (highest to lowest):
  /// 1. Explicit DATA wire values
  /// 2. Execution context values (upstream outputs flowing along execution edges)
  /// 3. Graph-level default inputs
  /// 4. Node metadata default inputs
  private Mono<Void> executeNodeWithDependencies(
    ScriptGraph graph,
    String nodeId,
    ReactiveScriptContext context,
    ExecutionRun run,
    ExecutionContext execContext
  ) {
    if (context.isCancelled()) {
      return Mono.empty();
    }

    if (!run.incrementAndCheckLimit()) {
      log.error("Execution limit exceeded for node {}", nodeId);
      context.eventListener().onNodeError(nodeId, "Execution limit exceeded");
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
    var metadata = NodeRegistry.getMetadata(graphNode.type());

    // Muted node bypass — propagate execution context as outputs so DATA edges pass through
    if (graphNode.muted()) {
      run.publishNodeOutputs(nodeId, execContext.values());
      context.eventListener().onNodeCompleted(nodeId, Map.of());
      return executeDownstream(graph, nodeId, StandardPorts.EXEC_OUT, context, run, execContext);
    }

    // Collect all DATA edges targeting this node
    var incomingDataEdges = graph.getIncomingDataEdges(nodeId);

    // Build the base inputs: node defaults < graph defaults < execution context
    var baseInputs = new HashMap<>(NodeRegistry.computeDefaultInputs(metadata));
    if (graphNode.defaultInputs() != null) {
      for (var entry : graphNode.defaultInputs().entrySet()) {
        baseInputs.put(entry.getKey(), NodeValue.of(entry.getValue()));
      }
    }
    baseInputs.putAll(execContext.values());

    if (incomingDataEdges.isEmpty()) {
      return executeNode(nodeImpl, metadata, nodeId, baseInputs, graph, context, run, execContext);
    }

    var upstreamMonos = incomingDataEdges.stream()
      .map(edge -> run.awaitNodeOutputs(edge.sourceNodeId())
        .<Map.Entry<String, NodeValue>>handle((outputs, sink) -> {
          var sourceKey = edge.sourceHandle();
          var targetKey = edge.targetHandle();
          var value = outputs.get(sourceKey);
          if (value != null) {
            sink.next(Map.entry(targetKey, value));
          }
        }))
      .toList();

    return Flux.merge(upstreamMonos)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, b) -> b))
      .map(resolvedInputs -> {
        var merged = new HashMap<>(baseInputs);
        merged.putAll(resolvedInputs);
        return merged;
      })
      .flatMap(inputs -> executeNode(nodeImpl, metadata, nodeId, inputs, graph, context, run, execContext));
  }

  /// Executes a single node with resolved inputs.
  /// After execution, follows dynamic exec output routing.
  /// Error handling: nodes with exec_error ports route errors; others stop the branch.
  private Mono<Void> executeNode(
    ScriptNode nodeImpl,
    NodeMetadata metadata,
    String nodeId,
    Map<String, NodeValue> inputs,
    ScriptGraph graph,
    ReactiveScriptContext context,
    ExecutionRun run,
    ExecutionContext execContext
  ) {
    context.eventListener().onNodeStarted(nodeId);

    var nodeRuntime = createNodeRuntime(graph, nodeId, context, run, execContext);

    return nodeImpl.executeReactive(nodeRuntime, inputs)
      .doOnNext(outputs -> {
        run.publishNodeOutputs(nodeId, outputs);
        context.eventListener().onNodeCompleted(nodeId, outputs);
      })
      .doOnError(e -> {
        var message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        log.error("Error executing node {}: {}", nodeId, message, e);
        context.eventListener().onNodeError(nodeId, message);
      })
      .onErrorResume(e -> {
        var hasErrorPort = metadata.outputs().stream()
          .anyMatch(p -> p.type() == PortType.EXEC && StandardPorts.EXEC_ERROR.equals(p.id()));
        if (hasErrorPort) {
          var msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
          return Mono.just(Map.of(
            StandardPorts.EXEC_ERROR, NodeValue.ofBoolean(true),
            "success", NodeValue.ofBoolean(false),
            "errorMessage", NodeValue.ofString(msg != null ? msg : e.getClass().getSimpleName())
          ));
        }
        // No error port — stop this branch
        return Mono.empty();
      })
      .flatMap(outputs -> {
        var execPortIds = metadata.outputs().stream()
          .filter(p -> p.type() == PortType.EXEC)
          .map(PortDefinition::id)
          .collect(Collectors.toSet());
        var contextOutputs = new HashMap<>(outputs);
        execPortIds.forEach(contextOutputs::remove);
        var newExecContext = execContext.mergeWith(contextOutputs);
        return followExecOutputs(graph, nodeId, metadata, outputs, context, run, newExecContext);
      });
  }

  /// Creates a NodeRuntime wrapper that captures the current execution state.
  /// Self-driving nodes (loops, sequences) use runtime.executeDownstream() to
  /// call back into the engine.
  private NodeRuntime createNodeRuntime(
    ScriptGraph graph,
    String nodeId,
    ReactiveScriptContext context,
    ExecutionRun run,
    ExecutionContext execContext
  ) {
    return new NodeRuntime() {
      @Override
      public ScriptStateStore stateStore() {
        return context.stateStore();
      }

      @Override
      public InstanceManager instance() {
        return context.instance();
      }

      @Override
      public SoulFireScheduler scheduler() {
        return context.scheduler();
      }

      @Override
      public boolean isTickSynchronous() {
        return run.isTickSynchronous();
      }

      @Override
      public void log(String level, String message) {
        context.log(level, message);
      }

      @Override
      public Mono<Void> executeDownstream(String handle, Map<String, NodeValue> outputs) {
        var newCtx = execContext.mergeWith(outputs);
        return ReactiveScriptEngine.this.executeDownstream(graph, nodeId, handle, context, run, newCtx);
      }
    };
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
      .doOnSuccess(_ -> eventListener.onScriptCompleted(true))
      .doOnError(_ -> eventListener.onScriptCompleted(false));
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
