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

import com.google.common.collect.MultimapBuilder;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.*;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    public List<GraphInstructions> getActions(BotEntityState node) {
        var levelState = node.levelState();

        var blockSubscribers = MultimapBuilder.hashKeys().arrayListValues().<Vector3i, BlockSubscription>build();
        for (var direction : MovementDirection.VALUES) {
            var diagonal = direction.isDiagonal();
            for (var modifier : MovementModifier.VALUES) {
                if (diagonal) {
                    for (var side : MovementSide.VALUES) {
                        var movement = new PlayerMovement(node, direction, side, modifier);
                        var freeSubscription = new BlockSubscription(movement, SubscriptionType.FREE);
                        for (var block : movement.listRequiredFreeBlocks()) {
                            blockSubscribers.put(block, freeSubscription);
                        }

                        blockSubscribers.put(movement.requiredSolidBlock(), new BlockSubscription(movement, SubscriptionType.SOLID));
                    }
                } else {
                    var movement = new PlayerMovement(node, direction, null, modifier);
                    var freeSubscription = new BlockSubscription(movement, SubscriptionType.FREE);
                    for (var block : movement.listRequiredFreeBlocks()) {
                        blockSubscribers.put(block, freeSubscription);
                    }

                    blockSubscribers.put(movement.requiredSolidBlock(), new BlockSubscription(movement, SubscriptionType.SOLID));
                }
            }
        }

        blockSubscribers.asMap().entrySet().parallelStream()
                .forEach(entry -> {
                    var blockState = levelState.getBlockStateAt(entry.getKey())
                            .orElseThrow(OutOfLevelException::new);

                    // We cache only this, but not solid because solid will only occur a single time
                    var calculatedFree = false;
                    var isFree = false;
                    for (var subscriber : entry.getValue()) {
                        var movement = subscriber.movement();
                        if (movement.isImpossible()) {
                            continue;
                        }

                        switch (subscriber.type) {
                            case FREE -> {
                                if (!calculatedFree) {
                                    // We can walk through blocks like air or grass
                                    isFree = blockState.blockShapeType().hasNoCollisions();
                                    calculatedFree = true;
                                }

                                if (!isFree) {
                                    movement.setImpossible(true);
                                }
                            }
                            case SOLID -> {
                                var blockShapeType = blockState.blockShapeType();

                                // Block with a current state that has no collision (Like grass, open fence)
                                if (blockShapeType.hasNoCollisions()) {
                                    movement.setImpossible(true);
                                    continue;
                                }

                                // Prevent walking over cake, slabs, fences, etc.
                                if (!blockShapeType.isFullBlock()) {
                                    // Could destroy and place block here, but that's too much work
                                    movement.setImpossible(true);
                                }
                            }
                        }
                    }
                });

        var targetResults = blockSubscribers.values().stream()
                .map(BlockSubscription::movement)
                .map(PlayerMovement::getInstructions)
                .toList();

        log.debug("Found possible actions for {}", node.position());

        return targetResults;
    }

    enum SubscriptionType {
        FREE,
        SOLID
    }

    record BlockSubscription(PlayerMovement movement, SubscriptionType type) {
    }
}
