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
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.LevelHeightAccessor;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.SFItemHelpers;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@RequiredArgsConstructor
public class PathConstraint {
  private static final boolean ALLOW_BREAKING_UNDIGGABLE = Boolean.getBoolean("sf.pathfinding-allow-breaking-undiggable");
  private static final boolean DO_NOT_SQUEEZE_THROUGH_DIAGONALS = Boolean.getBoolean("sf.pathfinding-do-not-squeezing-through-diagonals");
  private static final boolean DO_NOT_AVOID_HARMFUL_ENTITIES = Boolean.getBoolean("sf.pathfinding-do-not-avoid-harmful-entities");
  private static final int MAX_CLOSE_TO_ENEMY_PENALTY = Integer.getInteger("sf.pathfinding-max-close-to-enemy-penalty", 50);
  private final LocalPlayer entity;
  private final LevelHeightAccessor levelHeightAccessor;
  private final CachedLazyObject<List<EntityRangeData>> unfriendlyEntities = new CachedLazyObject<>(this::getUnfriendlyEntitiesExpensive, 10, TimeUnit.SECONDS);

  public PathConstraint(BotConnection botConnection) {
    this(botConnection.dataManager().localPlayer(), botConnection.dataManager().currentLevel());
  }

  public boolean doUsableBlocksDecreaseWhenPlaced() {
    return entity == null || !entity.abilitiesState().instabuild();
  }

  public boolean isPlaceable(SFItemStack item) {
    return SFItemHelpers.isSafeFullBlockItem(item);
  }

  public boolean isTool(SFItemStack item) {
    return SFItemHelpers.isTool(item);
  }

  public boolean isOutOfLevel(BlockState blockState, SFVec3i pos) {
    return blockState.blockType() == BlockType.VOID_AIR && !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  public boolean canBreakBlockPos(SFVec3i pos) {
    return !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  public boolean canPlaceBlockPos(SFVec3i pos) {
    return !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  public boolean canBreakBlockType(BlockType blockType) {
    if (ALLOW_BREAKING_UNDIGGABLE) {
      return true;
    }

    return SFBlockHelpers.isDiggable(blockType);
  }

  public boolean collidesWithAtEdge(DiagonalCollisionCalculator.CollisionData collisionData) {
    if (DO_NOT_SQUEEZE_THROUGH_DIAGONALS) {
      return collisionData.blockState().collisionShape().hasCollisions();
    }

    if (collisionData.blockState().collisionShape().hasNoCollisions()) {
      return false;
    }

    return DiagonalCollisionCalculator.collidesWith(collisionData);
  }

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

    return entity.level().getEntities()
      .stream()
      .filter(e -> e != entity)
      .filter(e -> !entity.entityType().friendly())
      .filter(e -> e.entityType().defaultFollowRange() > 0)
      .map(e -> new EntityRangeData(
        e.entityType().defaultFollowRange(),
        SFVec3i.fromInt(e.blockPosition())
      ))
      .toList();
  }

  private record EntityRangeData(double followRange, SFVec3i entityPosition) {}
}
