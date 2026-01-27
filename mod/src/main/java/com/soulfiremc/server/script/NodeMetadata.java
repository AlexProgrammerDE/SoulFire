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

/// Complete metadata for a node type.
/// Contains all information needed to render the node in a client
/// without hardcoded knowledge of specific node types.
/// Supports Blender-style features: dynamic socket groups and mode-based visibility.
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

  /// Dynamic socket group definitions (Blender-style add/remove sockets).
  /// Allows nodes to have variable numbers of inputs/outputs.
  @Value.Default
  default List<DynamicSocketGroup> dynamicGroups() {
    return List.of();
  }

  /// Whether this node supports mode-based socket visibility.
  /// When true, some sockets may be hidden based on node data values.
  @Value.Default
  default boolean hasDynamicVisibility() {
    return false;
  }

  /// The data field that controls socket visibility (e.g., "operation", "mode").
  /// Used with visibleWhen conditions on port definitions.
  @Nullable
  String visibilityControlField();

  /// Finds an input port by its ID.
  default PortDefinition findInput(String portId) {
    return inputs().stream()
      .filter(p -> p.id().equals(portId))
      .findFirst()
      .orElse(null);
  }

  /// Finds an output port by its ID.
  default PortDefinition findOutput(String portId) {
    return outputs().stream()
      .filter(p -> p.id().equals(portId))
      .findFirst()
      .orElse(null);
  }
}
