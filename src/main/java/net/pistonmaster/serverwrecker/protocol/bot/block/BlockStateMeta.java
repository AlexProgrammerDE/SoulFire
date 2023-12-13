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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.pistonmaster.serverwrecker.data.BlockShapeType;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.movement.AABB;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Objects;

public record BlockStateMeta(BlockType blockType, BlockShapeType blockShapeType, int precalculatedHash) {
    private static final BlockShapeType EMPTY_SHAPE = BlockShapeType.getById(0);

    public BlockStateMeta(BlockType blockType, BlockShapeType blockShapeType) {
        this(blockType, blockShapeType, Objects.hash(blockType, blockShapeType));
    }

    public BlockStateMeta(String blockName, int stateIndex) {
        this(Objects.requireNonNull(BlockType.getByName(blockName), "BlockType was null!"), stateIndex);
    }

    private BlockStateMeta(BlockType blockType, int stateIndex) {
        this(blockType, getBlockShapeType(blockType, stateIndex));
    }

    public static BlockStateMeta forDefaultBlockType(BlockType blockType) {
        return new BlockStateMeta(blockType, 0);
    }

    private static BlockShapeType getBlockShapeType(BlockType blockType, int stateIndex) {
        var size = blockType.blockShapeTypes().size();
        if (size == 0) {
            // This block has no shape stored, this is for example for air or grass
            return EMPTY_SHAPE;
        } else if (size == 1) {
            return blockType.blockShapeTypes().getFirst();
        } else {
            return blockType.blockShapeTypes().get(stateIndex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockStateMeta blockStateMeta)) return false;
        return precalculatedHash == blockStateMeta.precalculatedHash;
    }

    @Override
    public int hashCode() {
        return precalculatedHash;
    }

    public List<AABB> getCollisionBoxes(Vector3i block) {
        var shapes = blockShapeType.blockShapes();

        var collisionBoxes = new ObjectArrayList<AABB>(shapes.size());
        for (var shape : shapes) {
            var shapeBB = new AABB(
                    shape.minX(),
                    shape.minY(),
                    shape.minZ(),
                    shape.maxX(),
                    shape.maxY(),
                    shape.maxZ()
            );

            // Apply random offset if needed
            shapeBB = shapeBB.move(blockType.blockProperties().getOffsetForBlock(block));

            // Apply block offset
            shapeBB = shapeBB.move(block.getX(), block.getY(), block.getZ());

            collisionBoxes.add(shapeBB);
        }

        return collisionBoxes;
    }
}
