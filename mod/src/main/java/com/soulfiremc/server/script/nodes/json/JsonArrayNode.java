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
import com.google.gson.JsonNull;
import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// JSON node that creates a JSON array from multiple inputs.
public final class JsonArrayNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("json.array")
    .displayName("JSON Array")
    .category(CategoryRegistry.JSON)
    .addInputs(
      PortDefinition.multiInput("items", "Items", PortType.ANY, "Items to include in array")
    )
    .addOutputs(
      PortDefinition.output("array", "Array", PortType.STRING, "JSON array string")
    )
    .description("Creates a JSON array from multiple inputs")
    .icon("list")
    .color("#F59E0B")
    .addKeywords("json", "array", "list", "create", "build")
    .build();

  private static final Gson GSON = new Gson();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var items = getListInput(inputs, "items");

    var array = new JsonArray();
    for (var item : items) {
      var element = item.asJsonElement();
      array.add(element != null ? element : JsonNull.INSTANCE);
    }

    return completedMono(result("array", GSON.toJson(array)));
  }
}
