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
package com.soulfiremc.server.util.structs;

import com.soulfiremc.server.util.mcstructs.AABB;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.List;

public record Segment(Vector3d startPoint, Vector3d endPoint) {
  public boolean intersects(List<AABB> boxes) {
    for (var box : boxes) {
      if (isInside(box)) { // if both points are inside the box, we can skip this box
        continue;
      }

      if (intersectsBox(box)) {
        return true;
      }
    }
    return false;
  }

  public boolean isInside(AABB box) {
    var originInside =
      startPoint.getX() >= box.minX
        && startPoint.getX() <= box.maxX
        && startPoint.getY() >= box.minY
        && startPoint.getY() <= box.maxY
        && startPoint.getZ() >= box.minZ
        && startPoint.getZ() <= box.maxZ;

    var directionInside =
      endPoint.getX() >= box.minX
        && endPoint.getX() <= box.maxX
        && endPoint.getY() >= box.minY
        && endPoint.getY() <= box.maxY
        && endPoint.getZ() >= box.minZ
        && endPoint.getZ() <= box.maxZ;

    return originInside && directionInside;
  }

  private boolean intersectsBox(AABB box) {
    var tmin = (box.minX - startPoint.getX()) / (endPoint.getX() - startPoint.getX());
    var tmax = (box.maxX - startPoint.getX()) / (endPoint.getX() - startPoint.getX());

    if (tmin > tmax) {
      var temp = tmin;
      tmin = tmax;
      tmax = temp;
    }

    var tymin = (box.minY - startPoint.getY()) / (endPoint.getY() - startPoint.getY());
    var tymax = (box.maxY - startPoint.getY()) / (endPoint.getY() - startPoint.getY());

    if (tymin > tymax) {
      var temp = tymin;
      tymin = tymax;
      tymax = temp;
    }

    if ((tmin > tymax) || (tymin > tmax)) {
      return false;
    }

    if (tymin > tmin) {
      tmin = tymin;
    }

    if (tymax < tmax) {
      tmax = tymax;
    }

    var tzmin = (box.minZ - startPoint.getZ()) / (endPoint.getZ() - startPoint.getZ());
    var tzmax = (box.maxZ - startPoint.getZ()) / (endPoint.getZ() - startPoint.getZ());

    if (tzmin > tzmax) {
      var temp = tzmin;
      tzmin = tzmax;
      tzmax = temp;
    }

    return (!(tmin > tzmax)) && (!(tzmin > tmax));
  }
}
