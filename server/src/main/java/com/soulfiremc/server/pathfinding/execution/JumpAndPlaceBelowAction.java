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
import com.soulfiremc.server.protocol.bot.MultiPlayerGameMode;
import com.soulfiremc.server.util.SFBlockHelpers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

@Slf4j
@RequiredArgsConstructor
public final class JumpAndPlaceBelowAction implements WorldAction {
  private final SFVec3i blockPlacePosition;
  private final MultiPlayerGameMode.BlockPlaceAgainstData blockPlaceAgainstData;
  private boolean putOnHotbar = false;
  private boolean finishedPlacing = false;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.dataManager().currentLevel();

    return SFBlockHelpers.isFullBlock(level.getBlockState(blockPlacePosition));
  }

  @Override
  public SFVec3i targetPosition(BotConnection connection) {
    return blockPlacePosition.add(0, 1, 0);
  }

  @Override
  public void tick(BotConnection connection) {
    var dataManager = connection.dataManager();
    var clientEntity = dataManager.localPlayer();
    connection.controlState().resetAll();

    if (!putOnHotbar) {
      if (ItemPlaceHelper.placeBestBlockInHand(connection)) {
        putOnHotbar = true;
      }

      return;
    }

    if (finishedPlacing) {
      return;
    }

    var deltaMovement = clientEntity.deltaMovement();
    if (clientEntity.y() < blockPlacePosition.y + 1
      // Ensure we're roughly standing still
      && DoubleMath.fuzzyEquals(deltaMovement.getY(), -clientEntity.getEntityBaseGravity(), 0.1)
      // No X movement
      && deltaMovement.getX() == 0
      // No Z movement
      && deltaMovement.getZ() == 0) {
      // Make sure we are so high that we can place the block
      connection.controlState().jumping(true);
      return;
    } else {
      connection.controlState().jumping(false);
    }

    connection.dataManager().gameModeState().placeBlock(Hand.MAIN_HAND, blockPlaceAgainstData);
    finishedPlacing = true;
  }

  @Override
  public int getAllowedTicks() {
    // 3-seconds max to place a block
    return 3 * 20;
  }

  @Override
  public String toString() {
    return "JumpAndPlaceBelowAction -> " + blockPlacePosition.add(0, 1, 0).formatXYZ();
  }
}
