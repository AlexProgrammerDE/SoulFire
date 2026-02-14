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

/// Trigger node that fires when the bot dies.
/// Outputs: bot (the bot that died), deathMessage (string)
public final class OnDeathNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_death")
    .displayName("On Death")
    .category(CategoryRegistry.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that died"),
      PortDefinition.output("deathMessage", "Death Message", PortType.STRING, "The death message")
    )
    .isTrigger(true)
    .description("Fires when the bot dies")
    .icon("skull")
    .color("#4CAF50")
    .addKeywords("death", "die", "dead", "killed", "respawn")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = getBotInput(inputs);
    var deathMessage = getStringInput(inputs, "deathMessage", "");

    // Output data so it can be wired to downstream nodes
    return completedMono(results(
      "bot", bot,
      "deathMessage", deathMessage
    ));
  }
}
