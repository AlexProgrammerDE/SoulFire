package net.pistonmaster.serverwrecker.pathfinding.minecraft;

import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.pathfinding.Graph;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;
import java.util.Set;

public record MinecraftGraph(SessionDataManager sessionDataManager) implements Graph<MinecraftAction> {
    @Override
    public Set<MinecraftAction> getConnections(MinecraftAction node) {
        Vector3d from = node.getTargetPos();

        LevelState levelState = sessionDataManager.getCurrentLevel();
        if (levelState == null) {
            return Set.of();
        }

        Set<MinecraftAction> targetSet = new HashSet<>();
        for (BasicMovementAction action : BasicMovementAction.values()) {
            PlayerMovement playerMovement = new PlayerMovement(from, action);
            Vector3i targetPos = playerMovement.getTargetPos().toInt();

            BlockType blockType = levelState.getBlockTypeAt(targetPos);
            BlockType aboveBlockType = levelState.getBlockTypeAt(targetPos.add(0, 1, 0));
            if (BlockTypeHelper.isSolid(blockType) && !BlockTypeHelper.isSolid(aboveBlockType)) {
                targetSet.add(playerMovement);
            }
        }

        return targetSet;
    }
}
