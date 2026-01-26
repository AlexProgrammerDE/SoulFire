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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Trigger node that fires every game tick (20 times per second).
/// Outputs: bot (the bot that ticked), tickCount (ticks since script started)
public final class OnTickNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_tick")
    .displayName("On Tick")
    .category(NodeCategory.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that ticked"),
      PortDefinition.output("tickCount", "Tick Count", PortType.NUMBER, "Ticks since script started")
    )
    .isTrigger(true)
    .description("Fires every game tick (20 times per second) for each bot")
    .icon("clock")
    .color("#4CAF50")
    .addKeywords("tick", "update", "loop", "frame")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    // The bot and tick count are passed through the inputs from the trigger system
    var bot = getBotInput(inputs);
    var tickCount = getLongInput(inputs, "tickCount", 0L);

    // Output bot so it can be wired to downstream nodes
    return completed(results(
      "bot", bot,
      "tickCount", tickCount
    ));
  }
}
