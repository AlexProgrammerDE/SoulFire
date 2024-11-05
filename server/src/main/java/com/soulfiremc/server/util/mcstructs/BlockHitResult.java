package com.soulfiremc.server.util.mcstructs;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public class BlockHitResult extends HitResult {
  private final Direction direction;
  private final Vector3i blockPos;
  private final boolean miss;
  private final boolean inside;
  private final boolean worldBorderHit;

  public static BlockHitResult miss(Vector3d location, Direction direction, Vector3i pos) {
    return new BlockHitResult(true, location, direction, pos, false, false);
  }

  public BlockHitResult(Vector3d arg, Direction arg2, Vector3i arg3, boolean bl) {
    this(false, arg, arg2, arg3, bl, false);
  }

  public BlockHitResult(Vector3d arg, Direction arg2, Vector3i arg3, boolean bl, boolean bl2) {
    this(false, arg, arg2, arg3, bl, bl2);
  }

  private BlockHitResult(boolean bl, Vector3d arg, Direction arg2, Vector3i arg3, boolean bl2, boolean bl3) {
    super(arg);
    this.miss = bl;
    this.direction = arg2;
    this.blockPos = arg3;
    this.inside = bl2;
    this.worldBorderHit = bl3;
  }

  public BlockHitResult withDirection(Direction newFace) {
    return new BlockHitResult(this.miss, this.location, newFace, this.blockPos, this.inside, this.worldBorderHit);
  }

  public BlockHitResult withPosition(Vector3i pos) {
    return new BlockHitResult(this.miss, this.location, this.direction, pos, this.inside, this.worldBorderHit);
  }

  public BlockHitResult hitBorder() {
    return new BlockHitResult(this.miss, this.location, this.direction, this.blockPos, this.inside, true);
  }

  public Vector3i getVector3i() {
    return this.blockPos;
  }

  public Direction getDirection() {
    return this.direction;
  }

  @Override
  public HitResult.Type getType() {
    return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
  }

  public boolean isInside() {
    return this.inside;
  }

  public boolean isWorldBorderHit() {
    return this.worldBorderHit;
  }
}
