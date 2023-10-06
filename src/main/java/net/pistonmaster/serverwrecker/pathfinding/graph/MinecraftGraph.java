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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final int INSTRUCTION_COUNT;

    static {
        var count = 0;
        for (var direction : MovementDirection.values()) {
            for (var ignored : MovementModifier.values()) {
                if (direction.isDiagonal()) {
                    for (var ignored2 : MovementSide.values()) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
        }
        INSTRUCTION_COUNT = count;
    }

    public List<GraphInstructions> getActions(BotEntityState node) {
        // Just cache lookups of our checks. Here it is best because they are super close to each other.
        LoadingCache<Vector3i, Optional<BlockStateMeta>> blockCache = Caffeine.newBuilder().build(k ->
                node.levelState().getBlockStateAt(k));

        var targetSet = new ObjectArrayList<PlayerMovement>(INSTRUCTION_COUNT);
        for (var direction : MovementDirection.values()) {
            for (var modifier : MovementModifier.values()) {
                if (direction.isDiagonal()) {
                    for (var side : MovementSide.values()) {
                        targetSet.add(new PlayerMovement(tagsState, node, direction, modifier, side, blockCache));
                    }
                } else {
                    targetSet.add(new PlayerMovement(tagsState, node, direction, modifier, null, blockCache));
                }
            }
        }

        var targetResults = targetSet.stream()
                .parallel()
                .map(PlayerMovement::getInstructions)
                .toList();

        log.debug("Found {} possible actions for {} and cached {} blocks", targetResults.stream()
                .filter(a -> !a.isImpossible())
                .count(), node.position(), blockCache.estimatedSize());

        return targetResults;
    }
}
