package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record BlockType(int id, String name, String displayName, float hardness, int stackSize,
                        boolean diggable, boolean requiresCorrectTool, List<BlockShapeType> blockShapeTypes) {
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

    public static BlockType getByMcName(String mcName) {
        for (var blockType : VALUES) {
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
