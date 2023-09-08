package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record BlockType(int id, String name, String displayName, double hardness, double resistance,
                        int stackSize, boolean diggable, BoundingBoxType boundingBox, List<ItemType> tools,
                        double collisionHeight) {
    public static final List<BlockType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static BlockType register(BlockType blockType) {
        VALUES.add(blockType);
        return blockType;
    }

    public static BlockType getById(int id) {
        for (BlockType blockType : VALUES) {
            if (blockType.id() == id) {
                return blockType;
            }
        }

        return null;
    }

    public static BlockType getByMcName(String mcName) {
        for (BlockType blockType : VALUES) {
            if (("minecraft:" + blockType.name()).equals(mcName)) {
                return blockType;
            }
        }

        return null;
    }

    public boolean isFluid() {
        return this == WATER || this == LAVA;
    }
}
