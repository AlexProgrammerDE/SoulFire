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
package com.soulfiremc.server.script.nodes.trigger;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Trigger node that fires after entity physics each game tick.
/// Executes synchronously on the tick thread, so action nodes run immediately.
/// Use this for attack logic that needs up-to-date entity positions after movement.
/// Outputs: bot (the bot that ticked), tickCount (ticks since script started)
public final class OnPostEntityTickNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_post_entity_tick")
    .displayName("On Post Entity Tick")
    .category(CategoryRegistry.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that ticked"),
      PortDefinition.output("tickCount", "Tick Count", PortType.NUMBER, "Ticks since script started")
    )
    .isTrigger(true)
    .description("Fires after entity physics each tick (synchronous on tick thread)")
    .icon("clock")
    .color("#4CAF50")
    .addKeywords("tick", "update", "loop", "post", "entity", "after", "attack")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = getBotInput(inputs);
    var tickCount = getLongInput(inputs, "tickCount", 0L);

    return completedMono(results(
      "bot", bot,
      "tickCount", tickCount
    ));
  }
}
