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
package net.pistonmaster.soulfire.server.data;

import net.pistonmaster.soulfire.server.util.BlockTypeHelper;

import java.util.Optional;

public class BlockItems {
    public static final BlockType[] VALUES = new BlockType[ItemType.FROM_ID.size()];
    public static final ItemType[] VALUES_REVERSE = new ItemType[BlockType.FROM_ID.size()];

    static {
        for (var itemType : ItemType.FROM_ID.values()) {
            for (var blockType : BlockType.FROM_ID.values()) {
                var blockShapeTypes = BlockState.forDefaultBlockType(blockType).blockShapeGroup();

                // Let's not use bedrock as a building block
                if (BlockTypeHelper.isDiggable(blockType)
                        && itemType.name().equals(blockType.name())
                        && blockShapeTypes.isFullBlock()) {
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
