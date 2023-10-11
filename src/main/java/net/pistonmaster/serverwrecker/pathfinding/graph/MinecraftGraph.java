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
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.*;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3i;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final int MAX_MOVEMENTS = 60;
    private static final int EXPECTED_BLOCKS = 58;
    private static final int MAX_SUBSCRIBERS = 15;

    public Iterable<GraphInstructions> getActions(BotEntityState node) {
        var levelState = node.levelState();

        var movements = new ObjectArrayList<PlayerMovement>(MAX_MOVEMENTS);
        var blockSubscribers = new QuickVectorSubscriberMap();
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

        blockSubscribers.forEach((k, v) -> {
            BlockStateMeta blockState = null;

            // We cache only this, but not solid because solid will only occur a single time
            var calculatedFree = false;
            var isFree = false;
            for (var subscriber : v) {
                if (subscriber == null) {
                    break;
                }

                var movement = subscriber.movement();
                if (movement.isImpossible()) {
                    continue;
                }

                if (blockState == null) {
                    blockState = levelState.getBlockStateAt(k)
                            .orElseThrow(OutOfLevelException::new);
                }

                switch (subscriber.type) {
                    case FREE -> {
                        if (!calculatedFree) {
                            // We can walk through blocks like air or grass
                            isFree = blockState.blockShapeType().hasNoCollisions()
                                    && !BlockTypeHelper.isFluid(blockState.blockType());
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

        var results = new ObjectArrayList<GraphInstructions>(MAX_MOVEMENTS);
        for (var movement : movements) {
            if (movement.isImpossible()) {
                continue;
            }

            results.add(movement.getInstructions());
        }

        return results;
    }

    private void registerMovement(ObjectArrayList<PlayerMovement> movements,
                                  QuickVectorSubscriberMap blockSubscribers,
                                  PlayerMovement movement) {
        movements.add(movement);

        var freeSubscription = new BlockSubscription(movement, SubscriptionType.FREE);
        for (var freeBlock : movement.listRequiredFreeBlocks()) {
            blockSubscribers.add(freeBlock, freeSubscription);
        }

        var solidSubscription = new BlockSubscription(movement, SubscriptionType.SOLID);
        blockSubscribers.add(movement.requiredSolidBlock(), solidSubscription);
    }

    enum SubscriptionType {
        FREE,
        SOLID
    }

    record BlockSubscription(PlayerMovement movement, SubscriptionType type) {
    }

    private static class QuickVectorSubscriberMap {
        private final Vector3i[] keys = new Vector3i[EXPECTED_BLOCKS];
        private final BlockSubscription[][] values = new BlockSubscription[EXPECTED_BLOCKS][MAX_SUBSCRIBERS];

        public void add(Vector3i key, BlockSubscription value) {
            for (var i = 0; i < EXPECTED_BLOCKS; i++) {
                var currentKey = keys[i];
                var isNull = currentKey == null;
                if (isNull) {
                    keys[i] = key;
                }

                if (isNull || currentKey.equals(key)) {
                    var valueArray = values[i];
                    for (var j = 0; j < MAX_SUBSCRIBERS; j++) {
                        if (valueArray[j] == null) {
                            valueArray[j] = value;
                            return;
                        }
                    }

                    throw new IllegalStateException("Too many subscribers for a single block!");
                }
            }
        }

        public void forEach(SubscriptionEntryConsumer consumer) {
            for (var i = 0; i < EXPECTED_BLOCKS; i++) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    private interface SubscriptionEntryConsumer {
        void accept(Vector3i key, BlockSubscription[] value);
    }
}
