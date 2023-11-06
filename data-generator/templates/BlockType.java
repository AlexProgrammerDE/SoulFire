package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record BlockType(int id, String name, String displayName, float hardness, int stackSize,
                        boolean diggable, BlockProperties blockProperties, List<BlockShapeType> blockShapeTypes) {
    public static final List<BlockType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static BlockType register(BlockType blockType) {
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

    public boolean isFluid() {
        return this == WATER || this == LAVA;
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
}
