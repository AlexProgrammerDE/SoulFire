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

import java.util.ArrayList;
import java.util.List;

public class BlockItems {
    public static final List<ItemType> VALUES = new ArrayList<>();

    static {
        for (ItemType itemType : ItemType.VALUES) {
            for (BlockType blockType : BlockType.VALUES) {
                if (blockType.diggable()
                        && blockType.boundingBox() == BoundingBoxType.BLOCK
                        && itemType.name().equals(blockType.name())
                        && blockType.blockShapeType().collisionHeight() == 1) {
                    VALUES.add(itemType);
                }
            }
        }
    }

    public static boolean isBlockItem(ItemType itemType) {
        return VALUES.contains(itemType);
    }
}
