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

import com.soulfiremc.server.pathfinding.graph.actions.movement.BodyPart;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementSide;
import com.soulfiremc.server.pathfinding.world.BlockState;

/// Data needed for diagonal collision calculations.
/// @param blockState The block state to check collision against
/// @param diagonalArrayIndex The index of the diagonal direction
/// @param bodyPart Which body part is being checked
/// @param side Which side of the diagonal movement
public record CollisionData(BlockState blockState, int diagonalArrayIndex, BodyPart bodyPart, MovementSide side) {}
