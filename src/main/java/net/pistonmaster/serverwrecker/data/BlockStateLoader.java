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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStateLoader {
    private static final Map<String, List<BlockShapeType>> BLOCK_SHAPES = new HashMap<>();

    static {
        try (InputStream inputStream = BlockShapeType.class.getClassLoader().getResourceAsStream("minecraft/blockstates.txt")) {
            if (inputStream == null) {
                throw new IllegalStateException("blockstates.txt not found!");
            }

            new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines().forEach(line -> {
                String[] parts = line.split("\\|");
                String name = parts[0];

                List<BlockShapeType> blockShapeTypes = new ArrayList<>();
                if (parts.length > 1) {
                    String part = parts[1];

                    String[] subParts = part.split(",");
                    for (String subPart : subParts) {
                        Integer id = Integer.parseInt(subPart);

                        BlockShapeType blockShapeType = BlockShapeType.getById(id);
                        blockShapeTypes.add(blockShapeType);
                    }
                }

                BLOCK_SHAPES.put(name, blockShapeTypes);
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<BlockShapeType> getBlockShapes(String name) {
        return BLOCK_SHAPES.get(name);
    }
}
