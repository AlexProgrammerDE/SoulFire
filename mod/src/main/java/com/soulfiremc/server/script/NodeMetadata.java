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

import java.util.List;

/// Complete metadata for a node type.
/// Contains all information needed to render the node in a client
/// without hardcoded knowledge of specific node types.
///
/// @param type               the unique type identifier
/// @param displayName        human-readable name for display
/// @param description        description of what the node does
/// @param category           category for organizing in the palette
/// @param isTrigger          whether this is a trigger (entry point) node
/// @param inputs             input port definitions
/// @param outputs            output port definitions
/// @param icon               icon identifier (required)
/// @param color              optional color hint (hex code)
/// @param keywords           search keywords
/// @param deprecated         whether this node is deprecated
/// @param deprecationMessage if deprecated, what to use instead
public record NodeMetadata(
  String type,
  String displayName,
  String description,
  NodeCategory category,
  boolean isTrigger,
  List<PortDefinition> inputs,
  List<PortDefinition> outputs,
  String icon,
  @Nullable String color,
  List<String> keywords,
  boolean deprecated,
  @Nullable String deprecationMessage
) {
  /// Builder for creating NodeMetadata instances.
  public static Builder builder(String type) {
    return new Builder(type);
  }

  public static final class Builder {
    private final String type;
    private String displayName;
    private String description = "";
    private NodeCategory category = NodeCategory.UTILITY;
    private boolean isTrigger = false;
    private List<PortDefinition> inputs = List.of();
    private List<PortDefinition> outputs = List.of();
    private String icon = "box"; // Default icon
    private @Nullable String color;
    private List<String> keywords = List.of();
    private boolean deprecated = false;
    private @Nullable String deprecationMessage;

    private Builder(String type) {
      this.type = type;
      this.displayName = type; // Default to type if not set
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder category(NodeCategory category) {
      this.category = category;
      return this;
    }

    public Builder trigger() {
      this.isTrigger = true;
      this.category = NodeCategory.TRIGGERS;
      return this;
    }

    public Builder inputs(PortDefinition... inputs) {
      this.inputs = List.of(inputs);
      return this;
    }

    public Builder inputs(List<PortDefinition> inputs) {
      this.inputs = inputs;
      return this;
    }

    public Builder outputs(PortDefinition... outputs) {
      this.outputs = List.of(outputs);
      return this;
    }

    public Builder outputs(List<PortDefinition> outputs) {
      this.outputs = outputs;
      return this;
    }

    public Builder icon(String icon) {
      this.icon = icon;
      return this;
    }

    public Builder color(String color) {
      this.color = color;
      return this;
    }

    public Builder keywords(String... keywords) {
      this.keywords = List.of(keywords);
      return this;
    }

    public Builder deprecated(String message) {
      this.deprecated = true;
      this.deprecationMessage = message;
      return this;
    }

    public NodeMetadata build() {
      return new NodeMetadata(
        type, displayName, description, category, isTrigger,
        inputs, outputs, icon, color, keywords, deprecated, deprecationMessage
      );
    }
  }
}
