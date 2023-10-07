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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    public List<GraphInstructions> getActions(BotEntityState node) {
        // Just cache lookups of our checks. Here it is best because they are super close to each other.
        var blockCache = new ConcurrentHashMap<Vector3i, Optional<BlockStateMeta>>();

        var targetSet = new ObjectArrayList<GraphAction>();
        for (var direction : MovementDirection.VALUES) {
            for (var modifier : MovementModifier.VALUES) {
                if (direction.isDiagonal()) {
                    for (var side : MovementSide.VALUES) {
                        targetSet.add(new PlayerMovement(tagsState, node, direction, modifier, side, blockCache));
                    }
                } else {
                    targetSet.add(new PlayerMovement(tagsState, node, direction, modifier, null, blockCache));
                }
            }
        }

        var targetResults = targetSet.parallelStream()
                .map(GraphAction::getInstructions)
                .toList();

        log.debug("Found possible actions for {} and cached {} blocks", node.position(), blockCache.size());

        return targetResults;
    }
}
