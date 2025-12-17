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
package com.soulfiremc.server.pathfinding.execution;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;

/// Represents a block break action at a specific position with mining cost data.
/// The actual execution logic is implemented in the Minecraft-specific module.
public record BlockBreakAction(MovementMiningCost miningCost) implements WorldAction {
  /// Returns the block position to break.
  public SFVec3i blockPosition() {
    return miningCost.block();
  }
  @Override
  public int getAllowedTicks() {
    // 60-seconds max to break a block
    return 60 * 20;
  }

  @Override
  public String toString() {
    return "BlockBreakAction -> " + miningCost.block().formatXYZ();
  }
}
