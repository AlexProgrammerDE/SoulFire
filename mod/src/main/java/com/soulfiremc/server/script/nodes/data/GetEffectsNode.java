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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/// Data node that gets the bot's active potion effects.
/// Outputs: effectCount, effectNames (comma-separated list)
public final class GetEffectsNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_effects")
    .displayName("Get Effects")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("effectCount", "Count", PortType.NUMBER, "Number of active effects"),
      PortDefinition.output("effectNames", "Effects", PortType.STRING, "Comma-separated effect names"),
      PortDefinition.output("hasEffects", "Has Effects", PortType.BOOLEAN, "True if any effects are active")
    )
    .description("Gets the bot's active potion effects")
    .icon("flask-conical")
    .color("#9C27B0")
    .addKeywords("effects", "potion", "buff", "debuff", "status")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completedMono(results(
        "effectCount", 0,
        "effectNames", "",
        "hasEffects", false
      ));
    }

    var effects = player.getActiveEffects();
    var effectNames = effects.stream()
      .map(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()))
      .filter(Objects::nonNull)
      .map(Identifier::getPath)
      .toList();

    return completedMono(results(
      "effectCount", effects.size(),
      "effectNames", String.join(", ", effectNames),
      "hasEffects", !effects.isEmpty()
    ));
  }
}
