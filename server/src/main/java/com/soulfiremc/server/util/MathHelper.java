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

import java.util.stream.IntStream;

public class MathHelper {
  private MathHelper() {}

  /**
   * Returns the greatest integer less than or equal to the double argument.
   *
   * @param value A double
   * @return The greatest integer less than or equal to the double argument
   */
  public static int floorDouble(double value) {
    var i = (int) value;
    return value < (double) i ? i - 1 : i;
  }

  public static short shortClamp(short value, short min, short max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static double doubleClamp(double value, double min, double max) {
    if (value < min) {
      return min;
    } else {
      return Math.min(value, max);
    }
  }

  public static double lengthSquared(double x, double y, double z) {
    return x * x + y * y + z * z;
  }

  public static double square(double x) {
    return x * x;
  }

  public static int square(int x) {
    return x * x;
  }

  public static long getSeed(int i, int j, int k) {
    var l = (i * 3129871L) ^ (long) k * 116129781L ^ (long) j;
    l = l * l * 42317861L + l * 11L;
    return l >> 16;
  }

  public static float wrapDegrees(float value) {
    var mod = value % 360.0F;
    if (mod >= 180.0F) {
      mod -= 360.0F;
    }

    if (mod < -180.0F) {
      mod += 360.0F;
    }

    return mod;
  }

  public static boolean isOutsideTolerance(double a, double b, double tolerance) {
    return Math.abs(a - b) > tolerance;
  }

  /**
   * Sums two integers, returning Integer.MAX_VALUE if the sum overflows.
   *
   * @param a The first integer to sum
   * @param b The second integer to sum
   * @return The sum of the two integers, or Integer.MAX_VALUE if the sum overflows
   */
  public static int sumCapOverflow(int a, int b) {
    if (a > Integer.MAX_VALUE - b) {
      return Integer.MAX_VALUE;
    }

    return a + b;
  }

  /**
   * Sums the values of the stream, returning Integer.MAX_VALUE if the sum overflows.
   *
   * @param stream The stream to sum
   * @return The sum of the stream, or Integer.MAX_VALUE if the sum overflows
   */
  public static int sumCapOverflow(IntStream stream) {
    return stream.reduce(0, MathHelper::sumCapOverflow);
  }
}
