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
package com.soulfiremc.server.pathfinding.cost;

/// Represents the cost of mining a specific block type with the best available tool.
/// This is computed once per block type and cached for performance.
///
/// @param miningCost The cost in pathfinding units to mine this block
/// @param willDropUsableBlockItem Whether mining this block will drop a usable block item
public record BlockMiningCosts(double miningCost, boolean willDropUsableBlockItem) {}
