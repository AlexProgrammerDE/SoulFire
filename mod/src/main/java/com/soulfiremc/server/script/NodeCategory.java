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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Predefined categories for organizing script nodes.
/// Each category has an id, display name, icon, and sort order.
public enum NodeCategory {
  TRIGGERS("triggers", "Triggers", "zap", "Event triggers that start script execution", 0),
  ACTIONS("actions", "Actions", "play", "Nodes that perform bot actions", 1),
  DATA("data", "Data", "database", "Nodes that read game data", 2),
  FLOW("flow", "Flow Control", "git-branch", "Control execution flow and branching", 3),
  LOGIC("logic", "Logic", "toggle-left", "Boolean logic operations", 4),
  MATH("math", "Math", "calculator", "Mathematical operations", 5),
  STRING("string", "String", "type", "Text manipulation operations", 6),
  LIST("list", "List", "list", "List/array operations", 7),
  CONSTANTS("constants", "Constants", "hash", "Constant value nodes", 8),
  UTILITY("utility", "Utility", "tool", "Utility and conversion nodes", 9);

  private final String id;
  private final String displayName;
  private final String icon;
  private final String description;
  private final int sortOrder;

  private static final Map<String, NodeCategory> BY_ID = Arrays.stream(values())
    .collect(Collectors.toMap(NodeCategory::id, Function.identity()));

  NodeCategory(String id, String displayName, String icon, String description, int sortOrder) {
    this.id = id;
    this.displayName = displayName;
    this.icon = icon;
    this.description = description;
    this.sortOrder = sortOrder;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  public String icon() {
    return icon;
  }

  public String description() {
    return description;
  }

  public int sortOrder() {
    return sortOrder;
  }

  /// Gets a category by its ID, or UTILITY as fallback.
  public static NodeCategory fromId(String id) {
    return BY_ID.getOrDefault(id, UTILITY);
  }

  /// Gets all categories sorted by sort order.
  public static List<NodeCategory> allSorted() {
    return Arrays.stream(values())
      .sorted((a, b) -> Integer.compare(a.sortOrder, b.sortOrder))
      .toList();
  }
}
