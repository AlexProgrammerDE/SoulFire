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

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;

@Getter
@Setter
public class VecDeltaCodec {
  private Vector3d base = Vector3d.ZERO;

  public Vector3d decode(double x, double y, double z) {
    if (x == 0L && y == 0L && z == 0L) {
      return this.base;
    } else {
      var decodedX = x == 0L ? this.base.getX() : this.base.getX() + x;
      var decodedY = y == 0L ? this.base.getY() : this.base.getY() + y;
      var decodedZ = z == 0L ? this.base.getZ() : this.base.getZ() + z;
      return Vector3d.from(decodedX, decodedY, decodedZ);
    }
  }

  public Vector3d delta(Vector3d value) {
    return value.sub(this.base);
  }
}
