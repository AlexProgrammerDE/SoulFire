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
import com.soulfiremc.server.protocol.bot.block.GlobalBlockPalette;
import com.soulfiremc.server.protocol.bot.state.entity.Player;

public class CollisionCalculator {
  private static final boolean[][][] COLLISIONS;

  static {
    var blockStates = GlobalBlockPalette.INSTANCE.getBlockStates();

    COLLISIONS = new boolean[blockStates.length][][];
    for (var i = 0; i < blockStates.length; i++) {
      var diagonalsArray = new boolean[MovementDirection.DIAGONALS.length][];
      var blockState = blockStates[i];

      for (var diagonal : MovementDirection.DIAGONALS) {
        var bodyPartArray = new boolean[BodyPart.VALUES.length];
        for (var bodyPart : BodyPart.VALUES) {
          bodyPartArray[bodyPart.ordinal()] = blockState.collidesWith(
            bodyPart.offset(diagonal.offset(SFVec3i.ZERO)).toVector3i(),
            Player.STANDING_DIMENSIONS.makeBoundingBox(diagonal.edgeOffset(SFVec3i.ZERO).toVector3d())
          );
        }
        diagonalsArray[diagonal.diagonalArrayIndex()] = bodyPartArray;
      }

      COLLISIONS[i] = diagonalsArray;
    }
  }

  public static boolean collidesWith(BlockState blockState, int diagonalArrayIndex, BodyPart bodyPart) {
    return COLLISIONS[blockState.id()][diagonalArrayIndex][bodyPart.ordinal()];
  }
}
