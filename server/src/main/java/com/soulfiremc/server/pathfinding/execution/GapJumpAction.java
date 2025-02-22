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
public final class GapJumpAction implements WorldAction {
  private final SFVec3i blockPosition;
  private boolean didLook = false;
  private boolean lockYRot = false;
  private int noJumpTicks = 0;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var clientEntity = connection.dataManager().localPlayer();
    var botPosition = clientEntity.pos();
    var level = connection.dataManager().currentLevel();

    var blockMeta = level.getBlockState(blockPosition);
    var targetMiddleBlock = VectorHelper.topMiddleOfBlock(blockPosition, blockMeta);
    if (MathHelper.isOutsideTolerance(botPosition.getY(), targetMiddleBlock.getY(), 0.2)) {
      // We want to be on the same Y level
      return false;
    } else {
      var distance = botPosition.distance(targetMiddleBlock);
      return distance <= 0.3;
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

    connection.controlState().forward(true);

    if (shouldJump()) {
      connection.controlState().jumping(true);
    }
  }

  private boolean shouldJump() {
    if (noJumpTicks < 1) {
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
    return "GapJumpAction -> " + blockPosition.formatXYZ();
  }
}
