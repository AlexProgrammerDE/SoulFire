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
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.pathfinding.graph.actions.movement.BodyPart;
import com.soulfiremc.server.pathfinding.graph.actions.movement.DiagonalDirection;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementSide;
import com.soulfiremc.server.util.structs.IDMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class DiagonalCollisionCalculator {
  private static final Vec3 START_POSITION = new Vec3(0.5, 0, 0.5);
  private static final Vec3[] STEPS = new Vec3[]{new Vec3(0.25, 0, 0.25), new Vec3(0.5, 0, 0.5), new Vec3(0.75, 0, 0.75)};
  private static final IDMap<BlockState, boolean[][][]> COLLISIONS = new IDMap<>(Block.BLOCK_STATE_REGISTRY.iterator().hasNext(), blockState -> {
    var diagonalsArray = new boolean[DiagonalDirection.VALUES.length][][];

    for (var diagonal : DiagonalDirection.VALUES) {
      var baseOffset = diagonal.offset(BlockPos.ZERO);

      var bodyPartArray = new boolean[BodyPart.VALUES.length][];
      for (var bodyPart : BodyPart.VALUES) {
        var sideArray = new boolean[MovementSide.VALUES.length];
        for (var side : MovementSide.VALUES) {
          var collides = false;

          for (var step : STEPS) {
            var currentPosition = START_POSITION.add(step.mul(baseOffset.toVec3()));
            collides = blockState.collidesWith(
              bodyPart.offset(diagonal.side(side).offset(BlockPos.ZERO)).toVector3i(),
              Player.STANDING_DIMENSIONS.makeBoundingBox(currentPosition)
            );

            if (collides) {
              break;
            }
          }

          sideArray[side.ordinal()] = collides;
        }

        bodyPartArray[bodyPart.ordinal()] = sideArray;
      }

      diagonalsArray[diagonal.ordinal()] = bodyPartArray;
    }

    return diagonalsArray;
  });

  public static boolean collidesWith(CollisionData collisionData) {
    return COLLISIONS.get(collisionData.blockState)[collisionData.diagonalArrayIndex][collisionData.bodyPart.ordinal()][collisionData.side.ordinal()];
  }

  public record CollisionData(BlockState blockState, int diagonalArrayIndex, BodyPart bodyPart, MovementSide side) {
  }
}
