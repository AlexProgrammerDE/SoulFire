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

import com.soulfiremc.server.util.mcstructs.GivensParameters;
import com.soulfiremc.server.util.structs.Triple;
import org.joml.*;
import org.joml.Math;

public class MatrixUtil {
  private static final float G = 3.0F + 2.0F * org.joml.Math.sqrt(2.0F);
  private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

  private MatrixUtil() {
  }

  public static Matrix4f mulComponentWise(Matrix4f matrix, float scalar) {
    return matrix.set(
      matrix.m00() * scalar,
      matrix.m01() * scalar,
      matrix.m02() * scalar,
      matrix.m03() * scalar,
      matrix.m10() * scalar,
      matrix.m11() * scalar,
      matrix.m12() * scalar,
      matrix.m13() * scalar,
      matrix.m20() * scalar,
      matrix.m21() * scalar,
      matrix.m22() * scalar,
      matrix.m23() * scalar,
      matrix.m30() * scalar,
      matrix.m31() * scalar,
      matrix.m32() * scalar,
      matrix.m33() * scalar
    );
  }

  private static GivensParameters approxGivensQuat(float topCorner, float oppositeDiagonalAverage, float bottomCorner) {
    var i = 2.0F * (topCorner - bottomCorner);
    return G * oppositeDiagonalAverage * oppositeDiagonalAverage < i * i ? GivensParameters.fromUnnormalized(oppositeDiagonalAverage, i) : PI_4;
  }

  private static GivensParameters qrGivensQuat(float input1, float input2) {
    var h = (float) java.lang.Math.hypot(input1, input2);
    var i = h > 1.0E-6F ? input2 : 0.0F;
    var j = org.joml.Math.abs(input1) + Math.max(h, 1.0E-6F);
    if (input1 < 0.0F) {
      var k = i;
      i = j;
      j = k;
    }

    return GivensParameters.fromUnnormalized(i, j);
  }

  private static void similarityTransform(Matrix3f input, Matrix3f tempStorage) {
    input.mul(tempStorage);
    tempStorage.transpose();
    tempStorage.mul(input);
    input.set(tempStorage);
  }

  private static void stepJacobi(Matrix3f input, Matrix3f tempStorage, Quaternionf resultEigenvector, Quaternionf resultEigenvalue) {
    if (input.m01 * input.m01 + input.m10 * input.m10 > 1.0E-6F) {
      var lv = approxGivensQuat(input.m00, 0.5F * (input.m01 + input.m10), input.m11);
      var quaternionf3 = lv.aroundZ(resultEigenvector);
      resultEigenvalue.mul(quaternionf3);
      lv.aroundZ(tempStorage);
      similarityTransform(input, tempStorage);
    }

    if (input.m02 * input.m02 + input.m20 * input.m20 > 1.0E-6F) {
      var lv = approxGivensQuat(input.m00, 0.5F * (input.m02 + input.m20), input.m22).inverse();
      var quaternionf3 = lv.aroundY(resultEigenvector);
      resultEigenvalue.mul(quaternionf3);
      lv.aroundY(tempStorage);
      similarityTransform(input, tempStorage);
    }

    if (input.m12 * input.m12 + input.m21 * input.m21 > 1.0E-6F) {
      var lv = approxGivensQuat(input.m11, 0.5F * (input.m12 + input.m21), input.m22);
      var quaternionf3 = lv.aroundX(resultEigenvector);
      resultEigenvalue.mul(quaternionf3);
      lv.aroundX(tempStorage);
      similarityTransform(input, tempStorage);
    }
  }

  public static Quaternionf eigenvalueJacobi(Matrix3f input, int iterations) {
    var quaternionf = new Quaternionf();
    var matrix3f2 = new Matrix3f();
    var quaternionf2 = new Quaternionf();

    for (var j = 0; j < iterations; j++) {
      stepJacobi(input, matrix3f2, quaternionf2, quaternionf);
    }

    quaternionf.normalize();
    return quaternionf;
  }

  public static Triple<Quaternionf, Vector3f, Quaternionf> svdDecompose(Matrix3f matrix) {
    var matrix3f2 = new Matrix3f(matrix);
    matrix3f2.transpose();
    matrix3f2.mul(matrix);
    var quaternionf = eigenvalueJacobi(matrix3f2, 5);
    var f = matrix3f2.m00;
    var g = matrix3f2.m11;
    var bl = (double) f < 1.0E-6;
    var bl2 = (double) g < 1.0E-6;
    var matrix3f4 = matrix.rotate(quaternionf);
    var quaternionf2 = new Quaternionf();
    var quaternionf3 = new Quaternionf();
    GivensParameters lv;
    if (bl) {
      lv = qrGivensQuat(matrix3f4.m11, -matrix3f4.m10);
    } else {
      lv = qrGivensQuat(matrix3f4.m00, matrix3f4.m01);
    }

    var quaternionf4 = lv.aroundZ(quaternionf3);
    var matrix3f5 = lv.aroundZ(matrix3f2);
    quaternionf2.mul(quaternionf4);
    matrix3f5.transpose().mul(matrix3f4);
    if (bl) {
      lv = qrGivensQuat(matrix3f5.m22, -matrix3f5.m20);
    } else {
      lv = qrGivensQuat(matrix3f5.m00, matrix3f5.m02);
    }

    lv = lv.inverse();
    var quaternionf5 = lv.aroundY(quaternionf3);
    var matrix3f6 = lv.aroundY(matrix3f4);
    quaternionf2.mul(quaternionf5);
    matrix3f6.transpose().mul(matrix3f5);
    if (bl2) {
      lv = qrGivensQuat(matrix3f6.m22, -matrix3f6.m21);
    } else {
      lv = qrGivensQuat(matrix3f6.m11, matrix3f6.m12);
    }

    var quaternionf6 = lv.aroundX(quaternionf3);
    var matrix3f7 = lv.aroundX(matrix3f5);
    quaternionf2.mul(quaternionf6);
    matrix3f7.transpose().mul(matrix3f6);
    var vector3f = new Vector3f(matrix3f7.m00, matrix3f7.m11, matrix3f7.m22);
    return Triple.of(quaternionf2, vector3f, quaternionf.conjugate());
  }

  public static boolean isIdentity(Matrix4f matrix) {
    return (matrix.properties() & 4) != 0;
  }

  public static boolean isPureTranslation(Matrix4f matrix) {
    return (matrix.properties() & 8) != 0;
  }

  public static boolean isOrthonormal(Matrix4f matrix) {
    return (matrix.properties() & 16) != 0;
  }
}
