/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.data;

import lombok.AccessLevel;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record BlockType(int id, String name, float destroyTime, float explosionResistance,
                        boolean air, boolean fallingBlock, boolean replaceable,
                        boolean requiresCorrectToolForDrops, boolean fluidSource,
                        OffsetData offsetData, List<BlockShapeType> blockShapeTypes) {
    public static final List<BlockType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static BlockType register(String name) {
        var blockType = GsonDataHelper.fromJson("/minecraft/blocks.json", name, BlockType.class)
                .withBlockShapeTypes(BlockStateLoader.getBlockShapes(name));
        VALUES.add(blockType);
        return blockType;
    }

    public static BlockType getById(int id) {
        for (var blockType : VALUES) {
            if (blockType.id() == id) {
                return blockType;
            }
        }

        return null;
    }

    public static BlockType getByName(String name) {
        for (var blockType : VALUES) {
            if (blockType.name().equals(name) || ("minecraft:" + blockType.name()).equals(name)) {
                return blockType;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockType blockType)) return false;
        return id == blockType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public record OffsetData(float maxHorizontalOffset, float maxVerticalOffset, OffsetType offsetType) {
        public enum OffsetType {
            XZ,
            XYZ
        }
    }
}
