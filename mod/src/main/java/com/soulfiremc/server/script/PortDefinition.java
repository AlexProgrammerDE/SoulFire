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
      .description("Execution output")
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

  /// Whether this input accepts multiple connections (Blender-style multi-input).
  /// When true, all connected values are collected into a list.
  @Value.Default
  default boolean multiInput() {
    return false;
  }

  /// Creates a multi-input port that accepts multiple connections.
  /// All connected values are collected into a list.
  static PortDefinition multiInput(String id, String displayName, PortType type, String description) {
    return ImmutablePortDefinition.builder()
      .id(type.name().toLowerCase() + "-" + id)
      .displayName(displayName)
      .type(type)
      .required(false)
      .multiInput(true)
      .description(description)
      .build();
  }
}
