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

import java.util.Map;

/// Data node that gets the biome at the bot's current position.
/// Outputs: biome (string identifier)
public final class GetBiomeNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_biome")
    .displayName("Get Biome")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("biome", "Biome", PortType.STRING, "Current biome identifier")
    )
    .description("Gets the biome at the bot's current position")
    .icon("trees")
    .color("#9C27B0")
    .addKeywords("biome", "terrain", "environment", "forest", "desert", "ocean")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var level = bot.minecraft().level;
    var player = bot.minecraft().player;

    if (level == null || player == null) {
      return completedMono(result("biome", "unknown"));
    }

    var biomeHolder = level.getBiome(player.blockPosition());
    var biomeKey = biomeHolder.unwrapKey();
    var biomeName = biomeKey.map(key -> key.identifier().toString()).orElse("unknown");

    return completedMono(result("biome", biomeName));
  }
}
