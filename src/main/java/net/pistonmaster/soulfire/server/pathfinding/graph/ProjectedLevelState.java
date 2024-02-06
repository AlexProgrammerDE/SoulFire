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
package net.pistonmaster.soulfire.server.pathfinding.graph;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.Costs;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.protocol.bot.block.BlockAccessor;
import net.pistonmaster.soulfire.server.util.Vec2ObjectOpenHashMap;

/**
 * An immutable representation of the world state.
 * This takes a world state and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedLevelState {
    private static final BlockState AIR_BLOCK_STATE = BlockState.forDefaultBlockType(BlockType.AIR);

    private final BlockAccessor accessor;
    private final Vec2ObjectOpenHashMap<SWVec3i, BlockState> blockChanges;

    public ProjectedLevelState(BlockAccessor accessor) {
        this(
                accessor,
                new Vec2ObjectOpenHashMap<>()
        );
    }

    public ProjectedLevelState withChangeToSolidBlock(SWVec3i position) {
        var blockChanges = this.blockChanges.clone();
        blockChanges.put(position, Costs.SOLID_PLACED_BLOCK_STATE);

        return new ProjectedLevelState(accessor, blockChanges);
    }

    public ProjectedLevelState withChangeToAir(SWVec3i position) {
        var blockChanges = this.blockChanges.clone();
        blockChanges.put(position, AIR_BLOCK_STATE);

        return new ProjectedLevelState(accessor, blockChanges);
    }

    public ProjectedLevelState withChanges(SWVec3i[] air, SWVec3i solid) {
        var blockChanges = this.blockChanges.clone();
        blockChanges.ensureCapacity(blockChanges.size()
                + (air != null ? air.length : 0)
                + (solid != null ? 1 : 0));

        if (air != null) {
            for (var position : air) {
                if (position == null) {
                    continue;
                }

                blockChanges.put(position, AIR_BLOCK_STATE);
            }
        }

        if (solid != null) {
            blockChanges.put(solid, Costs.SOLID_PLACED_BLOCK_STATE);
        }

        return new ProjectedLevelState(accessor, blockChanges);
    }

    public BlockState getBlockStateAt(SWVec3i position) {
        var blockChange = blockChanges.get(position);
        if (blockChange != null) {
            return blockChange;
        }

        return accessor.getBlockStateAt(position.x, position.y, position.z);
    }

    public boolean isChanged(SWVec3i position) {
        return blockChanges.containsKey(position);
    }
}
