/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot.block;

import net.pistonmaster.serverwrecker.data.BlockShapeType;
import net.pistonmaster.serverwrecker.data.BlockType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record BlockStateMeta(BlockType blockType, @Nullable BlockShapeType blockShapeType) {
    public BlockStateMeta(String blockName, int stateIndex) {
        this(Objects.requireNonNull(BlockType.getByMcName(blockName), "BlockType was null!"), stateIndex);
    }

    private BlockStateMeta(BlockType blockType, int stateIndex) {
        this(blockType, getBlockShapeType(blockType, stateIndex));
    }

    private static BlockShapeType getBlockShapeType(BlockType blockType, int stateIndex) {
        int size = blockType.blockShapeTypes().size();
        if (size == 0) {
            // This block has no shape, this is for example for air or grass
            return null;
        } else if (size == 1) {
            return blockType.blockShapeTypes().get(0);
        } else {
            return blockType.blockShapeTypes().get(stateIndex);
        }
    }
}
