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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/// Data node that filters bots by name pattern.
/// Input: bots (List of BotConnection), pattern (regex string)
/// Output: bots (List of BotConnection matching pattern)
public final class FilterBotsNode extends AbstractScriptNode {
  private static final Map<String, Pattern> PATTERN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
  private static final int MAX_CACHE_SIZE = 128;

  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.filter_bots")
    .displayName("Filter Bots")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.listInput("bots", "Bots", PortType.BOT, "List of bots to filter"),
      PortDefinition.inputWithDefault("pattern", "Pattern", PortType.STRING, "\".*\"", "Regex pattern to match bot names")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.listOutput("bots", "Bots", PortType.BOT, "Filtered list of bots")
    )
    .description("Filters a list of bots by name using a regex pattern")
    .icon("filter")
    .color("#9C27B0")
    .addKeywords("filter", "bots", "regex", "match", "name")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var botValues = getListInput(inputs, "bots");
    var patternStr = getStringInput(inputs, "pattern", ".*");

    var cached = PATTERN_CACHE.get(patternStr);
    if (cached == null) {
      try {
        cached = Pattern.compile(patternStr);
        if (PATTERN_CACHE.size() < MAX_CACHE_SIZE) {
          PATTERN_CACHE.put(patternStr, cached);
        }
      } catch (PatternSyntaxException _) {
        return completedMono(result("bots", List.of()));
      }
    }
    var pattern = cached;

    var filtered = botValues.stream()
      .map(NodeValue::asBot)
      .filter(bot -> bot != null && pattern.matcher(bot.accountName()).matches())
      .map(NodeValue::ofBot)
      .toList();

    return completedMono(result("bots", filtered));
  }
}
