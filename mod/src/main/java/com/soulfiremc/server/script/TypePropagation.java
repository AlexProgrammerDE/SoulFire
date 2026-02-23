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

import java.util.*;

/// Forward type propagation through ANY ports to detect type mismatches.
public final class TypePropagation {
  private TypePropagation() {}

  /// Analyzes type flow through ANY ports and returns diagnostics for mismatches.
  public static List<ScriptGraph.ValidationDiagnostic> analyze(
    Map<String, ScriptGraph.GraphNode> nodes,
    List<ScriptGraph.GraphEdge> edges
  ) {
    var diagnostics = new ArrayList<ScriptGraph.ValidationDiagnostic>();

    // Map from "nodeId:portId" to inferred concrete type
    var inferredTypes = new HashMap<String, PortType>();

    // Initialize known types from node metadata outputs
    for (var node : nodes.values()) {
      if (!NodeRegistry.isRegistered(node.type())) continue;
      var metadata = NodeRegistry.getMetadata(node.type());
      for (var output : metadata.outputs()) {
        if (output.type() != PortType.EXEC && output.type() != PortType.ANY) {
          inferredTypes.put(node.id() + ":" + output.id(), output.type());
        }
      }
    }

    // Filter to DATA edges only
    var dataEdges = edges.stream()
      .filter(e -> e.edgeType() == ScriptGraph.EdgeType.DATA)
      .toList();

    // Worklist BFS: propagate types through ANY ports
    var changed = true;
    var iterations = 0;
    var maxIterations = dataEdges.size() * 2 + 1; // Safety bound

    while (changed && iterations < maxIterations) {
      changed = false;
      iterations++;

      for (var edge : dataEdges) {
        var sourceKey = edge.sourceNodeId() + ":" + edge.sourceHandle();
        var targetKey = edge.targetNodeId() + ":" + edge.targetHandle();

        var sourceType = inferredTypes.get(sourceKey);
        if (sourceType == null) continue;

        // Look up target port's declared type
        var targetNode = nodes.get(edge.targetNodeId());
        if (targetNode == null || !NodeRegistry.isRegistered(targetNode.type())) continue;
        var targetMeta = NodeRegistry.getMetadata(targetNode.type());

        var targetPort = targetMeta.inputs().stream()
          .filter(p -> p.id().equals(edge.targetHandle())).findFirst();
        if (targetPort.isEmpty()) continue;

        var declaredTargetType = targetPort.get().type();

        if (declaredTargetType == PortType.ANY) {
          // Infer type at this ANY input
          if (!inferredTypes.containsKey(targetKey)) {
            inferredTypes.put(targetKey, sourceType);
            changed = true;

            // Also propagate to ANY outputs of the same node
            for (var output : targetMeta.outputs()) {
              if (output.type() == PortType.ANY) {
                var outKey = edge.targetNodeId() + ":" + output.id();
                if (!inferredTypes.containsKey(outKey)) {
                  inferredTypes.put(outKey, sourceType);
                  changed = true;
                }
              }
            }
          }
        } else if (declaredTargetType != PortType.EXEC && declaredTargetType != PortType.STRING) {
          // Check compatibility: concrete source type reaching a typed target
          if (!TypeCompatibility.isCompatible(sourceType, declaredTargetType)) {
            diagnostics.add(new ScriptGraph.ValidationDiagnostic(
              edge.targetNodeId(),
              edge.sourceNodeId() + ":" + edge.sourceHandle() + "->" + edge.targetNodeId() + ":" + edge.targetHandle(),
              "Type narrowing: inferred " + sourceType + " flowing through ANY reaches " + declaredTargetType + " port '" + edge.targetHandle() + "'",
              ScriptGraph.Severity.WARNING
            ));
          }
        }
      }
    }

    return diagnostics;
  }
}
