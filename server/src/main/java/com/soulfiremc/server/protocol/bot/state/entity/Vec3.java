package com.soulfiremc.server.protocol.bot.state.entity;

import com.mojang.serialization.Codec;

import java.util.EnumSet;
import java.util.List;

public class Vec3 {
  public static final Codec<Vec3> CODEC = Codec.DOUBLE
    .listOf()
    .comapFlatMap(
      list -> Util.fixedSize(list, 3).map(listx -> new Vec3((Double)listx.get(0), (Double)listx.get(1), (Double)listx.get(2))),
      arg -> List.of(arg.x(), arg.y(), arg.z())
    );
  public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
  public final double x;
  public final double y;
  public final double z;

  public static Vec3 fromRGB24(int packed) {
    double d = (double)(packed >> 16 & 0xFF) / 255.0;
    double e = (double)(packed >> 8 & 0xFF) / 255.0;
    double f = (double)(packed & 0xFF) / 255.0;
    return new Vec3(d, e, f);
  }

  public static Vec3 atLowerCornerOf(Vec3i toCopy) {
    return new Vec3((double)toCopy.getX(), (double)toCopy.getY(), (double)toCopy.getZ());
  }

  public static Vec3 atLowerCornerWithOffset(Vec3i toCopy, double offsetX, double offsetY, double offsetZ) {
    return new Vec3((double)toCopy.getX() + offsetX, (double)toCopy.getY() + offsetY, (double)toCopy.getZ() + offsetZ);
  }

  public static Vec3 atCenterOf(Vec3i toCopy) {
    return atLowerCornerWithOffset(toCopy, 0.5, 0.5, 0.5);
  }

  public static Vec3 atBottomCenterOf(Vec3i toCopy) {
    return atLowerCornerWithOffset(toCopy, 0.5, 0.0, 0.5);
  }

  public static Vec3 upFromBottomCenterOf(Vec3i toCopy, double verticalOffset) {
    return atLowerCornerWithOffset(toCopy, 0.5, verticalOffset, 0.5);
  }

  public Vec3(double d, double e, double f) {
    this.x = d;
    this.y = e;
    this.z = f;
  }

