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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/// Complete metadata for a node type.
/// Contains all information needed to render the node in a client
/// without hardcoded knowledge of specific node types.
@Value.Immutable
public interface NodeMetadata {
  /// Convenience builder starting point.
  static ImmutableNodeMetadata.Builder builder() {
    return ImmutableNodeMetadata.builder();
  }

  /// Convenience method to create an input list from varargs.
  static List<PortDefinition> inputs(PortDefinition... ports) {
    return List.of(ports);
  }

  /// Convenience method to create an output list from varargs.
  static List<PortDefinition> outputs(PortDefinition... ports) {
    return List.of(ports);
  }

  /// The unique type identifier for this node.
  String type();

  /// Human-readable name for display.
  String displayName();

  /// Description of what the node does.
  @Value.Default
  default String description() {
    return "";
  }

  /// Category for organizing in the palette.
  NodeCategory category();

  /// Whether this is a trigger (entry point) node.
  @Value.Default
  default boolean isTrigger() {
    return false;
  }

  /// Input port definitions.
  @Value.Default
  default List<PortDefinition> inputs() {
    return List.of();
  }

  /// Output port definitions.
  @Value.Default
  default List<PortDefinition> outputs() {
    return List.of();
  }

  /// Icon identifier (required).
  @Value.Default
  default String icon() {
    return "box";
  }

  /// Optional color hint (hex code).
  @Nullable
  String color();

  /// Search keywords.
  @Value.Default
  default List<String> keywords() {
    return List.of();
  }

  /// Whether this node is deprecated.
  @Value.Default
  default boolean deprecated() {
    return false;
  }

  /// If deprecated, what to use instead.
  @Nullable
  String deprecationMessage();

  /// Whether this is a layout node (reroute, frame, etc.).
  /// Layout nodes have special minimal rendering and don't execute logic.
  @Value.Default
  default boolean isLayoutNode() {
    return false;
  }

  /// Whether this node can be muted (bypassed during execution).
  /// When muted, inputs pass through to outputs unchanged.
  @Value.Default
  default boolean supportsMuting() {
    return true;
  }

  /// Whether this node supports inline preview of its output.
  @Value.Default
  default boolean supportsPreview() {
    return false;
  }

  /// Whether this node is expensive (slow but non-blocking).
  /// Used for static analysis warnings.
  @Value.Default
  default boolean isExpensive() {
    return false;
  }

  /// Whether this node blocks the calling thread (I/O, waits, network).
  /// Nodes that block the thread are unsafe in tick-synchronous paths.
  @Value.Default
  default boolean blocksThread() {
    return false;
  }

  /// Cached set of EXEC output port IDs for this node type.
  /// Used by the engine to avoid recomputing per node execution.
  @Value.Lazy
  default Set<String> execOutputPortIds() {
    return outputs().stream()
      .filter(p -> p.type() == PortType.EXEC)
      .map(PortDefinition::id)
      .collect(Collectors.toUnmodifiableSet());
  }

  /// Whether this node type has an exec_error output port.
  @Value.Lazy
  default boolean hasExecErrorPort() {
    return execOutputPortIds().contains(StandardPorts.EXEC_ERROR);
  }
}
