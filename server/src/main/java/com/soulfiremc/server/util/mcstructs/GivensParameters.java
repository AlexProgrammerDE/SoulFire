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

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public record GivensParameters(float sinHalf, float cosHalf) {
  public static GivensParameters fromUnnormalized(float sinHalf, float cosHalf) {
    var h = org.joml.Math.invsqrt(sinHalf * sinHalf + cosHalf * cosHalf);
    return new GivensParameters(h * sinHalf, h * cosHalf);
  }

  public static GivensParameters fromPositiveAngle(float angle) {
    var sin = org.joml.Math.sin(angle / 2.0F);
    var cosFromSin = Math.cosFromSin(sin, angle / 2.0F);
    return new GivensParameters(sin, cosFromSin);
  }

  public GivensParameters inverse() {
    return new GivensParameters(-this.sinHalf, this.cosHalf);
  }

  public Quaternionf aroundX(Quaternionf quaternion) {
    return quaternion.set(this.sinHalf, 0.0F, 0.0F, this.cosHalf);
  }

  public Quaternionf aroundY(Quaternionf quaternion) {
    return quaternion.set(0.0F, this.sinHalf, 0.0F, this.cosHalf);
  }

  public Quaternionf aroundZ(Quaternionf quaternion) {
    return quaternion.set(0.0F, 0.0F, this.sinHalf, this.cosHalf);
  }

  public float cos() {
    return this.cosHalf * this.cosHalf - this.sinHalf * this.sinHalf;
  }

  public float sin() {
    return 2.0F * this.sinHalf * this.cosHalf;
  }

  public Matrix3f aroundX(Matrix3f matrix) {
    matrix.m01 = 0.0F;
    matrix.m02 = 0.0F;
    matrix.m10 = 0.0F;
    matrix.m20 = 0.0F;
    var cos = this.cos();
    var sin = this.sin();
    matrix.m11 = cos;
    matrix.m22 = cos;
    matrix.m12 = sin;
    matrix.m21 = -sin;
    matrix.m00 = 1.0F;
    return matrix;
  }

  public Matrix3f aroundY(Matrix3f matrix) {
    matrix.m01 = 0.0F;
    matrix.m10 = 0.0F;
    matrix.m12 = 0.0F;
    matrix.m21 = 0.0F;
    var cos = this.cos();
    var sin = this.sin();
    matrix.m00 = cos;
    matrix.m22 = cos;
    matrix.m02 = -sin;
    matrix.m20 = sin;
    matrix.m11 = 1.0F;
    return matrix;
  }

  public Matrix3f aroundZ(Matrix3f matrix) {
    matrix.m02 = 0.0F;
    matrix.m12 = 0.0F;
    matrix.m20 = 0.0F;
    matrix.m21 = 0.0F;
    var cos = this.cos();
    var sin = this.sin();
    matrix.m00 = cos;
    matrix.m11 = cos;
    matrix.m01 = sin;
    matrix.m10 = -sin;
    matrix.m22 = 1.0F;
    return matrix;
  }
}
