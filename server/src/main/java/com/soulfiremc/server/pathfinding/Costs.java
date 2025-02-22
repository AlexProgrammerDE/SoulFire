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

import com.soulfiremc.server.data.*;
import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.util.SFBlockHelpers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * This class helps in calculating the costs of different actions. It is used in the pathfinding
 * algorithm to determine the best path to a goal.
 * The heuristic used is the distance in blocks. So getting from point A to point B is calculated
 * using the distance in blocks. The cost of breaking a block is calculated using the time it takes
 * in ticks to break a block and then converted to a relative heuristic.
 */
public final class Costs {
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
    TagsState tagsState,
    @Nullable LocalPlayer entity,
    ProjectedInventory inventory,
    BlockType blockType) {
    var lowestMiningTicks = Integer.MAX_VALUE;
    SFItemStack bestItem = null;
    var willDropUsableBlockItem = false;
    for (var slot : inventory.usableToolsAndNull()) {
      var miningTicks = getRequiredMiningTicks(tagsState, entity, true, slot, blockType);
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
    TagsState tagsState,
    @Nullable LocalPlayer entity,
    boolean onGround,
    @Nullable SFItemStack itemStack,
    BlockType blockType) {
    var correctToolUsed = isCorrectToolUsed(tagsState, itemStack, blockType);

    // If this value adds up over all ticks to 1, the block is fully mined
    var damage = getBlockDamagePerTick(tagsState, entity, onGround, itemStack, blockType);

    var creativeMode = entity != null && entity.abilitiesState().instabuild();
    var willDropUsableBlockItem = correctToolUsed && !creativeMode && SFBlockHelpers.isUsableBlockItem(blockType);

    // Insta mine
    if (damage >= 1) {
      return new TickResult(0, willDropUsableBlockItem);
    }

    return new TickResult((int) Math.ceil(1 / damage), willDropUsableBlockItem);
  }

  private static float getBlockDamagePerTick(TagsState tagsState,
                                             @Nullable LocalPlayer entity,
                                             boolean onGround,
                                             @Nullable SFItemStack itemStack,
                                             BlockType blockType) {
    if (entity != null && entity.abilitiesState().instabuild()) {
      // We instantly break any block in creative mode
      return 1.0F;
    }

    var blockDestroyTime = blockType.destroyTime();
    if (blockDestroyTime == -1.0F) {
      return 0.0F;
    } else {
      var currentToolDivision = isCorrectToolUsed(tagsState, itemStack, blockType) ? 30 : 100;
      return getPlayerBlockDamagePerTick(tagsState, entity, onGround, itemStack, blockType)
        / blockDestroyTime / (float) currentToolDivision;
    }
  }

  private static float getPlayerBlockDamagePerTick(TagsState tagsState,
                                                   @Nullable LocalPlayer entity,
                                                   boolean onGround,
                                                   @Nullable SFItemStack itemStack,
                                                   BlockType blockType) {
    var speedMultiplier = getSpeedMultiplier(tagsState, itemStack, blockType);

    if (entity != null) {
      if (speedMultiplier > 1) {
        speedMultiplier += (float) entity.attributeValue(AttributeType.MINING_EFFICIENCY);
      }

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

      speedMultiplier *= (float) entity.attributeValue(AttributeType.BLOCK_BREAK_SPEED);
      if (entity.isEyeInFluid(FluidTags.WATER)) {
        speedMultiplier *= (float) entity.attributeValue(AttributeType.SUBMERGED_MINING_SPEED);
      }
    }

    if (!onGround) {
      speedMultiplier /= 5.0F;
    }

    return speedMultiplier;
  }

  private static float getSpeedMultiplier(
    TagsState tagsState, SFItemStack itemStack, BlockType blockType) {
    if (itemStack == null) {
      return 1;
    }

    var tool = itemStack.getDataComponents().getOptional(DataComponentTypes.TOOL);
    if (tool.isEmpty()) {
      return 1;
    }

    for (var rule : tool.get().getRules()) {
      if (rule.getSpeed() != null && isInHolderSet(tagsState, rule.getBlocks(), blockType)) {
        return rule.getSpeed();
      }
    }

    return tool.get().getDefaultMiningSpeed();
  }

  private static boolean isCorrectToolUsed(TagsState tagsState, SFItemStack itemStack, BlockType blockType) {
    if (!blockType.requiresCorrectToolForDrops()) {
      return true;
    }

    if (itemStack == null) {
      return false;
    }

    var tool = itemStack.getDataComponents().getOptional(DataComponentTypes.TOOL);
    if (tool.isEmpty()) {
      return false;
    }

    for (var rule : tool.get().getRules()) {
      if (rule.getCorrectForDrops() != null && isInHolderSet(tagsState, rule.getBlocks(), blockType)) {
        return rule.getCorrectForDrops();
      }
    }

    return false;
  }

  private static boolean isInHolderSet(TagsState tagsState, HolderSet holderSet, BlockType blockType) {
    return Arrays.stream(holderSet.resolve(t -> tagsState.<BlockType>getValuesOfTag(TagKey.key(t, RegistryKeys.BLOCK))))
      .anyMatch(i -> i == blockType.id());
  }

  private static OptionalInt getDigSpeedAmplifier(EntityEffectState effectState) {
    var hasteEffect = effectState.getEffect(EffectType.DIG_SPEED);
    var conduitPowerEffect = effectState.getEffect(EffectType.CONDUIT_POWER);

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
    var miningFatigueEffect = effectState.getEffect(EffectType.DIG_SLOWDOWN);

    return miningFatigueEffect
      .map(effectData -> OptionalInt.of(effectData.amplifier()))
      .orElseGet(OptionalInt::empty);
  }

  public record BlockMiningCosts(
    double miningCost, @Nullable SFItemStack usedTool, boolean willDropUsableBlockItem) {}

  public record TickResult(int ticks, boolean willDropUsableBlockItem) {}
}
