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
package com.soulfiremc.server.script.nodes.data;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets a bot by its name.
/// Input: name (string, the bot's account name)
/// Outputs: bot (BotConnection or null), found (boolean)
public final class GetBotByNameNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_bot_by_name")
    .displayName("Get Bot By Name")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.inputWithDefault("name", "Name", PortType.STRING, "\"\"", "The bot's account name")
    )
    .addOutputs(
      PortDefinition.output("bot", "Bot", PortType.BOT, "The found bot (null if not found)"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether the bot was found")
    )
    .description("Finds a bot by its account name")
    .icon("search")
    .color("#9C27B0")
    .addKeywords("bot", "name", "find", "get", "search")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var name = getStringInput(inputs, "name", "");

    if (name.isEmpty()) {
      return completed(results("bot", null, "found", false));
    }

    var bot = runtime.instance().botConnections().values().stream()
      .filter(b -> b.accountName().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);

    return completed(results(
      "bot", bot,
      "found", bot != null
    ));
  }
}
