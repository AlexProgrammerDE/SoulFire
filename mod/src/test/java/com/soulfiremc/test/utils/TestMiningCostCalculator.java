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
package com.soulfiremc.test.utils;

import com.soulfiremc.server.pathfinding.cost.BlockMiningCosts;
import com.soulfiremc.server.pathfinding.cost.Costs;
import com.soulfiremc.server.pathfinding.cost.MiningCostCalculator;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.util.SFBlockHelpers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NonNull;

/// A test implementation of MiningCostCalculator that calculates mining costs
/// without requiring a real player entity. This uses Minecraft's block and tool
/// data directly to compute realistic mining times.
public final class TestMiningCostCalculator implements MiningCostCalculator {
  public static final TestMiningCostCalculator INSTANCE = new TestMiningCostCalculator();

  private TestMiningCostCalculator() {}

  @Override
  public @Nullable BlockMiningCosts calculateBlockBreakCost(@NonNull ProjectedInventory inventory, @NonNull BlockState blockState, double breakBlockPenalty) {
    // Check if block is unbreakable (like bedrock)
    var destroyTime = blockState.getBlock().defaultDestroyTime();
    if (destroyTime < 0) {
      return null; // Unbreakable block
    }

    var lowestMiningTicks = Integer.MAX_VALUE;
    ItemStack bestItem = null;
    var willDropUsableBlockItem = false;

    for (var slot : inventory.usableToolsAndEmpty()) {
      var miningResult = calculateMiningTicks(slot, blockState);
      if (miningResult.ticks() < lowestMiningTicks) {
        lowestMiningTicks = miningResult.ticks();
        bestItem = slot;
        willDropUsableBlockItem = miningResult.willDropUsableBlockItem();
      }
    }

    if (lowestMiningTicks == Integer.MAX_VALUE) {
      return null;
    }

    return new BlockMiningCosts(
      (lowestMiningTicks / Costs.TICKS_PER_BLOCK) + breakBlockPenalty,
      bestItem,
      willDropUsableBlockItem);
  }

  private MiningResult calculateMiningTicks(ItemStack itemStack, BlockState blockState) {
    var destroyTime = blockState.getBlock().defaultDestroyTime();

    // Instant break for blocks with 0 destroy time (air, etc.)
    if (destroyTime == 0) {
      return new MiningResult(0, false);
    }

    // Get speed multiplier from tool
    float speedMultiplier = 1.0F;
    Tool toolComponent = itemStack.getComponents().get(DataComponents.TOOL);
    if (toolComponent != null) {
      speedMultiplier = toolComponent.getMiningSpeed(blockState);
    }

    // Check if the block requires a specific tool to drop items
    boolean requiresCorrectTool = blockState.is(BlockTags.NEEDS_DIAMOND_TOOL)
      || blockState.is(BlockTags.NEEDS_IRON_TOOL)
      || blockState.is(BlockTags.NEEDS_STONE_TOOL);

    boolean hasCorrectTool = false;
    if (toolComponent != null) {
      // Check if the tool is correct for this block
      hasCorrectTool = toolComponent.isCorrectForDrops(blockState);
    }

    // If block needs a tool and we don't have it, we can still break it but won't get drops
    // For pathfinding purposes, we still consider it breakable
    boolean willDropUsableBlockItem = (!requiresCorrectTool || hasCorrectTool)
      && SFBlockHelpers.isUsableBlockItem(blockState.getBlock());

    // Calculate base damage per tick
    // This formula is derived from Minecraft's block breaking mechanics
    // Damage per tick = speedMultiplier / (destroyTime * 30) for blocks not requiring tools
    // Damage per tick = speedMultiplier / (destroyTime * 100) for blocks requiring tools but without correct tool
    float damage;
    if (requiresCorrectTool && !hasCorrectTool) {
      // Wrong tool or no tool for block that needs specific tool - much slower
      damage = speedMultiplier / (destroyTime * 100.0f);
    } else {
      // Correct tool or block doesn't need specific tool
      damage = speedMultiplier / (destroyTime * 30.0f);
    }

    // Special case: instant mine
    if (damage >= 1.0f) {
      return new MiningResult(0, willDropUsableBlockItem);
    }

    // Calculate ticks needed
    int ticks = (int) Math.ceil(1.0f / damage);
    return new MiningResult(ticks, willDropUsableBlockItem);
  }

  private record MiningResult(int ticks, boolean willDropUsableBlockItem) {}
}
