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
package net.pistonmaster.serverwrecker.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.Map;
import java.util.Optional;

public class BlockItems {
    public static final Map<ItemType, BlockType> VALUES = new Object2ObjectArrayMap<>();

    static {
        for (var itemType : ItemType.VALUES) {
            for (var blockType : BlockType.VALUES) {
                var blockShapeTypes = blockType.blockShapeTypes();

                if (blockType.diggable()
                        && blockShapeTypes.size() == 1
                        && itemType.name().equals(blockType.name())
                        && blockShapeTypes.get(0).isFullBlock()) {
                    VALUES.put(itemType, blockType);
                }
            }
        }
    }

    public static Optional<BlockType> isBlockItem(ItemType itemType) {
        return Optional.ofNullable(VALUES.get(itemType));
    }
}
