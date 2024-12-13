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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ExperienceOrbEntity extends Entity {
  private final int expValue;

  public ExperienceOrbEntity(Level level, int expValue) {
    super(EntityType.EXPERIENCE_ORB, level);
    this.expValue = expValue;
  }

  @Override
  public Vector3i getBlockPosBelowThatAffectsMyMovement() {
    return this.getOnPos(0.999999F);
  }

  @Override
  protected double getDefaultGravity() {
    return 0.03;
  }
}
