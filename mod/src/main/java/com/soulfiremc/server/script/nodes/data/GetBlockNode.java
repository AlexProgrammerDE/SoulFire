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
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets block information at a specific position.
/// Inputs: x, y, z (block coordinates)
/// Outputs: blockId (string), isAir (boolean), isSolid (boolean)
public final class GetBlockNode extends AbstractScriptNode {
  public static final String TYPE = "data.get_block";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("x", NodeValue.ofNumber(0), "y", NodeValue.ofNumber(64), "z", NodeValue.ofNumber(0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs, context);
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
