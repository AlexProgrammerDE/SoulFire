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
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that finds the nearest block of a specific type.
/// Input: blockType (string, e.g., "minecraft:diamond_ore")
/// Input: maxDistance (int, maximum search radius in blocks)
/// Outputs: found (boolean), x, y, z (position), distance (double)
public final class FindBlockNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.find_block")
    .displayName("Find Block")
    .category(NodeCategory.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to search around"),
      PortDefinition.inputWithDefault("blockType", "Block Type", PortType.STRING, "\"minecraft:diamond_ore\"", "Block ID to search for"),
      PortDefinition.inputWithDefault("maxDistance", "Max Distance", PortType.NUMBER, "16", "Maximum search radius")
    )
    .addOutputs(
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether a block was found"),
      PortDefinition.output("x", "X", PortType.NUMBER, "Block X coordinate"),
      PortDefinition.output("y", "Y", PortType.NUMBER, "Block Y coordinate"),
      PortDefinition.output("z", "Z", PortType.NUMBER, "Block Z coordinate"),
      PortDefinition.output("distance", "Distance", PortType.NUMBER, "Distance to the block")
    )
    .description("Finds the nearest block of a specific type within range")
    .icon("cube")
    .color("#9C27B0")
    .addKeywords("find", "block", "search", "nearest", "ore", "locate")
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
    var blockTypeInput = getStringInput(inputs, "blockType", "minecraft:diamond_ore");
    var maxDistance = getIntInput(inputs, "maxDistance", 16);

    if (level == null || player == null) {
      return completed(notFoundResult());
    }

    // Parse block type
    var blockTypeId = blockTypeInput.contains(":") ? blockTypeInput : "minecraft:" + blockTypeInput;
    var blockType = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockTypeId));

    if (blockType == null) {
      return completed(notFoundResult());
    }

    var playerBlockPos = player.blockPosition();
    BlockPos nearestPos = null;
    double nearestDistSq = Double.MAX_VALUE;

    // Search in a cube around the player
    for (int dx = -maxDistance; dx <= maxDistance; dx++) {
      for (int dy = -maxDistance; dy <= maxDistance; dy++) {
        for (int dz = -maxDistance; dz <= maxDistance; dz++) {
          var checkPos = playerBlockPos.offset(dx, dy, dz);
          var blockState = level.getBlockState(checkPos);

          if (blockState.getBlock() == blockType) {
            var distSq = checkPos.distSqr(playerBlockPos);
            if (distSq < nearestDistSq) {
              nearestDistSq = distSq;
              nearestPos = checkPos;
            }
          }
        }
      }
    }

    if (nearestPos == null) {
      return completed(notFoundResult());
    }

    return completed(results(
      "found", true,
      "x", nearestPos.getX(),
      "y", nearestPos.getY(),
      "z", nearestPos.getZ(),
      "distance", Math.sqrt(nearestDistSq)
    ));
  }

  private Map<String, NodeValue> notFoundResult() {
    return results(
      "found", false,
      "x", 0,
      "y", 0,
      "z", 0,
      "distance", -1.0
    );
  }
}
