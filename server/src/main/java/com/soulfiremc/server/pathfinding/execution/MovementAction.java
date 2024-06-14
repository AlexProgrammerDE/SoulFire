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

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;

@Slf4j
@RequiredArgsConstructor
public final class MovementAction implements WorldAction {
  private static final double STEP_HEIGHT = 0.6;
  private final SFVec3i blockPosition;
  // Corner jumps normally require you to stand closer to the block to jump
  private final boolean walkFewTicksNoJump;
  private boolean didLook = false;
  private boolean lockYaw = false;
  private int noJumpTicks = 0;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var clientEntity = connection.dataManager().clientEntity();
    var botPosition = clientEntity.pos();
    var level = connection.dataManager().currentLevel();

    var blockMeta = level.getBlockState(blockPosition);
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition.toVector3d(), blockMeta);
    if (MathHelper.isOutsideTolerance(botPosition.getY(), targetMiddleBlock.getY(), 0.2)) {
      // We want to be on the same Y level
      return false;
    } else {
      var halfDiagonal = clientEntity.boundingBox().diagonalXZLength() / 2;

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
    var clientEntity = connection.dataManager().clientEntity();
    clientEntity.controlState().resetAll();

    var level = connection.dataManager().currentLevel();

    var blockMeta = level.getBlockState(blockPosition);
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition.toVector3d(), blockMeta);

    var previousYaw = clientEntity.yaw();
    clientEntity.lookAt(RotationOrigin.EYES, targetMiddleBlock);
    clientEntity.pitch(0);
    var newYaw = clientEntity.yaw();

    var yawDifference = Math.abs(MathHelper.wrapDegrees(newYaw - previousYaw));

    // We should only set the yaw once to the server to prevent the bot looking weird due to
    // inaccuracy
    if (!didLook) {
      didLook = true;
    } else if (yawDifference > 5 || lockYaw) {
      lockYaw = true;
      clientEntity.lastYaw(newYaw);
    }

    clientEntity.controlState().forward(true);

    var botPosition = clientEntity.pos();
    if (targetMiddleBlock.getY() - STEP_HEIGHT > botPosition.getY()
      && shouldJump()) {
      clientEntity.controlState().jumping(true);
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
