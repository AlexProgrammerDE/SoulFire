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
import com.soulfiremc.server.protocol.bot.state.ChunkData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.ToString;

@ToString
public class GlobalBlockPalette {
  public static final GlobalBlockPalette INSTANCE;

  static {
    var stateMap = new Int2ObjectOpenHashMap<BlockState>();
    for (var blockEntry : BlockType.FROM_ID.values()) {
      for (var state : blockEntry.statesData().possibleStates()) {
        stateMap.put(state.id(), state);
      }
    }

    INSTANCE = new GlobalBlockPalette(stateMap);
  }

  @Getter
  private final int maxStates;
  @Getter
  private final int blockBitsPerEntry;
  private final BlockState[] stateIdToBlockState;

  public GlobalBlockPalette(Int2ObjectMap<BlockState> states) {
    this.maxStates = states.size();
    this.blockBitsPerEntry = ChunkData.log2RoundUp(maxStates);
    this.stateIdToBlockState = new BlockState[maxStates];
    for (var entry : states.int2ObjectEntrySet()) {
      this.stateIdToBlockState[entry.getIntKey()] = entry.getValue();
    }
  }

  public BlockState getBlockStateForStateId(int id) {
    return stateIdToBlockState[id];
  }
}
