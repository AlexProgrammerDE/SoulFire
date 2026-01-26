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

/// Definition of an input or output port on a node.
/// Used to describe the node's interface for client rendering.
///
/// @param id          the port identifier used in connections
/// @param displayName the human-readable name for display
/// @param type        the data type of this port
/// @param required    whether this input is required (only for inputs)
/// @param defaultValue the default value as JSON string (only for inputs)
/// @param description optional description of what this port is for
/// @param elementType for list ports, the type of elements
public record PortDefinition(
  String id,
  String displayName,
  PortType type,
  boolean required,
  @Nullable String defaultValue,
  String description,
  @Nullable PortType elementType
) {
  /// Creates an execution input port.
  public static PortDefinition execIn() {
    return new PortDefinition("exec_in", "In", PortType.EXEC, false, null, "Execution input", null);
  }

  /// Creates an execution output port.
  public static PortDefinition execOut() {
    return new PortDefinition("exec_out", "Out", PortType.EXEC, false, null, "Execution output", null);
  }

  /// Creates a required input port.
  public static PortDefinition input(String id, String displayName, PortType type, String description) {
    return new PortDefinition(id, displayName, type, true, null, description, null);
  }

  /// Creates an optional input port with a default value.
  public static PortDefinition inputWithDefault(String id, String displayName, PortType type, String defaultValue, String description) {
    return new PortDefinition(id, displayName, type, false, defaultValue, description, null);
  }

  /// Creates an output port.
  public static PortDefinition output(String id, String displayName, PortType type, String description) {
    return new PortDefinition(id, displayName, type, false, null, description, null);
  }

  /// Creates a list input port with element type.
  public static PortDefinition listInput(String id, String displayName, PortType elementType, String description) {
    return new PortDefinition(id, displayName, PortType.LIST, true, null, description, elementType);
  }

  /// Creates a list output port with element type.
  public static PortDefinition listOutput(String id, String displayName, PortType elementType, String description) {
    return new PortDefinition(id, displayName, PortType.LIST, false, null, description, elementType);
  }
}
