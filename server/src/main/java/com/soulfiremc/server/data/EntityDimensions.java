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
package com.soulfiremc.server.data;

import com.soulfiremc.server.util.mcstructs.AABB;
import org.cloudburstmc.math.vector.Vector3d;

public record EntityDimensions(
  float width,
  float height,
  float eyeHeight,
  boolean fixed
) {
  private static float defaultEyeHeight(float height) {
    return height * 0.85F;
  }

  public static EntityDimensions fixed(float width, float height) {
    return new EntityDimensions(width, height, defaultEyeHeight(height), true);
  }

  public static EntityDimensions scalable(float width, float height) {
    return new EntityDimensions(width, height, defaultEyeHeight(height), false);
  }

  public EntityDimensions scale(float scale) {
    return this.scale(scale, scale);
  }

  public EntityDimensions scale(float widthScale, float heightScale) {
    return this.fixed || (widthScale == 1 && heightScale == 1) ? this : new EntityDimensions(
      this.width * widthScale,
      this.height * heightScale,
      this.eyeHeight,
      false
    );
  }

  public AABB makeBoundingBox(Vector3d pos) {
    return new AABB(
      pos.getX() - this.width / 2,
      pos.getY(),
      pos.getZ() - this.width / 2,
      pos.getX() + this.width / 2,
      pos.getY() + this.height,
      pos.getZ() + this.width / 2
    );
  }

  public EntityDimensions withEyeHeight(float eyeHeight) {
    return new EntityDimensions(this.width, this.height, eyeHeight, this.fixed);
  }
}
