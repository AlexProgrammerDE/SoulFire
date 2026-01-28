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
package com.soulfiremc.server.script.nodes.variable;

import com.google.gson.JsonElement;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Variable node that retrieves a value from the bot's persistent metadata.
public final class GetPersistentBotVariableNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("variable.get_persistent")
    .displayName("Get Persistent Variable")
    .category(CategoryRegistry.VARIABLE)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "Bot to get variable from"),
      PortDefinition.inputWithDefault("namespace", "Namespace", PortType.STRING, "\"script\"", "Variable namespace"),
      PortDefinition.input("key", "Key", PortType.STRING, "Variable key"),
      PortDefinition.inputWithDefault("defaultValue", "Default", PortType.ANY, "null", "Value if not found")
    )
    .addOutputs(
      PortDefinition.output("value", "Value", PortType.ANY, "Retrieved value"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether variable was found")
    )
    .description("Retrieves a value from the bot's persistent metadata")
    .icon("download")
    .color("#10B981")
    .addKeywords("variable", "persistent", "load", "get", "retrieve", "permanent")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var namespace = getStringInput(inputs, "namespace", "script");
    var key = getStringInput(inputs, "key", "");
    var defaultValue = inputs.get("defaultValue");

    if (key.isEmpty()) {
      return completed(results(
        "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
        "found", false
      ));
    }

    try {
      MetadataKey<JsonElement> metaKey = MetadataKey.of(namespace, key, JsonElement.class);
      var jsonValue = bot.persistentMetadata().get(metaKey);

      if (jsonValue == null) {
        return completed(results(
          "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
          "found", false
        ));
      }

      return completed(results(
        "value", NodeValue.fromJson(jsonValue),
        "found", true
      ));
    } catch (Exception e) {
      return completed(results(
        "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
        "found", false
      ));
    }
  }
}
