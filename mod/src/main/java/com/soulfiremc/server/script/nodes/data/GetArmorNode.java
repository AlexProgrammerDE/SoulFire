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
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the bot's armor information.
/// Outputs: armorValue, armorToughness
public final class GetArmorNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_armor")
    .displayName("Get Armor")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to get armor from")
    )
    .addOutputs(
      PortDefinition.output("armorValue", "Armor", PortType.NUMBER, "Total armor points"),
      PortDefinition.output("armorToughness", "Toughness", PortType.NUMBER, "Armor toughness value")
    )
    .description("Gets the bot's armor value and toughness")
    .icon("shield")
    .color("#9C27B0")
    .addKeywords("armor", "defence", "protection", "toughness")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completed(results("armorValue", 0.0, "armorToughness", 0.0));
    }

    return completed(results(
      "armorValue", player.getArmorValue(),
      "armorToughness", player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
    ));
  }
}
