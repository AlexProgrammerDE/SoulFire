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

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public class BlockHitResult extends HitResult {
  private final Direction direction;
  private final Vector3i blockPos;
  private final boolean miss;
  private final boolean inside;
  private final boolean worldBorderHit;

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

  public static BlockHitResult miss(Vector3d location, Direction direction, Vector3i pos) {
    return new BlockHitResult(true, location, direction, pos, false, false);
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
