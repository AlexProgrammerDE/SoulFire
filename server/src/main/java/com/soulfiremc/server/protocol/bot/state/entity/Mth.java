package com.soulfiremc.server.protocol.bot.state.entity;


import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class Mth {
  private static final long UUID_VERSION = 61440L;
  private static final long UUID_VERSION_TYPE_4 = 16384L;
  private static final long UUID_VARIANT = -4611686018427387904L;
  private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
  public static final float PI = (float) Math.PI;
  public static final float HALF_PI = (float) (Math.PI / 2);
  public static final float TWO_PI = (float) (Math.PI * 2);
  public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
  public static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
  public static final float EPSILON = 1.0E-5F;
  public static final float SQRT_OF_TWO = sqrt(2.0F);
  private static final float SIN_SCALE = 10430.378F;
  public static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
  public static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
  public static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
  private static final float[] SIN = Util.make(new float[65536], fs -> {
    for (int ix = 0; ix < fs.length; ix++) {
      fs[ix] = (float)Math.sin((double)ix * Math.PI * 2.0 / 65536.0);
    }
  });
  private static final RandomSource RANDOM = RandomSource.createThreadSafe();
  private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
    0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
  };
  private static final double ONE_SIXTH = 0.16666666666666666;
  private static final int FRAC_EXP = 8;
  private static final int LUT_SIZE = 257;
  private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
  private static final double[] ASIN_TAB = new double[257];
  private static final double[] COS_TAB = new double[257];

  public static float sin(float value) {
    return SIN[(int)(value * 10430.378F) & 65535];
  }

  public static float cos(float value) {
    return SIN[(int)(value * 10430.378F + 16384.0F) & 65535];
  }

  public static float sqrt(float value) {
    return (float)Math.sqrt((double)value);
  }

  public static int floor(float value) {
    int i = (int)value;
    return value < (float)i ? i - 1 : i;
  }

  public static int floor(double value) {
    int i = (int)value;
    return value < (double)i ? i - 1 : i;
  }

  public static long lfloor(double value) {
    long l = (long)value;
    return value < (double)l ? l - 1L : l;
  }

  public static float abs(float value) {
    return Math.abs(value);
  }

  public static int abs(int value) {
    return Math.abs(value);
  }

  public static int ceil(float value) {
    int i = (int)value;
    return value > (float)i ? i + 1 : i;
  }

  public static int ceil(double value) {
    int i = (int)value;
    return value > (double)i ? i + 1 : i;
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

  public static double clampedLerp(double start, double end, double delta) {
    if (delta < 0.0) {
      return start;
    } else {
      return delta > 1.0 ? end : lerp(delta, start, end);
    }
  }

  public static float clampedLerp(float start, float end, float delta) {
    if (delta < 0.0F) {
      return start;
    } else {
      return delta > 1.0F ? end : lerp(delta, start, end);
    }
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

  public static int nextInt(RandomSource random, int minimum, int maximum) {
    return minimum >= maximum ? minimum : random.nextInt(maximum - minimum + 1) + minimum;
  }

  public static float nextFloat(RandomSource random, float minimum, float maximum) {
    return minimum >= maximum ? minimum : random.nextFloat() * (maximum - minimum) + minimum;
  }

  public static double nextDouble(RandomSource random, double minimum, double maximum) {
    return minimum >= maximum ? minimum : random.nextDouble() * (maximum - minimum) + minimum;
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

  public static int wrapDegrees(int angle) {
    int j = angle % 360;
    if (j >= 180) {
      j -= 360;
    }

    if (j < -180) {
      j += 360;
    }

    return j;
  }

  public static float wrapDegrees(float value) {
    float g = value % 360.0F;
    if (g >= 180.0F) {
      g -= 360.0F;
    }

    if (g < -180.0F) {
      g += 360.0F;
    }

    return g;
  }

  public static double wrapDegrees(double value) {
    double e = value % 360.0;
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
    float i = degreesDifference(rotationToAdjust, actualRotation);
    float j = clamp(i, -maxDifference, maxDifference);
    return actualRotation - j;
  }

  public static float approach(float value, float limit, float stepSize) {
    stepSize = abs(stepSize);
    return value < limit ? clamp(value + stepSize, value, limit) : clamp(value - stepSize, limit, value);
  }

  public static float approachDegrees(float angle, float limit, float stepSize) {
    float i = degreesDifference(angle, limit);
    return approach(angle, angle + i, stepSize);
  }

  public static int getInt(String value, int defaultValue) {
    return NumberUtils.toInt(value, defaultValue);
  }

  public static int smallestEncompassingPowerOfTwo(int value) {
    int j = value - 1;
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
    return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)value * 125613361L >> 27) & 31];
  }

  public static int log2(int value) {
    return ceillog2(value) - (isPowerOfTwo(value) ? 0 : 1);
  }

  public static int color(float r, float g, float b) {
    return FastColor.ARGB32.color(0, floor(r * 255.0F), floor(g * 255.0F), floor(b * 255.0F));
  }

  public static float frac(float number) {
    return number - (float)floor(number);
  }

  public static double frac(double number) {
    return number - (double)lfloor(number);
  }

  @Deprecated
  public static long getSeed(Vec3i pos) {
    return getSeed(pos.getX(), pos.getY(), pos.getZ());
  }

  @Deprecated
  public static long getSeed(int x, int y, int z) {
    long l = (long)(x * 3129871) ^ (long)z * 116129781L ^ (long)y;
    l = l * l * 42317861L + l * 11L;
    return l >> 16;
  }

  public static UUID createInsecureUUID(RandomSource random) {
    long l = random.nextLong() & -61441L | 16384L;
    long m = random.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
    return new UUID(l, m);
  }

  public static UUID createInsecureUUID() {
    return createInsecureUUID(RANDOM);
  }

  public static double inverseLerp(double delta, double start, double end) {
    return (delta - start) / (end - start);
  }

  public static float inverseLerp(float delta, float start, float end) {
    return (delta - start) / (end - start);
  }

  public static boolean rayIntersectsAABB(Vec3 start, Vec3 end, AABB boundingBox) {
    double d = (boundingBox.minX + boundingBox.maxX) * 0.5;
    double e = (boundingBox.maxX - boundingBox.minX) * 0.5;
    double f = start.x - d;
    if (Math.abs(f) > e && f * end.x >= 0.0) {
      return false;
    } else {
      double g = (boundingBox.minY + boundingBox.maxY) * 0.5;
      double h = (boundingBox.maxY - boundingBox.minY) * 0.5;
      double i = start.y - g;
      if (Math.abs(i) > h && i * end.y >= 0.0) {
        return false;
      } else {
        double j = (boundingBox.minZ + boundingBox.maxZ) * 0.5;
        double k = (boundingBox.maxZ - boundingBox.minZ) * 0.5;
        double l = start.z - j;
        if (Math.abs(l) > k && l * end.z >= 0.0) {
          return false;
        } else {
          double m = Math.abs(end.x);
          double n = Math.abs(end.y);
          double o = Math.abs(end.z);
          double p = end.y * l - end.z * i;
          if (Math.abs(p) > h * o + k * n) {
            return false;
          } else {
            p = end.z * f - end.x * l;
            if (Math.abs(p) > e * o + k * m) {
              return false;
            } else {
              p = end.x * i - end.y * f;
              return Math.abs(p) < e * n + h * m;
            }
          }
        }
      }
    }
  }

  public static double atan2(double y, double x) {
    double f = x * x + y * y;
    if (Double.isNaN(f)) {
      return Double.NaN;
    } else {
      boolean bl = y < 0.0;
      if (bl) {
        y = -y;
      }

      boolean bl2 = x < 0.0;
      if (bl2) {
        x = -x;
      }

      boolean bl3 = y > x;
      if (bl3) {
        double g = x;
        x = y;
        y = g;
      }

      double g = fastInvSqrt(f);
      x *= g;
      y *= g;
      double h = FRAC_BIAS + y;
      int i = (int)Double.doubleToRawLongBits(h);
      double j = ASIN_TAB[i];
      double k = COS_TAB[i];
      double l = h - FRAC_BIAS;
      double m = y * k - x * l;
      double n = (6.0 + m * m) * m * 0.16666666666666666;
      double o = j + n;
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

  @Deprecated
  public static double fastInvSqrt(double number) {
    double e = 0.5 * number;
    long l = Double.doubleToRawLongBits(number);
    l = 6910469410427058090L - (l >> 1);
    number = Double.longBitsToDouble(l);
    return number * (1.5 - e * number * number);
  }

  public static float fastInvCubeRoot(float number) {
    int i = Float.floatToIntBits(number);
    i = 1419967116 - i / 3;
    float g = Float.intBitsToFloat(i);
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
    int k = max - min;

    while (k > 0) {
      int l = k / 2;
      int m = min + l;
      if (isTargetBeforeOrAt.test(m)) {
        k = l;
      } else {
        min = m + 1;
        k -= l + 1;
      }
    }

    return min;
  }

  public static int lerpInt(float delta, int start, int end) {
    return start + floor(delta * (float)(end - start));
  }

  public static int lerpDiscrete(float delta, int start, int end) {
    int k = end - start;
    return start + floor(delta * (float)(k - 1)) + (delta > 0.0F ? 1 : 0);
  }

  public static float lerp(float delta, float start, float end) {
    return start + delta * (end - start);
  }

  public static double lerp(double delta, double start, double end) {
    return start + delta * (end - start);
  }

  public static double lerp2(double delta1, double delta2, double start1, double end1, double start2, double end2) {
    return lerp(delta2, lerp(delta1, start1, end1), lerp(delta1, start2, end2));
  }

  public static double lerp3(
    double delta1,
    double delta2,
    double delta3,
    double start1,
    double end1,
    double start2,
    double end2,
    double start3,
    double end3,
    double start4,
    double end4
  ) {
    return lerp(delta3, lerp2(delta1, delta2, start1, end1, start2, end2), lerp2(delta1, delta2, start3, end3, start4, end4));
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

  public static float rotLerp(float delta, float start, float end) {
    return start + delta * wrapDegrees(end - start);
  }

  public static double rotLerp(double delta, double start, double end) {
    return start + delta * wrapDegrees(end - start);
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

  public static double clampedMap(double input, double inputMin, double inputMax, double ouputMin, double outputMax) {
    return clampedLerp(ouputMin, outputMax, inverseLerp(input, inputMin, inputMax));
  }

  public static float clampedMap(float input, float inputMin, float inputMax, float outputMin, float outputMax) {
    return clampedLerp(outputMin, outputMax, inverseLerp(input, inputMin, inputMax));
  }

  public static double map(double input, double inputMin, double inputMax, double outputMin, double outputMax) {
    return lerp(inverseLerp(input, inputMin, inputMax), outputMin, outputMax);
  }

  public static float map(float input, float inputMin, float inputMax, float outputMin, float outputMax) {
    return lerp(inverseLerp(input, inputMin, inputMax), outputMin, outputMax);
  }

  public static double wobble(double input) {
    return input + (2.0 * RandomSource.create((long)floor(input * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
  }

  public static int roundToward(int value, int factor) {
    return positiveCeilDiv(value, factor) * factor;
  }

  public static int positiveCeilDiv(int x, int y) {
    return -Math.floorDiv(-x, y);
  }

  public static int randomBetweenInclusive(RandomSource random, int minInclusive, int maxInclusive) {
    return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
  }

  public static float randomBetween(RandomSource random, float minInclusive, float maxExclusive) {
    return random.nextFloat() * (maxExclusive - minInclusive) + minInclusive;
  }

  public static float normal(RandomSource random, float mean, float deviation) {
    return mean + (float)random.nextGaussian() * deviation;
  }

  public static double lengthSquared(double xDistance, double yDistance) {
    return xDistance * xDistance + yDistance * yDistance;
  }

  public static double length(double xDistance, double yDistance) {
    return Math.sqrt(lengthSquared(xDistance, yDistance));
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
    return floor(value / (double)factor) * factor;
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
        int m = Math.abs(input - l);
        return input - m >= lowerBound || input + m <= upperBound;
      }, m -> {
        boolean bl = m <= input;
        int n = Math.abs(input - m);
        boolean bl2 = input + n + steps <= upperBound;
        if (!bl || !bl2) {
          int o = input - n - (bl ? steps : 0);
          if (o >= lowerBound) {
            return o;
          }
        }

        return input + n + steps;
      }) : IntStream.empty();
    }
  }

  public static Quaternionf rotationAroundAxis(Vector3f axis, Quaternionf cameraOrentation, Quaternionf output) {
    float f = axis.dot(cameraOrentation.x, cameraOrentation.y, cameraOrentation.z);
    return output.set(axis.x * f, axis.y * f, axis.z * f, cameraOrentation.w).normalize();
  }

  public static int mulAndTruncate(Fraction fraction, int factor) {
    return fraction.getNumerator() * factor / fraction.getDenominator();
  }

  static {
    for (int i = 0; i < 257; i++) {
      double d = (double)i / 256.0;
      double e = Math.asin(d);
      COS_TAB[i] = Math.cos(e);
      ASIN_TAB[i] = e;
    }
  }
}

