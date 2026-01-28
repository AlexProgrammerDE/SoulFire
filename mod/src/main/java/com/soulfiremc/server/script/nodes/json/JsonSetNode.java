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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/// JSON node that sets a value in JSON at a specified path.
public final class JsonSetNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("json.set")
    .displayName("JSON Set")
    .category(CategoryRegistry.JSON)
    .addInputs(
      PortDefinition.inputWithDefault("json", "JSON", PortType.STRING, "\"{}\"", "JSON string to modify"),
      PortDefinition.input("path", "Path", PortType.STRING, "Path where to set value"),
      PortDefinition.input("value", "Value", PortType.ANY, "Value to set")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "Modified JSON string"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether the operation succeeded")
    )
    .description("Sets a value in JSON at a specified path")
    .icon("edit")
    .color("#F59E0B")
    .addKeywords("json", "set", "modify", "update", "write")
    .build();

  private static final Gson GSON = new Gson();
  private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("\\[(\\d+)]");

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var jsonInput = getStringInput(inputs, "json", "{}");
    var path = getStringInput(inputs, "path", "");
    var value = inputs.get("value");

    if (path.isEmpty()) {
      return completed(results(
        "result", jsonInput,
        "success", false
      ));
    }

    try {
      var root = JsonParser.parseString(jsonInput);
      var valueElement = value != null ? value.asJsonElement() : null;
      if (valueElement == null) {
        valueElement = com.google.gson.JsonNull.INSTANCE;
      }

      // Navigate to parent and set value
      var parts = path.split("\\.");
      var current = root;

      for (int i = 0; i < parts.length - 1; i++) {
        var part = parts[i];
        current = navigateOrCreate(current, part);
        if (current == null) {
          return completed(results(
            "result", jsonInput,
            "success", false
          ));
        }
      }

      // Set the value at the final path
      var finalPart = parts[parts.length - 1];
      setAtPath(current, finalPart, valueElement);

      return completed(results(
        "result", GSON.toJson(root),
        "success", true
      ));
    } catch (Exception e) {
      return completed(results(
        "result", jsonInput,
        "success", false
      ));
    }
  }

  private JsonElement navigateOrCreate(JsonElement current, String part) {
    var matcher = ARRAY_INDEX_PATTERN.matcher(part);
    var fieldName = matcher.replaceAll("");

    if (!fieldName.isEmpty()) {
      if (!current.isJsonObject()) {
        return null;
      }
      var obj = current.getAsJsonObject();
      if (!obj.has(fieldName)) {
        obj.add(fieldName, new JsonObject());
      }
      current = obj.get(fieldName);
    }

    matcher.reset();
    while (matcher.find()) {
      var index = Integer.parseInt(matcher.group(1));
      if (!current.isJsonArray()) {
        return null;
      }
      var array = current.getAsJsonArray();
      while (array.size() <= index) {
        array.add(new JsonObject());
      }
      current = array.get(index);
    }

    return current;
  }

  private void setAtPath(JsonElement current, String part, JsonElement value) {
    var matcher = ARRAY_INDEX_PATTERN.matcher(part);
    var fieldName = matcher.replaceAll("");

    if (matcher.find()) {
      // Has array index
      matcher.reset();
      if (!fieldName.isEmpty()) {
        if (current.isJsonObject()) {
          var obj = current.getAsJsonObject();
          if (!obj.has(fieldName)) {
            obj.add(fieldName, new JsonArray());
          }
          current = obj.get(fieldName);
        }
      }

      while (matcher.find()) {
        var index = Integer.parseInt(matcher.group(1));
        if (current.isJsonArray()) {
          var array = current.getAsJsonArray();
          while (array.size() <= index) {
            array.add(com.google.gson.JsonNull.INSTANCE);
          }
          if (!matcher.find()) {
            array.set(index, value);
          } else {
            current = array.get(index);
            matcher.reset(part.substring(matcher.start() - 1));
            matcher.find();
          }
        }
      }
    } else if (!fieldName.isEmpty() && current.isJsonObject()) {
      current.getAsJsonObject().add(fieldName, value);
    }
  }
}
