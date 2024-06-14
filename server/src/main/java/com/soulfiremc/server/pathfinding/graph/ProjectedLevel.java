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
import com.soulfiremc.server.protocol.bot.block.BlockAccessor;
import com.soulfiremc.server.protocol.bot.state.LevelHeightAccessor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * An immutable representation of the world state. This takes a world state and projects changes
 * onto it. This way we calculate the way we can do actions after a block was broken/placed.
 */
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ProjectedLevel {
  private final LevelHeightAccessor levelHeightAccessor;
  private final BlockAccessor blockAccessor;

  public BlockState getBlockState(SFVec3i position) {
    return blockAccessor.getBlockState(position.x, position.y, position.z);
  }

  public boolean isPlaceable(SFVec3i position) {
    return !levelHeightAccessor.isOutsideBuildHeight(position.y);
  }
}
