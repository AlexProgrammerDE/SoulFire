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

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/// Data node that filters bots by name pattern.
/// Input: bots (List of BotConnection), pattern (regex string)
/// Output: bots (List of BotConnection matching pattern)
public final class FilterBotsNode extends AbstractScriptNode {
  public static final String TYPE = "data.filter_bots";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("pattern", NodeValue.ofString(".*"));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var botValues = getListInput(inputs, "bots");
    var patternStr = getStringInput(inputs, "pattern", ".*");

    Pattern pattern;
    try {
      pattern = Pattern.compile(patternStr);
    } catch (PatternSyntaxException e) {
      // If pattern is invalid, return empty list
      return completed(result("bots", List.of()));
    }

    var filtered = botValues.stream()
      .map(NodeValue::asBot)
      .filter(bot -> bot != null && pattern.matcher(bot.accountName()).matches())
      .map(NodeValue::ofBot)
      .toList();

    return completed(result("bots", filtered));
  }
}
