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

/// Definition of an input or output port on a node.
/// Used to describe the node's interface for client rendering.
/// Supports Blender-style features: single-connection default, multi-input sockets,
/// conditional visibility, and dynamic socket groups.
@Value.Immutable
@Value.Style(stagedBuilder = true)
public interface PortDefinition {
  /// Creates an execution input port.
  static PortDefinition execIn() {
    return ImmutablePortDefinition.builder()
      .id("exec-in")
      .displayName("In")
      .type(PortType.EXEC)
      .description("Execution input")
      .build();
  }

  /// Creates an execution output port.
  static PortDefinition execOut() {
    return ImmutablePortDefinition.builder()
      .id("exec-out")
      .displayName("Out")
      .type(PortType.EXEC)
      .maxConnections(0) // Outputs can have unlimited connections
      .description("Execution output")
      .build();
  }

  /// Creates a named execution output port (for branching nodes).
  /// Port ID format: "exec-name" (e.g., "exec-true", "exec-false")
  static PortDefinition execOut(String id, String displayName, String description) {
    return ImmutablePortDefinition.builder()
      .id("exec-" + id)
      .displayName(displayName)
      .type(PortType.EXEC)
      .maxConnections(0) // Outputs can have unlimited connections
      .description(description)
      .build();
  }

  /// Creates a required input port.
  /// Port ID format: "type-name" (e.g., "number-interval", "bot-target")
  static PortDefinition input(String id, String displayName, PortType type, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + id)
      .displayName(displayName)
      .type(type)
      .required(true)
      .description(description)
      .build();
  }

  /// Creates an optional input port with a default value.
  /// Port ID format: "type-name" (e.g., "number-interval", "string-message")
  static PortDefinition inputWithDefault(String id, String displayName, PortType type, String defaultValue, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + id)
      .displayName(displayName)
      .type(type)
      .required(false)
      .defaultValue(defaultValue)
      .description(description)
      .build();
  }

  /// Creates an output port.
  /// Port ID format: "type-name" (e.g., "boolean-success", "number-count")
  static PortDefinition output(String id, String displayName, PortType type, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + id)
      .displayName(displayName)
      .type(type)
      .maxConnections(0) // Outputs can have unlimited connections
      .description(description)
      .build();
  }

  /// Creates a list input port with element type.
  /// Port ID format: "list-name" (e.g., "list-items", "list-targets")
  static PortDefinition listInput(String id, String displayName, PortType elementType, String description) {
    return ImmutablePortDefinition.builder()
      .id("list-" + id)
      .displayName(displayName)
      .type(PortType.LIST)
      .required(true)
      .elementType(elementType)
      .description(description)
      .build();
  }

  /// Creates a list output port with element type.
  /// Port ID format: "list-name" (e.g., "list-results", "list-bots")
  static PortDefinition listOutput(String id, String displayName, PortType elementType, String description) {
    return ImmutablePortDefinition.builder()
      .id("list-" + id)
      .displayName(displayName)
      .type(PortType.LIST)
      .elementType(elementType)
      .maxConnections(0) // Outputs can have unlimited connections
      .description(description)
      .build();
  }

  /// Creates a multi-input port that accepts multiple ordered connections (Blender pill-shaped style).
  /// All connected values are collected into an ordered list.
  /// Port ID format: "list-name" (e.g., "list-inputs")
  static PortDefinition multiInput(String id, String displayName, PortType elementType, String description) {
    return ImmutablePortDefinition.builder()
      .id("list-" + id)
      .displayName(displayName)
      .type(PortType.LIST)
      .elementType(elementType)
      .multiInput(true)
      .maxConnections(0) // Unlimited connections for multi-input
      .description(description)
      .build();
  }

  /// Creates an input port visible only when a condition is met (Blender mode-based visibility).
  /// @param visibleWhen condition string, e.g., "operation=SCALE" or "mode!=SIMPLE"
  static PortDefinition conditionalInput(String id, String displayName, PortType type,
                                         String visibleWhen, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + id)
      .displayName(displayName)
      .type(type)
      .required(true)
      .visibleWhen(visibleWhen)
      .description(description)
      .build();
  }

  /// Creates an optional input port with default that's visible only when a condition is met.
  /// @param visibleWhen condition string, e.g., "operation=SCALE" or "mode!=SIMPLE"
  static PortDefinition conditionalInputWithDefault(String id, String displayName, PortType type,
                                                    String defaultValue, String visibleWhen, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + id)
      .displayName(displayName)
      .type(type)
      .required(false)
      .defaultValue(defaultValue)
      .visibleWhen(visibleWhen)
      .description(description)
      .build();
  }

  /// Creates a dynamic input template for socket groups that can be added/removed.
  /// Used with DynamicSocketGroup to create nodes with variable numbers of inputs.
  static PortDefinition dynamicInput(String groupId, String displayName, PortType type, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + groupId)
      .displayName(displayName)
      .type(type)
      .dynamicGroup(groupId)
      .description(description)
      .build();
  }

  /// Creates a dynamic output template for socket groups that can be added/removed.
  static PortDefinition dynamicOutput(String groupId, String displayName, PortType type, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + groupId)
      .displayName(displayName)
      .type(type)
      .dynamicGroup(groupId)
      .maxConnections(0) // Outputs can have unlimited connections
      .description(description)
      .build();
  }

  String id();

  String displayName();

  PortType type();

  @Value.Default
  default boolean required() {
    return false;
  }

  @Nullable
  String defaultValue();

  @Value.Default
  default String description() {
    return "";
  }

  @Nullable
  PortType elementType();

  /// Maximum connections allowed on this port.
  /// Default is 1 for inputs (Blender-style single connection), 0 (unlimited) for outputs.
  /// Set to 0 for unlimited connections.
  @Value.Default
  default int maxConnections() {
    return 1; // Blender default: single connection per input
  }

  /// Whether this is a multi-input socket (pill-shaped in Blender style).
  /// When true, accepts multiple ordered connections that are merged into a list.
  @Value.Default
  default boolean multiInput() {
    return false;
  }

  /// Visibility condition - port only shown when node data field matches.
  /// Format: "field=value" or "field!=value" or comma-separated conditions.
  /// Example: "operation=SCALE" means only visible when data.operation == "SCALE"
  @Nullable
  String visibleWhen();

  /// For dynamic socket groups - the group identifier.
  /// Sockets with same group_id can be added/removed together at runtime.
  @Nullable
  String dynamicGroup();

  /// Sort order within the port list (for dynamic sockets ordering).
  @Value.Default
  default int sortOrder() {
    return 0;
  }

  /// Checks if this port is an output port (not an input).
  /// Output ports typically have maxConnections=0 (unlimited).
  default boolean isOutput() {
    return maxConnections() == 0 && !multiInput();
  }
}
