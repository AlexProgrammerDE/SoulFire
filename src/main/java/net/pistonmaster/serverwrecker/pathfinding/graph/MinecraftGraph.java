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
package net.pistonmaster.serverwrecker.pathfinding.graph;

import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MinecraftGraph {
    public List<GraphAction> getConnections(BotEntityState node) {
        List<GraphAction> targetSet = new ArrayList<>();
        for (MovementDirection direction : MovementDirection.values()) {
            for (MovementModifier modifier : MovementModifier.values()) {
                for (MovementSide side : MovementSide.values()) {
                    PlayerMovement playerMovement = new PlayerMovement(node, direction, modifier, side);

                    targetSet.add(playerMovement);
                }
            }
        }

        log.debug("Found {} possible actions for {}", targetSet.size(), node.position());

        return targetSet;
    }
}
