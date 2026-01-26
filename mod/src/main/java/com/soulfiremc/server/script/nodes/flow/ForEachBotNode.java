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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that iterates over a list of bots.
/// Input: bots (List of BotConnection)
/// Output: bot (current BotConnection in iteration), index (current index), count (total bots)
///
/// The bot output should be wired to downstream action nodes that need it.
public final class ForEachBotNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.foreach_bot")
    .displayName("For Each Bot")
    .category(NodeCategory.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.listInput("bots", "Bots", PortType.BOT, "List of bots to iterate"),
      PortDefinition.inputWithDefault("currentIndex", "Current Index", PortType.NUMBER, "0", "Current iteration index")
    )
    .addOutputs(
      PortDefinition.output("exec_loop", "Loop", PortType.EXEC, "Executes for each bot"),
      PortDefinition.output("exec_done", "Done", PortType.EXEC, "Executes when iteration completes"),
      PortDefinition.output("bot", "Bot", PortType.BOT, "Current bot in iteration"),
      PortDefinition.output("index", "Index", PortType.NUMBER, "Current bot index"),
      PortDefinition.output("count", "Count", PortType.NUMBER, "Total number of bots"),
      PortDefinition.output("isComplete", "Complete", PortType.BOOLEAN, "Whether iteration is done"),
      PortDefinition.output("nextIndex", "Next Index", PortType.NUMBER, "Next iteration index")
    )
    .description("Iterates over each bot in a list")
    .icon("users")
    .color("#607D8B")
    .addKeywords("foreach", "bot", "iterate", "loop")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var botValues = getListInput(inputs, "bots");
    var currentIndex = getIntInput(inputs, "currentIndex", 0);

    var isComplete = currentIndex >= botValues.size();

    if (isComplete) {
      return completed(results(
        "bot", null,
        "index", currentIndex,
        "count", botValues.size(),
        "isComplete", true,
        "nextIndex", currentIndex + 1
      ));
    }

    var currentBotValue = botValues.get(currentIndex);
    var currentBot = currentBotValue.asBot();

    return completed(results(
      "bot", currentBot,
      "index", currentIndex,
      "count", botValues.size(),
      "isComplete", false,
      "nextIndex", currentIndex + 1
    ));
  }
}
