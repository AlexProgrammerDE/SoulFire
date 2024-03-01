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
package com.soulfiremc.server.protocol.bot.movement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@AllArgsConstructor
@ToString
public class MutableVector3d {
  public double x;
  public double y;
  public double z;

  public Vector3d toImmutable() {
    return Vector3d.from(x, y, z);
  }

  public Vector3i toImmutableInt() {
    return Vector3i.from(x, y, z);
  }

  public MutableVector3d offset(double x, double v, double z) {
    return new MutableVector3d(this.x + x, this.y + v, this.z + z);
  }

  public MutableVector3d floored() {
    return new MutableVector3d(Math.floor(x), Math.floor(y), Math.floor(z));
  }

  public void add(Vector3i vector3i) {
    this.x += vector3i.getX();
    this.y += vector3i.getY();
    this.z += vector3i.getZ();
  }

  public void add(Vector3d vector3d) {
    this.x += vector3d.getX();
    this.y += vector3d.getY();
    this.z += vector3d.getZ();
  }

  public double norm() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  public MutableVector3d normalize() {
    var norm = norm();
    if (norm != 0) {
      x /= norm;
      y /= norm;
      z /= norm;
    }

    return this;
  }

  public void translate(int i, int i1, int i2) {
    x += i;
    y += i1;
    z += i2;
  }
}
