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
package net.pistonmaster.soulfire.data;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.AccessLevel;
import lombok.With;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record BlockType(int id, String name, float destroyTime, float explosionResistance,
                        boolean air, boolean fallingBlock, boolean replaceable,
                        boolean requiresCorrectToolForDrops, boolean fluidSource,
                        OffsetData offsetData, BlockStatesData statesData) {
    public static final Int2ReferenceMap<BlockType> FROM_ID = new Int2ReferenceOpenHashMap<>();
    public static final Object2ReferenceMap<String, BlockType> FROM_NAME = new Object2ReferenceOpenHashMap<>();

    // VALUES REPLACE

    public static BlockType register(String name) {
        var blockType = GsonDataHelper.fromJson("/minecraft/blocks.json", name, BlockType.class);
        blockType = blockType.withStatesData(BlockStatesData.fromJsonArray(
                blockType,
                GsonDataHelper.fromJson("/minecraft/blocks.json", name, JsonObject.class)
                        .getAsJsonArray("states")));

        FROM_ID.put(blockType.id(), blockType);
        FROM_NAME.put(blockType.name(), blockType);
        return blockType;
    }

    public static BlockType getById(int id) {
        return FROM_ID.get(id);
    }

    public static BlockType getByName(String name) {
        return FROM_NAME.get(name.replace("minecraft:", ""));
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
