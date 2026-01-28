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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// JSON node that creates a JSON object from key-value pairs.
public final class JsonObjectNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("json.object")
    .displayName("JSON Object")
    .category(CategoryRegistry.JSON)
    .addInputs(
      PortDefinition.listInput("keys", "Keys", PortType.STRING, "List of keys"),
      PortDefinition.listInput("values", "Values", PortType.ANY, "List of values")
    )
    .addOutputs(
      PortDefinition.output("object", "Object", PortType.STRING, "JSON object string")
    )
    .description("Creates a JSON object from key-value pairs")
    .icon("braces")
    .color("#F59E0B")
    .addKeywords("json", "object", "map", "create", "build", "dictionary")
    .build();

  private static final Gson GSON = new Gson();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var keys = getStringListInput(inputs, "keys");
    var values = getListInput(inputs, "values");

    var obj = new JsonObject();
    var size = Math.min(keys.size(), values.size());

    for (int i = 0; i < size; i++) {
      var key = keys.get(i);
      var value = values.get(i);
      var element = value.asJsonElement();
      obj.add(key, element != null ? element : JsonNull.INSTANCE);
    }

    return completed(result("object", GSON.toJson(obj)));
  }
}
