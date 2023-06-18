package net.pistonmaster.serverwrecker.util;

import net.pistonmaster.serverwrecker.data.BlockType;

public class BlockTypeHelper {
    public static boolean isSolid(BlockType type) {
        return type.hardness() >= 0;
    }
}
