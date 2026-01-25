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

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/// Represents the graph structure of a visual script.
/// Contains nodes and edges that define the execution flow and data connections.
@Getter
public final class ScriptGraph {
  private final String scriptId;
  private final String scriptName;
  private final Map<String, GraphNode> nodes;
  private final List<GraphEdge> edges;
  private final Map<String, List<GraphEdge>> outgoingExecutionEdges;
  private final Map<String, List<GraphEdge>> incomingDataEdges;

  private ScriptGraph(
    String scriptId,
    String scriptName,
    Map<String, GraphNode> nodes,
    List<GraphEdge> edges
  ) {
    this.scriptId = scriptId;
    this.scriptName = scriptName;
    this.nodes = Collections.unmodifiableMap(nodes);
    this.edges = Collections.unmodifiableList(edges);

    // Pre-compute edge lookups for efficient traversal
    var outgoing = new HashMap<String, List<GraphEdge>>();
    var incoming = new HashMap<String, List<GraphEdge>>();

    for (var edge : edges) {
      var sourceKey = edge.sourceNodeId + ":" + edge.sourceHandle;
      outgoing.computeIfAbsent(sourceKey, _ -> new ArrayList<>()).add(edge);

      var targetKey = edge.targetNodeId + ":" + edge.targetHandle;
      incoming.computeIfAbsent(targetKey, _ -> new ArrayList<>()).add(edge);
    }

    this.outgoingExecutionEdges = Collections.unmodifiableMap(outgoing);
    this.incomingDataEdges = Collections.unmodifiableMap(incoming);
  }

  /// Creates a new builder for constructing a ScriptGraph.
  public static Builder builder(String scriptId, String scriptName) {
    return new Builder(scriptId, scriptName);
  }

  /// Gets a node by its ID.
  ///
  /// @param nodeId the node identifier
  /// @return the node, or null if not found
  @Nullable
  public GraphNode getNode(String nodeId) {
    return nodes.get(nodeId);
  }

  /// Finds all trigger nodes (entry points) in the graph.
  /// Trigger nodes are nodes with no incoming execution edges.
  ///
  /// @return list of trigger node IDs
  public List<String> findTriggerNodes() {
    var nodesWithIncomingExecution = new HashSet<String>();
    for (var edge : edges) {
      if (edge.edgeType == EdgeType.EXECUTION) {
        nodesWithIncomingExecution.add(edge.targetNodeId);
      }
    }

    var triggers = new ArrayList<String>();
    for (var node : nodes.values()) {
      if (!nodesWithIncomingExecution.contains(node.id)) {
        triggers.add(node.id);
      }
    }
    return triggers;
  }

  /// Gets the next execution nodes from a given node's output handle.
  ///
  /// @param nodeId       the source node ID
  /// @param outputHandle the execution output handle name
  /// @return list of target node IDs to execute next
  public List<String> getNextExecutionNodes(String nodeId, String outputHandle) {
    var key = nodeId + ":" + outputHandle;
    var edges = outgoingExecutionEdges.get(key);
    if (edges == null) {
      return List.of();
    }

    return edges.stream()
      .filter(e -> e.edgeType == EdgeType.EXECUTION)
      .map(GraphEdge::targetNodeId)
      .toList();
  }

  /// Resolves input values for a node by following data edges.
  /// Returns a map of input names to their values from connected nodes.
  ///
  /// @param nodeId  the node to resolve inputs for
  /// @param context the execution context containing node outputs
  /// @return map of input handle names to resolved values
  public Map<String, Object> resolveInputs(String nodeId, ScriptContext context) {
    var inputs = new HashMap<String, Object>();

    // First, apply default values from the node
    var node = nodes.get(nodeId);
    if (node != null && node.defaultInputs != null) {
      inputs.putAll(node.defaultInputs);
    }

    // Then, override with values from connected data edges
    for (var edge : edges) {
      if (edge.edgeType == EdgeType.DATA && edge.targetNodeId.equals(nodeId)) {
        var sourceOutputs = context.getNodeOutputs(edge.sourceNodeId);
        var value = sourceOutputs.get(edge.sourceHandle);
        if (value != null) {
          inputs.put(edge.targetHandle, value);
        }
      }
    }

    return inputs;
  }

