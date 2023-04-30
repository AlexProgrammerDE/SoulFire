package net.pistonmaster.serverwrecker.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public final class BlockType {
    public static final List<BlockType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    private final int id;
    private final String name;
    private final String displayName;
    private final double hardness;
    private final double resistance;
    private final int stackSize;
    private final boolean diggable;
    private final String material;
    private final BoundingBoxType boundingBox;

    public static BlockType register(BlockType blockType) {
        VALUES.add(blockType);
        return blockType;
    }

    public static BlockType getById(int id) {
        for (BlockType blockType : VALUES) {
            if (blockType.getId() == id) {
                return blockType;
            }
        }

        return null;
    }

    public static BlockType getByMcName(String mcName) {
        for (BlockType blockType : VALUES) {
            if (("minecraft:" + blockType.getName()).equals(mcName)) {
                return blockType;
            }
        }

        return null;
    }

    public boolean isFluid() {
        return this == WATER || this == LAVA;
    }
}
