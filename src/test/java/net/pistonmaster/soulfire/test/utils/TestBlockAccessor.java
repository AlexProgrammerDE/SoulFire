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
package net.pistonmaster.soulfire.test.utils;

import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.protocol.bot.block.BlockAccessor;
import net.pistonmaster.soulfire.server.util.Vec2ObjectOpenHashMap;

public class TestBlockAccessor implements BlockAccessor {
    private final Vec2ObjectOpenHashMap<SWVec3i, BlockState> blocks = new Vec2ObjectOpenHashMap<>();
    private final BlockState defaultBlock;

    public TestBlockAccessor() {
        this(BlockState.forDefaultBlockType(BlockType.AIR));
    }

    public TestBlockAccessor(BlockState defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public void setBlockAt(int x, int y, int z, BlockType block) {
        blocks.put(new SWVec3i(x, y, z), BlockState.forDefaultBlockType(block));
    }

    @Override
    public BlockState getBlockStateAt(int x, int y, int z) {
        return blocks.getOrDefault(new SWVec3i(x, y, z), defaultBlock);
    }
}
