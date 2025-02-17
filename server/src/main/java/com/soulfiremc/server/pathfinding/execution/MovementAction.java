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

import com.google.common.math.DoubleMath;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;

@Slf4j
@RequiredArgsConstructor
public final class MovementAction implements WorldAction {
  private static final double STEP_HEIGHT = 0.6;
  private final SFVec3i blockPosition;
  // Corner jumps normally require you to stand closer to the block to jump
  private final boolean walkFewTicksNoJump;
  private boolean didLook = false;
  private boolean lockYRot = false;
  private boolean wasStill = false;
  private int noJumpTicks = 0;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var clientEntity = connection.dataManager().localPlayer();
    var botPosition = clientEntity.pos();
    var level = connection.dataManager().currentLevel();

    var blockMeta = level.getBlockState(blockPosition);
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition, blockMeta);
    if (MathHelper.isOutsideTolerance(botPosition.getY(), targetMiddleBlock.getY(), 0.25)) {
      // We want to be on the same Y level
      return false;
    } else {
      return isAtTargetXZ(clientEntity, botPosition, targetMiddleBlock);
    }
  }

  private boolean isAtTargetXZ(LocalPlayer clientEntity, Vector3d botPosition, Vector3d targetMiddleBlock) {
    var halfDiagonal = clientEntity.getBoundingBox().minXZ() / 2;

    // Leave more space to allow falling
    var adjustedHalfDiagonal = halfDiagonal - 0.1;
    return botPosition.distance(targetMiddleBlock) < adjustedHalfDiagonal;
  }

  @Override
  public SFVec3i targetPosition(BotConnection connection) {
    return blockPosition;
  }

  @Override
  public void tick(BotConnection connection) {
    var clientEntity = connection.dataManager().localPlayer();
    connection.controlState().resetAll();

    var level = connection.dataManager().currentLevel();

    var blockMeta = level.getBlockState(blockPosition);
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition, blockMeta);

    var previousYRot = clientEntity.yRot();
    clientEntity.lookAt(RotationOrigin.EYES, targetMiddleBlock);
    clientEntity.setXRot(0);
    var newYRot = clientEntity.yRot();

    var yRotDifference = Math.abs(MathHelper.wrapDegrees(newYRot - previousYRot));

    // We should only set the yRot once to the server to prevent the bot looking weird due to
    // inaccuracy
    if (!didLook) {
      didLook = true;
    } else if (yRotDifference > 5 || lockYRot) {
      lockYRot = true;
      clientEntity.lastYRot(newYRot);
    }

    var botPosition = clientEntity.pos();
    var needsJump = targetMiddleBlock.getY() - STEP_HEIGHT > botPosition.getY();
    if (needsJump) {
      // Make sure not to move if we have still other motion going on
      if (!wasStill) {
        var deltaMovementXZ = VectorHelper.toVector2dXZ(clientEntity.deltaMovement());
        var isBaseGravity = DoubleMath.fuzzyEquals(clientEntity.deltaMovement().getY(), -clientEntity.getEntityBaseGravity(), 0.1);
        var isStill = deltaMovementXZ.equals(Vector2d.ZERO);
        var isMovingRoughlyTowardsBlock = !deltaMovementXZ.equals(Vector2d.ZERO)
          && deltaMovementXZ.normalize().dot(VectorHelper.toVector2dXZ(targetMiddleBlock.sub(clientEntity.pos())).normalize()) > 0.8;
        if (isBaseGravity && (isStill || isMovingRoughlyTowardsBlock)) {
          wasStill = true;
        } else {
          return;
        }
      }

      if (shouldJump()) {
        connection.controlState().jumping(true);
      }
    }

    if (!isAtTargetXZ(clientEntity, botPosition, targetMiddleBlock)) {
      connection.controlState().forward(true);
    }
  }

  private boolean shouldJump() {
    if (!walkFewTicksNoJump) {
      return true;
    }

    if (noJumpTicks < 3) {
      noJumpTicks++;
      return false;
    } else {
      noJumpTicks = 0;
      return true;
    }
  }

  @Override
  public int getAllowedTicks() {
    // 5-seconds max to walk to a block
    return 5 * 20;
  }

  @Override
  public String toString() {
    return "MovementAction -> " + blockPosition.formatXYZ();
  }
}
