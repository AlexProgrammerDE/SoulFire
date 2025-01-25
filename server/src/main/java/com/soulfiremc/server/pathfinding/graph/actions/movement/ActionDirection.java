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
package com.soulfiremc.server.pathfinding.graph.actions.movement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionDirection {
  NORTH {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == SOUTH;
    }
  },
  SOUTH {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == NORTH;
    }
  },
  EAST {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == WEST;
    }
  },
  WEST {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == EAST;
    }
  },
  NORTH_EAST {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == SOUTH_WEST || direction == SOUTH || direction == WEST;
    }
  },
  NORTH_WEST {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == SOUTH_EAST || direction == SOUTH || direction == EAST;
    }
  },
  SOUTH_EAST {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == NORTH_WEST || direction == NORTH || direction == WEST;
    }
  },
  SOUTH_WEST {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == NORTH_EAST || direction == NORTH || direction == EAST;
    }
  },
  UP {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == DOWN;
    }
  },
  DOWN {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return direction == UP;
    }
  },
  // Jumps, diagonal jumps, falls, diagonal falls, etc.
  // For those it's sometimes smart to fall down and then go in reverse direction, but lower/higher
  SPECIAL {
    @Override
    public boolean isOpposite(ActionDirection direction) {
      return false;
    }
  };

  public static final ActionDirection[] VALUES = values();

  /**
   * Checks if the given direction is the opposite of this direction.
   * This also includes that the opposite of UP is DOWN and vice versa.
   * It also includes diagonals, so NORTH_EAST is the opposite of SOUTH_WEST,
   * but also the opposite of SOUTH and WEST.
   *
   * @param direction The direction to check
   * @return If the given direction is the opposite of this direction
   */
  public abstract boolean isOpposite(ActionDirection direction);
}
