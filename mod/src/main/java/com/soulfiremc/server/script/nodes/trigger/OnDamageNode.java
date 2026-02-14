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

/// Trigger node that fires when the bot takes damage (health decreases).
/// Outputs: bot, amount, previousHealth, newHealth
public final class OnDamageNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_damage")
    .displayName("On Damage")
    .category(CategoryRegistry.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that took damage"),
      PortDefinition.output("amount", "Amount", PortType.NUMBER, "Amount of damage taken"),
      PortDefinition.output("previousHealth", "Previous Health", PortType.NUMBER, "Health before damage"),
      PortDefinition.output("newHealth", "New Health", PortType.NUMBER, "Health after damage")
    )
    .isTrigger(true)
    .description("Fires when the bot takes damage (health decreases)")
    .icon("heart-crack")
    .color("#4CAF50")
    .addKeywords("damage", "hurt", "health", "attack", "hit")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    // Trigger nodes pass through inputs from the trigger service
    var bot = getBotInput(inputs);
    var amount = getFloatInput(inputs, "amount", 0f);
    var previousHealth = getFloatInput(inputs, "previousHealth", 0f);
    var newHealth = getFloatInput(inputs, "newHealth", 0f);

    return completedMono(results(
      "bot", bot,
      "amount", amount,
      "previousHealth", previousHealth,
      "newHealth", newHealth
    ));
  }
}
