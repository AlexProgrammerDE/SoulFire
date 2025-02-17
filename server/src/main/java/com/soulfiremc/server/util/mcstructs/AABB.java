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

import com.soulfiremc.server.util.MathHelper;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class AABB {
  public static final double EPSILON = 1.0E-7;
  public final double minX;
  public final double minY;
  public final double minZ;
  public final double maxX;
  public final double maxY;
  public final double maxZ;

  public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    this.minX = Math.min(minX, maxX);
    this.minY = Math.min(minY, maxY);
    this.minZ = Math.min(minZ, maxZ);
    this.maxX = Math.max(minX, maxX);
    this.maxY = Math.max(minY, maxY);
    this.maxZ = Math.max(minZ, maxZ);
  }

  public AABB(Vector3i arg) {
    this(arg.getX(), arg.getY(), arg.getZ(), arg.getX() + 1, arg.getY() + 1, arg.getZ() + 1);
  }

  public AABB(Vector3d arg, Vector3d arg2) {
    this(arg.getX(), arg.getY(), arg.getZ(), arg2.getX(), arg2.getY(), arg2.getZ());
  }

  public static AABB unitCubeFromLowerCorner(Vector3d vector) {
    return new AABB(vector.getX(), vector.getY(), vector.getZ(), vector.getX() + 1.0, vector.getY() + 1.0, vector.getZ() + 1.0);
  }

  public static AABB encapsulatingFullBlocks(Vector3i startPos, Vector3i endPos) {
    return new AABB(
      Math.min(startPos.getX(), endPos.getX()),
      Math.min(startPos.getY(), endPos.getY()),
      Math.min(startPos.getZ(), endPos.getZ()),
      Math.max(startPos.getX(), endPos.getX()) + 1,
      Math.max(startPos.getY(), endPos.getY()) + 1,
      Math.max(startPos.getZ(), endPos.getZ()) + 1
    );
  }

  public static Optional<Vector3d> clip(double d, double e, double f, double g, double h, double i, Vector3d arg, Vector3d arg2) {
    var ds = new double[]{1.0};
    var j = arg2.getX() - arg.getX();
    var k = arg2.getY() - arg.getY();
    var l = arg2.getZ() - arg.getZ();
    var lv = getDirection(d, e, f, g, h, i, arg, ds, null, j, k, l);
    if (lv == null) {
      return Optional.empty();
    } else {
      var m = ds[0];
      return Optional.of(arg.add(m * j, m * k, m * l));
    }
  }

  @Nullable
  public static BlockHitResult clip(Iterable<AABB> boxes, Vector3d start, Vector3d end, Vector3i pos) {
    var ds = new double[]{1.0};
    Direction direction = null;
    var x = end.getX() - start.getX();
    var y = end.getY() - start.getY();
    var z = end.getZ() - start.getZ();

    for (var lv2 : boxes) {
      // Modification: Our AABBs are already absolute
      direction = getDirection(lv2, start, ds, direction, x, y, z);
    }

    if (direction == null) {
      return null;
    } else {
      var g = ds[0];
      return new BlockHitResult(start.add(g * x, g * y, g * z), direction, pos, false);
    }
  }

  @Nullable
  private static Direction getDirection(AABB aabb, Vector3d start, double[] minDistance, @Nullable Direction facing, double deltaX, double deltaY, double deltaZ) {
    return getDirection(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, start, minDistance, facing, deltaX, deltaY, deltaZ);
  }

  @Nullable
  private static Direction getDirection(
    double d, double e, double f, double g, double h, double i, Vector3d arg, double[] ds, @Nullable Direction arg2, double j, double k, double l
  ) {
    if (j > EPSILON) {
      arg2 = clipPoint(ds, arg2, j, k, l, d, e, h, f, i, Direction.WEST, arg.getX(), arg.getY(), arg.getZ());
    } else if (j < -EPSILON) {
      arg2 = clipPoint(ds, arg2, j, k, l, g, e, h, f, i, Direction.EAST, arg.getX(), arg.getY(), arg.getZ());
    }

    if (k > EPSILON) {
      arg2 = clipPoint(ds, arg2, k, l, j, e, f, i, d, g, Direction.DOWN, arg.getY(), arg.getZ(), arg.getX());
    } else if (k < -EPSILON) {
      arg2 = clipPoint(ds, arg2, k, l, j, h, f, i, d, g, Direction.UP, arg.getY(), arg.getZ(), arg.getX());
    }

    if (l > EPSILON) {
      arg2 = clipPoint(ds, arg2, l, j, k, f, d, g, e, h, Direction.NORTH, arg.getZ(), arg.getX(), arg.getY());
    } else if (l < -EPSILON) {
      arg2 = clipPoint(ds, arg2, l, j, k, i, d, g, e, h, Direction.SOUTH, arg.getZ(), arg.getX(), arg.getY());
    }

    return arg2;
  }

  @Nullable
  private static Direction clipPoint(
    double[] minDistance,
    @Nullable Direction prevDirection,
    double distanceSide,
    double distanceOtherA,
    double distanceOtherB,
    double minSide,
    double minOtherA,
    double maxOtherA,
    double minOtherB,
    double maxOtherB,
    Direction hitSide,
    double startSide,
    double startOtherA,
    double startOtherB
  ) {
    var o = (minSide - startSide) / distanceSide;
    var p = startOtherA + o * distanceOtherA;
    var q = startOtherB + o * distanceOtherB;
    if (0.0 < o && o < minDistance[0] && minOtherA - EPSILON < p && p < maxOtherA + EPSILON && minOtherB - EPSILON < q && q < maxOtherB + EPSILON) {
      minDistance[0] = o;
      return hitSide;
    } else {
      return prevDirection;
    }
  }

  public static AABB ofSize(Vector3d center, double xSize, double ySize, double zSize) {
    return new AABB(
      center.getX() - xSize / 2.0, center.getY() - ySize / 2.0, center.getZ() - zSize / 2.0, center.getX() + xSize / 2.0, center.getY() + ySize / 2.0, center.getZ() + zSize / 2.0
    );
  }

  public AABB setMinX(double minX) {
    return new AABB(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
  }

  public AABB setMinY(double minY) {
    return new AABB(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
  }

  public AABB setMinZ(double minZ) {
    return new AABB(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
  }

  public AABB setMaxX(double maxX) {
    return new AABB(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
  }

  public AABB setMaxY(double maxY) {
    return new AABB(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
  }

  public AABB setMaxZ(double maxZ) {
    return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
  }

  public double min(Direction.Axis axis) {
    return axis.choose(this.minX, this.minY, this.minZ);
  }

  public double max(Direction.Axis axis) {
    return axis.choose(this.maxX, this.maxY, this.maxZ);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    } else if (!(object instanceof AABB lv)) {
      return false;
    } else if (Double.compare(lv.minX, this.minX) != 0) {
      return false;
    } else if (Double.compare(lv.minY, this.minY) != 0) {
      return false;
    } else if (Double.compare(lv.minZ, this.minZ) != 0) {
      return false;
    } else if (Double.compare(lv.maxX, this.maxX) != 0) {
      return false;
    } else {
      return Double.compare(lv.maxY, this.maxY) == 0 && Double.compare(lv.maxZ, this.maxZ) == 0;
    }
  }

  @Override
  public int hashCode() {
    var i = Double.hashCode(this.minX);
    i = 31 * i + Double.hashCode(this.minY);
    i = 31 * i + Double.hashCode(this.minZ);
    i = 31 * i + Double.hashCode(this.maxX);
    i = 31 * i + Double.hashCode(this.maxY);
    return 31 * i + Double.hashCode(this.maxZ);
  }

  public AABB contract(double x, double y, double z) {
    var g = this.minX;
    var h = this.minY;
    var i = this.minZ;
    var j = this.maxX;
    var k = this.maxY;
    var l = this.maxZ;
    if (x < 0.0) {
      g -= x;
    } else if (x > 0.0) {
      j -= x;
    }

    if (y < 0.0) {
      h -= y;
    } else if (y > 0.0) {
      k -= y;
    }

    if (z < 0.0) {
      i -= z;
    } else if (z > 0.0) {
      l -= z;
    }

    return new AABB(g, h, i, j, k, l);
  }

  public AABB expandTowards(Vector3d vector) {
    return this.expandTowards(vector.getX(), vector.getY(), vector.getZ());
  }

  public AABB expandTowards(double x, double y, double z) {
    var g = this.minX;
    var h = this.minY;
    var i = this.minZ;
    var j = this.maxX;
    var k = this.maxY;
    var l = this.maxZ;
    if (x < 0.0) {
      g += x;
    } else if (x > 0.0) {
      j += x;
    }

    if (y < 0.0) {
      h += y;
    } else if (y > 0.0) {
      k += y;
    }

    if (z < 0.0) {
      i += z;
    } else if (z > 0.0) {
      l += z;
    }

    return new AABB(g, h, i, j, k, l);
  }

  public AABB inflate(double x, double y, double z) {
    var g = this.minX - x;
    var h = this.minY - y;
    var i = this.minZ - z;
    var j = this.maxX + x;
    var k = this.maxY + y;
    var l = this.maxZ + z;
    return new AABB(g, h, i, j, k, l);
  }

  public AABB inflate(double value) {
    return this.inflate(value, value, value);
  }

  public AABB intersect(AABB other) {
    var d = Math.max(this.minX, other.minX);
    var e = Math.max(this.minY, other.minY);
    var f = Math.max(this.minZ, other.minZ);
    var g = Math.min(this.maxX, other.maxX);
    var h = Math.min(this.maxY, other.maxY);
    var i = Math.min(this.maxZ, other.maxZ);
    return new AABB(d, e, f, g, h, i);
  }

  public AABB minmax(AABB other) {
    var d = Math.min(this.minX, other.minX);
    var e = Math.min(this.minY, other.minY);
    var f = Math.min(this.minZ, other.minZ);
    var g = Math.max(this.maxX, other.maxX);
    var h = Math.max(this.maxY, other.maxY);
    var i = Math.max(this.maxZ, other.maxZ);
    return new AABB(d, e, f, g, h, i);
  }

  public AABB move(double x, double y, double z) {
    return new AABB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
  }

  public AABB move(Vector3i pos) {
    return new AABB(
      this.minX + (double) pos.getX(),
      this.minY + (double) pos.getY(),
      this.minZ + (double) pos.getZ(),
      this.maxX + (double) pos.getX(),
      this.maxY + (double) pos.getY(),
      this.maxZ + (double) pos.getZ()
    );
  }

  public AABB move(Vector3d vec) {
    return this.move(vec.getX(), vec.getY(), vec.getZ());
  }

  public AABB move(Vector3f vec) {
    return this.move(vec.getX(), vec.getY(), vec.getZ());
  }

  public boolean intersects(AABB other) {
    return this.intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
  }

  public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
    return this.minX < x2 && this.maxX > x1 && this.minY < y2 && this.maxY > y1 && this.minZ < z2 && this.maxZ > z1;
  }

  public boolean intersects(Vector3d min, Vector3d max) {
    return this.intersects(
      Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()), Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ())
    );
  }

  public boolean contains(Vector3d vec) {
    return this.contains(vec.getX(), vec.getY(), vec.getZ());
  }

  public boolean contains(double x, double y, double z) {
    return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
  }

  public double getSize() {
    var d = this.getXsize();
    var e = this.getYsize();
    var f = this.getZsize();
    return (d + e + f) / 3.0;
  }

  public double getXsize() {
    return this.maxX - this.minX;
  }

  public double getYsize() {
    return this.maxY - this.minY;
  }

  public double getZsize() {
    return this.maxZ - this.minZ;
  }

  public AABB deflate(double x, double y, double z) {
    return this.inflate(-x, -y, -z);
  }

  public AABB deflate(double value) {
    return this.inflate(-value);
  }

  public Optional<Vector3d> clip(Vector3d from, Vector3d to) {
    return clip(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, from, to);
  }

  public boolean collidedAlongVector(Vector3d arg, List<AABB> list) {
    var lv = this.getCenter();
    var lv2 = lv.add(arg);

    for (var lv3 : list) {
      var lv4 = lv3.inflate(this.getXsize() * 0.5, this.getYsize() * 0.5, this.getZsize() * 0.5);
      if (lv4.contains(lv2) || lv4.contains(lv)) {
        return true;
      }

      if (lv4.clip(lv, lv2).isPresent()) {
        return true;
      }
    }

    return false;
  }

  public double distanceToSqr(Vector3d vec) {
    var d = Math.max(Math.max(this.minX - vec.getX(), vec.getX() - this.maxX), 0.0);
    var e = Math.max(Math.max(this.minY - vec.getY(), vec.getY() - this.maxY), 0.0);
    var f = Math.max(Math.max(this.minZ - vec.getZ(), vec.getZ() - this.maxZ), 0.0);
    return MathHelper.lengthSquared(d, e, f);
  }

  @Override
  public String toString() {
    return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
  }

  public boolean hasNaN() {
    return Double.isNaN(this.minX)
      || Double.isNaN(this.minY)
      || Double.isNaN(this.minZ)
      || Double.isNaN(this.maxX)
      || Double.isNaN(this.maxY)
      || Double.isNaN(this.maxZ);
  }

  public Vector3d getCenter() {
    return Vector3d.from(MathHelper.lerp(0.5, this.minX, this.maxX), MathHelper.lerp(0.5, this.minY, this.maxY), MathHelper.lerp(0.5, this.minZ, this.maxZ));
  }

  public Vector3d getBottomCenter() {
    return Vector3d.from(MathHelper.lerp(0.5, this.minX, this.maxX), this.minY, MathHelper.lerp(0.5, this.minZ, this.maxZ));
  }

  public Vector3d getMinPosition() {
    return Vector3d.from(this.minX, this.minY, this.minZ);
  }

  public Vector3d getMaxPosition() {
    return Vector3d.from(this.maxX, this.maxY, this.maxZ);
  }

  public boolean fullBlock() {
    return this.minX == 0 && this.minY == 0 && this.minZ == 0 && this.maxX == 1 && this.maxY == 1 && this.maxZ == 1;
  }

  public double minXZ() {
    var x = this.maxX - this.minX;
    var z = this.maxZ - this.minZ;
    return Math.min(x, z);
  }

  public boolean isFullBlockXZ() {
    return minX == 0 && minZ == 0 && maxX == 1 && maxZ == 1;
  }

  public boolean isEmpty() {
    return this.minX >= this.maxX || this.minY >= this.maxY || this.minZ >= this.maxZ;
  }

  public DoubleList getCoords(Direction.Axis axis) {
    return switch (axis) {
      case X -> DoubleList.of(this.minX, this.maxX);
      case Y -> DoubleList.of(this.minY, this.maxY);
      case Z -> DoubleList.of(this.minZ, this.maxZ);
    };
  }

  protected double get(Direction.Axis axis, int index) {
    return this.getCoords(axis).getDouble(index);
  }

  public int getSize(Direction.Axis axis) {
    return this.getCoords(axis).size() - 1;
  }

  protected int findIndex(Direction.Axis axis, double position) {
    return MathHelper.binarySearch(0, this.getSize(axis) + 1, value -> position < this.get(axis, value)) - 1;
  }

  public double collide(Direction.Axis movementAxis, AABB collisionBox, double desiredOffset) {
    return this.collideX(AxisCycle.between(movementAxis, Direction.Axis.X), collisionBox, desiredOffset);
  }

  public boolean isFullWide(AxisCycle axis, int x, int y, int z) {
    return this.isFullWide(axis.cycle(x, y, z, Direction.Axis.X), axis.cycle(x, y, z, Direction.Axis.Y), axis.cycle(x, y, z, Direction.Axis.Z));
  }

  public boolean isFullWide(int x, int y, int z) {
    if (x < 0 || y < 0 || z < 0) {
      return false;
    } else {
      return x < getSize(Direction.Axis.X) && y < getSize(Direction.Axis.Y) && z < getSize(Direction.Axis.Z) && this.isFull(x, y, z);
    }
  }

  public boolean isFull(int x, int y, int z) {
    return x == 0 && y == 0 && z == 0; // TODO: Think about is this is correct
  }

  protected double collideX(AxisCycle movementAxis, AABB collisionBox, double desiredOffset) {
    if (this.isEmpty()) {
      return desiredOffset;
    } else if (Math.abs(desiredOffset) < 1.0E-7) {
      return 0.0;
    } else {
      var axisInverse = movementAxis.inverse();
      var xCycle = axisInverse.cycle(Direction.Axis.X);
      var yCycle = axisInverse.cycle(Direction.Axis.Y);
      var zCycle = axisInverse.cycle(Direction.Axis.Z);
      var maxOnCycle = collisionBox.max(xCycle);
      var minOnCycle = collisionBox.min(xCycle);
      var i = this.findIndex(xCycle, minOnCycle + 1.0E-7);
      var startX = this.findIndex(xCycle, maxOnCycle - 1.0E-7);
      var startY = Math.max(0, this.findIndex(yCycle, collisionBox.min(yCycle) + 1.0E-7));
      var endY = Math.min(this.getSize(yCycle), this.findIndex(yCycle, collisionBox.max(yCycle) - 1.0E-7) + 1);
      var startZ = Math.max(0, this.findIndex(zCycle, collisionBox.min(zCycle) + 1.0E-7));
      var endZ = Math.min(this.getSize(zCycle), this.findIndex(zCycle, collisionBox.max(zCycle) - 1.0E-7) + 1);
      var endX = this.getSize(xCycle);
      if (desiredOffset > 0.0) {
        for (var x = startX + 1; x < endX; x++) {
          for (var y = startY; y < endY; y++) {
            for (var z = startZ; z < endZ; z++) {
              if (this.isFullWide(axisInverse, x, y, z)) {
                var g = this.get(xCycle, x) - maxOnCycle;
                if (g >= -1.0E-7) {
                  desiredOffset = Math.min(desiredOffset, g);
                }

                return desiredOffset;
              }
            }
          }
        }
      } else if (desiredOffset < 0.0) {
        for (var x = i - 1; x >= 0; x--) {
          for (var y = startY; y < endY; y++) {
            for (var z = startZ; z < endZ; z++) {
              if (this.isFullWide(axisInverse, x, y, z)) {
                var g = this.get(xCycle, x + 1) - minOnCycle;
                if (g <= 1.0E-7) {
                  desiredOffset = Math.max(desiredOffset, g);
                }

                return desiredOffset;
              }
            }
          }
        }
      }

      return desiredOffset;
    }
  }
}
