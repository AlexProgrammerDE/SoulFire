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

/// Data node that gets the bot's hunger/food information.
/// Outputs: foodLevel (0-20), saturation (float)
public final class GetHungerNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_hunger")
    .displayName("Get Hunger")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.botIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("foodLevel", "Food Level", PortType.NUMBER, "Current food level (0-20)"),
      PortDefinition.output("saturation", "Saturation", PortType.NUMBER, "Current saturation"),
      PortDefinition.output("needsFood", "Needs Food", PortType.BOOLEAN, "Whether the bot needs food (food level < 20)")
    )
    .description("Gets the bot's food level and saturation")
    .icon("utensils")
    .color("#9C27B0")
    .addKeywords("hunger", "food", "saturation", "eat")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completedMono(results("foodLevel", 20, "saturation", 5.0f, "needsFood", false));
    }

    var foodData = player.getFoodData();
    return completedMono(results(
      "foodLevel", foodData.getFoodLevel(),
      "saturation", foodData.getSaturationLevel(),
      "needsFood", foodData.needsFood()
    ));
  }
}
