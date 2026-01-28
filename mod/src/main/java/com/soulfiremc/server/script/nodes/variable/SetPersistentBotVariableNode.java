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

import com.google.gson.JsonNull;
import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Variable node that stores a value in the bot's persistent metadata (survives disconnects).
public final class SetPersistentBotVariableNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("variable.set_persistent")
    .displayName("Set Persistent Variable")
    .category(CategoryRegistry.VARIABLE)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("bot", "Bot", PortType.BOT, "Bot to store variable for"),
      PortDefinition.inputWithDefault("namespace", "Namespace", PortType.STRING, "\"script\"", "Variable namespace"),
      PortDefinition.input("key", "Key", PortType.STRING, "Variable key"),
      PortDefinition.input("value", "Value", PortType.ANY, "Value to store")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether storage succeeded")
    )
    .description("Stores a value in the bot's persistent metadata (survives disconnects)")
    .icon("save")
    .color("#10B981")
    .addKeywords("variable", "persistent", "save", "store", "set", "permanent")
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
    var value = inputs.get("value");

    if (key.isEmpty()) {
      return completed(result("success", false));
    }

    try {
      var jsonValue = value != null ? value.asJsonElement() : JsonNull.INSTANCE;
      if (jsonValue == null) {
        jsonValue = JsonNull.INSTANCE;
      }
      bot.persistentMetadata().set(namespace, key, jsonValue);
      return completed(result("success", true));
    } catch (Exception e) {
      return completed(result("success", false));
    }
  }
}
