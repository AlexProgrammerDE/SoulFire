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

import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.*;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.function.Consumer;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final int MAX_MOVEMENTS = 60;
    private static final int EXPECTED_BLOCKS = 58;
    private static final int MAX_SUBSCRIBERS = 15;
    private static final Object2ObjectFunction<? super Vector3i, ? extends ObjectList<BlockSubscription>> CREATE_MISSING_FUNCTION =
            k -> new ObjectArrayList<>(MAX_SUBSCRIBERS);
    private static final Consumer<? super Object2ObjectMap.Entry<Vector3i, ObjectList<BlockSubscription>>> SUBSCRIPTION_CONSUMER = e -> {
        BlockStateMeta blockState = null;

        // We cache only this, but not solid because solid will only occur a single time
        var calculatedFree = false;
        var isFree = false;
        var calculatedSolid = false;
        var isSolid = false;
        for (var subscriber : e.getValue()) {
            if (subscriber == null) {
                break;
            }

            var movement = subscriber.movement();
            if (movement.isImpossible()) {
                continue;
            }

            if (blockState == null) {
                blockState = subscriber.movement.getPreviousEntityState().levelState().getBlockStateAt(e.getKey())
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
                    if (!calculatedSolid) {
                        // Only count full blocks like stone or dirt as solid
                        isSolid = blockState.blockShapeType().isFullBlock();
                        calculatedSolid = true;
                    }

                    if (!isSolid) {
                        movement.setImpossible(true);
                    }
                }
                case ADD_CORNER_COST_IF_SOLID -> {
                    // No need to apply the cost multiple times.
                    if (movement.isAppliedCornerCost()) {
                        continue;
                    }

                    if (!calculatedSolid) {
                        // Only count full blocks like stone or dirt as solid
                        isSolid = blockState.blockShapeType().isFullBlock();
                        calculatedSolid = true;
                    }

                    if (isSolid) {
                        movement.addCornerCost();
                    }
                }
            }
        }
    };

    public GraphInstructions[] getActions(BotEntityState node) {
        var movements = new PlayerMovement[MAX_MOVEMENTS];

        {
            var blockSubscribers = new Object2ObjectOpenCustomHashMap<Vector3i, ObjectList<BlockSubscription>>(EXPECTED_BLOCKS, VectorHelper.VECTOR3I_HASH_STRATEGY);

            var size = 0;
            for (var direction : MovementDirection.VALUES) {
                var diagonal = direction.isDiagonal();
                for (var modifier : MovementModifier.VALUES) {
                    if (diagonal) {
                        for (var side : MovementSide.VALUES) {
                            registerMovement(blockSubscribers, movements[size++] = new PlayerMovement(node, direction, side, modifier));
                        }
                    } else {
                        registerMovement(blockSubscribers, movements[size++] = new PlayerMovement(node, direction, null, modifier));
                    }
                }
            }

            blockSubscribers.object2ObjectEntrySet().fastForEach(SUBSCRIPTION_CONSUMER);
        }

        var results = new GraphInstructions[MAX_MOVEMENTS];
        {
            var size = 0;
            for (var j = 0; j < MAX_MOVEMENTS; j++) {
                var movement = movements[j];
                if (movement.isImpossible()) {
                    continue;
                }

                results[size++] = movement.getInstructions();
            }
        }

        return results;
    }

    private void registerMovement(Object2ObjectMap<Vector3i, ObjectList<BlockSubscription>> blockSubscribers,
                                  PlayerMovement movement) {
        var freeSubscription = new BlockSubscription(movement, SubscriptionType.FREE);
        for (var freeBlock : movement.listRequiredFreeBlocks()) {
            blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                    .add(freeSubscription);
        }

        var addCostIfSolidList = movement.listAddCostIfSolidBlocks();
        if (!addCostIfSolidList.isEmpty()) {
            var addCostIfSolidSubscription = new BlockSubscription(movement, SubscriptionType.ADD_CORNER_COST_IF_SOLID);
            for (var addCostIfSolidBlock : addCostIfSolidList) {
                blockSubscribers.computeIfAbsent(addCostIfSolidBlock, CREATE_MISSING_FUNCTION)
                        .add(addCostIfSolidSubscription);
            }
        }

        var solidSubscription = new BlockSubscription(movement, SubscriptionType.SOLID);
        var solidBlock = movement.requiredSolidBlock();
        blockSubscribers.computeIfAbsent(solidBlock, CREATE_MISSING_FUNCTION)
                .add(solidSubscription);
    }

    enum SubscriptionType {
        FREE,
        SOLID,
        ADD_CORNER_COST_IF_SOLID
    }

    record BlockSubscription(PlayerMovement movement, SubscriptionType type) {
    }
}
