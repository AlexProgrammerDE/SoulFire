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
import com.google.gson.GsonBuilder;
import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// JSON node that converts a value to a JSON string with optional pretty printing.
public final class JsonStringifyNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("json.stringify")
    .displayName("JSON Stringify")
    .category(CategoryRegistry.JSON)
    .addInputs(
      PortDefinition.input("value", "Value", PortType.ANY, "Value to convert"),
      PortDefinition.inputWithDefault("pretty", "Pretty", PortType.BOOLEAN, "false", "Pretty print with indentation")
    )
    .addOutputs(
      PortDefinition.output("json", "JSON", PortType.STRING, "JSON string representation")
    )
    .description("Converts a value to a JSON string with optional pretty printing")
    .icon("file-code")
    .color("#F59E0B")
    .addKeywords("json", "stringify", "serialize", "encode", "format")
    .build();

  private static final Gson GSON = new Gson();
  private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = inputs.get("value");
    var pretty = getBooleanInput(inputs, "pretty", false);

    if (value == null || value.isNull()) {
      return completedMono(result("json", "null"));
    }

    var jsonElement = value.asJsonElement();
    if (jsonElement == null) {
      // For non-JSON values, convert to string
      return completedMono(result("json", "\"" + value.toString() + "\""));
    }

    var gson = pretty ? GSON_PRETTY : GSON;
    return completedMono(result("json", gson.toJson(jsonElement)));
  }
}
