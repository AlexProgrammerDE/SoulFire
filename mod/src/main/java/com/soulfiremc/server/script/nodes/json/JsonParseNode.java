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
import com.google.gson.JsonSyntaxException;
import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// JSON node that parses a JSON string into a usable object.
public final class JsonParseNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("json.parse")
    .displayName("JSON Parse")
    .category(CategoryRegistry.JSON)
    .addInputs(
      PortDefinition.input("json", "JSON", PortType.STRING, "JSON string to parse")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.ANY, "Parsed JSON value"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether parsing succeeded"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Parse error if failed")
    )
    .description("Parses a JSON string into a usable object")
    .icon("file-json")
    .color("#F59E0B")
    .addKeywords("json", "parse", "deserialize", "decode")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var jsonInput = getStringInput(inputs, "json", "");

    try {
      var element = JsonParser.parseString(jsonInput);
      return completedMono(results(
        "result", NodeValue.fromJson(element),
        "success", true,
        "errorMessage", ""
      ));
    } catch (JsonSyntaxException e) {
      return completedMono(results(
        "result", NodeValue.ofNull(),
        "success", false,
        "errorMessage", e.getMessage() != null ? e.getMessage() : "Invalid JSON"
      ));
    }
  }
}
