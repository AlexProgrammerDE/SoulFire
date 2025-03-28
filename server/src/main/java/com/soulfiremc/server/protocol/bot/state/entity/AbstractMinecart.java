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

import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.bot.state.Level;

public final class AbstractMinecart extends VehicleEntity {
  public AbstractMinecart(EntityType entityType, Level level) {
    super(entityType, level);
    this.blocksBuilding = true;
  }

  @Override
  public boolean canCollideWith(Entity entity) {
    return entity.canBeCollidedWith() || entity.isPushable();
  }

  @Override
  public boolean isPushable() {
    return true;
  }

  @Override
  public void push(Entity entity) {
  }

  public boolean isRideable() {
    return false;
  }
}
