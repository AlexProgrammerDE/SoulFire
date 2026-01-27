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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/// Validates script graphs for errors before execution.
/// Checks for: multiple connections to single-input sockets, type compatibility,
/// required inputs without connections, cycles, and unknown node types.
public final class ScriptValidator {

  private ScriptValidator() {
    // Utility class
  }

  /// Validates a script graph and returns all found errors.
  ///
  /// @param graph the script graph to validate
  /// @return validation result containing errors and edges to remove
  public static ValidationResult validate(ScriptGraph graph) {
    var errors = new ArrayList<ValidationError>();
    var edgesToRemove = new ArrayList<String>();

    // 1. Check for unknown node types
    validateNodeTypes(graph, errors);

    // 2. Check connection constraints per input port
    validateConnectionConstraints(graph, errors, edgesToRemove);

    // 3. Check type compatibility
    validateTypeCompatibility(graph, errors);

    // 4. Check required inputs are connected
    validateRequiredInputs(graph, errors);

    // 5. Check for cycles
    validateNoCycles(graph, errors);

    return new ValidationResult(errors.isEmpty(), errors, edgesToRemove);
  }

  /// Validates that all node types are registered.
  private static void validateNodeTypes(ScriptGraph graph, List<ValidationError> errors) {
    for (var node : graph.nodes().values()) {
      if (!NodeRegistry.isRegistered(node.type())) {
        errors.add(new ValidationError(
          ErrorType.INVALID_NODE_TYPE,
          "Unknown node type: " + node.type(),
          null, node.id(), null
        ));
      }
    }
  }

  /// Validates connection constraints (single-connection inputs, multi-input handling).
  private static void validateConnectionConstraints(ScriptGraph graph, List<ValidationError> errors,
                                                    List<String> edgesToRemove) {
    // Group edges by target (nodeId:portId)
    var inputConnections = new HashMap<String, List<EdgeInfo>>();

    for (var edge : graph.edges()) {
      if (edge.edgeType() == ScriptGraph.EdgeType.DATA) {
        var key = edge.targetNodeId() + ":" + edge.targetHandle();
        inputConnections.computeIfAbsent(key, k -> new ArrayList<>())
          .add(new EdgeInfo(edge, getEdgeId(edge)));
      }
    }

    // Check each input port for multiple connections
    for (var entry : inputConnections.entrySet()) {
      var edges = entry.getValue();
      if (edges.size() <= 1) {
        continue; // Single connection is always OK
      }

      var parts = entry.getKey().split(":", 2);
      var nodeId = parts[0];
      var portId = parts[1];

      var node = graph.nodes().get(nodeId);
      if (node == null || !NodeRegistry.isRegistered(node.type())) {
        continue;
      }

      var metadata = NodeRegistry.create(node.type()).getMetadata();
      var port = metadata.findInput(portId);

      if (port != null && !port.multiInput() && port.maxConnections() == 1) {
        // Single-connection violation - keep the last one, mark others for removal
        // Sort by edge ID for deterministic ordering
        edges.sort(Comparator.comparing(EdgeInfo::id));

        for (int i = 0; i < edges.size() - 1; i++) {
          edgesToRemove.add(edges.get(i).id());
        }

        errors.add(new ValidationError(
          ErrorType.MULTIPLE_CONNECTIONS,
          "Multiple connections to single-input port '" + port.displayName() + "'",
          edges.get(0).id(), nodeId, portId
        ));
      }
    }
  }

