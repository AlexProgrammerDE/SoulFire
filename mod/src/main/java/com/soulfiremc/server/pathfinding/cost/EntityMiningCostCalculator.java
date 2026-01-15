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
package com.soulfiremc.server.pathfinding.cost;

import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;

public record EntityMiningCostCalculator(LocalPlayer player) implements MiningCostCalculator {
  @Override
  public @Nullable BlockMiningCosts calculateBlockBreakCost(ProjectedInventory inventory, BlockState blockState, double breakBlockPenalty) {
    var lowestMiningTicks = Integer.MAX_VALUE;
    ItemStack bestItem = null;
    var willDropUsableBlockItem = false;
    for (var slot : inventory.usableToolsAndEmpty()) {
      var miningTicks = Costs.getRequiredMiningTicks(player, slot, blockState);
      if (miningTicks.ticks() < lowestMiningTicks) {
        lowestMiningTicks = miningTicks.ticks();
        bestItem = slot;
        willDropUsableBlockItem = miningTicks.willDropUsableBlockItem();
      }
    }

    if (lowestMiningTicks == Integer.MAX_VALUE) {
      return null;
    }

    return new BlockMiningCosts(
      (lowestMiningTicks / Costs.TICKS_PER_BLOCK) + breakBlockPenalty, bestItem, willDropUsableBlockItem);
  }
}
