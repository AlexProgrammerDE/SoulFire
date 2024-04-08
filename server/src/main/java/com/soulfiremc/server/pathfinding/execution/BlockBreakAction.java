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
package com.soulfiremc.server.pathfinding.execution;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.BlockTypeHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3d;

@Slf4j
@RequiredArgsConstructor
public final class BlockBreakAction implements WorldAction {
  @Getter
  private final SFVec3i blockPosition;
  private final SideHint sideHint;
  boolean finishedDigging = false;
  private boolean didLook = false;
  private boolean putInHand = false;
  private int remainingTicks = -1;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.sessionDataManager().currentLevel();

    return BlockTypeHelper.isEmptyBlock(level.getBlockStateAt(blockPosition).blockType());
  }

  @Override
  public void tick(BotConnection connection) {
    var sessionDataManager = connection.sessionDataManager();
    var clientEntity = sessionDataManager.clientEntity();
    sessionDataManager.controlState().resetAll();

    var level = sessionDataManager.currentLevel();
    if (!didLook) {
      didLook = true;
      var previousYaw = clientEntity.yaw();
      var previousPitch = clientEntity.pitch();
      clientEntity.lookAt(
        RotationOrigin.EYES,
        sideHint.getMiddleOfFace(blockPosition));
      if (previousPitch != clientEntity.pitch() || previousYaw != clientEntity.yaw()) {
        clientEntity.sendRot();
      }
    }

    if (!putInHand) {
      if (ItemPlaceHelper.placeBestToolInHand(sessionDataManager, blockPosition)) {
        putInHand = true;
      }

      return;
    }

    if (finishedDigging) {
      return;
    }

    if (remainingTicks == -1) {
      var optionalBlockType = level.getBlockStateAt(blockPosition).blockType();
      if (optionalBlockType == BlockType.VOID_AIR) {
        log.warn("Block at {} is not in view range!", blockPosition);
        return;
      }

      remainingTicks =
        Costs.getRequiredMiningTicks(
            sessionDataManager.tagsState(),
            sessionDataManager.clientEntity(),
            sessionDataManager.inventoryManager(),
            clientEntity.onGround(),
            sessionDataManager.inventoryManager().playerInventory().getHeldItem().item(),
            optionalBlockType)
          .ticks();
      sessionDataManager.botActionManager().sendStartBreakBlock(blockPosition.toVector3i(), sideHint.toDirection());
    } else if (--remainingTicks == 0) {
      sessionDataManager.botActionManager().sendEndBreakBlock(blockPosition.toVector3i(), sideHint.toDirection());
      finishedDigging = true;
    } else {
      sessionDataManager.botActionManager().sendBreakBlockAnimation();
    }
  }

  @Override
  public int getAllowedTicks() {
    // 20-seconds max to break a block
    return 20 * 20;
  }

  @Override
  public String toString() {
    return "BlockBreakAction -> " + blockPosition.formatXYZ();
  }

  public enum SideHint {
    TOP,
    BOTTOM,
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public Direction toDirection() {
      return switch (this) {
        case TOP -> Direction.UP;
        case BOTTOM -> Direction.DOWN;
        case NORTH -> Direction.NORTH;
        case SOUTH -> Direction.SOUTH;
        case EAST -> Direction.EAST;
        case WEST -> Direction.WEST;
      };
    }

    public Vector3d getMiddleOfFace(SFVec3i block) {
      return switch (this) {
        case TOP -> block.toVector3d().add(0.5, 1, 0.5);
        case BOTTOM -> block.toVector3d().add(0.5, 0, 0.5);
        case NORTH -> block.toVector3d().add(0.5, 0.5, 0);
        case SOUTH -> block.toVector3d().add(0.5, 0.5, 1);
        case EAST -> block.toVector3d().add(1, 0.5, 0.5);
        case WEST -> block.toVector3d().add(0, 0.5, 0.5);
      };
    }
  }
}
