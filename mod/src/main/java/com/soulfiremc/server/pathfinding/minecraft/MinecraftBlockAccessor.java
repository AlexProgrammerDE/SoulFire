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
package com.soulfiremc.server.pathfinding.minecraft;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.world.BlockAccessor;
import com.soulfiremc.server.pathfinding.world.BlockState;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;

/// Minecraft implementation of BlockAccessor interface.
@RequiredArgsConstructor
public final class MinecraftBlockAccessor implements BlockAccessor {
  private final BlockGetter blockGetter;
  private final LevelHeightAccessor heightAccessor;

  @Override
  public BlockState getBlockState(SFVec3i position) {
    var mcState = blockGetter.getBlockState(new BlockPos(position.x, position.y, position.z));
    return new MinecraftBlockState(mcState);
  }

  @Override
  public boolean isOutsideBuildHeight(int y) {
    return heightAccessor.isOutsideBuildHeight(y);
  }

  /// Unwrap to the underlying Minecraft BlockGetter.
  public BlockGetter unwrap() {
    return blockGetter;
  }
}
