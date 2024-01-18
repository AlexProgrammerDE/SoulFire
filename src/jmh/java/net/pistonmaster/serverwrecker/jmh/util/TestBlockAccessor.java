package net.pistonmaster.serverwrecker.jmh.util;

import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockAccessor;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockStateMeta;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class TestBlockAccessor implements BlockAccessor {
    private final Map<Vector3i, BlockStateMeta> blocks = new HashMap<>();

    public void setBlockAt(int x, int y, int z, BlockType block) {
        blocks.put(Vector3i.from(x, y, z), BlockStateMeta.forDefaultBlockType(block));
    }

    @Override
    public BlockStateMeta getBlockStateAt(int x, int y, int z) {
        var block = blocks.get(Vector3i.from(x, y, z));
        if (block == null) {
            return BlockStateMeta.forDefaultBlockType(BlockType.VOID_AIR);
        }

        return block;
    }
}