  public Vec3(Vector3f vector3f) {
    this((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z());
  }

  public Vec3 vectorTo(Vec3 vec) {
    return new Vec3(vec.x - this.x, vec.y - this.y, vec.z - this.z);
  }

  public Vec3 normalize() {
    double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    return d < 1.0E-4 ? ZERO : new Vec3(this.x / d, this.y / d, this.z / d);
  }

  public double dot(Vec3 vec) {
    return this.x * vec.x + this.y * vec.y + this.z * vec.z;
  }

  public Vec3 cross(Vec3 vec) {
    return new Vec3(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
  }

  public Vec3 subtract(Vec3 vec) {
    return this.subtract(vec.x, vec.y, vec.z);
  }

  public Vec3 subtract(double x, double y, double z) {
    return this.add(-x, -y, -z);
  }

  public Vec3 add(Vec3 vec) {
    return this.add(vec.x, vec.y, vec.z);
  }

  public Vec3 add(double x, double y, double z) {
    return new Vec3(this.x + x, this.y + y, this.z + z);
  }

  public boolean closerThan(Position pos, double distance) {
    return this.distanceToSqr(pos.x(), pos.y(), pos.z()) < distance * distance;
  }

  public double distanceTo(Vec3 vec) {
    double d = vec.x - this.x;
    double e = vec.y - this.y;
    double f = vec.z - this.z;
    return Math.sqrt(d * d + e * e + f * f);
  }

  public double distanceToSqr(Vec3 vec) {
    double d = vec.x - this.x;
    double e = vec.y - this.y;
    double f = vec.z - this.z;
    return d * d + e * e + f * f;
  }

  public double distanceToSqr(double x, double y, double z) {
    double g = x - this.x;
    double h = y - this.y;
    double i = z - this.z;
    return g * g + h * h + i * i;
  }

  public boolean closerThan(Vec3 pos, double horizontalDistance, double verticalDistance) {
    double f = pos.x() - this.x;
    double g = pos.y() - this.y;
    double h = pos.z() - this.z;
    return Mth.lengthSquared(f, h) < Mth.square(horizontalDistance) && Math.abs(g) < verticalDistance;
  }

  public Vec3 scale(double factor) {
    return this.multiply(factor, factor, factor);
  }

  public Vec3 reverse() {
    return this.scale(-1.0);
  }

  public Vec3 multiply(Vec3 vec) {
    return this.multiply(vec.x, vec.y, vec.z);
  }

  public Vec3 multiply(double factorX, double factorY, double factorZ) {
    return new Vec3(this.x * factorX, this.y * factorY, this.z * factorZ);
  }

  public Vec3 offsetRandom(RandomSource random, float factor) {
    return this.add(
      (double)((random.nextFloat() - 0.5F) * factor), (double)((random.nextFloat() - 0.5F) * factor), (double)((random.nextFloat() - 0.5F) * factor)
    );
  }

  public double length() {
    return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
  }

  public double lengthSqr() {
    return this.x * this.x + this.y * this.y + this.z * this.z;
  }

  public double horizontalDistance() {
    return Math.sqrt(this.x * this.x + this.z * this.z);
  }

  public double horizontalDistanceSqr() {
    return this.x * this.x + this.z * this.z;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    } else if (!(object instanceof Vec3 lv)) {
      return false;
    } else if (Double.compare(lv.x, this.x) != 0) {
      return false;
    } else {
      return Double.compare(lv.y, this.y) != 0 ? false : Double.compare(lv.z, this.z) == 0;
    }
  }

  @Override
  public int hashCode() {
    long l = Double.doubleToLongBits(this.x);
    int i = (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.y);
    i = 31 * i + (int)(l ^ l >>> 32);
    l = Double.doubleToLongBits(this.z);
    return 31 * i + (int)(l ^ l >>> 32);
  }

  @Override
  public String toString() {
    return "(" + this.x + ", " + this.y + ", " + this.z + ")";
  }

  public Vec3 lerp(Vec3 to, double delta) {
    return new Vec3(Mth.lerp(delta, this.x, to.x), Mth.lerp(delta, this.y, to.y), Mth.lerp(delta, this.z, to.z));
  }

  public Vec3 xRot(float pitch) {
    float g = Mth.cos(pitch);
    float h = Mth.sin(pitch);
    double d = this.x;
    double e = this.y * (double)g + this.z * (double)h;
    double i = this.z * (double)g - this.y * (double)h;
    return new Vec3(d, e, i);
  }

  public Vec3 yRot(float yaw) {
    float g = Mth.cos(yaw);
    float h = Mth.sin(yaw);
    double d = this.x * (double)g + this.z * (double)h;
    double e = this.y;
    double i = this.z * (double)g - this.x * (double)h;
    return new Vec3(d, e, i);
  }

  public Vec3 zRot(float roll) {
    float g = Mth.cos(roll);
    float h = Mth.sin(roll);
    double d = this.x * (double)g + this.y * (double)h;
    double e = this.y * (double)g - this.x * (double)h;
    double i = this.z;
    return new Vec3(d, e, i);
  }

  public static Vec3 directionFromRotation(Vec2 vec) {
    return directionFromRotation(vec.x, vec.y);
  }

  public static Vec3 directionFromRotation(float pitch, float yaw) {
    float h = Mth.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
    float i = Mth.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
    float j = -Mth.cos(-pitch * (float) (Math.PI / 180.0));
    float k = Mth.sin(-pitch * (float) (Math.PI / 180.0));
    return new Vec3((double)(i * j), (double)k, (double)(h * j));
  }

  public Vec3 align(EnumSet<Direction.Axis> axes) {
    double d = axes.contains(Direction.Axis.X) ? (double)Mth.floor(this.x) : this.x;
    double e = axes.contains(Direction.Axis.Y) ? (double)Mth.floor(this.y) : this.y;
    double f = axes.contains(Direction.Axis.Z) ? (double)Mth.floor(this.z) : this.z;
    return new Vec3(d, e, f);
  }

  public double get(Direction.Axis axis) {
    return axis.choose(this.x, this.y, this.z);
  }

  public Vec3 with(Direction.Axis axis, double length) {
    double e = axis == Direction.Axis.X ? length : this.x;
    double f = axis == Direction.Axis.Y ? length : this.y;
    double g = axis == Direction.Axis.Z ? length : this.z;
    return new Vec3(e, f, g);
  }

  public Vec3 relative(Direction direction, double length) {
    Vec3i lv = direction.getNormal();
    return new Vec3(this.x + length * (double)lv.getX(), this.y + length * (double)lv.getY(), this.z + length * (double)lv.getZ());
  }

  public final double x() {
    return this.x;
  }

  public final double y() {
    return this.y;
  }

  public final double z() {
    return this.z;
  }

  public Vector3f toVector3f() {
    return new Vector3f((float)this.x, (float)this.y, (float)this.z);
  }
}
