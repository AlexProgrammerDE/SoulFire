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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that iterates over a list of bots.
/// Self-driving: uses runtime.executeDownstream() to iterate over bots,
/// outputting the current bot per iteration.
public final class ForEachBotNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.foreach_bot")
    .displayName("For Each Bot")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.listInput("bots", "Bots", PortType.BOT, "List of bots to iterate")
    )
    .addOutputs(
      PortDefinition.output("exec_loop", "Loop", PortType.EXEC, "Executes for each bot"),
      PortDefinition.output("exec_done", "Done", PortType.EXEC, "Executes when iteration completes"),
      PortDefinition.output("bot", "Bot", PortType.BOT, "Current bot in iteration"),
      PortDefinition.output("index", "Index", PortType.NUMBER, "Current bot index"),
      PortDefinition.output("count", "Count", PortType.NUMBER, "Total number of bots"),
      PortDefinition.output("isComplete", "Complete", PortType.BOOLEAN, "Whether iteration is done")
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
    return completed(results(
      "bot", botValues.isEmpty() ? null : botValues.getFirst().asBot(),
      "index", 0,
      "count", botValues.size(),
      "isComplete", botValues.isEmpty()
    ));
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var botValues = getListInput(inputs, "bots");
    var count = botValues.size();

    return Flux.range(0, count)
      .concatMap(i -> {
        var currentBot = botValues.get(i).asBot();
        return runtime.executeDownstream("exec_loop", Map.of(
          "bot", currentBot != null ? NodeValue.ofBot(currentBot) : NodeValue.ofNull(),
          "index", NodeValue.ofNumber(i),
          "count", NodeValue.ofNumber(count),
          "isComplete", NodeValue.ofBoolean(false)
        ));
      })
      .then(runtime.executeDownstream("exec_done", Map.of(
        "bot", NodeValue.ofNull(),
        "index", NodeValue.ofNumber(count),
        "count", NodeValue.ofNumber(count),
        "isComplete", NodeValue.ofBoolean(true)
      )))
      .thenReturn(results("index", count, "count", count, "isComplete", true));
  }
}
