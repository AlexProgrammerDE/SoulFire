/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.pathfinding;

import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.util.SFBlockHelpers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class helps in calculating the costs of different actions. It is used in the pathfinding
 * algorithm to determine the best path to a goal.
 * The heuristic used is the distance in blocks. So getting from point A to point B is calculated
 * using the distance in blocks. The cost of breaking a block is calculated using the time it takes
 * in ticks to break a block and then converted to a relative heuristic.
 */
public final class Costs {
  public static final ThreadLocal<ItemStack> SELECTED_ITEM_MIXIN_OVERRIDE = new ThreadLocal<>();
  /**
   * The distance in blocks between two points that are directly next to each other.
   */
  public static final double STRAIGHT = 1;
  /**
   * The distance in blocks between two points that are diagonal to each other.
   * Calculated using the Pythagorean theorem.
   */
  public static final double DIAGONAL = Math.sqrt(2);
  /**
   * We don't want a bot that frequently tries to break blocks instead of walking around them.
   */
  public static final double BREAK_BLOCK_PENALTY = Integer.getInteger("sf.pathfinding-break-block-penalty", 2);
  /**
   * We don't want a bot that frequently tries to place blocks instead of finding smarter paths.
   */
  public static final double PLACE_BLOCK_PENALTY = Integer.getInteger("sf.pathfinding-place-block-penalty", 5);
  /**
   * A normal server runs at 20 ticks per second.
   */
  public static final double TICKS_PER_SECOND = 20;
  /**
   * Normal player walking speed in blocks per second.
   */
  public static final double BLOCKS_PER_SECOND = 4.317;
  /**
   * Multiply calculated ticks using this number to get a good relative heuristic.
   */
  public static final double TICKS_PER_BLOCK = TICKS_PER_SECOND / BLOCKS_PER_SECOND;
  /**
   * It takes ~9 ticks for a player to jump up, decelerate and then land one block higher.
   */
  public static final double JUMP_UP_BLOCK = 9 / TICKS_PER_BLOCK;
  /**
   * It takes ~8 ticks for a player to jump up, decelerate and then land on the same y level.
   */
  public static final double JUMP_LAND_GROUND = 12 / TICKS_PER_BLOCK;
  /**
   * When you jump a gap you roughly do a full jump and walk 2 blocks in front.
   */
  public static final double ONE_GAP_JUMP = JUMP_LAND_GROUND + STRAIGHT + STRAIGHT;
  /**
   * Falling 1 block takes ~5.63 ticks.
   */
  public static final double FALL_1 = 5.63 / TICKS_PER_BLOCK;
  /**
   * Falling 2 blocks takes ~7.79 ticks.
   */
  public static final double FALL_2 = 7.79 / TICKS_PER_BLOCK;
  /**
   * Falling 3 blocks takes ~9.48 ticks.
   */
  public static final double FALL_3 = 9.48 / TICKS_PER_BLOCK;
  /**
   * Sliding around a corner is roughly like walking two blocks.
   * That's why even through the distance from A to B diagonally is DIAGONAL, the cost is actually 2.
   * That is why we need to add 2 - DIAGONAL to the cost of sliding around a corner as that adds
   * up the cost to 2.
   */
  public static final double CORNER_SLIDE = 2 - DIAGONAL;

  private Costs() {}

  public static @Nullable BlockMiningCosts calculateBlockBreakCost(
    LocalPlayer entity,
    ProjectedInventory inventory,
    BlockState blockState) {
    var lowestMiningTicks = Integer.MAX_VALUE;
    ItemStack bestItem = null;
    var willDropUsableBlockItem = false;
    for (var slot : inventory.usableToolsAndEmpty()) {
      var miningTicks = getRequiredMiningTicks(entity, slot, blockState);
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
      (lowestMiningTicks / TICKS_PER_BLOCK) + BREAK_BLOCK_PENALTY, bestItem, willDropUsableBlockItem);
  }

  // Time in ticks
  public static TickResult getRequiredMiningTicks(
    LocalPlayer entity,
    ItemStack itemStack,
    BlockState blockState) {
    SELECTED_ITEM_MIXIN_OVERRIDE.set(itemStack);
    var correctToolUsed = entity.hasCorrectToolForDrops(blockState);
    // If this value adds up over all ticks to 1, the block is fully mined
    var damage = blockState.getDestroyProgress(entity, entity.level(), BlockPos.ZERO);
    SELECTED_ITEM_MIXIN_OVERRIDE.remove();

    var creativeMode = entity.getAbilities().instabuild;
    var willDropUsableBlockItem = correctToolUsed && !creativeMode && SFBlockHelpers.isUsableBlockItem(blockState.getBlock());

    // Insta mine
    if (damage >= 1) {
      return new TickResult(0, willDropUsableBlockItem);
    }

    return new TickResult((int) Math.ceil(1 / damage), willDropUsableBlockItem);
  }

  public record BlockMiningCosts(
    double miningCost, @NonNull ItemStack usedTool, boolean willDropUsableBlockItem) {}

  public record TickResult(int ticks, boolean willDropUsableBlockItem) {}
}
