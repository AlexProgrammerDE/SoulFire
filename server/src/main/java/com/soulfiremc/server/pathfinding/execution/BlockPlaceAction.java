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
import com.soulfiremc.server.protocol.bot.MultiPlayerGameMode;
import com.soulfiremc.server.util.SFBlockHelpers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

@Slf4j
@RequiredArgsConstructor
public final class BlockPlaceAction implements WorldAction {
  @Getter
  private final SFVec3i blockPosition;
  private final MultiPlayerGameMode.BlockPlaceAgainstData blockPlaceAgainstData;
  private boolean putOnHotbar = false;
  private boolean finishedPlacing = false;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.dataManager().currentLevel();

    return SFBlockHelpers.isFullBlock(level.getBlockState(blockPosition));
  }

  @Override
  public SFVec3i targetPosition(BotConnection connection) {
    return SFVec3i.fromDouble(connection.dataManager().localPlayer().pos());
  }

  @Override
  public void tick(BotConnection connection) {
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
    return "BlockPlaceAction -> " + blockPosition.formatXYZ();
  }
}
