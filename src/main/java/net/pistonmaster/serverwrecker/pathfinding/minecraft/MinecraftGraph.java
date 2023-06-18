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

import java.util.HashSet;
import java.util.Set;

@Slf4j
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

            BlockType feetBlockType = levelState.getBlockTypeAt(targetPos);
            BlockType headBlockType = levelState.getBlockTypeAt(targetPos.add(0, 1, 0));
            if (!BlockTypeHelper.isSolid(feetBlockType) && !BlockTypeHelper.isSolid(headBlockType)) {
                targetSet.add(new BlockPosition(playerMovement.getTargetPos()));
            }
        }

        return targetSet;
    }
}
