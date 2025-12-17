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

/// Marker interface for actions that can be performed in the world.
/// This is an abstraction that allows the pathfinding library to work
/// without depending on Minecraft-specific code.
///
/// Implementations of this interface will be Minecraft-specific and handle
/// the actual execution of the action.
public interface WorldAction {
  /// Returns the number of ticks allowed for this action before timeout.
  int getAllowedTicks();
}
