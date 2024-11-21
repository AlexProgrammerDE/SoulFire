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

import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.mcstructs.Direction;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public record FluidState(
  FluidType type,
  int amount,
  float ownHeight,
  boolean source,
  boolean empty,
  BlockStateProperties properties
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

    var d = 0.0;
    var e = 0.0;
    var currentBlock = Vector3i.ZERO;

    for (var direction : Direction.Plane.HORIZONTAL) {
      currentBlock = direction.offset(blockPos);
      var relativeFluidState = level.getBlockState(currentBlock).fluidState();
      if (this.affectsFlow(relativeFluidState)) {
        var f = relativeFluidState.ownHeight();
        var g = 0.0F;
        if (f == 0.0F) {
          if (!level.getBlockState(currentBlock).blocksMotion()) {
            var lv4 = currentBlock.add(0, -1, 0);
            var lv5 = level.getBlockState(lv4).fluidState();
            if (this.affectsFlow(lv5)) {
              f = lv5.ownHeight();
              if (f > 0.0F) {
                g = ownHeight - (f - 0.8888889F);
              }
            }
          }
        } else if (f > 0.0F) {
          g = ownHeight - f;
        }

        if (g != 0.0F) {
          d += (float) direction.getStepX() * g;
          e += (float) direction.getStepZ() * g;
        }
      }
    }

    var lv6 = Vector3d.from(d, 0.0, e);
    if (properties.getBoolean("falling")) {
      for (var direction : Direction.Plane.HORIZONTAL) {
        currentBlock = direction.offset(blockPos);
        if (this.isSolidFace(level, currentBlock, direction) || this.isSolidFace(level, currentBlock.add(0, 1, 0), direction)) {
          lv6 = lv6.normalize().add(0.0, -6.0, 0.0);
          break;
        }
      }
    }

    return lv6.normalize();
  }

  private boolean isSolidFace(Level level, Vector3i neighborPos, Direction side) {
    var blockState = level.getBlockState(neighborPos);
    var fluidState = blockState.fluidState();
    if (fluidState.type().equals(type)) {
      return false;
    } else if (side == Direction.UP) {
      return true;
    } else {
      return blockState.blockType().iceBlock() ? false : blockState.isFaceSturdy(level, neighborPos, side);
    }
  }
}
