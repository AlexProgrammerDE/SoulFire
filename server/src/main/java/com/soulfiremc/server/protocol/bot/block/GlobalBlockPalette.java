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
package com.soulfiremc.server.protocol.bot.block;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import lombok.ToString;

import java.util.Collection;
import java.util.List;

@ToString
public class GlobalBlockPalette {
  public static final GlobalBlockPalette INSTANCE;

  static {
    INSTANCE = new GlobalBlockPalette(BlockType.REGISTRY.values()
      .stream().<BlockState>mapMulti((blockEntry, consumer) -> {
        for (var state : blockEntry.statesData().possibleStates()) {
          consumer.accept(state);
        }
      }).toList());
  }

  private final BlockState[] stateIdToBlockState;

  public GlobalBlockPalette(Collection<BlockState> states) {
    this.stateIdToBlockState = new BlockState[states.size()];
    for (var entry : states) {
      this.stateIdToBlockState[entry.id()] = entry;
    }
  }

  public BlockState getBlockStateForStateId(int id) {
    return stateIdToBlockState[id];
  }

  public Collection<BlockState> getBlockStates() {
    return List.of(stateIdToBlockState);
  }
}
