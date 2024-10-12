package com.soulfiremc.server.protocol.bot.state.entity;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AABB {
  private static final double EPSILON = 1.0E-7;
  public final double minX;
  public final double minY;
  public final double minZ;
  public final double maxX;
  public final double maxY;
  public final double maxZ;

  public AABB(double d, double e, double f, double g, double h, double i) {
    this.minX = Math.min(d, g);
    this.minY = Math.min(e, h);
    this.minZ = Math.min(f, i);
    this.maxX = Math.max(d, g);
    this.maxY = Math.max(e, h);
    this.maxZ = Math.max(f, i);
  }

  public AABB(BlockPos arg) {
    this((double)arg.getX(), (double)arg.getY(), (double)arg.getZ(), (double)(arg.getX() + 1), (double)(arg.getY() + 1), (double)(arg.getZ() + 1));
  }

  public AABB(Vec3 arg, Vec3 arg2) {
    this(arg.x, arg.y, arg.z, arg2.x, arg2.y, arg2.z);
  }

  public static AABB of(BoundingBox mutableBox) {
    return new AABB(
      (double)mutableBox.minX(),
      (double)mutableBox.minY(),
      (double)mutableBox.minZ(),
      (double)(mutableBox.maxX() + 1),
      (double)(mutableBox.maxY() + 1),
      (double)(mutableBox.maxZ() + 1)
    );
  }

  public static AABB unitCubeFromLowerCorner(Vec3 vector) {
    return new AABB(vector.x, vector.y, vector.z, vector.x + 1.0, vector.y + 1.0, vector.z + 1.0);
  }

  public static AABB encapsulatingFullBlocks(BlockPos startPos, BlockPos endPos) {
    return new AABB(
      (double)Math.min(startPos.getX(), endPos.getX()),
      (double)Math.min(startPos.getY(), endPos.getY()),
      (double)Math.min(startPos.getZ(), endPos.getZ()),
      (double)(Math.max(startPos.getX(), endPos.getX()) + 1),
      (double)(Math.max(startPos.getY(), endPos.getY()) + 1),
      (double)(Math.max(startPos.getZ(), endPos.getZ()) + 1)
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
      return Double.compare(lv.maxY, this.maxY) != 0 ? false : Double.compare(lv.maxZ, this.maxZ) == 0;
    }
  }

  @Override
  public int hashCode() {
    long l = Double.doubleToLongBits(this.minX);
    int i = (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.minY);
    i = 31 * i + (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.minZ);
    i = 31 * i + (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.maxX);
    i = 31 * i + (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.maxY);
    i = 31 * i + (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.maxZ);
    return 31 * i + (int)(l ^ l >>> 32);
  }

  public AABB contract(double x, double y, double z) {
    double g = this.minX;
    double h = this.minY;
    double i = this.minZ;
    double j = this.maxX;
    double k = this.maxY;
    double l = this.maxZ;
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

  public AABB expandTowards(Vec3 vector) {
    return this.expandTowards(vector.x, vector.y, vector.z);
  }

  public AABB expandTowards(double x, double y, double z) {
    double g = this.minX;
    double h = this.minY;
    double i = this.minZ;
    double j = this.maxX;
    double k = this.maxY;
    double l = this.maxZ;
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
    double g = this.minX - x;
    double h = this.minY - y;
    double i = this.minZ - z;
    double j = this.maxX + x;
    double k = this.maxY + y;
    double l = this.maxZ + z;
    return new AABB(g, h, i, j, k, l);
  }

  public AABB inflate(double value) {
    return this.inflate(value, value, value);
  }

  public AABB intersect(AABB other) {
    double d = Math.max(this.minX, other.minX);
    double e = Math.max(this.minY, other.minY);
    double f = Math.max(this.minZ, other.minZ);
    double g = Math.min(this.maxX, other.maxX);
    double h = Math.min(this.maxY, other.maxY);
    double i = Math.min(this.maxZ, other.maxZ);
    return new AABB(d, e, f, g, h, i);
  }

  public AABB minmax(AABB other) {
    double d = Math.min(this.minX, other.minX);
    double e = Math.min(this.minY, other.minY);
    double f = Math.min(this.minZ, other.minZ);
    double g = Math.max(this.maxX, other.maxX);
    double h = Math.max(this.maxY, other.maxY);
    double i = Math.max(this.maxZ, other.maxZ);
    return new AABB(d, e, f, g, h, i);
  }

  public AABB move(double x, double y, double z) {
    return new AABB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
  }

  public AABB move(BlockPos pos) {
    return new AABB(
      this.minX + (double)pos.getX(),
      this.minY + (double)pos.getY(),
      this.minZ + (double)pos.getZ(),
      this.maxX + (double)pos.getX(),
      this.maxY + (double)pos.getY(),
      this.maxZ + (double)pos.getZ()
    );
  }

  public AABB move(Vec3 vec) {
    return this.move(vec.x, vec.y, vec.z);
  }

  public AABB move(Vector3f vec) {
    return this.move((double)vec.x, (double)vec.y, (double)vec.z);
  }

  public boolean intersects(AABB other) {
    return this.intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
  }

  public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
    return this.minX < x2 && this.maxX > x1 && this.minY < y2 && this.maxY > y1 && this.minZ < z2 && this.maxZ > z1;
  }

  public boolean intersects(Vec3 min, Vec3 max) {
    return this.intersects(
      Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z), Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z)
    );
  }

  public boolean contains(Vec3 vec) {
    return this.contains(vec.x, vec.y, vec.z);
  }

  public boolean contains(double x, double y, double z) {
    return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
  }

  public double getSize() {
    double d = this.getXsize();
    double e = this.getYsize();
    double f = this.getZsize();
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

  public Optional<Vec3> clip(Vec3 from, Vec3 to) {
    double[] ds = new double[]{1.0};
    double d = to.x - from.x;
    double e = to.y - from.y;
    double f = to.z - from.z;
    Direction lv = getDirection(this, from, ds, null, d, e, f);
    if (lv == null) {
      return Optional.empty();
    } else {
      double g = ds[0];
      return Optional.of(from.add(g * d, g * e, g * f));
    }
  }

  @Nullable
  public static BlockHitResult clip(Iterable<AABB> boxes, Vec3 start, Vec3 end, BlockPos pos) {
    double[] ds = new double[]{1.0};
    Direction lv = null;
    double d = end.x - start.x;
    double e = end.y - start.y;
    double f = end.z - start.z;

    for (AABB lv2 : boxes) {
      lv = getDirection(lv2.move(pos), start, ds, lv, d, e, f);
    }

    if (lv == null) {
      return null;
    } else {
      double g = ds[0];
      return new BlockHitResult(start.add(g * d, g * e, g * f), lv, pos, false);
    }
  }

  @Nullable
  private static Direction getDirection(AABB aabb, Vec3 start, double[] minDistance, @Nullable Direction facing, double deltaX, double deltaY, double deltaZ) {
    if (deltaX > 1.0E-7) {
      facing = clipPoint(
        minDistance, facing, deltaX, deltaY, deltaZ, aabb.minX, aabb.minY, aabb.maxY, aabb.minZ, aabb.maxZ, Direction.WEST, start.x, start.y, start.z
      );
    } else if (deltaX < -1.0E-7) {
      facing = clipPoint(
        minDistance, facing, deltaX, deltaY, deltaZ, aabb.maxX, aabb.minY, aabb.maxY, aabb.minZ, aabb.maxZ, Direction.EAST, start.x, start.y, start.z
      );
    }

    if (deltaY > 1.0E-7) {
      facing = clipPoint(
        minDistance, facing, deltaY, deltaZ, deltaX, aabb.minY, aabb.minZ, aabb.maxZ, aabb.minX, aabb.maxX, Direction.DOWN, start.y, start.z, start.x
      );
    } else if (deltaY < -1.0E-7) {
      facing = clipPoint(
        minDistance, facing, deltaY, deltaZ, deltaX, aabb.maxY, aabb.minZ, aabb.maxZ, aabb.minX, aabb.maxX, Direction.UP, start.y, start.z, start.x
      );
    }

    if (deltaZ > 1.0E-7) {
      facing = clipPoint(
        minDistance, facing, deltaZ, deltaX, deltaY, aabb.minZ, aabb.minX, aabb.maxX, aabb.minY, aabb.maxY, Direction.NORTH, start.z, start.x, start.y
      );
    } else if (deltaZ < -1.0E-7) {
      facing = clipPoint(
        minDistance, facing, deltaZ, deltaX, deltaY, aabb.maxZ, aabb.minX, aabb.maxX, aabb.minY, aabb.maxY, Direction.SOUTH, start.z, start.x, start.y
      );
    }

    return facing;
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
    double o = (minSide - startSide) / distanceSide;
    double p = startOtherA + o * distanceOtherA;
    double q = startOtherB + o * distanceOtherB;
    if (0.0 < o && o < minDistance[0] && minOtherA - 1.0E-7 < p && p < maxOtherA + 1.0E-7 && minOtherB - 1.0E-7 < q && q < maxOtherB + 1.0E-7) {
      minDistance[0] = o;
      return hitSide;
    } else {
      return prevDirection;
    }
  }

  public double distanceToSqr(Vec3 vec) {
    double d = Math.max(Math.max(this.minX - vec.x, vec.x - this.maxX), 0.0);
    double e = Math.max(Math.max(this.minY - vec.y, vec.y - this.maxY), 0.0);
    double f = Math.max(Math.max(this.minZ - vec.z, vec.z - this.maxZ), 0.0);
    return Mth.lengthSquared(d, e, f);
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

  public Vec3 getCenter() {
    return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), Mth.lerp(0.5, this.minY, this.maxY), Mth.lerp(0.5, this.minZ, this.maxZ));
  }

  public Vec3 getBottomCenter() {
    return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), this.minY, Mth.lerp(0.5, this.minZ, this.maxZ));
  }

  public Vec3 getMinPosition() {
    return new Vec3(this.minX, this.minY, this.minZ);
  }

  public Vec3 getMaxPosition() {
    return new Vec3(this.maxX, this.maxY, this.maxZ);
  }

  public static AABB ofSize(Vec3 center, double xSize, double ySize, double zSize) {
    return new AABB(
      center.x - xSize / 2.0, center.y - ySize / 2.0, center.z - zSize / 2.0, center.x + xSize / 2.0, center.y + ySize / 2.0, center.z + zSize / 2.0
    );
  }
}
