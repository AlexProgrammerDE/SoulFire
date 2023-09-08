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
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record BlockShapeType(int id, List<BlockShape> blockShapes) {
    public static final List<BlockShapeType> VALUES = new ArrayList<>();

    static {
        try (InputStream inputStream = BlockShapeType.class.getClassLoader().getResourceAsStream("blockshapetypes.txt");) {
            if (inputStream == null) {
                throw new IllegalStateException("blockshapetypes.txt not found!");
            }

            String[] lines = new String(inputStream.readAllBytes()).split("\n");

            for (String line : lines) {
                String[] parts = line.split("\\|");

                int id = Integer.parseInt(parts[0]);
                List<BlockShape> blockShapes = new ArrayList<>();

                if (parts.length > 1) {
                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i];
                        String[] subParts = part.split(",");
                        blockShapes.add(new BlockShape(
                                Integer.parseInt(subParts[0]),
                                Integer.parseInt(subParts[1]),
                                Integer.parseInt(subParts[2]),
                                Integer.parseInt(subParts[3]),
                                Integer.parseInt(subParts[4]),
                                Integer.parseInt(subParts[5])
                        ));
                    }
                }

                VALUES.add(new BlockShapeType(id, blockShapes));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static BlockShapeType register(BlockShapeType blockShapeType) {
        VALUES.add(blockShapeType);
        return blockShapeType;
    }

    public static BlockShapeType getById(int id) {
        for (BlockShapeType blockShapeType : VALUES) {
            if (blockShapeType.id() == id) {
                return blockShapeType;
            }
        }

        return null;
    }

    public double collisionHeight() {
        return blockShapes.stream().mapToDouble(BlockShape::maxY).max().orElse(0);
    }
}
