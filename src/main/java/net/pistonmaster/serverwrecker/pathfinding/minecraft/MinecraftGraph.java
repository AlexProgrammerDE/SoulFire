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
package net.pistonmaster.serverwrecker.pathfinding.minecraft;

import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.pathfinding.Graph;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public record MinecraftGraph(SessionDataManager sessionDataManager) implements Graph {
    @Override
    public Map<BlockPosition, MinecraftAction> getConnections(BlockPosition node) {
        Vector3d from = node.position();

        LevelState levelState = sessionDataManager.getCurrentLevel();
        if (levelState == null) {
            return Map.of();
        }

        Map<Vector3i, Boolean> solidBlockMap = new HashMap<>();
        Map<BlockPosition, MinecraftAction> targetSet = new HashMap<>();
        actions: for (BasicMovementEnum action : BasicMovementEnum.values()) {
            log.debug("Checking action {}", action);
            PlayerMovement playerMovement = new PlayerMovement(from, action);

            for (Vector3i requiredFreeBlock : playerMovement.requiredFreeBlocks()) {
                Boolean solid = solidBlockMap.get(requiredFreeBlock);
                if (solid != null) {
                    if (solid) {
                        log.debug("We cached the block {} is solid, so we can't move there!", requiredFreeBlock);
                        continue actions;
                    } else {
                        continue;
                    }
                }

                BlockType blockType = levelState.getBlockTypeAt(requiredFreeBlock);
                if (BlockTypeHelper.isSolid(blockType)) {
                    log.debug("Block {} is solid (type {}), so we can't move there!", requiredFreeBlock, blockType.name());
                    solidBlockMap.put(requiredFreeBlock, true);
                    continue actions;
                } else {
                    solidBlockMap.put(requiredFreeBlock, false);
                }
            }

            for (Vector3i requiredSolidBlock : playerMovement.requiredSolidBlocks()) {
                Boolean solid = solidBlockMap.get(requiredSolidBlock);
                if (solid != null) {
                    if (!solid) {
                        log.debug("We cached the block {} is not solid, so we can't move on it!", requiredSolidBlock);
                        continue actions;
                    } else {
                        continue;
                    }
                }

                BlockType blockType = levelState.getBlockTypeAt(requiredSolidBlock);
                if (!BlockTypeHelper.isSolid(blockType)) {
                    log.debug("Block {} is not solid, so we can't move on it!", requiredSolidBlock);
                    solidBlockMap.put(requiredSolidBlock, false);
                    continue actions;
                } else {
                    solidBlockMap.put(requiredSolidBlock, true);
                }
            }

            targetSet.put(new BlockPosition(playerMovement.getTargetPos()), playerMovement);
        }

        log.debug("Found {} possible actions for {}", targetSet.values(), node);

        return targetSet;
    }
}
