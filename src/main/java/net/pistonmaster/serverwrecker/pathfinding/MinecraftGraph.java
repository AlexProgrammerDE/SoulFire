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
package net.pistonmaster.serverwrecker.pathfinding;

import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public record MinecraftGraph(SessionDataManager sessionDataManager) {
    public List<MinecraftAction> getConnections(BlockPosition node) {
        Vector3d from = node.position();

        LevelState levelState = sessionDataManager.getCurrentLevel();
        if (levelState == null) {
            return List.of();
        }

        List<MinecraftAction> targetSet = new ArrayList<>();
        for (BasicMovementEnum action : BasicMovementEnum.values()) {
            log.debug("Checking action {}", action);
            PlayerMovement playerMovement = new PlayerMovement(from, action, sessionDataManager);

            targetSet.add(playerMovement);
        }

        log.debug("Found {} possible actions for {}", targetSet, node);

        return targetSet;
    }
}
