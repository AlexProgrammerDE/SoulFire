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
package com.soulfiremc.server.pathfinding.graph.constraint;

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.DiagonalCollisionCalculator;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.SFItemHelpers;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@RequiredArgsConstructor
public class PathConstraintImpl implements PathConstraint {
  private static final boolean ALLOW_BREAKING_UNDIGGABLE = Boolean.getBoolean("sf.pathfinding-allow-breaking-undiggable");
  private static final boolean DO_NOT_SQUEEZE_THROUGH_DIAGONALS = Boolean.getBoolean("sf.pathfinding-do-not-squeezing-through-diagonals");
  private static final boolean DO_NOT_AVOID_HARMFUL_ENTITIES = Boolean.getBoolean("sf.pathfinding-do-not-avoid-harmful-entities");
  private static final int MAX_CLOSE_TO_ENEMY_PENALTY = Integer.getInteger("sf.pathfinding-max-close-to-enemy-penalty", 50);
  @Nullable
  private final LocalPlayer entity;
  private final LevelHeightAccessor levelHeightAccessor;
  private final CachedLazyObject<List<EntityRangeData>> unfriendlyEntities = new CachedLazyObject<>(this::getUnfriendlyEntitiesExpensive, 10, TimeUnit.SECONDS);

  public PathConstraintImpl(BotConnection botConnection) {
    this(botConnection.minecraft().player, botConnection.minecraft().level);
  }

  @Override
  public boolean doUsableBlocksDecreaseWhenPlaced() {
    return entity == null || !entity.hasInfiniteMaterials();
  }

  @Override
  public boolean canBlocksDropWhenBroken() {
    return entity == null || !entity.preventsBlockDrops();
  }

  @Override
  public boolean canBreakBlocks() {
    return true;
  }

  @Override
  public boolean canPlaceBlocks() {
    return true;
  }

  @Override
  public boolean isPlaceable(ItemStack item) {
    return SFItemHelpers.isSafeFullBlockItem(item);
  }

  @Override
  public boolean isTool(ItemStack item) {
    return SFItemHelpers.isTool(item);
  }

  @Override
  public boolean isOutOfLevel(BlockState blockState, SFVec3i pos) {
    return blockState.getBlock() == Blocks.VOID_AIR && !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  @Override
  public boolean canBreakBlock(SFVec3i pos, BlockState blockState) {
    if (!canBreakBlocks()) {
      return false;
    } else if (levelHeightAccessor.isOutsideBuildHeight(pos.y)) {
      return false;
    } else if (ALLOW_BREAKING_UNDIGGABLE) {
      return true;
    } else {
      return SFBlockHelpers.isDiggable(blockState.getBlock());
    }
  }

  @Override
  public boolean canPlaceBlock(SFVec3i pos) {
    return canPlaceBlocks() && !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  @Override
  public boolean collidesWithAtEdge(DiagonalCollisionCalculator.CollisionData collisionData) {
    if (DO_NOT_SQUEEZE_THROUGH_DIAGONALS) {
      return SFBlockHelpers.COLLISION_SHAPE_NOT_EMPTY.get(collisionData.blockState());
    }

    if (SFBlockHelpers.COLLISION_SHAPE_EMPTY.get(collisionData.blockState())) {
      return false;
    }

    return DiagonalCollisionCalculator.collidesWith(collisionData);
  }

  @Override
  public GraphInstructions modifyAsNeeded(GraphInstructions instruction) {
    if (!DO_NOT_AVOID_HARMFUL_ENTITIES) {
      var addedPenalty = 0D;
      for (var entity : unfriendlyEntities.get()) {
        var followRange = entity.followRange;
        var distance = instruction.blockPosition().distance(entity.entityPosition);
        if (distance <= followRange) {
          addedPenalty += MAX_CLOSE_TO_ENEMY_PENALTY * (followRange - distance) / followRange;
        }
      }

      if (addedPenalty > 0) {
        instruction = instruction.withActionCost(instruction.actionCost() + addedPenalty);
      }
    }

    return instruction;
  }

  private List<EntityRangeData> getUnfriendlyEntitiesExpensive() {
    if (entity == null) {
      return List.of();
    }

    return StreamSupport.stream(((ClientLevel) entity.level()).entitiesForRendering().spliterator(), false)
      .filter(e -> e != entity)
      .filter(_ -> !entity.getType().getCategory().isFriendly())
      .flatMap(e -> e instanceof LivingEntity livingEntity
        && livingEntity.getAttributeValue(Attributes.FOLLOW_RANGE) > 0
        ? Stream.of(livingEntity) : Stream.empty())
      .map(e -> new EntityRangeData(
        e.getAttributeValue(Attributes.FOLLOW_RANGE),
        SFVec3i.fromInt(e.blockPosition())
      ))
      .toList();
  }

  private record EntityRangeData(double followRange, SFVec3i entityPosition) {}
}
