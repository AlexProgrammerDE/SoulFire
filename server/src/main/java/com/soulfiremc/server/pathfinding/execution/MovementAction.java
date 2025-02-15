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
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector2d;
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
  private int noJumpTicks = 0;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var clientEntity = connection.dataManager().localPlayer();
    var botPosition = clientEntity.pos();
    var level = connection.dataManager().currentLevel();

    var blockMeta = level.getBlockState(blockPosition);
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition.toVector3d(), blockMeta);
    if (MathHelper.isOutsideTolerance(botPosition.getY(), targetMiddleBlock.getY(), 0.25)) {
      // We want to be on the same Y level
      return false;
    } else {
      var halfDiagonal = clientEntity.getBoundingBox().diagonalXZLength() / 2;

      // Leave more space to allow falling
      var adjustedHalfDiagonal = halfDiagonal - 0.05;
      return botPosition.distance(targetMiddleBlock) < adjustedHalfDiagonal;
    }
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
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition.toVector3d(), blockMeta);

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

    connection.controlState().forward(true);

    var deltaMovementY = clientEntity.deltaMovement().getY();
    var deltaMovementXZ = VectorHelper.toVector2dXZ(clientEntity.deltaMovement());
    var lookAngleXZ = VectorHelper.toVector2dXZ(clientEntity.getLookAngle());
    var botPosition = clientEntity.pos();
    if (targetMiddleBlock.getY() - STEP_HEIGHT > botPosition.getY()
      && shouldJump()
      && DoubleMath.fuzzyEquals(deltaMovementY, -clientEntity.getEntityBaseGravity(), 0.1)
      && (deltaMovementXZ.equals(Vector2d.ZERO) || deltaMovementXZ.normalize().dot(lookAngleXZ.normalize()) > 0.8)
    ) {
      connection.controlState().jumping(true);
    }
  }

  private boolean shouldJump() {
    if (!walkFewTicksNoJump) {
      return true;
    }

    if (noJumpTicks < 2) {
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
