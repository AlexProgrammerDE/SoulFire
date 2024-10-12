package com.soulfiremc.server.protocol.bot.state.entity;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum Direction {
  DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
  UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
  NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
  SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
  WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
  EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

  private final int data3d;
  private final int oppositeIndex;
  private final int data2d;
  private final String name;
  private final Direction.Axis axis;
  private final Direction.AxisDirection axisDirection;
  private final Vec3i normal;
  private static final Direction[] VALUES = values();
  private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES).sorted(Comparator.comparingInt(arg -> arg.data3d)).toArray(Direction[]::new);
  private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES)
    .filter(arg -> arg.getAxis().isHorizontal())
    .sorted(Comparator.comparingInt(arg -> arg.data2d))
    .toArray(Direction[]::new);

  private Direction(
    final int j, final int k, final int l, final String string2, final Direction.AxisDirection arg, final Direction.Axis arg2, final Vec3i arg3
  ) {
    this.data3d = j;
    this.data2d = l;
    this.oppositeIndex = k;
    this.name = string2;
    this.axis = arg2;
    this.axisDirection = arg;
    this.normal = arg3;
  }

  public static Direction[] orderedByNearest(Entity entity) {
    float f = entity.getViewXRot(1.0F) * (float) (Math.PI / 180.0);
    float g = -entity.getViewYRot(1.0F) * (float) (Math.PI / 180.0);
    float h = Mth.sin(f);
    float i = Mth.cos(f);
    float j = Mth.sin(g);
    float k = Mth.cos(g);
    boolean bl = j > 0.0F;
    boolean bl2 = h < 0.0F;
    boolean bl3 = k > 0.0F;
    float l = bl ? j : -j;
    float m = bl2 ? -h : h;
    float n = bl3 ? k : -k;
    float o = l * i;
    float p = n * i;
    Direction lv = bl ? EAST : WEST;
    Direction lv2 = bl2 ? UP : DOWN;
    Direction lv3 = bl3 ? SOUTH : NORTH;
    if (l > n) {
      if (m > o) {
        return makeDirectionArray(lv2, lv, lv3);
      } else {
        return p > m ? makeDirectionArray(lv, lv3, lv2) : makeDirectionArray(lv, lv2, lv3);
      }
    } else if (m > p) {
      return makeDirectionArray(lv2, lv3, lv);
    } else {
      return o > m ? makeDirectionArray(lv3, lv, lv2) : makeDirectionArray(lv3, lv2, lv);
    }
  }

  private static Direction[] makeDirectionArray(Direction first, Direction second, Direction third) {
    return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
  }

  public static Direction rotate(Matrix4f matrix, Direction direction) {
    Vec3i lv = direction.getNormal();
    Vector4f vector4f = matrix.transform(new Vector4f((float)lv.getX(), (float)lv.getY(), (float)lv.getZ(), 0.0F));
    return getNearest(vector4f.x(), vector4f.y(), vector4f.z());
  }

  public static Collection<Direction> allShuffled(RandomSource random) {
    return Util.shuffledCopy(values(), random);
  }

  public static Stream<Direction> stream() {
    return Stream.of(VALUES);
  }

  public Quaternionf getRotation() {
    return switch (this) {
      case DOWN -> new Quaternionf().rotationX((float) Math.PI);
      case UP -> new Quaternionf();
      case NORTH -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) Math.PI);
      case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
      case WEST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2));
      case EAST -> new Quaternionf().rotationXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
    };
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

  public static Direction getFacingAxis(Entity entity, Direction.Axis axis) {
    return switch (axis) {
      case X -> EAST.isFacingAngle(entity.getViewYRot(1.0F)) ? EAST : WEST;
      case Y -> entity.getViewXRot(1.0F) < 0.0F ? UP : DOWN;
      case Z -> SOUTH.isFacingAngle(entity.getViewYRot(1.0F)) ? SOUTH : NORTH;
    };
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
      default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
      case WEST -> UP;
      case EAST -> DOWN;
    };
  }

  private Direction getCounterClockWiseZ() {
    return switch (this) {
      case DOWN -> EAST;
      case UP -> WEST;
      default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
      case WEST -> DOWN;
      case EAST -> UP;
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
    return new Vector3f((float)this.getStepX(), (float)this.getStepY(), (float)this.getStepZ());
  }

  public String getName() {
    return this.name;
  }

  public Direction.Axis getAxis() {
    return this.axis;
  }

  @Nullable
  public static Direction byName(@Nullable String name) {
    return CODEC.byName(name);
  }

  public static Direction from3DDataValue(int index) {
    return BY_3D_DATA[Mth.abs(index % BY_3D_DATA.length)];
  }

  public static Direction from2DDataValue(int horizontalIndex) {
    return BY_2D_DATA[Mth.abs(horizontalIndex % BY_2D_DATA.length)];
  }

  @Nullable
  public static Direction fromDelta(int x, int y, int z) {
    if (x == 0) {
      if (y == 0) {
        if (z > 0) {
          return SOUTH;
        }

        if (z < 0) {
          return NORTH;
        }
      } else if (z == 0) {
        if (y > 0) {
          return UP;
        }

        return DOWN;
      }
    } else if (y == 0 && z == 0) {
      if (x > 0) {
        return EAST;
      }

      return WEST;
    }

    return null;
  }

  public static Direction fromYRot(double angle) {
    return from2DDataValue(Mth.floor(angle / 90.0 + 0.5) & 3);
  }

  public static Direction fromAxisAndDirection(Direction.Axis axis, Direction.AxisDirection axisDirection) {
    return switch (axis) {
      case X -> axisDirection == Direction.AxisDirection.POSITIVE ? EAST : WEST;
      case Y -> axisDirection == Direction.AxisDirection.POSITIVE ? UP : DOWN;
      case Z -> axisDirection == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
    };
  }

  public float toYRot() {
    return (float)((this.data2d & 3) * 90);
  }

  public static Direction getRandom(RandomSource random) {
    return Util.getRandom(VALUES, random);
  }

  public static Direction getNearest(double x, double y, double z) {
    return getNearest((float)x, (float)y, (float)z);
  }

  public static Direction getNearest(float x, float y, float z) {
    Direction lv = NORTH;
    float i = Float.MIN_VALUE;

    for (Direction lv2 : VALUES) {
      float j = x * (float)lv2.normal.getX() + y * (float)lv2.normal.getY() + z * (float)lv2.normal.getZ();
      if (j > i) {
        i = j;
        lv = lv2;
      }
    }

    return lv;
  }

  public static Direction getNearest(Vec3 ois) {
    return getNearest(ois.x, ois.y, ois.z);
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public String getSerializedName() {
    return this.name;
  }

  private static DataResult<Direction> verifyVertical(Direction direction) {
    return direction.getAxis().isVertical() ? DataResult.success(direction) : DataResult.error(() -> "Expected a vertical direction");
  }

  public static Direction get(Direction.AxisDirection axisDirection, Direction.Axis axis) {
    for (Direction lv : VALUES) {
      if (lv.getAxisDirection() == axisDirection && lv.getAxis() == axis) {
        return lv;
      }
    }

    throw new IllegalArgumentException("No such direction: " + axisDirection + " " + axis);
  }

  public Vec3i getNormal() {
    return this.normal;
  }

  public boolean isFacingAngle(float degrees) {
    float g = degrees * (float) (Math.PI / 180.0);
    float h = -Mth.sin(g);
    float i = Mth.cos(g);
    return (float)this.normal.getX() * h + (float)this.normal.getZ() * i > 0.0F;
  }

  public static enum Axis implements StringRepresentable, Predicate<Direction> {
    X("x") {
      @Override
      public int choose(int x, int y, int z) {
        return x;
      }

      @Override
      public double choose(double x, double y, double z) {
        return x;
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
    };

    public static final Direction.Axis[] VALUES = values();
    public static final StringRepresentable.EnumCodec<Direction.Axis> CODEC = StringRepresentable.fromEnum(Direction.Axis::values);
    private final String name;

    Axis(final String string2) {
      this.name = string2;
    }

    @Nullable
    public static Direction.Axis byName(String name) {
      return CODEC.byName(name);
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

    @Override
    public String toString() {
      return this.name;
    }

    public static Direction.Axis getRandom(RandomSource random) {
      return Util.getRandom(VALUES, random);
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

    @Override
    public String getSerializedName() {
      return this.name;
    }

    public abstract int choose(int x, int y, int z);

    public abstract double choose(double x, double y, double z);
  }

  public static enum AxisDirection {
    POSITIVE(1, "Towards positive"),
    NEGATIVE(-1, "Towards negative");

    private final int step;
    private final String name;

    private AxisDirection(final int j, final String string2) {
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

  public static enum Plane implements Iterable<Direction>, Predicate<Direction> {
    HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
    VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

    private final Direction[] faces;
    private final Direction.Axis[] axis;

    private Plane(final Direction[] args, final Direction.Axis[] args2) {
      this.faces = args;
      this.axis = args2;
    }

    public Direction getRandomDirection(RandomSource random) {
      return Util.getRandom(this.faces, random);
    }

    public Direction.Axis getRandomAxis(RandomSource random) {
      return Util.getRandom(this.axis, random);
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

    public List<Direction> shuffledCopy(RandomSource random) {
      return Util.shuffledCopy(this.faces, random);
    }

    public int length() {
      return this.faces.length;
    }
  }
}
