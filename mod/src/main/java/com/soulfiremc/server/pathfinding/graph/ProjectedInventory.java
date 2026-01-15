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
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.pathfinding.cost.BlockMiningCosts;
import com.soulfiremc.server.pathfinding.cost.EntityMiningCostCalculator;
import com.soulfiremc.server.pathfinding.cost.MiningCostCalculator;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraint;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.structs.IDBooleanMap;
import com.soulfiremc.server.util.structs.IDMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.compress.utils.Lists;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/// An immutable representation of a player inventory. This takes an inventory and projects places/breaks
/// onto it. This way we calculate the way we can do actions after a block was broken/placed.
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public final class ProjectedInventory {
  @Getter
  @ToString.Include
  private final int usableBlockItems;
  @Getter
  @ToString.Include
  private final ItemStack[] usableToolsAndEmpty;
  private final IDMap<BlockState, BlockMiningCosts> sharedMiningCosts;
  private final IDBooleanMap<BlockState> stairsBlockToStandOn;

  public ProjectedInventory(Inventory playerInventory, LocalPlayer entity, PathConstraint pathConstraint) {
    this(
      Lists.newArrayList(playerInventory.iterator())
        .stream()
        .filter(item -> !item.isEmpty())
        .toList(),
      new EntityMiningCostCalculator(entity),
      pathConstraint);
  }

  public ProjectedInventory(List<ItemStack> items, MiningCostCalculator costCalculator, PathConstraint pathConstraint) {
    var blockItems = 0;
    var usableToolsAndEmpty = new HashSet<ItemStack>();

    // Empty slot
    usableToolsAndEmpty.add(ItemStack.EMPTY);

    for (var item : items) {
      if (pathConstraint.isPlaceable(item)) {
        blockItems += item.getCount();
      } else if (pathConstraint.isTool(item)) {
        usableToolsAndEmpty.add(item);
      }
    }

    this.usableBlockItems = blockItems;
    this.usableToolsAndEmpty = usableToolsAndEmpty.toArray(new ItemStack[0]);
    var breakBlockPenalty = pathConstraint.breakBlockPenalty();
    this.sharedMiningCosts = new IDMap<>(Block.BLOCK_STATE_REGISTRY,
      blockType -> costCalculator.calculateBlockBreakCost(this, blockType, breakBlockPenalty));
    this.stairsBlockToStandOn = new IDBooleanMap<>(Block.BLOCK_STATE_REGISTRY,
      state -> state.is(BlockTags.STAIRS) && !SFBlockHelpers.isHurtWhenStoodOn(state));
  }

  public BlockMiningCosts getMiningCosts(BlockState blockState) {
    return Objects.requireNonNull(sharedMiningCosts.get(blockState), "Block not destructible");
  }

  public boolean isStairsBlockToStandOn(BlockState blockState) {
    return stairsBlockToStandOn.get(blockState);
  }
}
