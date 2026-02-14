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

import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Variable node that retrieves a value from the bot's session metadata.
public final class GetSessionBotVariableNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("variable.get_session")
    .displayName("Get Session Variable")
    .category(CategoryRegistry.VARIABLE)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("key", "Key", PortType.STRING, "Variable key"),
      PortDefinition.inputWithDefault("defaultValue", "Default", PortType.ANY, "null", "Value if not found")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("value", "Value", PortType.ANY, "Retrieved value"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether variable was found")
    )
    .description("Retrieves a value from the bot's session metadata")
    .icon("file-input")
    .color("#10B981")
    .addKeywords("variable", "session", "temporary", "get", "retrieve", "memory")
    .build();

  // Same static key as SetSessionBotVariableNode
  private static final MetadataKey<ConcurrentHashMap<String, NodeValue>> SESSION_VARS_KEY =
    MetadataKey.of("script_session", "variables", ConcurrentHashMap.class);

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var key = getStringInput(inputs, "key", "");
    var defaultValue = inputs.get("defaultValue");

    if (key.isEmpty()) {
      return completedMono(results(
        "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
        "found", false
      ));
    }

    try {
      var sessionVars = bot.metadata().getOrSet(SESSION_VARS_KEY, ConcurrentHashMap::new);
      var value = sessionVars.get(key);

      if (value == null) {
        return completedMono(results(
          "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
          "found", false
        ));
      }

      return completedMono(results(
        "value", value,
        "found", true
      ));
    } catch (Exception _) {
      return completedMono(results(
        "value", defaultValue != null ? defaultValue : NodeValue.ofNull(),
        "found", false
      ));
    }
  }
}
