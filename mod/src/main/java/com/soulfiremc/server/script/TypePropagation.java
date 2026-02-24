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

/// Forward type propagation through generic and ANY ports to detect type mismatches.
/// Supports TypeDescriptor-based inference with type variable unification.
public final class TypePropagation {
  private TypePropagation() {}

  /// Analyzes type flow through ports and returns diagnostics for mismatches.
  /// Uses TypeDescriptor unification for generic type inference.
  public static List<ScriptGraph.ValidationDiagnostic> analyze(
    Map<String, ScriptGraph.GraphNode> nodes,
    List<ScriptGraph.GraphEdge> edges
  ) {
    var diagnostics = new ArrayList<ScriptGraph.ValidationDiagnostic>();
    var reportedEdges = new HashSet<String>();

    // Per-node type variable bindings (nodeId -> {varName -> resolved TypeDescriptor})
    var nodeBindings = new HashMap<String, Map<String, TypeDescriptor>>();

    // Map from "nodeId:portId" to inferred TypeDescriptor
    var inferredDescriptors = new HashMap<String, TypeDescriptor>();

    // Initialize known types from node metadata outputs (concrete types only)
    for (var node : nodes.values()) {
      if (!NodeRegistry.isRegistered(node.type())) continue;
      var metadata = NodeRegistry.getMetadata(node.type());
      for (var output : metadata.outputs()) {
        if (output.type() == PortType.EXEC) continue;
        var td = output.effectiveTypeDescriptor();
        if (!td.hasTypeVariables()) {
          inferredDescriptors.put(node.id() + ":" + output.id(), td);
        }
      }
    }

    // Filter to DATA edges only
    var dataEdges = edges.stream()
      .filter(e -> e.edgeType() == ScriptGraph.EdgeType.DATA)
      .toList();

    // Worklist BFS: propagate types through generic and ANY ports
    var changed = true;
    var iterations = 0;
    var maxIterations = dataEdges.size() * 3 + 1; // Safety bound

    while (changed && iterations < maxIterations) {
      changed = false;
      iterations++;

      for (var edge : dataEdges) {
        var sourceKey = edge.sourceNodeId() + ":" + edge.sourceHandle();
        var targetKey = edge.targetNodeId() + ":" + edge.targetHandle();

        var sourceDescriptor = inferredDescriptors.get(sourceKey);
        if (sourceDescriptor == null) continue;

        // Look up target port
        var targetNode = nodes.get(edge.targetNodeId());
        if (targetNode == null || !NodeRegistry.isRegistered(targetNode.type())) continue;
        var targetMeta = NodeRegistry.getMetadata(targetNode.type());

        var targetPort = targetMeta.inputs().stream()
          .filter(p -> p.id().equals(edge.targetHandle())).findFirst();
        if (targetPort.isEmpty()) continue;

        var targetDescriptor = targetPort.get().effectiveTypeDescriptor();

        // Get or create bindings for target node
        var bindings = nodeBindings.computeIfAbsent(edge.targetNodeId(), _ -> new HashMap<>());

        if (targetDescriptor.hasTypeVariables()) {
          // Attempt unification to resolve type variables
          var tempBindings = new HashMap<>(bindings);
          if (TypeDescriptor.unify(sourceDescriptor, targetDescriptor, tempBindings)) {
            // Check if bindings changed
            if (!tempBindings.equals(bindings)) {
              bindings.putAll(tempBindings);
              changed = true;

              // Infer the target port's resolved type
              var resolvedTarget = targetDescriptor.resolve(bindings);
              if (!resolvedTarget.hasTypeVariables()) {
                inferredDescriptors.put(targetKey, resolvedTarget);
              }

              // Propagate resolved types to outputs that share type variables
              for (var output : targetMeta.outputs()) {
                if (output.type() == PortType.EXEC) continue;
                var outputDescriptor = output.effectiveTypeDescriptor();
                if (outputDescriptor.hasTypeVariables()) {
                  var resolvedOutput = outputDescriptor.resolve(bindings);
                  if (!resolvedOutput.hasTypeVariables()) {
                    var outKey = edge.targetNodeId() + ":" + output.id();
                    if (!inferredDescriptors.containsKey(outKey) || !inferredDescriptors.get(outKey).equals(resolvedOutput)) {
                      inferredDescriptors.put(outKey, resolvedOutput);
                      changed = true;
                    }
                  }
                }
              }
            }
          } else {
            // Unification failed: type mismatch
            var edgeKey = edge.sourceNodeId() + ":" + edge.sourceHandle() + "->" + edge.targetNodeId() + ":" + edge.targetHandle();
            if (reportedEdges.add(edgeKey)) {
              diagnostics.add(new ScriptGraph.ValidationDiagnostic(
                edge.targetNodeId(),
                edgeKey,
                "Generic type mismatch: " + sourceDescriptor.displayString() + " cannot unify with " + targetDescriptor.resolve(bindings).displayString() + " on port '" + edge.targetHandle() + "'",
                ScriptGraph.Severity.WARNING
              ));
            }
          }
        } else if (targetDescriptor instanceof TypeDescriptor.Simple(PortType targetType)) {
          // Legacy behavior: simple type propagation through ANY ports
          if (targetType == PortType.ANY) {
            if (!inferredDescriptors.containsKey(targetKey)) {
              inferredDescriptors.put(targetKey, sourceDescriptor);
              changed = true;

              // Propagate to ANY outputs of the same node (legacy passthrough)
              for (var output : targetMeta.outputs()) {
                if (output.type() == PortType.EXEC) continue;
                var outputDescriptor = output.effectiveTypeDescriptor();
                if (outputDescriptor instanceof TypeDescriptor.Simple(PortType outType) && outType == PortType.ANY) {
                  var outKey = edge.targetNodeId() + ":" + output.id();
                  if (!inferredDescriptors.containsKey(outKey)) {
                    inferredDescriptors.put(outKey, sourceDescriptor);
                    changed = true;
                  }
                }
              }
            }
          } else if (targetType != PortType.EXEC && targetType != PortType.STRING) {
            // Check compatibility: concrete source reaching a typed target
            var sourceBaseType = sourceDescriptor.baseType();
            if (!TypeCompatibility.isCompatible(sourceBaseType, targetType)) {
              var edgeKey = edge.sourceNodeId() + ":" + edge.sourceHandle() + "->" + edge.targetNodeId() + ":" + edge.targetHandle();
              if (reportedEdges.add(edgeKey)) {
                diagnostics.add(new ScriptGraph.ValidationDiagnostic(
                  edge.targetNodeId(),
                  edgeKey,
                  "Type narrowing: inferred " + sourceDescriptor.displayString() + " flowing through ANY reaches " + targetType + " port '" + edge.targetHandle() + "'",
                  ScriptGraph.Severity.WARNING
                ));
              }
            }
          }
        }
      }
    }

    return diagnostics;
  }
}
