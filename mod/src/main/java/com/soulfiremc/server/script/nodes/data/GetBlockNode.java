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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets block information at a specific position.
/// Inputs: x, y, z (block coordinates)
/// Outputs: blockId (string), isAir (boolean), isSolid (boolean)
public final class GetBlockNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_block")
    .displayName("Get Block")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("x", "X", PortType.NUMBER, "0", "Block X coordinate"),
      PortDefinition.inputWithDefault("y", "Y", PortType.NUMBER, "64", "Block Y coordinate"),
      PortDefinition.inputWithDefault("z", "Z", PortType.NUMBER, "0", "Block Z coordinate")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("blockId", "Block ID", PortType.STRING, "The block's ID"),
      PortDefinition.output("isAir", "Is Air", PortType.BOOLEAN, "Whether the block is air"),
      PortDefinition.output("isSolid", "Is Solid", PortType.BOOLEAN, "Whether the block is solid")
    )
    .description("Gets information about a block at a specific position")
    .icon("grid-3x3")
    .color("#9C27B0")
    .addKeywords("block", "get", "info", "type", "world")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @SuppressWarnings("deprecation")
  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var level = bot.minecraft().level;
    var x = getIntInput(inputs, "x", 0);
    var y = getIntInput(inputs, "y", 64);
    var z = getIntInput(inputs, "z", 0);

    if (level == null) {
      return completed(results("blockId", "minecraft:air", "isAir", true, "isSolid", false));
    }

    var blockPos = new BlockPos(x, y, z);
    var blockState = level.getBlockState(blockPos);
    var block = blockState.getBlock();
    var blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

    return completed(results(
      "blockId", blockId,
      "isAir", blockState.isAir(),
      "isSolid", blockState.isSolid()
    ));
  }
}
