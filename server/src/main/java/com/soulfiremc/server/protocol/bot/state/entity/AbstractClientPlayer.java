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
package com.soulfiremc.server.protocol.bot.state.entity;

import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.Level;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

@Getter
@Setter
public abstract class AbstractClientPlayer extends Player {
  private final BotConnection connection;
  private PlayerListEntry playerListEntry;

  public AbstractClientPlayer(BotConnection connection, Level level, GameProfile gameProfile) {
    super(level, level.levelData().spawnPos(), level.levelData().spawnAngle(), gameProfile);
    this.connection = connection;
  }

  @Override
  public boolean isSpectator() {
    return getPlayerListEntry().getGameMode() == GameMode.SPECTATOR;
  }

  @Override
  public boolean isCreative() {
    return getPlayerListEntry().getGameMode() == GameMode.CREATIVE;
  }

  private PlayerListEntry getPlayerListEntry() {
    if (playerListEntry == null) {
      playerListEntry = connection.getEntityProfile(uuid).orElseThrow();
    }

    return playerListEntry;
  }
}
