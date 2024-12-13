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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.jetbrains.annotations.Nullable;

@Getter
public class GameModeState {
  private GameMode localPlayerMode = GameMode.SURVIVAL;
  private GameMode previousLocalPlayerMode;

  public void adjustPlayer(Player player) {
    player.abilitiesState().updatePlayerAbilities(localPlayerMode);
  }

  public void setLocalMode(LocalPlayer player, GameMode localPlayerMode, @Nullable GameMode previousLocalPlayerMode) {
    this.localPlayerMode = localPlayerMode;
    this.previousLocalPlayerMode = previousLocalPlayerMode;
    player.abilitiesState().updatePlayerAbilities(localPlayerMode);
  }

  public void setLocalMode(LocalPlayer player, GameMode type) {
    if (type != this.localPlayerMode) {
      this.previousLocalPlayerMode = this.localPlayerMode;
    }

    this.localPlayerMode = type;
    player.abilitiesState().updatePlayerAbilities(localPlayerMode);
  }
}
