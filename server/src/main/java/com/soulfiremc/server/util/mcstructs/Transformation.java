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

import com.soulfiremc.server.util.MatrixUtil;
import com.soulfiremc.server.util.SFHelpers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

public final class Transformation {
  private static final Transformation IDENTITY = SFHelpers.make(() -> {
    var transformation = new Transformation(new Matrix4f());
    transformation.translation = new Vector3f();
    transformation.leftRotation = new Quaternionf();
    transformation.scale = new Vector3f(1.0F, 1.0F, 1.0F);
    transformation.rightRotation = new Quaternionf();
    transformation.decomposed = true;
    return transformation;
  });
  private final Matrix4f matrix;
  private boolean decomposed;
  @Nullable
  private Vector3f translation;
  @Nullable
  private Quaternionf leftRotation;
  @Nullable
  private Vector3f scale;
  @Nullable
  private Quaternionf rightRotation;

  public Transformation(@Nullable Matrix4f matrix4f) {
    this.matrix = Objects.requireNonNullElseGet(matrix4f, Matrix4f::new);
  }

  public Transformation(@Nullable Vector3f vector3f, @Nullable Quaternionf quaternionf, @Nullable Vector3f vector3f2, @Nullable Quaternionf quaternionf2) {
    this.matrix = compose(vector3f, quaternionf, vector3f2, quaternionf2);
    this.translation = vector3f != null ? vector3f : new Vector3f();
    this.leftRotation = quaternionf != null ? quaternionf : new Quaternionf();
    this.scale = vector3f2 != null ? vector3f2 : new Vector3f(1.0F, 1.0F, 1.0F);
    this.rightRotation = quaternionf2 != null ? quaternionf2 : new Quaternionf();
    this.decomposed = true;
  }

  public static Transformation identity() {
    return IDENTITY;
  }

  private static Matrix4f compose(
      @Nullable Vector3f translation, @Nullable Quaternionf leftRotation, @Nullable Vector3f scale, @Nullable Quaternionf rightRotation
  ) {
    var matrix4f = new Matrix4f();
    if (translation != null) {
      matrix4f.translation(translation);
    }

    if (leftRotation != null) {
      matrix4f.rotate(leftRotation);
    }

    if (scale != null) {
      matrix4f.scale(scale);
    }

    if (rightRotation != null) {
      matrix4f.rotate(rightRotation);
    }

    return matrix4f;
  }

  public Transformation compose(Transformation other) {
    var matrix4f = this.getMatrix();
    matrix4f.mul(other.getMatrix());
    return new Transformation(matrix4f);
  }

  @Nullable
  public Transformation inverse() {
    if (this == IDENTITY) {
      return this;
    } else {
      var matrix4f = this.getMatrix().invert();
      return matrix4f.isFinite() ? new Transformation(matrix4f) : null;
    }
  }

  private void ensureDecomposed() {
    if (!this.decomposed) {
      var f = 1.0F / this.matrix.m33();
      var triple = MatrixUtil.svdDecompose(new Matrix3f(this.matrix).scale(f));
      this.translation = this.matrix.getTranslation(new Vector3f()).mul(f);
      this.leftRotation = new Quaternionf(triple.left());
      this.scale = new Vector3f(triple.middle());
      this.rightRotation = new Quaternionf(triple.right());
      this.decomposed = true;
    }
  }

  public Matrix4f getMatrix() {
    return new Matrix4f(this.matrix);
  }

  public Vector3f getTranslation() {
    this.ensureDecomposed();
    return new Vector3f(this.translation);
  }

  public Quaternionf getLeftRotation() {
    this.ensureDecomposed();
    return new Quaternionf(this.leftRotation);
  }

  public Vector3f getScale() {
    this.ensureDecomposed();
    return new Vector3f(this.scale);
  }

  public Quaternionf getRightRotation() {
    this.ensureDecomposed();
    return new Quaternionf(this.rightRotation);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    } else if (object != null && this.getClass() == object.getClass()) {
      var lv = (Transformation) object;
      return Objects.equals(this.matrix, lv.matrix);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.matrix);
  }

  public Transformation slerp(Transformation transformation, float delta) {
    var vector3f = this.getTranslation();
    var quaternionf = this.getLeftRotation();
    var vector3f2 = this.getScale();
    var quaternionf2 = this.getRightRotation();
    vector3f.lerp(transformation.getTranslation(), delta);
    quaternionf.slerp(transformation.getLeftRotation(), delta);
    vector3f2.lerp(transformation.getScale(), delta);
    quaternionf2.slerp(transformation.getRightRotation(), delta);
    return new Transformation(vector3f, quaternionf, vector3f2, quaternionf2);
  }
}
