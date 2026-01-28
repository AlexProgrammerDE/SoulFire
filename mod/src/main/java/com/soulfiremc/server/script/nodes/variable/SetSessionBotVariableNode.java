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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/// Variable node that stores a value in the bot's session metadata (lost on disconnect).
public final class SetSessionBotVariableNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("variable.set_session")
    .displayName("Set Session Variable")
    .category(CategoryRegistry.VARIABLE)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("bot", "Bot", PortType.BOT, "Bot to store variable for"),
      PortDefinition.input("key", "Key", PortType.STRING, "Variable key"),
      PortDefinition.input("value", "Value", PortType.ANY, "Value to store")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether storage succeeded")
    )
    .description("Stores a value in the bot's session metadata (lost on disconnect)")
    .icon("upload")
    .color("#10B981")
    .addKeywords("variable", "session", "temporary", "store", "set", "memory")
    .build();

  // Static storage for session variables, keyed by bot ID
  private static final MetadataKey<ConcurrentHashMap<String, NodeValue>> SESSION_VARS_KEY =
    MetadataKey.of("script_session", "variables", ConcurrentHashMap.class);

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var key = getStringInput(inputs, "key", "");
    var value = inputs.get("value");

    if (key.isEmpty()) {
      return completed(result("success", false));
    }

    try {
      var sessionVars = bot.metadata().getOrSet(SESSION_VARS_KEY, ConcurrentHashMap::new);
      sessionVars.put(key, value != null ? value : NodeValue.ofNull());
      return completed(result("success", true));
    } catch (Exception e) {
      return completed(result("success", false));
    }
  }
}
