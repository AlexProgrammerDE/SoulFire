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
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.protocol.bot.state.entity.LivingEntity;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import com.soulfiremc.server.script.api.entity.ScriptEntityAPI;
import com.soulfiremc.server.script.api.entity.ScriptLivingEntityAPI;
import com.soulfiremc.server.script.api.entity.ScriptPlayerAPI;
import org.graalvm.polyglot.HostAccess;

import java.util.List;

public record ScriptLevelAPI(Level level) {
  @HostAccess.Export
  public String getBlockAt(int x, int y, int z) {
    return level.getBlockState(x, y, z).blockType().key().toString();
  }

  @HostAccess.Export
  public List<ScriptEntityAPI> getEntities() {
    return level.entityTracker().getEntities().stream().map(e -> switch (e) {
      case Player player -> new ScriptPlayerAPI(player);
      case LivingEntity livingEntity -> new ScriptLivingEntityAPI(livingEntity);
      default -> new ScriptEntityAPI(e);
    }).toList();
  }
}
