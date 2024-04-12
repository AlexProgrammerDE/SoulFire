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

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.EnchantmentType;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.data.ToolSpeedType;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.util.MathHelper;
import java.util.OptionalInt;
import org.jetbrains.annotations.Nullable;

/**
 * This class helps in calculating the costs of different actions. It is used in the pathfinding
 * algorithm to determine the best path to a goal.
 * The heuristic used is the distance in blocks. So getting from point A to point B is calculated
 * using the distance in blocks. The cost of breaking a block is calculated using the time it takes
 * in ticks to break a block and then converted to a relative heuristic.
 */
public class Costs {
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
  public static final double BREAK_BLOCK_ADDITION = 2;
  /**
   * We don't want a bot that frequently tries to place blocks instead of finding smarter paths.
   */
  public static final double PLACE_BLOCK = 5;
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
  public static final double TOWER_COST = JUMP_UP_BLOCK + PLACE_BLOCK;
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

  public static final BlockState AIR_BLOCK_STATE = BlockState.forDefaultBlockType(BlockType.AIR);

  /**
   * For performance reasons, we do not want to calculate new costs for every possible block placed.
   * This is the state every placed block on the graph has. This allows the inventory to just store
   * the number of blocks and tools instead of the actual items. Although this decreases the result
   * "quality" a bit, it is a good tradeoff for performance.
   */
  public static final BlockState SOLID_PLACED_BLOCK_STATE =
    BlockState.forDefaultBlockType(BlockType.STONE);

  private Costs() {}

  public static BlockMiningCosts calculateBlockBreakCost(
    TagsState tagsState, ProjectedInventory inventory, BlockType blockType) {
    var lowestMiningTicks = Integer.MAX_VALUE;
    SFItemStack bestItem = null;
    var correctToolUsed = false;
    for (var slot : inventory.usableToolsAndNull()) {
      var miningTicks = getRequiredMiningTicks(tagsState, null, null, true, slot, blockType);
      if (miningTicks.ticks() < lowestMiningTicks) {
        lowestMiningTicks = miningTicks.ticks();
        bestItem = slot;
        correctToolUsed = miningTicks.willDrop();
      }
    }

    if (lowestMiningTicks == Integer.MAX_VALUE) {
      // We would expect there is at least a cost to break a block without a tool
      throw new IllegalStateException("No way found to break block!");
    }

    return new BlockMiningCosts(
      (lowestMiningTicks / TICKS_PER_BLOCK) + BREAK_BLOCK_ADDITION, bestItem, correctToolUsed);
  }

  // Time in ticks
  public static TickResult getRequiredMiningTicks(
    TagsState tagsState,
    @Nullable ClientEntity entity,
    @Nullable PlayerInventoryContainer inventoryContainer,
    boolean onGround,
    @Nullable SFItemStack itemStack,
    BlockType blockType) {
    float speedMultiplier;
    if (itemStack == null) {
      speedMultiplier = 1;
    } else {
      speedMultiplier = ToolSpeedType.getBlockToolSpeed(tagsState, itemStack.type(), blockType);
    }

    if (itemStack != null && speedMultiplier > 1) {
      var efficiency = itemStack.getEnchantmentLevel(EnchantmentType.EFFICIENCY);
      if (efficiency > 0) {
        // Efficiency is capped at 255
        efficiency = MathHelper.shortClamp(efficiency, (short) 0, (short) 255);
        speedMultiplier += (float) (efficiency * efficiency + 1);
      }
    }

    if (entity != null) {
      var digSpeedAmplifier = getDigSpeedAmplifier(entity.effectState());
      if (digSpeedAmplifier.isPresent()) {
        speedMultiplier *= 1.0F + (float) (digSpeedAmplifier.getAsInt() + 1) * 0.2F;
      }

      var digSlowdownAmplifier = getDigSlowdownAmplifier(entity.effectState());
      if (digSlowdownAmplifier.isPresent()) {
        speedMultiplier *=
          switch (digSlowdownAmplifier.getAsInt()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
          };
      }

      if (inventoryContainer != null && entity.isEyeInFluid(FluidTags.WATER)
        && !inventoryContainer.hasEnchantment(EnchantmentType.AQUA_AFFINITY)) {
        speedMultiplier /= 5.0F;
      }
    }

    if (!onGround) {
      speedMultiplier /= 5.0F;
    }

    var damage = speedMultiplier / blockType.destroyTime();

    var correctToolUsed =
      isCorrectToolUsed(tagsState, itemStack == null ? null : itemStack.type(), blockType);
    damage /= correctToolUsed ? 30 : 100;

    // Insta mine
    if (damage > 1) {
      return new TickResult(0, correctToolUsed);
    }

    return new TickResult((int) Math.ceil(1 / damage), correctToolUsed);
  }

  private static boolean isCorrectToolUsed(
    TagsState tagsState, ItemType itemType, BlockType blockType) {
    if (!blockType.requiresCorrectToolForDrops()) {
      return true;
    }

    if (itemType == null) {
      return false;
    }

    return ToolSpeedType.isRightToolFor(tagsState, itemType, blockType);
  }

  private static OptionalInt getDigSpeedAmplifier(EntityEffectState effectState) {
    var hasteEffect = effectState.getEffect(Effect.HASTE);
    var conduitPowerEffect = effectState.getEffect(Effect.CONDUIT_POWER);

    if (hasteEffect.isPresent() && conduitPowerEffect.isPresent()) {
      return OptionalInt.of(
        Math.max(hasteEffect.get().amplifier(), conduitPowerEffect.get().amplifier()));
    } else {
      return hasteEffect
        .map(effectData -> OptionalInt.of(effectData.amplifier()))
        .orElseGet(
          () ->
            conduitPowerEffect
              .map(effectData -> OptionalInt.of(effectData.amplifier()))
              .orElseGet(OptionalInt::empty));
    }
  }

  private static OptionalInt getDigSlowdownAmplifier(EntityEffectState effectState) {
    var miningFatigueEffect = effectState.getEffect(Effect.MINING_FATIGUE);

    return miningFatigueEffect
      .map(effectData -> OptionalInt.of(effectData.amplifier()))
      .orElseGet(OptionalInt::empty);
  }

  public record BlockMiningCosts(
    double miningCost, @Nullable SFItemStack usedTool, boolean willDrop) {}

  public record TickResult(int ticks, boolean willDrop) {}
}
