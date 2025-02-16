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

import com.soulfiremc.server.util.mcstructs.AABB;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Locale;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class MathHelper {
  private static final float[] SIN = SFHelpers.make(new float[65536], fs -> {
    for (var ix = 0; ix < fs.length; ix++) {
      fs[ix] = (float) Math.sin((double) ix * Math.PI * 2.0 / 65536.0);
    }
  });
  private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
    0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
  };
  private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
  private static final double[] ASIN_TAB = new double[257];
  private static final double[] COS_TAB = new double[257];

  static {
    for (var i = 0; i < 257; i++) {
      var d = (double) i / 256.0;
      var e = Math.asin(d);
      COS_TAB[i] = Math.cos(e);
      ASIN_TAB[i] = e;
    }
  }

  private MathHelper() {}

  public static boolean haveSameSign(double a, double b) {
    return a * b >= 0;
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

  public static float sin(float value) {
    return SIN[(int) (value * 10430.378F) & 65535];
  }

  public static float cos(float value) {
    return SIN[(int) (value * 10430.378F + 16384.0F) & 65535];
  }

  public static float sqrt(float value) {
    return (float) Math.sqrt(value);
  }

  public static int floor(float value) {
    var i = (int) value;
    return value < (float) i ? i - 1 : i;
  }

  public static int floor(double value) {
    var i = (int) value;
    return value < (double) i ? i - 1 : i;
  }

  public static long lfloor(double value) {
    var l = (long) value;
    return value < (double) l ? l - 1L : l;
  }

  public static float abs(float value) {
    return Math.abs(value);
  }

  public static int abs(int value) {
    return Math.abs(value);
  }

  public static int ceil(float value) {
    var i = (int) value;
    return value > (float) i ? i + 1 : i;
  }

  public static int ceil(double value) {
    var i = (int) value;
    return value > (double) i ? i + 1 : i;
  }

  public static int clamp(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }

  public static long clamp(long value, long min, long max) {
    return Math.min(Math.max(value, min), max);
  }

  public static float clamp(float value, float min, float max) {
    return value < min ? min : Math.min(value, max);
  }

  public static double clamp(double value, double min, double max) {
    return value < min ? min : Math.min(value, max);
  }

  public static double absMax(double x, double y) {
    if (x < 0.0) {
      x = -x;
    }

    if (y < 0.0) {
      y = -y;
    }

    return Math.max(x, y);
  }

  public static int floorDiv(int dividend, int divisor) {
    return Math.floorDiv(dividend, divisor);
  }

  public static boolean equal(float x, float y) {
    return Math.abs(y - x) < 1.0E-5F;
  }

  public static boolean equal(double x, double y) {
    return Math.abs(y - x) < 1.0E-5F;
  }

  public static int positiveModulo(int x, int y) {
    return Math.floorMod(x, y);
  }

  public static float positiveModulo(float numerator, float denominator) {
    return (numerator % denominator + denominator) % denominator;
  }

  public static double positiveModulo(double numerator, double denominator) {
    return (numerator % denominator + denominator) % denominator;
  }

  public static boolean isMultipleOf(int number, int multiple) {
    return number % multiple == 0;
  }

  public static byte packDegrees(float f) {
    return (byte) floor(f * 256.0F / 360.0F);
  }

  public static float unpackDegrees(byte b) {
    return (float) (b * 360) / 256.0F;
  }

  public static int wrapDegrees(int angle) {
    var j = angle % 360;
    if (j >= 180) {
      j -= 360;
    }

    if (j < -180) {
      j += 360;
    }

    return j;
  }

  public static float wrapDegrees(long l) {
    var f = (float) (l % 360L);
    if (f >= 180.0F) {
      f -= 360.0F;
    }

    if (f < -180.0F) {
      f += 360.0F;
    }

    return f;
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

  public static double wrapDegrees(double value) {
    var e = value % 360.0;
    if (e >= 180.0) {
      e -= 360.0;
    }

    if (e < -180.0) {
      e += 360.0;
    }

    return e;
  }

  public static float degreesDifference(float start, float end) {
    return wrapDegrees(end - start);
  }

  public static float degreesDifferenceAbs(float start, float end) {
    return abs(degreesDifference(start, end));
  }

  public static float rotateIfNecessary(float rotationToAdjust, float actualRotation, float maxDifference) {
    var i = degreesDifference(rotationToAdjust, actualRotation);
    var j = clamp(i, -maxDifference, maxDifference);
    return actualRotation - j;
  }

  public static float approach(float value, float limit, float stepSize) {
    stepSize = abs(stepSize);
    return value < limit ? clamp(value + stepSize, value, limit) : clamp(value - stepSize, limit, value);
  }

  public static float approachDegrees(float angle, float limit, float stepSize) {
    var i = degreesDifference(angle, limit);
    return approach(angle, angle + i, stepSize);
  }

  public static int smallestEncompassingPowerOfTwo(int value) {
    var j = value - 1;
    j |= j >> 1;
    j |= j >> 2;
    j |= j >> 4;
    j |= j >> 8;
    j |= j >> 16;
    return j + 1;
  }

  public static boolean isPowerOfTwo(int value) {
    return value != 0 && (value & value - 1) == 0;
  }

  public static int ceillog2(int value) {
    value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
    return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) ((long) value * 125613361L >> 27) & 31];
  }

  public static int log2(int value) {
    return ceillog2(value) - (isPowerOfTwo(value) ? 0 : 1);
  }

  public static float frac(float number) {
    return number - (float) floor(number);
  }

  public static double frac(double number) {
    return number - (double) lfloor(number);
  }

  public static long getSeed(int x, int y, int z) {
    var l = (x * 3129871L) ^ (long) z * 116129781L ^ (long) y;
    l = l * l * 42317861L + l * 11L;
    return l >> 16;
  }

  public static double inverseLerp(double delta, double start, double end) {
    return (delta - start) / (end - start);
  }

  public static float inverseLerp(float delta, float start, float end) {
    return (delta - start) / (end - start);
  }

  public static boolean rayIntersectsAABB(Vector3d start, Vector3d end, AABB boundingBox) {
    var d = (boundingBox.minX + boundingBox.maxX) * 0.5;
    var e = (boundingBox.maxX - boundingBox.minX) * 0.5;
    var f = start.getX() - d;
    if (Math.abs(f) > e && f * end.getX() >= 0.0) {
      return false;
    } else {
      var g = (boundingBox.minY + boundingBox.maxY) * 0.5;
      var h = (boundingBox.maxY - boundingBox.minY) * 0.5;
      var i = start.getY() - g;
      if (Math.abs(i) > h && i * end.getY() >= 0.0) {
        return false;
      } else {
        var j = (boundingBox.minZ + boundingBox.maxZ) * 0.5;
        var k = (boundingBox.maxZ - boundingBox.minZ) * 0.5;
        var l = start.getZ() - j;
        if (Math.abs(l) > k && l * end.getZ() >= 0.0) {
          return false;
        } else {
          var m = Math.abs(end.getX());
          var n = Math.abs(end.getY());
          var o = Math.abs(end.getZ());
          var p = end.getY() * l - end.getZ() * i;
          if (Math.abs(p) > h * o + k * n) {
            return false;
          } else {
            p = end.getZ() * f - end.getX() * l;
            if (Math.abs(p) > e * o + k * m) {
              return false;
            } else {
              p = end.getX() * i - end.getY() * f;
              return Math.abs(p) < e * n + h * m;
            }
          }
        }
      }
    }
  }

  public static double atan2(double y, double x) {
    var f = x * x + y * y;
    if (Double.isNaN(f)) {
      return Double.NaN;
    } else {
      var bl = y < 0.0;
      if (bl) {
        y = -y;
      }

      var bl2 = x < 0.0;
      if (bl2) {
        x = -x;
      }

      var bl3 = y > x;
      if (bl3) {
        var g = x;
        x = y;
        y = g;
      }

      var g = fastInvSqrt(f);
      x *= g;
      y *= g;
      var h = FRAC_BIAS + y;
      var i = (int) Double.doubleToRawLongBits(h);
      var j = ASIN_TAB[i];
      var k = COS_TAB[i];
      var l = h - FRAC_BIAS;
      var m = y * k - x * l;
      var n = (6.0 + m * m) * m * 0.16666666666666666;
      var o = j + n;
      if (bl3) {
        o = (Math.PI / 2) - o;
      }

      if (bl2) {
        o = Math.PI - o;
      }

      if (bl) {
        o = -o;
      }

      return o;
    }
  }

  public static double fastInvSqrt(double number) {
    var e = 0.5 * number;
    var l = Double.doubleToRawLongBits(number);
    l = 6910469410427058090L - (l >> 1);
    number = Double.longBitsToDouble(l);
    return number * (1.5 - e * number * number);
  }

  public static float fastInvCubeRoot(float number) {
    var i = Float.floatToIntBits(number);
    i = 1419967116 - i / 3;
    var g = Float.intBitsToFloat(i);
    g = 0.6666667F * g + 1.0F / (3.0F * g * g * number);
    return 0.6666667F * g + 1.0F / (3.0F * g * g * number);
  }

  public static int murmurHash3Mixer(int input) {
    input ^= input >>> 16;
    input *= -2048144789;
    input ^= input >>> 13;
    input *= -1028477387;
    return input ^ input >>> 16;
  }

  public static int binarySearch(int min, int max, IntPredicate isTargetBeforeOrAt) {
    var k = max - min;

    while (k > 0) {
      var l = k / 2;
      var m = min + l;
      if (isTargetBeforeOrAt.test(m)) {
        k = l;
      } else {
        min = m + 1;
        k -= l + 1;
      }
    }

    return min;
  }

  public static float catmullrom(float delta, float controlPoint1, float controlPoint2, float controlPoint3, float controlPoint4) {
    return 0.5F
      * (
      2.0F * controlPoint2
        + (controlPoint3 - controlPoint1) * delta
        + (2.0F * controlPoint1 - 5.0F * controlPoint2 + 4.0F * controlPoint3 - controlPoint4) * delta * delta
        + (3.0F * controlPoint2 - controlPoint1 - 3.0F * controlPoint3 + controlPoint4) * delta * delta * delta
    );
  }

  public static double smoothstep(double input) {
    return input * input * input * (input * (input * 6.0 - 15.0) + 10.0);
  }

  public static double smoothstepDerivative(double input) {
    return 30.0 * input * input * (input - 1.0) * (input - 1.0);
  }

  public static int sign(double x) {
    if (x == 0.0) {
      return 0;
    } else {
      return x > 0.0 ? 1 : -1;
    }
  }

  public static float triangleWave(float input, float period) {
    return (Math.abs(input % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
  }

  public static float square(float value) {
    return value * value;
  }

  public static double square(double value) {
    return value * value;
  }

  public static int square(int value) {
    return value * value;
  }

  public static long square(long value) {
    return value * value;
  }

  public static double rotLerp(double delta, double start, double end) {
    return start + delta * wrapDegrees(end - start);
  }

  public static double lerp(double delta, double start, double end) {
    return start + delta * (end - start);
  }

  public static float lerp(float delta, float start, float end) {
    return start + delta * (end - start);
  }

  public static int roundToward(int value, int factor) {
    return positiveCeilDiv(value, factor) * factor;
  }

  public static int positiveCeilDiv(int x, int y) {
    return -Math.floorDiv(-x, y);
  }

  public static double lengthSquared(double xDistance, double yDistance) {
    return xDistance * xDistance + yDistance * yDistance;
  }

  public static double length(double xDistance, double yDistance) {
    return Math.sqrt(lengthSquared(xDistance, yDistance));
  }

  public static float length(float f, float g) {
    return (float) Math.sqrt(lengthSquared(f, g));
  }

  public static double lengthSquared(double xDistance, double yDistance, double zDistance) {
    return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
  }

  public static double length(double xDistance, double yDistance, double zDistance) {
    return Math.sqrt(lengthSquared(xDistance, yDistance, zDistance));
  }

  public static float lengthSquared(float xDistance, float yDistance, float zDistance) {
    return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
  }

  public static int quantize(double value, int factor) {
    return floor(value / (double) factor) * factor;
  }

  public static IntStream outFromOrigin(int input, int lowerBound, int upperBound) {
    return outFromOrigin(input, lowerBound, upperBound, 1);
  }

  public static IntStream outFromOrigin(int input, int lowerBound, int upperBound, int steps) {
    if (lowerBound > upperBound) {
      throw new IllegalArgumentException(String.format(Locale.ROOT, "upperbound %d expected to be > lowerBound %d", upperBound, lowerBound));
    } else if (steps < 1) {
      throw new IllegalArgumentException(String.format(Locale.ROOT, "steps expected to be >= 1, was %d", steps));
    } else {
      return input >= lowerBound && input <= upperBound ? IntStream.iterate(input, l -> {
        var m = Math.abs(input - l);
        return input - m >= lowerBound || input + m <= upperBound;
      }, m -> {
        var bl = m <= input;
        var n = Math.abs(input - m);
        var bl2 = input + n + steps <= upperBound;
        if (!bl || !bl2) {
          var o = input - n - (bl ? steps : 0);
          if (o >= lowerBound) {
            return o;
          }
        }

        return input + n + steps;
      }) : IntStream.empty();
    }
  }

  public static float easeInOutSine(float f) {
    return -(cos((float) Math.PI * f) - 1.0F) / 2.0F;
  }
}
