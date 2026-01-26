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

/// Definition of a node category for organizing nodes in the palette.
@Value.Immutable
public interface NodeCategory {
  /// Convenience builder starting point.
  static ImmutableNodeCategory.Builder builder() {
    return ImmutableNodeCategory.builder();
  }

  /// Creates a category with all required fields.
  static NodeCategory of(String id, String displayName, String icon, String description, int sortOrder) {
    return builder()
      .id(id)
      .displayName(displayName)
      .icon(icon)
      .description(description)
      .sortOrder(sortOrder)
      .build();
  }

  /// The unique identifier for this category.
  String id();

  /// Human-readable name for display.
  String displayName();

  /// Icon identifier for the category.
  String icon();

  /// Description of what nodes in this category do.
  @Value.Default
  default String description() {
    return "";
  }

  /// Sort order for display (lower numbers appear first).
  @Value.Default
  default int sortOrder() {
    return 100;
  }
}
