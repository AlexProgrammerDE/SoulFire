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
package com.soulfiremc.server.pathfinding.graph.actions;

import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.GapJumpAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ParkourDirection;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

public final class ParkourMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final ParkourDirection direction;
  private final SFVec3i targetFeetBlock;

  public ParkourMovement(ParkourDirection direction) {
    this.direction = direction;
    this.targetFeetBlock = direction.offset(direction.offset(FEET_POSITION_RELATIVE_BLOCK));
  }

  public List<Pair<SFVec3i, BlockFace>> listRequiredFreeBlocks() {
    var requiredFreeBlocks = new ObjectArrayList<Pair<SFVec3i, BlockFace>>();

    // Make block above the head block free for jump
    requiredFreeBlocks.add(Pair.of(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0), BlockFace.BOTTOM));

    var oneFurther = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    var blockDigDirection = direction.toSkyDirection().opposite().toBlockFace();

    // Room for jumping
    requiredFreeBlocks.add(Pair.of(oneFurther, blockDigDirection));
    requiredFreeBlocks.add(Pair.of(oneFurther.add(0, 1, 0), blockDigDirection));
    requiredFreeBlocks.add(Pair.of(oneFurther.add(0, 2, 0), blockDigDirection));

    var twoFurther = direction.offset(oneFurther);

    // Room for jumping
    requiredFreeBlocks.add(Pair.of(twoFurther, blockDigDirection));
    requiredFreeBlocks.add(Pair.of(twoFurther.add(0, 1, 0), blockDigDirection));
    requiredFreeBlocks.add(Pair.of(twoFurther.add(0, 2, 0), blockDigDirection));

    return requiredFreeBlocks;
  }

  public SFVec3i requiredUnsafeBlock() {
    // The gap to jump over, needs to be unsafe for this movement to be possible
    return direction.offset(FEET_POSITION_RELATIVE_BLOCK).sub(0, 1, 0);
  }

  public SFVec3i requiredSolidBlock() {
    // Floor block
    return targetFeetBlock.sub(0, 1, 0);
  }

  @Override
  public GraphInstructions getInstructions(SFVec3i node) {
    var absoluteTargetFeetBlock = node.add(targetFeetBlock);

    return new GraphInstructions(
      absoluteTargetFeetBlock,
      Costs.ONE_GAP_JUMP,
      List.of(new GapJumpAction(absoluteTargetFeetBlock)));
  }

  @Override
  public ParkourMovement copy() {
    return this.clone();
  }

  @Override
  public ParkourMovement clone() {
    try {
      return (ParkourMovement) super.clone();
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }
}
