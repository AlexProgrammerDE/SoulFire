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

public class Shulker extends Mob {
  public Shulker(EntityType entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public boolean canBeCollidedWith() {
    return this.isAlive();
  }

  @Override
  public void push(Entity entity) {
  }

  @Override
  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    this.lerpSteps = 0;
    this.setPos(x, y, z);
    this.setRot(yRot, xRot);
  }
}