  /// Performs topological sort on the graph nodes.
  /// Returns nodes in execution order, respecting dependencies.
  ///
  /// @return list of node IDs in topological order
  /// @throws IllegalStateException if the graph contains cycles
  public List<String> topologicalSort() {
    var inDegree = new HashMap<String, Integer>();
    var adjList = new HashMap<String, List<String>>();

    // Initialize
    for (var nodeId : nodes.keySet()) {
      inDegree.put(nodeId, 0);
      adjList.put(nodeId, new ArrayList<>());
    }

    // Build adjacency list and count in-degrees
    for (var edge : edges) {
      if (edge.edgeType == EdgeType.EXECUTION) {
        adjList.get(edge.sourceNodeId).add(edge.targetNodeId);
        inDegree.merge(edge.targetNodeId, 1, Integer::sum);
      }
    }

    // Kahn's algorithm
    var queue = new ArrayDeque<String>();
    for (var entry : inDegree.entrySet()) {
      if (entry.getValue() == 0) {
        queue.add(entry.getKey());
      }
    }

    var result = new ArrayList<String>();
    while (!queue.isEmpty()) {
      var node = queue.poll();
      result.add(node);

      for (var neighbor : adjList.get(node)) {
        var newDegree = inDegree.get(neighbor) - 1;
        inDegree.put(neighbor, newDegree);
        if (newDegree == 0) {
          queue.add(neighbor);
        }
      }
    }

    if (result.size() != nodes.size()) {
      throw new IllegalStateException("Script graph contains cycles - cannot determine execution order");
    }

    return result;
  }

  /// Type of edge connection.
  public enum EdgeType {
    /// Execution flow edge - determines order of node execution.
    EXECUTION,
    /// Data flow edge - passes values between node ports.
    DATA
  }

  /// Represents a node in the graph.
  ///
  /// @param id            unique node identifier
  /// @param type          node type identifier (e.g., "action.pathfind")
  /// @param defaultInputs default values for input ports
  public record GraphNode(
    String id,
    String type,
    @Nullable Map<String, Object> defaultInputs
  ) {}

  /// Represents an edge connecting two nodes.
  ///
  /// @param sourceNodeId   the source node ID
  /// @param sourceHandle   the output handle on the source node
  /// @param targetNodeId   the target node ID
  /// @param targetHandle   the input handle on the target node
  /// @param edgeType       whether this is an execution or data edge
  public record GraphEdge(
    String sourceNodeId,
    String sourceHandle,
    String targetNodeId,
    String targetHandle,
    EdgeType edgeType
  ) {}

  /// Builder for constructing ScriptGraph instances.
  public static final class Builder {
    private final String scriptId;
    private final String scriptName;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();

    private Builder(String scriptId, String scriptName) {
      this.scriptId = scriptId;
      this.scriptName = scriptName;
    }

    /// Adds a node to the graph.
    public Builder addNode(GraphNode node) {
      nodes.put(node.id, node);
      return this;
    }

    /// Adds a node to the graph.
    public Builder addNode(String id, String type, @Nullable Map<String, Object> defaultInputs) {
      return addNode(new GraphNode(id, type, defaultInputs));
    }

    /// Adds an edge to the graph.
    public Builder addEdge(GraphEdge edge) {
      edges.add(edge);
      return this;
    }

    /// Adds an execution edge between nodes.
    public Builder addExecutionEdge(String sourceNodeId, String sourceHandle, String targetNodeId, String targetHandle) {
      return addEdge(new GraphEdge(sourceNodeId, sourceHandle, targetNodeId, targetHandle, EdgeType.EXECUTION));
    }

    /// Adds a data edge between nodes.
    public Builder addDataEdge(String sourceNodeId, String sourceHandle, String targetNodeId, String targetHandle) {
      return addEdge(new GraphEdge(sourceNodeId, sourceHandle, targetNodeId, targetHandle, EdgeType.DATA));
    }

    /// Builds the ScriptGraph.
    public ScriptGraph build() {
      return new ScriptGraph(scriptId, scriptName, nodes, edges);
    }
  }
}
