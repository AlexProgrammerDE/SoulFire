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

import org.immutables.value.Value;

/// Defines a group of sockets that can be dynamically added/removed at runtime.
/// Used for nodes like "Combine List" where users can add arbitrary numbers of inputs.
/// Similar to Blender's Repeat Zone or Join Geometry node dynamic inputs.
@Value.Immutable
public interface DynamicSocketGroup {
  /// Convenience method to create a dynamic input group.
  static DynamicSocketGroup inputGroup(String id, String displayName, PortDefinition template,
                                       int minCount, int maxCount) {
    return ImmutableDynamicSocketGroup.builder()
      .id(id)
      .displayName(displayName)
      .socketTemplate(template)
      .minCount(minCount)
      .maxCount(maxCount)
      .isInput(true)
      .build();
  }

  /// Convenience method to create a dynamic output group.
  static DynamicSocketGroup outputGroup(String id, String displayName, PortDefinition template,
                                        int minCount, int maxCount) {
    return ImmutableDynamicSocketGroup.builder()
      .id(id)
      .displayName(displayName)
      .socketTemplate(template)
      .minCount(minCount)
      .maxCount(maxCount)
      .isInput(false)
      .build();
  }

  /// Unique identifier for this group (e.g., "inputs", "elements").
  String id();

  /// Human-readable name for UI (e.g., "Input", "Element").
  String displayName();

  /// Template port definition for new sockets in this group.
  /// New sockets are created based on this template with unique IDs.
  PortDefinition socketTemplate();

  /// Minimum number of sockets in this group.
  @Value.Default
  default int minCount() {
    return 0;
  }

  /// Maximum number of sockets (0 = unlimited).
  @Value.Default
  default int maxCount() {
    return 0;
  }

  /// Whether this group is for inputs (true) or outputs (false).
  boolean isInput();

  /// Generates a socket ID for a specific index in this group.
  /// @param index the socket index (0-based)
  /// @return unique socket ID like "number-inputs-0"
  default String generateSocketId(int index) {
    return socketTemplate().id() + "-" + index;
  }

  /// Generates a display name for a specific index in this group.
  /// @param index the socket index (0-based)
  /// @return display name like "Input 1" (1-based for user friendliness)
  default String generateDisplayName(int index) {
    return displayName() + " " + (index + 1);
  }
}
