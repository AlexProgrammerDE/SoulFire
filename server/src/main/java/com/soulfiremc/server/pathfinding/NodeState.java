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
package com.soulfiremc.server.pathfinding;

import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;

/**
 * Represents the minimal state we are in the Minecraft world.
 */
public record NodeState(SFVec3i blockPosition, int usableBlockItems) {
  public static NodeState forInfo(SFVec3i blockPosition, ProjectedInventory inventory) {
    return new NodeState(blockPosition, inventory.usableBlockItems());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof NodeState(var otherBlockPosition, var otherUsableBlockItems))) {
      return false;
    }

    return usableBlockItems == otherUsableBlockItems && blockPosition.minecraftEquals(otherBlockPosition);
  }

  @Override
  public int hashCode() {
    var result = blockPosition.hashCode();
    result = 31 * result + usableBlockItems;
    return result;
  }
}
