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

import java.io.Serial;

/**
 * Exception thrown when a node is out of the render distance. The RouteFinder is supposed to catch
 * this exception and insert a path recalculation action and return the best path.
 */
public final class OutOfLevelException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public OutOfLevelException() {
    super();
  }
}
