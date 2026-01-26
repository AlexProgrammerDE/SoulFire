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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the biome at the bot's current position.
/// Outputs: biome (string identifier)
public final class GetBiomeNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_biome")
    .displayName("Get Biome")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to get biome from")
    )
    .addOutputs(
      PortDefinition.output("biome", "Biome", PortType.STRING, "Current biome identifier")
    )
    .description("Gets the biome at the bot's current position")
    .icon("trees")
    .color("#9C27B0")
    .addKeywords("biome", "terrain", "environment", "forest", "desert", "ocean")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var level = bot.minecraft().level;
    var player = bot.minecraft().player;

    if (level == null || player == null) {
      return completed(result("biome", "unknown"));
    }

    var biomeHolder = level.getBiome(player.blockPosition());
    var biomeKey = biomeHolder.unwrapKey();
    var biomeName = biomeKey.map(key -> key.identifier().toString()).orElse("unknown");

    return completed(result("biome", biomeName));
  }
}
