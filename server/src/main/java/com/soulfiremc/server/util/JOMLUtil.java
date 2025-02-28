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
package com.soulfiremc.server.util;

import org.cloudburstmc.math.vector.Vector4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class JOMLUtil {
  public static Quaternionf toQuaternion(Vector4f vector) {
    return new Quaternionf(vector.getX(), vector.getY(), vector.getZ(), vector.getW());
  }

  public static Vector3f fromCloudburst(org.cloudburstmc.math.vector.Vector3f vector) {
    return new Vector3f(vector.getX(), vector.getY(), vector.getZ());
  }
}
