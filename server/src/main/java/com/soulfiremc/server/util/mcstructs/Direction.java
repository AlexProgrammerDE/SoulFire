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

import com.google.common.collect.Iterators;
import com.soulfiremc.server.util.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum Direction {
  DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, Vector3i.from(0, -1, 0)),
  UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, Vector3i.from(0, 1, 0)),
  NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, Vector3i.from(0, 0, -1)),
  SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, Vector3i.from(0, 0, 1)),
  WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, Vector3i.from(-1, 0, 0)),
  EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, Vector3i.from(1, 0, 0));

  private static final Direction[] VALUES = values();
  private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES).sorted(Comparator.comparingInt(arg -> arg.data3d)).toArray(Direction[]::new);
  private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES)
    .filter(arg -> arg.getAxis().isHorizontal())
    .sorted(Comparator.comparingInt(arg -> arg.data2d))
    .toArray(Direction[]::new);
  private final int data3d;
  private final int oppositeIndex;
  private final int data2d;
  private final String name;
  private final Direction.Axis axis;
  private final Direction.AxisDirection axisDirection;
  private final Vector3i normal;

  Direction(
    final int j, final int k, final int l, final String string2, final Direction.AxisDirection arg, final Direction.Axis arg2, final Vector3i arg3
  ) {
    this.data3d = j;
    this.data2d = l;
    this.oppositeIndex = k;
    this.name = string2;
    this.axis = arg2;
    this.axisDirection = arg;
    this.normal = arg3;
  }

  private static Direction[] makeDirectionArray(Direction first, Direction second, Direction third) {
    return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
  }

  public static Stream<Direction> stream() {
    return Stream.of(VALUES);
  }

  public static float getYRot(Direction arg) {
    return switch (arg) {
      case NORTH -> 180.0F;
      case SOUTH -> 0.0F;
      case WEST -> 90.0F;
      case EAST -> -90.0F;
      default -> throw new IllegalStateException("No y-Rot for vertical axis: " + arg);
    };
  }

  public static Direction from3DDataValue(int index) {
    return BY_3D_DATA[MathHelper.abs(index % BY_3D_DATA.length)];
  }

  public static Direction from2DDataValue(int horizontalIndex) {
    return BY_2D_DATA[MathHelper.abs(horizontalIndex % BY_2D_DATA.length)];
  }

  public static Direction fromYRot(double angle) {
    return from2DDataValue(MathHelper.floor(angle / 90.0 + 0.5) & 3);
  }

  public static Direction fromAxisAndDirection(Direction.Axis axis, Direction.AxisDirection axisDirection) {
    return switch (axis) {
      case X -> axisDirection == Direction.AxisDirection.POSITIVE ? EAST : WEST;
      case Y -> axisDirection == Direction.AxisDirection.POSITIVE ? UP : DOWN;
      case Z -> axisDirection == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
    };
  }

  public static Direction getApproximateNearest(double d, double e, double f) {
    return getApproximateNearest((float) d, (float) e, (float) f);
  }

  public static Direction getApproximateNearest(float f, float g, float h) {
    var lv = NORTH;
    var i = Float.MIN_VALUE;

    for (var lv2 : VALUES) {
      var j = f * (float) lv2.normal.getX() + g * (float) lv2.normal.getY() + h * (float) lv2.normal.getZ();
      if (j > i) {
        i = j;
        lv = lv2;
      }
    }

    return lv;
  }

  public static Direction getApproximateNearest(Vector3d arg) {
    return getApproximateNearest(arg.getX(), arg.getY(), arg.getZ());
  }

  @Nullable
  @Contract("_,_,_,!null->!null;_,_,_,_->_")
  public static Direction getNearest(int i, int j, int k, @Nullable Direction arg) {
    var l = Math.abs(i);
    var m = Math.abs(j);
    var n = Math.abs(k);
    if (l > n && l > m) {
      return i < 0 ? WEST : EAST;
    } else if (n > l && n > m) {
      return k < 0 ? NORTH : SOUTH;
    } else if (m > l && m > n) {
      return j < 0 ? DOWN : UP;
    } else {
      return arg;
    }
  }

  @Nullable
  @Contract("_,!null->!null;_,_->_")
  public static Direction getNearest(Vector3i arg, @Nullable Direction arg2) {
    return getNearest(arg.getX(), arg.getY(), arg.getZ(), arg2);
  }

  public static Direction get(Direction.AxisDirection axisDirection, Direction.Axis axis) {
    for (var lv : VALUES) {
      if (lv.getAxisDirection() == axisDirection && lv.getAxis() == axis) {
        return lv;
      }
    }

    throw new IllegalArgumentException("No such direction: " + axisDirection + " " + axis);
  }

  public int get3DDataValue() {
    return this.data3d;
  }

  public int get2DDataValue() {
    return this.data2d;
  }

  public Direction.AxisDirection getAxisDirection() {
    return this.axisDirection;
  }

  public Direction getOpposite() {
    return from3DDataValue(this.oppositeIndex);
  }

  public Direction getClockWise(Direction.Axis axis) {
    return switch (axis) {
      case X -> this != WEST && this != EAST ? this.getClockWiseX() : this;
      case Y -> this != UP && this != DOWN ? this.getClockWise() : this;
      case Z -> this != NORTH && this != SOUTH ? this.getClockWiseZ() : this;
    };
  }

  public Direction getCounterClockWise(Direction.Axis axis) {
    return switch (axis) {
      case X -> this != WEST && this != EAST ? this.getCounterClockWiseX() : this;
      case Y -> this != UP && this != DOWN ? this.getCounterClockWise() : this;
      case Z -> this != NORTH && this != SOUTH ? this.getCounterClockWiseZ() : this;
    };
  }

  public Direction getClockWise() {
    return switch (this) {
      case NORTH -> EAST;
      case SOUTH -> WEST;
      case WEST -> NORTH;
      case EAST -> SOUTH;
      default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
    };
  }

  private Direction getClockWiseX() {
    return switch (this) {
      case DOWN -> SOUTH;
      case UP -> NORTH;
      case NORTH -> DOWN;
      case SOUTH -> UP;
      default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
    };
  }

  private Direction getCounterClockWiseX() {
    return switch (this) {
      case DOWN -> NORTH;
      case UP -> SOUTH;
      case NORTH -> UP;
      case SOUTH -> DOWN;
      default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
    };
  }

  private Direction getClockWiseZ() {
    return switch (this) {
      case DOWN -> WEST;
      case UP -> EAST;
      case WEST -> UP;
      case EAST -> DOWN;
      default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
    };
  }

  private Direction getCounterClockWiseZ() {
    return switch (this) {
      case DOWN -> EAST;
      case UP -> WEST;
      case WEST -> DOWN;
      case EAST -> UP;
      default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
    };
  }

  public Direction getCounterClockWise() {
    return switch (this) {
      case NORTH -> WEST;
      case SOUTH -> EAST;
      case WEST -> SOUTH;
      case EAST -> NORTH;
      default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
    };
  }

  public int getStepX() {
    return this.normal.getX();
  }

  public int getStepY() {
    return this.normal.getY();
  }

  public int getStepZ() {
    return this.normal.getZ();
  }

  public Vector3f step() {
    return Vector3f.from((float) this.getStepX(), (float) this.getStepY(), (float) this.getStepZ());
  }

  public String getName() {
    return this.name;
  }

  public Direction.Axis getAxis() {
    return this.axis;
  }

  public float toYRot() {
    return (float) ((this.data2d & 3) * 90);
  }

  public Vector3i offset(Vector3i arg) {
    return arg.add(this.normal);
  }

  @Override
  public String toString() {
    return this.name;
  }

  public Vector3i getUnitVector3i() {
    return this.normal;
  }

  public boolean isFacingAngle(float degrees) {
    var g = degrees * (float) (Math.PI / 180.0);
    var h = -MathHelper.sin(g);
    var i = MathHelper.cos(g);
    return (float) this.normal.getX() * h + (float) this.normal.getZ() * i > 0.0F;
  }

  public enum Axis implements Predicate<Direction> {
    X("x") {
      @Override
      public int choose(int x, int y, int z) {
        return x;
      }

      @Override
      public double choose(double x, double y, double z) {
        return x;
      }

      @Override
      public Direction getPositive() {
        return Direction.EAST;
      }

      @Override
      public Direction getNegative() {
        return Direction.WEST;
      }
    },
    Y("y") {
      @Override
      public int choose(int x, int y, int z) {
        return y;
      }

      @Override
      public double choose(double x, double y, double z) {
        return y;
      }

      @Override
      public Direction getPositive() {
        return Direction.UP;
      }

      @Override
      public Direction getNegative() {
        return Direction.DOWN;
      }
    },
    Z("z") {
      @Override
      public int choose(int x, int y, int z) {
        return z;
      }

      @Override
      public double choose(double x, double y, double z) {
        return z;
      }

      @Override
      public Direction getPositive() {
        return Direction.SOUTH;
      }

      @Override
      public Direction getNegative() {
        return Direction.NORTH;
      }
    };

    public static final Direction.Axis[] VALUES = values();
    private final String name;

    Axis(final String string2) {
      this.name = string2;
    }

    public String getName() {
      return this.name;
    }

    public boolean isVertical() {
      return this == Y;
    }

    public boolean isHorizontal() {
      return this == X || this == Z;
    }

    public abstract Direction getPositive();

    public abstract Direction getNegative();

    public Direction[] getDirections() {
      return new Direction[]{this.getPositive(), this.getNegative()};
    }

    @Override
    public String toString() {
      return this.name;
    }

    public boolean test(@Nullable Direction direction) {
      return direction != null && direction.getAxis() == this;
    }

    public Direction.Plane getPlane() {
      return switch (this) {
        case X, Z -> Direction.Plane.HORIZONTAL;
        case Y -> Direction.Plane.VERTICAL;
      };
    }

    public abstract int choose(int x, int y, int z);

    public abstract double choose(double x, double y, double z);
  }

  public enum AxisDirection {
    POSITIVE(1, "Towards positive"),
    NEGATIVE(-1, "Towards negative");

    private final int step;
    private final String name;

    AxisDirection(final int j, final String string2) {
      this.step = j;
      this.name = string2;
    }

    public int getStep() {
      return this.step;
    }

    public String getName() {
      return this.name;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public Direction.AxisDirection opposite() {
      return this == POSITIVE ? NEGATIVE : POSITIVE;
    }
  }

  public enum Plane implements Iterable<Direction>, Predicate<Direction> {
    HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
    VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

    private final Direction[] faces;
    private final Direction.Axis[] axis;

    Plane(final Direction[] args, final Direction.Axis[] args2) {
      this.faces = args;
      this.axis = args2;
    }

    public boolean test(@Nullable Direction direction) {
      return direction != null && direction.getAxis().getPlane() == this;
    }

    @Override
    public Iterator<Direction> iterator() {
      return Iterators.forArray(this.faces);
    }

    public Stream<Direction> stream() {
      return Arrays.stream(this.faces);
    }

    public int length() {
      return this.faces.length;
    }
  }
}
