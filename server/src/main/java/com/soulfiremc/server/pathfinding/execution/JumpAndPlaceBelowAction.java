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
import com.soulfiremc.server.protocol.bot.BotActionManager;
import com.soulfiremc.server.util.BlockTypeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

@Slf4j
@RequiredArgsConstructor
public final class JumpAndPlaceBelowAction implements WorldAction {
  private final SFVec3i blockPlacePosition;
  private final BotActionManager.BlockPlaceAgainstData blockPlaceAgainstData;
  private boolean putOnHotbar = false;
  private boolean finishedPlacing = false;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.dataManager().currentLevel();

    return BlockTypeHelper.isFullBlock(level.getBlockState(blockPlacePosition));
  }

  @Override
  public SFVec3i targetPosition(BotConnection connection) {
    return blockPlacePosition.add(0, 1, 0);
  }

  @Override
  public void tick(BotConnection connection) {
    var dataManager = connection.dataManager();
    var clientEntity = dataManager.clientEntity();
    dataManager.controlState().resetAll();

    if (!putOnHotbar) {
      if (ItemPlaceHelper.placeBestBlockInHand(dataManager)) {
        putOnHotbar = true;
      }

      return;
    }

    if (finishedPlacing) {
      return;
    }

    if (clientEntity.y() < blockPlacePosition.y + 1) {
      // Make sure we are so high that we can place the block
      dataManager.controlState().jumping(true);
      return;
    } else {
      dataManager.controlState().jumping(false);
    }

    connection.dataManager().botActionManager().placeBlock(Hand.MAIN_HAND, blockPlaceAgainstData);
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
