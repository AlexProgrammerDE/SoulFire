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

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BodyPart;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementDirection;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementSide;
import com.soulfiremc.server.protocol.bot.block.GlobalBlockPalette;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import org.cloudburstmc.math.vector.Vector3d;

public class CollisionCalculator {
  private static final Vector3d START_POSITION = Vector3d.from(0.5, 0, 0.5);
  private static final Vector3d[] STEPS = new Vector3d[]{Vector3d.from(0.25, 0, 0.25), Vector3d.from(0.5, 0, 0.5), Vector3d.from(0.75, 0, 0.75)};
  private static final boolean[][][][] COLLISIONS;

  static {
    var blockStates = GlobalBlockPalette.INSTANCE.getBlockStates();

    COLLISIONS = new boolean[blockStates.length][][][];
    for (var i = 0; i < blockStates.length; i++) {
      var diagonalsArray = new boolean[MovementDirection.DIAGONALS.length][][];
      var blockState = blockStates[i];

      for (var diagonal : MovementDirection.DIAGONALS) {
        var baseOffset = diagonal.offset(SFVec3i.ZERO);

        var bodyPartArray = new boolean[BodyPart.VALUES.length][];
        for (var bodyPart : BodyPart.VALUES) {
          var sideArray = new boolean[MovementSide.VALUES.length];
          for (var side : MovementSide.VALUES) {
            var collides = false;

            for (var step : STEPS) {
              if (collides) {
                break;
              }

              var currentPosition = START_POSITION.add(step.mul(baseOffset.toVector3d()));
              collides = blockState.collidesWith(
                bodyPart.offset(diagonal.side(side).offset(SFVec3i.ZERO)).toVector3i(),
                Player.STANDING_DIMENSIONS.makeBoundingBox(currentPosition)
              );
            }

            sideArray[side.ordinal()] = collides;
          }

          bodyPartArray[bodyPart.ordinal()] = sideArray;
        }
        diagonalsArray[diagonal.diagonalArrayIndex()] = bodyPartArray;
      }

      COLLISIONS[i] = diagonalsArray;
    }
  }

  public static boolean collidesWith(CollisionData collisionData) {
    return COLLISIONS[collisionData.blockState.id()][collisionData.diagonalArrayIndex][collisionData.bodyPart.ordinal()][collisionData.side.ordinal()];
  }

  public record CollisionData(BlockState blockState, int diagonalArrayIndex, BodyPart bodyPart, MovementSide side) {
  }
}
