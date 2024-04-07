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

import com.github.steveice10.mc.protocol.data.game.entity.object.ObjectData;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.bot.state.Level;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class RawEntity extends Entity {
  private final ObjectData data;

  public RawEntity(int entityId, UUID uuid, EntityType type, ObjectData data,
                   Level level,
                   double x, double y, double z,
                   float yaw, float pitch, float headYaw,
                   double motionX, double motionY, double motionZ) {
    super(entityId, uuid, type, level, x, y, z, yaw, pitch, headYaw, motionX, motionY, motionZ);
    this.data = data;
  }
}
