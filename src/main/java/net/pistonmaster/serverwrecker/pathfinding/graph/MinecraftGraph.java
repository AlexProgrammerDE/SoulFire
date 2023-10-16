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
import net.pistonmaster.serverwrecker.data.BlockItems;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.*;
import net.pistonmaster.serverwrecker.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3i;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final Object2ObjectFunction<? super Vector3i, ? extends ObjectList<BlockSubscription>> CREATE_MISSING_FUNCTION =
            k -> new ObjectArrayList<>();
    private static final GraphAction[] ACTIONS_TEMPLATE;
    private static final Vector3i[] SUBSCRIPTION_KEYS;
    private static final BlockSubscription[][] SUBSCRIPTION_VALUES;

    static {
        var blockSubscribers = new Object2ObjectArrayMap<Vector3i, ObjectList<BlockSubscription>>();

        var actions = new ObjectArrayList<GraphAction>();
        var baseVector = Vector3i.ZERO;
        for (var direction : MovementDirection.VALUES) {
            var diagonal = direction.isDiagonal();
            for (var modifier : MovementModifier.VALUES) {
                if (diagonal) {
                    for (var side : MovementSide.VALUES) {
                        actions.add(registerMovement(
                                blockSubscribers,
                                new PlayerMovement(baseVector, direction, side, modifier),
                                actions.size()
                        ));
                    }
                } else {
                    actions.add(registerMovement(
                            blockSubscribers,
                            new PlayerMovement(baseVector, direction, null, modifier),
                            actions.size()
                    ));
                }
            }
        }

        ACTIONS_TEMPLATE = actions.toArray(new GraphAction[0]);
        SUBSCRIPTION_KEYS = new Vector3i[blockSubscribers.size()];
        SUBSCRIPTION_VALUES = new BlockSubscription[blockSubscribers.size()][];

        var entrySetDescending = blockSubscribers.object2ObjectEntrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .toList();
        for (var i = 0; i < entrySetDescending.size(); i++) {
            var entry = entrySetDescending.get(i);
            SUBSCRIPTION_KEYS[i] = entry.getKey();
            SUBSCRIPTION_VALUES[i] = entry.getValue().toArray(new BlockSubscription[0]);
        }
    }

    public GraphInstructions[] getActions(BotEntityState node) {
        var actions = new GraphAction[ACTIONS_TEMPLATE.length];

        {
            for (var i = 0; i < ACTIONS_TEMPLATE.length; i++) {
                actions[i] = ACTIONS_TEMPLATE[i].copy(node);
            }

            for (var i = 0; i < SUBSCRIPTION_KEYS.length; i++) {
                var key = SUBSCRIPTION_KEYS[i];
                var value = SUBSCRIPTION_VALUES[i];

                BlockStateMeta blockState = null;

                // We cache only this, but not solid because solid will only occur a single time
                var calculatedFree = false;
                var isFree = false;
                for (var subscriber : value) {
                    var action = actions[subscriber.index];
                    if (action.isImpossible()) {
                        continue;
                    }

                    var basePosition = node.positionBlock();
                    var positionBlock = node.positionBlock().add(key);
                    if (blockState == null) {
                        blockState = node.levelState()
                                .getBlockStateAt(positionBlock)
                                .orElseThrow(OutOfLevelException::new);
                    }

                    var movement = (PlayerMovement) action;
                    switch (subscriber.type) {
                        case MOVEMENT_FREE -> {
                            if (!calculatedFree) {
                                // We can walk through blocks like air or grass
                                isFree = blockState.blockShapeType().hasNoCollisions()
                                        && !BlockTypeHelper.isFluid(blockState.blockType());
                                calculatedFree = true;
                            }

                            if (!isFree) {
                                if (movement.isAllowsBlockActions()
                                        && blockState.blockType().diggable()
                                        && BlockItems.hasItemType(blockState.blockType())) {
                                    var cacheableMiningCost = node.inventory()
                                            .getMiningCosts(tagsState, blockState);
                                    // We can mine this block, lets add costs and continue
                                    movement.getBlockBreakCosts()[subscriber.blockArrayIndex] = new MovementMiningCost(
                                            positionBlock,
                                            cacheableMiningCost.miningCost(),
                                            cacheableMiningCost.willDrop()
                                    );
                                } else {
                                    movement.setImpossible(true);
                                }
                            }
                        }
                        case MOVEMENT_SOLID -> {
                            if (!blockState.blockShapeType().isFullBlock()) {
                                if (movement.isAllowsBlockActions()
                                        && node.inventory().hasBlockToPlace()
                                        && BlockTypeHelper.isReplaceable(blockState.blockType())) {
                                    movement.setRequiresAgainstBlock(true);
                                } else {
                                    movement.setImpossible(true);
                                }
                            }
                        }
                        case MOVEMENT_AGAINST_PLACE_SOLID -> {
                            // We already found one, no need to check for more
                            if (movement.getBlockPlaceData() != null) {
                                continue;
                            }

                            // We found a valid block to place against
                            if (blockState.blockShapeType().isFullBlock()) {
                                var blockPlaceAgainst = subscriber.blockToPlaceAgainst();
                                movement.setBlockPlaceData(new BotActionManager.BlockPlaceData(
                                        basePosition.add(blockPlaceAgainst.againstPos()),
                                        blockPlaceAgainst.blockFace()
                                ));
                            }
                        }
                        case MOVEMENT_ADD_CORNER_COST_IF_SOLID -> {
                            // No need to apply the cost multiple times.
                            if (movement.isAppliedCornerCost()) {
                                continue;
                            }

                            if (blockState.blockShapeType().isFullBlock()) {
                                movement.addCornerCost();
                            } else if (BlockTypeHelper.isHurtOnTouch(blockState.blockType())) {
                                // Since this is a corner, we can also avoid touching blocks that hurt us, e.g., cacti
                                movement.setImpossible(true);
                            }
                        }
                    }
                }
            }
        }

        var results = new GraphInstructions[ACTIONS_TEMPLATE.length];
        {
            var size = 0;
            for (var i = 0; i < ACTIONS_TEMPLATE.length; i++) {
                var movement = actions[i];
                if (movement.isImpossibleToComplete()) {
                    continue;
                }

                results[size++] = movement.getInstructions(node);
            }
        }

        return results;
    }

    private static PlayerMovement registerMovement(Object2ObjectMap<Vector3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                   PlayerMovement movement, int index) {
        {
            var i = 0;
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                var freeSubscription = new BlockSubscription(index, SubscriptionType.MOVEMENT_FREE, i);
                blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                        .add(freeSubscription);
                i++;
            }
        }

        {
            var addCostIfSolidSubscription = new BlockSubscription(index, SubscriptionType.MOVEMENT_ADD_CORNER_COST_IF_SOLID);
            for (var addCostIfSolidBlock : movement.listAddCostIfSolidBlocks()) {
                blockSubscribers.computeIfAbsent(addCostIfSolidBlock, CREATE_MISSING_FUNCTION)
                        .add(addCostIfSolidSubscription);
            }
        }

        {
            var solidSubscription = new BlockSubscription(index, SubscriptionType.MOVEMENT_SOLID);
            var solidBlock = movement.requiredSolidBlock();
            blockSubscribers.computeIfAbsent(solidBlock, CREATE_MISSING_FUNCTION)
                    .add(solidSubscription);
        }

        {
            for (var againstBlock : movement.possibleBlocksToPlaceAgainst()) {
                var againstSolidSubscription = new BlockSubscription(index, SubscriptionType.MOVEMENT_AGAINST_PLACE_SOLID, againstBlock);
                blockSubscribers.computeIfAbsent(againstBlock.againstPos(), CREATE_MISSING_FUNCTION)
                        .add(againstSolidSubscription);
            }
        }

        return movement;
    }

    enum SubscriptionType {
        MOVEMENT_FREE,
        MOVEMENT_SOLID,
        MOVEMENT_ADD_CORNER_COST_IF_SOLID,
        MOVEMENT_AGAINST_PLACE_SOLID
    }

    record BlockSubscription(int index, SubscriptionType type, int blockArrayIndex,
                             BotActionManager.BlockPlaceData blockToPlaceAgainst) {
        BlockSubscription(int index, SubscriptionType type) {
            this(index, type, -1, null);
        }

        BlockSubscription(int index, SubscriptionType type, int blockArrayIndex) {
            this(index, type, blockArrayIndex, null);
        }

        BlockSubscription(int index, SubscriptionType type, BotActionManager.BlockPlaceData blockToPlaceAgainst) {
            this(index, type, -1, blockToPlaceAgainst);
        }
    }
}
