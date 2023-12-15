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
package net.pistonmaster.serverwrecker.server.data;

import java.util.Optional;

public class BlockItems {
    public static final BlockType[] VALUES = new BlockType[ItemType.VALUES.size()];
    public static final ItemType[] VALUES_REVERSE = new ItemType[BlockType.VALUES.size()];

    static {
        for (var itemType : ItemType.VALUES) {
            for (var blockType : BlockType.VALUES) {
                var blockShapeTypes = blockType.blockShapeTypes();

                // Let's not use bedrock as a building block
                if (blockType.diggable()
                        && blockShapeTypes.size() == 1
                        && itemType.name().equals(blockType.name())
                        && blockShapeTypes.getFirst().isFullBlock()) {
                    VALUES[itemType.id()] = blockType;
                    VALUES_REVERSE[blockType.id()] = itemType;
                }
            }
        }
    }

    public static Optional<BlockType> getBlockType(ItemType itemType) {
        return Optional.ofNullable(VALUES[itemType.id()]);
    }

    public static Optional<ItemType> getItemType(BlockType blockType) {
        return Optional.ofNullable(VALUES_REVERSE[blockType.id()]);
    }

    public static boolean hasItemType(BlockType blockType) {
        return VALUES_REVERSE[blockType.id()] != null;
    }
}
