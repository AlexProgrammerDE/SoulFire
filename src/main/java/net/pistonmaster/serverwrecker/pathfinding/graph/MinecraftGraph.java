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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.*;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final int MAX_MOVEMENTS = 60;
    private static final int MAX_BLOCKS = 58;
    private static final int MAX_SUBSCRIBERS = 15;

    public Iterable<GraphInstructions> getActions(BotEntityState node) {
        var levelState = node.levelState();

        var movements = new ObjectArrayList<PlayerMovement>(MAX_MOVEMENTS);
        var blockSubscribers = new Object2ObjectOpenHashMap<Vector3i, List<BlockSubscription>>(MAX_BLOCKS);
        for (var direction : MovementDirection.VALUES) {
            var diagonal = direction.isDiagonal();
            for (var modifier : MovementModifier.VALUES) {
                if (diagonal) {
                    for (var side : MovementSide.VALUES) {
                        registerMovement(movements, blockSubscribers, new PlayerMovement(node, direction, side, modifier));
                    }
                } else {
                    registerMovement(movements, blockSubscribers, new PlayerMovement(node, direction, null, modifier));
                }
            }
        }

        blockSubscribers.object2ObjectEntrySet().forEach(entry -> {
            var subscribers = entry.getValue();
            var nonePossible = subscribers.stream().allMatch(s -> s.movement().isImpossible());
            if (nonePossible) {
                return;
            }

            var blockState = levelState.getBlockStateAt(entry.getKey())
                    .orElseThrow(OutOfLevelException::new);

            // We cache only this, but not solid because solid will only occur a single time
            var calculatedFree = false;
            var isFree = false;
            for (var subscriber : subscribers) {
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

        return movements.stream()
                .filter(m -> !m.isImpossible())
                .map(PlayerMovement::getInstructions)::iterator;
    }

    private void registerMovement(ObjectArrayList<PlayerMovement> movements,
                                  Object2ObjectMap<Vector3i, List<BlockSubscription>> blockSubscribers,
                                  PlayerMovement movement) {
        movements.add(movement);

        var freeSubscription = new BlockSubscription(movement, SubscriptionType.FREE);
        for (var block : movement.listRequiredFreeBlocks()) {
            blockSubscribers.computeIfAbsent(block, k -> new ObjectArrayList<>(MAX_SUBSCRIBERS))
                    .add(freeSubscription);
        }

        var solidSubscription = new BlockSubscription(movement, SubscriptionType.SOLID);
        blockSubscribers.computeIfAbsent(movement.requiredSolidBlock(), k -> new ObjectArrayList<>(MAX_SUBSCRIBERS))
                .add(solidSubscription);
    }

    enum SubscriptionType {
        FREE,
        SOLID
    }

    record BlockSubscription(PlayerMovement movement, SubscriptionType type) {
    }
}
