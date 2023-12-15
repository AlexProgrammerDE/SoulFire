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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockStateProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings("unused")
public record BlockShapeType(int id, List<BlockShape> blockShapes, boolean defaultShape,
                             BlockStateProperties properties) {
    public static final List<BlockShapeType> VALUES = new ObjectArrayList<>();

    static {
        try (var inputStream = BlockShapeType.class.getClassLoader().getResourceAsStream("minecraft/blockshapes.txt")) {
            if (inputStream == null) {
                throw new IllegalStateException("blockshapes.txt not found!");
            }

            new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines().forEach(line -> {
                var parts = line.split("\\|");

                var id = Integer.parseInt(parts[0]);
                var blockShapes = new ObjectArrayList<BlockShape>();

                if (parts.length > 1) {
                    for (var i = 1; i < parts.length; i++) {
                        var part = parts[i];
                        var subParts = part.split(",");
                        blockShapes.add(new BlockShape(
                                Double.parseDouble(subParts[0]),
                                Double.parseDouble(subParts[1]),
                                Double.parseDouble(subParts[2]),
                                Double.parseDouble(subParts[3]),
                                Double.parseDouble(subParts[4]),
                                Double.parseDouble(subParts[5])
                        ));
                    }
                }

                VALUES.add(new BlockShapeType(id, blockShapes, ResourceData.BLOCK_STATE_DEFAULTS.contains(id), ResourceData.BLOCK_STATE_PROPERTIES.get(id)));
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static BlockShapeType register(BlockShapeType blockShapeType) {
        VALUES.add(blockShapeType);
        return blockShapeType;
    }

    public static BlockShapeType getById(int id) {
        for (var blockShapeType : VALUES) {
            if (blockShapeType.id() == id) {
                return blockShapeType;
            }
        }

        return null;
    }

    public double collisionHeight() {
        return blockShapes.stream().mapToDouble(BlockShape::maxY).max().orElse(0);
    }

    public boolean isFullBlock() {
        if (blockShapes.size() != 1) {
            return false;
        }

        return blockShapes.getFirst().isFullBlock();
    }

    public boolean hasNoCollisions() {
        return blockShapes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockShapeType blockShapeType)) return false;
        return id == blockShapeType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public boolean isEmpty() {
        return blockShapes.isEmpty();
    }
}
