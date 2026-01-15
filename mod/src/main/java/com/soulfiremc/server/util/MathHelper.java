/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util;

import java.util.stream.IntStream;

public final class MathHelper {
  private MathHelper() {}

  public static boolean haveSameSign(double a, double b) {
    return a * b >= 0;
  }

  public static boolean isOutsideTolerance(double a, double b, double tolerance) {
    return Math.abs(a - b) > tolerance;
  }

  /// Sums two integers, returning Integer.MAX_VALUE if the sum overflows.
  ///
  /// @param a The first integer to sum
  /// @param b The second integer to sum
  /// @return The sum of the two integers, or Integer.MAX_VALUE if the sum overflows
  public static int sumCapOverflow(int a, int b) {
    if (a > Integer.MAX_VALUE - b) {
      return Integer.MAX_VALUE;
    }

    return a + b;
  }

  /// Sums the values of the stream, returning Integer.MAX_VALUE if the sum overflows.
  ///
  /// @param stream The stream to sum
  /// @return The sum of the stream, or Integer.MAX_VALUE if the sum overflows
  public static int sumCapOverflow(IntStream stream) {
    return stream.reduce(0, MathHelper::sumCapOverflow);
  }

  public static float wrapDegrees(float value) {
    var g = value % 360.0F;
    if (g >= 180.0F) {
      g -= 360.0F;
    }

    if (g < -180.0F) {
      g += 360.0F;
    }

    return g;
  }
}
