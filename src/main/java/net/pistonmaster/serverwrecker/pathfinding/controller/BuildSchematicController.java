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
package net.pistonmaster.serverwrecker.pathfinding.controller;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.data.BlockType;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Map;

@RequiredArgsConstructor
public class BuildSchematicController {
    private final Map<Vector3i, BlockType> blocks;

    public BuildSchematicController(Map<Vector3i, BlockType> relativeBlocks, Vector3i base) {
        this(new Object2ObjectOpenHashMap<>());

        for (Map.Entry<Vector3i, BlockType> entry : relativeBlocks.entrySet()) {
            this.blocks.put(entry.getKey().add(base), entry.getValue());
        }
    }
}
