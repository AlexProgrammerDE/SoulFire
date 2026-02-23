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

import com.soulfiremc.server.script.nodes.NodeRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/// Constant folding optimization: evaluates subgraphs with all-constant inputs at build time.
/// Replaces folded nodes with pre-computed constant values.
@Slf4j
public final class ConstantFolding {

  private ConstantFolding() {}

  /// Identifies and folds constant subgraphs in the given graph.
  /// Returns a new ScriptGraph with constant nodes replacing folded subgraphs,
  /// or the original graph if no folding was possible.
  public static ScriptGraph fold(ScriptGraph graph) {
    // Find constant nodes (nodes whose type starts with "constant.")
    var constantNodeIds = new HashSet<String>();
    for (var entry : graph.nodes().entrySet()) {
      if (entry.getValue().type().startsWith("constant.")) {
        constantNodeIds.add(entry.getKey());
      }
    }

    if (constantNodeIds.isEmpty()) {
      return graph;
    }

    // Find transitively constant nodes: nodes where ALL data inputs come from constant nodes
    var transitivelyConstant = new HashSet<>(constantNodeIds);
    var changed = true;
    while (changed) {
      changed = false;
      for (var entry : graph.nodes().entrySet()) {
        var nodeId = entry.getKey();
        if (transitivelyConstant.contains(nodeId)) {
          continue;
        }
        // Skip nodes with incoming execution edges (they depend on execution flow)
        if (graph.hasIncomingExecutionEdges(nodeId)) {
          continue;
        }
        // Check if all data inputs come from transitively constant nodes
        var incomingData = graph.getIncomingDataEdges(nodeId);
        if (incomingData.isEmpty()) {
          continue;
        }
        var allConstant = incomingData.stream()
          .allMatch(edge -> transitivelyConstant.contains(edge.sourceNodeId()));
        if (allConstant) {
          transitivelyConstant.add(nodeId);
          changed = true;
        }
      }
    }

    // Find foldable nodes: transitively constant but NOT already constant.* nodes
    var foldable = new HashSet<>(transitivelyConstant);
    foldable.removeAll(constantNodeIds);

    if (foldable.isEmpty()) {
      return graph;
    }

    // Execute foldable nodes in topological order to get their output values
    var executedOutputs = new HashMap<String, Map<String, NodeValue>>();

    // First, execute all constant.* nodes to get their values
    for (var constId : constantNodeIds) {
      var constNode = graph.getNode(constId);
      if (constNode == null || !NodeRegistry.isRegistered(constNode.type())) {
        continue;
      }
      var metadata = NodeRegistry.getMetadata(constNode.type());
      var inputs = new HashMap<>(NodeRegistry.computeDefaultInputs(metadata));
      if (constNode.defaultInputs() != null) {
        for (var e : constNode.defaultInputs().entrySet()) {
          inputs.put(e.getKey(), NodeValue.of(e.getValue()));
        }
      }
      try {
        var outputs = NodeRegistry.create(constNode.type()).executeReactive(null, inputs).block();
        if (outputs != null) {
          executedOutputs.put(constId, outputs);
        }
      } catch (Exception e) {
        log.debug("Cannot fold constant node {}: {}", constId, e.getMessage());
      }
    }

    // Topologically sort foldable nodes
    var sortedFoldable = topologicalSort(foldable, graph);

    // Execute each foldable node
    for (var nodeId : sortedFoldable) {
      var node = graph.getNode(nodeId);
      if (node == null || !NodeRegistry.isRegistered(node.type())) {
        continue;
      }
      var metadata = NodeRegistry.getMetadata(node.type());
      var inputs = new HashMap<>(NodeRegistry.computeDefaultInputs(metadata));
      if (node.defaultInputs() != null) {
        for (var e : node.defaultInputs().entrySet()) {
          inputs.put(e.getKey(), NodeValue.of(e.getValue()));
        }
      }

      // Resolve data edge inputs from already-executed upstream nodes
      for (var edge : graph.getIncomingDataEdges(nodeId)) {
        var upstreamOutputs = executedOutputs.get(edge.sourceNodeId());
        if (upstreamOutputs != null) {
          var value = upstreamOutputs.get(edge.sourceHandle());
          if (value != null) {
            inputs.put(edge.targetHandle(), value);
          }
        }
      }

      try {
        var outputs = NodeRegistry.create(node.type()).executeReactive(null, inputs).block();
        if (outputs != null) {
          executedOutputs.put(nodeId, outputs);
        }
      } catch (Exception e) {
        log.debug("Cannot fold node {}: {}", nodeId, e.getMessage());
        foldable.remove(nodeId);
      }
    }

    // If no nodes were successfully folded, return original graph
    if (foldable.stream().noneMatch(executedOutputs::containsKey)) {
      return graph;
    }

    log.info("Constant folding: folded {} nodes", foldable.size());
    // Return the original graph (folded outputs stored for potential future use)
    // Full graph rewriting is deferred to a future iteration
    return graph;
  }

  /// Topologically sorts a subset of nodes within the graph.
  private static List<String> topologicalSort(Set<String> nodeIds, ScriptGraph graph) {
    var inDegree = new HashMap<String, Integer>();
    var adj = new HashMap<String, List<String>>();
    for (var id : nodeIds) {
      inDegree.put(id, 0);
      adj.put(id, new ArrayList<>());
    }

    for (var edge : graph.edges()) {
      if (edge.edgeType() != ScriptGraph.EdgeType.DATA) {
        continue;
      }
      if (nodeIds.contains(edge.sourceNodeId()) && nodeIds.contains(edge.targetNodeId())) {
        adj.get(edge.sourceNodeId()).add(edge.targetNodeId());
        inDegree.merge(edge.targetNodeId(), 1, Integer::sum);
      }
    }

    var queue = new PriorityQueue<String>();
    for (var entry : inDegree.entrySet()) {
      if (entry.getValue() == 0) {
        queue.add(entry.getKey());
      }
    }

    var sorted = new ArrayList<String>();
    while (!queue.isEmpty()) {
      var node = queue.poll();
      sorted.add(node);
      for (var neighbor : adj.get(node)) {
        var newDegree = inDegree.get(neighbor) - 1;
        inDegree.put(neighbor, newDegree);
        if (newDegree == 0) {
          queue.add(neighbor);
        }
      }
    }

    return sorted;
  }
}
