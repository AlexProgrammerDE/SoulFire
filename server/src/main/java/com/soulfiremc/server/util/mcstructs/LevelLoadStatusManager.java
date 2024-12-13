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
package com.soulfiremc.server.util.mcstructs;

import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;

public class LevelLoadStatusManager {
  private final LocalPlayer player;
  private final Level level;
  private LevelLoadStatusManager.Status status = LevelLoadStatusManager.Status.WAITING_FOR_SERVER;

  public LevelLoadStatusManager(LocalPlayer player, Level level) {
    this.player = player;
    this.level = level;
  }

  public void tick() {
    switch (this.status) {
      case WAITING_FOR_PLAYER_CHUNK -> {
        var blockPos = this.player.blockPosition();
        var outsideHeight = this.level.isOutsideBuildHeight(blockPos.getY());
        if (outsideHeight || this.level.isChunkPositionLoaded(blockPos.getX(), blockPos.getZ()) || this.player.isSpectator() || !this.player.isAlive()) {
          this.status = LevelLoadStatusManager.Status.LEVEL_READY;
        }
      }
      case WAITING_FOR_SERVER, LEVEL_READY -> {
      }
    }
  }

  public boolean levelReady() {
    return this.status == LevelLoadStatusManager.Status.LEVEL_READY;
  }

  public void loadingPacketsReceived() {
    if (this.status == LevelLoadStatusManager.Status.WAITING_FOR_SERVER) {
      this.status = LevelLoadStatusManager.Status.WAITING_FOR_PLAYER_CHUNK;
    }
  }

  enum Status {
    WAITING_FOR_SERVER,
    WAITING_FOR_PLAYER_CHUNK,
    LEVEL_READY
  }
}
