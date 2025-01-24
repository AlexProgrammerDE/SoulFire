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
package com.soulfiremc.server.data;

import com.soulfiremc.server.data.block.BlockProperties;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.VectorHelper;
import com.soulfiremc.server.util.mcstructs.Direction;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public record FluidState(
  FluidType type,
  int amount,
  float ownHeight,
  boolean source,
  boolean empty,
  BlockPropertiesHolder properties
) {
  private boolean hasSameAbove(Level level, Vector3i blockPos) {
    return type.equals(level.getBlockState(blockPos.add(0, 1, 0)).fluidState().type());
  }

  public float getHeight(Level level, Vector3i blockPos) {
    if (empty) {
      return 0.0F;
    } else {
      return hasSameAbove(level, blockPos) ? 1.0F : ownHeight;
    }
  }

  private boolean affectsFlow(FluidState state) {
    return state.empty() || state.type().equals(type);
  }

  public Vector3d getFlow(Level level, Vector3i blockPos) {
    if (empty) {
      return Vector3d.ZERO;
    }

    var xFlow = 0.0;
    var zFlow = 0.0;
    var currentBlock = Vector3i.ZERO;

    for (var direction : Direction.Plane.HORIZONTAL) {
      currentBlock = direction.offset(blockPos);
      var relativeFluidState = level.getBlockState(currentBlock).fluidState();
      if (this.affectsFlow(relativeFluidState)) {
        var otherStrength = relativeFluidState.ownHeight();
        var strength = 0.0F;
        if (otherStrength == 0.0F) {
          if (!level.getBlockState(currentBlock).blockType().blocksMotion()) {
            var neighborBlock = currentBlock.add(0, -1, 0);
            var neighborBlockState = level.getBlockState(neighborBlock).fluidState();
            if (this.affectsFlow(neighborBlockState)) {
              otherStrength = neighborBlockState.ownHeight();
              if (otherStrength > 0.0F) {
                strength = ownHeight - (otherStrength - 0.8888889F);
              }
            }
          }
        } else if (otherStrength > 0.0F) {
          strength = ownHeight - otherStrength;
        }

        if (strength != 0.0F) {
          xFlow += (float) direction.getStepX() * strength;
          zFlow += (float) direction.getStepZ() * strength;
        }
      }
    }

    var flowVector = Vector3d.from(xFlow, 0.0, zFlow);
    if (properties.get(BlockProperties.FALLING)) {
      for (var direction : Direction.Plane.HORIZONTAL) {
        currentBlock = direction.offset(blockPos);
        if (this.isSolidFace(level, currentBlock, direction) || this.isSolidFace(level, currentBlock.add(0, 1, 0), direction)) {
          flowVector = VectorHelper.normalizeSafe(flowVector).add(0.0, -6.0, 0.0);
          break;
        }
      }
    }

    return VectorHelper.normalizeSafe(flowVector);
  }

  private boolean isSolidFace(Level level, Vector3i neighborPos, Direction side) {
    var blockState = level.getBlockState(neighborPos);
    var fluidState = blockState.fluidState();
    if (fluidState.type().equals(type)) {
      return false;
    } else if (side == Direction.UP) {
      return true;
    } else {
      return !blockState.blockType().iceBlock() && blockState.supportShape().fullFaceDirections().contains(side);
    }
  }
}
