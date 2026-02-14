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

/// Data node that gets the bot's experience information.
/// Outputs: level, totalXp, xpProgress (0-1 progress to next level)
public final class GetExperienceNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_experience")
    .displayName("Get Experience")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("level", "Level", PortType.NUMBER, "Current experience level"),
      PortDefinition.output("totalXp", "Total XP", PortType.NUMBER, "Total experience points"),
      PortDefinition.output("xpProgress", "XP Progress", PortType.NUMBER, "Progress to next level (0-1)")
    )
    .description("Gets the bot's experience level and points")
    .icon("sparkles")
    .color("#9C27B0")
    .addKeywords("experience", "xp", "level", "points")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completedMono(results("level", 0, "totalXp", 0, "xpProgress", 0.0f));
    }

    return completedMono(results(
      "level", player.experienceLevel,
      "totalXp", player.totalExperience,
      "xpProgress", player.experienceProgress
    ));
  }
}
