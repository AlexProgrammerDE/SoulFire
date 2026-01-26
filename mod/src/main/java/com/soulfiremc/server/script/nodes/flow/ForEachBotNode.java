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

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.NodeRuntime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that iterates over a list of bots.
/// Input: bots (List of BotConnection)
/// Output: bot (current BotConnection in iteration), index (current index), count (total bots)
///
/// The bot output should be wired to downstream action nodes that need it.
public final class ForEachBotNode extends AbstractScriptNode {
  public static final String TYPE = "flow.foreach_bot";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var botValues = getListInput(inputs, "bots");

    if (botValues.isEmpty()) {
      return completed(results("bot", null, "index", -1, "count", 0));
    }

    // This node works differently - it needs the ScriptEngine to handle iteration.
    // For now, we output the first bot.
    // The actual iteration is handled by the engine following execution edges.
    var firstBotValue = botValues.getFirst();
    var firstBot = firstBotValue.asBot();

    return completed(results(
      "bot", firstBot,
      "index", 0,
      "count", botValues.size()
    ));
  }
}