  /// Validates type compatibility between connected ports.
  private static void validateTypeCompatibility(ScriptGraph graph, List<ValidationError> errors) {
    for (var edge : graph.edges()) {
      if (edge.edgeType() != ScriptGraph.EdgeType.DATA) {
        continue;
      }

      var sourceNode = graph.nodes().get(edge.sourceNodeId());
      var targetNode = graph.nodes().get(edge.targetNodeId());
      if (sourceNode == null || targetNode == null) {
        continue;
      }

      if (!NodeRegistry.isRegistered(sourceNode.type()) || !NodeRegistry.isRegistered(targetNode.type())) {
        continue;
      }

      var sourceMetadata = NodeRegistry.create(sourceNode.type()).getMetadata();
      var targetMetadata = NodeRegistry.create(targetNode.type()).getMetadata();

      var sourcePort = sourceMetadata.findOutput(edge.sourceHandle());
      var targetPort = targetMetadata.findInput(edge.targetHandle());

      if (sourcePort != null && targetPort != null) {
        if (!targetPort.type().canAccept(sourcePort.type())) {
          errors.add(new ValidationError(
            ErrorType.TYPE_INCOMPATIBLE,
            "Cannot connect " + sourcePort.type() + " to " + targetPort.type(),
            getEdgeId(edge), null, null
          ));
        }
      }
    }
  }

  /// Validates that required inputs are connected or have default values.
  private static void validateRequiredInputs(ScriptGraph graph, List<ValidationError> errors) {
    for (var node : graph.nodes().values()) {
      if (!NodeRegistry.isRegistered(node.type())) {
        continue;
      }

      var metadata = NodeRegistry.create(node.type()).getMetadata();

      for (var input : metadata.inputs()) {
        if (!input.required() || input.type() == PortType.EXEC) {
          continue;
        }

        // Check if there's a connection to this input
        var hasConnection = graph.edges().stream()
          .anyMatch(e -> e.edgeType() == ScriptGraph.EdgeType.DATA &&
                        e.targetNodeId().equals(node.id()) &&
                        e.targetHandle().equals(input.id()));

        // Check if there's a default value in node data
        var hasNodeDefault = node.defaultInputs() != null &&
                            node.defaultInputs().containsKey(extractPortName(input.id()));

        // Check if port has a default value
        var hasPortDefault = input.defaultValue() != null;

        if (!hasConnection && !hasNodeDefault && !hasPortDefault) {
          errors.add(new ValidationError(
            ErrorType.REQUIRED_UNCONNECTED,
            "Required input '" + input.displayName() + "' is not connected",
            null, node.id(), input.id()
          ));
        }
      }
    }
  }

  /// Validates that the graph contains no cycles in execution flow.
  private static void validateNoCycles(ScriptGraph graph, List<ValidationError> errors) {
    try {
      graph.topologicalSort();
    } catch (IllegalStateException e) {
      errors.add(new ValidationError(
        ErrorType.CYCLE_DETECTED,
        "Script contains cycles in execution flow",
        null, null, null
      ));
    }
  }

  /// Extracts the simple port name from a full port ID.
  /// Port IDs have format "type-name" (e.g., "number-value" -> "value").
  private static String extractPortName(String portId) {
    var dashIndex = portId.indexOf('-');
    return dashIndex >= 0 ? portId.substring(dashIndex + 1) : portId;
  }

  /// Generates a unique edge ID from an edge (for error reporting).
  private static String getEdgeId(ScriptGraph.GraphEdge edge) {
    return edge.sourceNodeId() + ":" + edge.sourceHandle() + "->" +
           edge.targetNodeId() + ":" + edge.targetHandle();
  }

  /// Types of validation errors.
  public enum ErrorType {
    /// Multiple edges connected to a single-input socket.
    MULTIPLE_CONNECTIONS,
    /// Source type cannot be converted to target type.
    TYPE_INCOMPATIBLE,
    /// Required input port has no connection and no default value.
    REQUIRED_UNCONNECTED,
    /// Graph contains cycles in execution flow.
    CYCLE_DETECTED,
    /// Unknown or unregistered node type.
    INVALID_NODE_TYPE
  }

  /// A single validation error.
  public record ValidationError(
    ErrorType type,
    String message,
    @Nullable String edgeId,
    @Nullable String nodeId,
    @Nullable String portId
  ) {}

  /// Result of script validation.
  public record ValidationResult(
    boolean valid,
    List<ValidationError> errors,
    List<String> edgesToRemove
  ) {}

  /// Internal edge info for tracking during validation.
  private record EdgeInfo(ScriptGraph.GraphEdge edge, String id) {}
}
