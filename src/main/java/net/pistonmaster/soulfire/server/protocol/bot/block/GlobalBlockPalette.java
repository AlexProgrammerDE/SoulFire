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
package net.pistonmaster.soulfire.server.protocol.bot.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;
import lombok.ToString;
import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.protocol.bot.state.ChunkData;

@ToString
public class GlobalBlockPalette {
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
