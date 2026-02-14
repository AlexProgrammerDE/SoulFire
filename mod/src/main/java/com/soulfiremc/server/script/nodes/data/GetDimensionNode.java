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
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the bot's current dimension.
/// Outputs: dimension (string), isOverworld, isNether, isEnd
public final class GetDimensionNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_dimension")
    .displayName("Get Dimension")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("dimension", "Dimension", PortType.STRING, "Current dimension name"),
      PortDefinition.output("isOverworld", "Is Overworld", PortType.BOOLEAN, "True if in overworld"),
      PortDefinition.output("isNether", "Is Nether", PortType.BOOLEAN, "True if in nether"),
      PortDefinition.output("isEnd", "Is End", PortType.BOOLEAN, "True if in the end")
    )
    .description("Gets the bot's current dimension")
    .icon("globe")
    .color("#9C27B0")
    .addKeywords("dimension", "world", "overworld", "nether", "end")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var level = bot.minecraft().level;

    if (level == null) {
      return completed(results(
        "dimension", "unknown",
        "isOverworld", false,
        "isNether", false,
        "isEnd", false
      ));
    }

    var dimensionKey = level.dimension();
    var dimensionName = dimensionKey.identifier().toString();

    return completed(results(
      "dimension", dimensionName,
      "isOverworld", dimensionKey == Level.OVERWORLD,
      "isNether", dimensionKey == Level.NETHER,
      "isEnd", dimensionKey == Level.END
    ));
  }
}
