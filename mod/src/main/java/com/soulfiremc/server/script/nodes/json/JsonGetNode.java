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
package com.soulfiremc.server.script.nodes.json;

import com.google.gson.JsonParser;
import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/// JSON node that extracts a value from JSON using a path expression.
public final class JsonGetNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("json.get")
    .displayName("JSON Get")
    .category(CategoryRegistry.JSON)
    .addInputs(
      PortDefinition.input("json", "JSON", PortType.STRING, "JSON string or object"),
      PortDefinition.input("path", "Path", PortType.STRING, "Path expression (e.g., data.users[0].name)"),
      PortDefinition.inputWithDefault("defaultValue", "Default", PortType.ANY, "null", "Value if path not found")
    )
    .addOutputs(
      PortDefinition.output("value", "Value", PortType.ANY, "Extracted value"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether the path was found")
    )
    .description("Extracts a value from JSON using a path expression")
    .icon("search")
    .color("#F59E0B")
    .addKeywords("json", "get", "extract", "path", "query", "access")
    .build();

  private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("\\[(\\d+)]");

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var jsonInput = getStringInput(inputs, "json", "{}");
    var path = getStringInput(inputs, "path", "");
    var defaultValue = inputs.get("defaultValue");

    if (path.isEmpty()) {
      try {
        var element = JsonParser.parseString(jsonInput);
        return completed(results(
          "value", NodeValue.fromJson(element),
          "found", true
        ));
      } catch (Exception _) {
        return completed(results(
          "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
          "found", false
        ));
      }
    }

    try {
      var element = JsonParser.parseString(jsonInput);
      var current = element;

      // Split path by dots, but handle array indices
      var parts = path.split("\\.");

      for (var part : parts) {
        if (current == null || current.isJsonNull()) {
          return completed(results(
            "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
            "found", false
          ));
        }

        // Check for array index in this part (e.g., "users[0]" or "[0]")
        var matcher = ARRAY_INDEX_PATTERN.matcher(part);
        var fieldName = matcher.replaceAll("");
        var arrayIndices = ARRAY_INDEX_PATTERN.matcher(part);

        // First, access the field if there's a field name
        if (!fieldName.isEmpty()) {
          if (!current.isJsonObject()) {
            return completed(results(
              "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
              "found", false
            ));
          }
          current = current.getAsJsonObject().get(fieldName);
        }

        // Then, access array indices
        while (arrayIndices.find() && current != null) {
          var index = Integer.parseInt(arrayIndices.group(1));
          if (!current.isJsonArray()) {
            return completed(results(
              "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
              "found", false
            ));
          }
          var array = current.getAsJsonArray();
          if (index < 0 || index >= array.size()) {
            return completed(results(
              "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
              "found", false
            ));
          }
          current = array.get(index);
        }
      }

      if (current == null) {
        return completed(results(
          "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
          "found", false
        ));
      }

      return completed(results(
        "value", NodeValue.fromJson(current),
        "found", true
      ));
    } catch (Exception _) {
      return completed(results(
        "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
        "found", false
      ));
    }
  }
}
