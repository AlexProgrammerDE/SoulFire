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

import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public enum BlockSafetyType {
  FALLING_AND_FLUIDS {
    @Override
    public boolean isUnsafeBlock(BlockState state) {
      return state.getFluidState().getType() != Fluids.EMPTY || state.getBlock() instanceof FallingBlock;
    }
  },
  FLUIDS {
    @Override
    public boolean isUnsafeBlock(BlockState state) {
      return state.getFluidState().getType() != Fluids.EMPTY;
    }
  };

  public abstract boolean isUnsafeBlock(BlockState state);
}
