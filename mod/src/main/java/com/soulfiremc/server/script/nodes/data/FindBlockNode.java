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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that finds the nearest block of a specific type.
/// Input: blockType (string, e.g., "minecraft:diamond_ore")
/// Input: maxDistance (int, maximum search radius in blocks)
/// Outputs: found (boolean), x, y, z (position), distance (double)
public final class FindBlockNode extends AbstractScriptNode {
  public static final String TYPE = "data.find_block";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("blockType", "minecraft:diamond_ore", "maxDistance", 16);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var bot = context.requireBot();
    var level = bot.minecraft().level;
    var player = bot.minecraft().player;
    var blockTypeInput = getStringInput(inputs, "blockType", "minecraft:diamond_ore");
    var maxDistance = getIntInput(inputs, "maxDistance", 16);

    if (level == null || player == null) {
      return completed(notFoundResult());
    }

    // Parse block type
    var blockTypeId = blockTypeInput.contains(":") ? blockTypeInput : "minecraft:" + blockTypeInput;
    var blockType = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockTypeId));

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

  private Map<String, Object> notFoundResult() {
    return results(
      "found", false,
      "x", 0,
      "y", 0,
      "z", 0,
      "distance", -1.0
    );
  }
}
