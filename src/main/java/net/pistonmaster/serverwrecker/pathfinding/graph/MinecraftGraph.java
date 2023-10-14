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
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.block.BlockBreakGraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.block.BlockDirection;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.block.BlockModifier;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.block.BlockPlaceGraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.MovementDirection;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.MovementModifier;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.MovementSide;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.PlayerMovement;
import net.pistonmaster.serverwrecker.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.function.Consumer;

@Slf4j
public record MinecraftGraph(TagsState tagsState,
                             Consumer<? super Object2ObjectMap.Entry<Vector3i, ObjectList<BlockSubscription>>> subscriptionConsumer) {
    private static final int MAX_ACTIONS = 84;
    private static final int EXPECTED_BLOCKS = 70;
    private static final int MAX_SUBSCRIBERS = 27;
    private static final Object2ObjectFunction<? super Vector3i, ? extends ObjectList<BlockSubscription>> CREATE_MISSING_FUNCTION =
            k -> new ObjectArrayList<>(MAX_SUBSCRIBERS);

    public MinecraftGraph(TagsState tagsState) {
        this(tagsState, e -> {
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

                var action = subscriber.action();
                if (action.isImpossible()) {
                    continue;
                }

                if (blockState == null) {
                    blockState = action.getPreviousEntityState().levelState()
                            .getBlockStateAt(e.getKey())
                            .orElseThrow(OutOfLevelException::new);
                }

                switch (subscriber.type) {
                    case MOVEMENT_FREE -> {
                        if (!calculatedFree) {
                            // We can walk through blocks like air or grass
                            isFree = blockState.blockShapeType().hasNoCollisions()
                                    && !BlockTypeHelper.isFluid(blockState.blockType());
                            calculatedFree = true;
                        }

                        if (!isFree) {
                            ((PlayerMovement) action).setImpossible(true);
                        }
                    }
                    case MOVEMENT_SOLID -> {
                        if (!calculatedSolid) {
                            // Only count full blocks like stone or dirt as solid
                            isSolid = blockState.blockShapeType().isFullBlock();
                            calculatedSolid = true;
                        }

                        if (!isSolid) {
                            ((PlayerMovement) action).setImpossible(true);
                        }
                    }
                    case MOVEMENT_ADD_CORNER_COST_IF_SOLID -> {
                        var movement = (PlayerMovement) action;

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
                    case BLOCK_PLACE_REPLACEABLE -> {
                        if (!BlockTypeHelper.isReplaceable(blockState.blockType())) {
                            // Target block cannot be replaced with another block
                            ((BlockPlaceGraphAction) action).setImpossible(true);
                        }
                    }
                    case BLOCK_PLACE_FREE -> {
                        if (!calculatedFree) {
                            // We can walk through blocks like air or grass
                            isFree = blockState.blockShapeType().hasNoCollisions()
                                    && !BlockTypeHelper.isFluid(blockState.blockType());
                            calculatedFree = true;
                        }

                        if (!isFree) {
                            // This required block to place the target block is not free
                            ((BlockPlaceGraphAction) action).setImpossible(true);
                        }
                    }
                    case BLOCK_PLACE_AGAINST_SOLID -> {
                        var blockPlace = (BlockPlaceGraphAction) action;

                        // We already found one, no need to check for more
                        if (blockPlace.getBlockToPlaceAgainst() != null) {
                            continue;
                        }

                        if (!calculatedSolid) {
                            // Only count full blocks like stone or dirt as solid
                            isSolid = blockState.blockShapeType().isFullBlock();
                            calculatedSolid = true;
                        }

                        // We found a valid block to place against
                        if (isSolid) {
                            blockPlace.setBlockToPlaceAgainst((BotActionManager.BlockPlaceData) subscriber.extraData());
                        }
                    }
                    case BLOCK_BREAK_SOLID_AND_ADD_COST -> {
                        if (!calculatedSolid) {
                            // Only count full blocks like stone or dirt as solid
                            isSolid = blockState.blockShapeType().isFullBlock();
                            calculatedSolid = true;
                        }

                        if (isSolid &&
                                blockState.blockType().diggable() &&
                                BlockItems.hasItemType(blockState.blockType())) {
                            var blockBreak = (BlockBreakGraphAction) action;
                            blockBreak.setCosts(blockBreak.getPreviousEntityState().inventory()
                                    .getMiningCosts(tagsState, blockState));
                        } else {
                            ((BlockBreakGraphAction) action).setImpossible(true);
                        }
                    }
                    case BLOCK_BREAK_FREE -> {
                        if (!calculatedFree) {
                            // We can walk through blocks like air or grass
                            isFree = blockState.blockShapeType().hasNoCollisions()
                                    && !BlockTypeHelper.isFluid(blockState.blockType());
                            calculatedFree = true;
                        }

                        if (!isFree) {
                            ((BlockBreakGraphAction) action).setImpossible(true);
                        }
                    }
                }
            }
        });
    }

    public GraphInstructions[] getActions(BotEntityState node) {
        var actions = new GraphAction[MAX_ACTIONS];

        {
            var blockSubscribers = new Object2ObjectOpenCustomHashMap<Vector3i, ObjectList<BlockSubscription>>(EXPECTED_BLOCKS, VectorHelper.VECTOR3I_HASH_STRATEGY);

            var size = 0;
            for (var direction : MovementDirection.VALUES) {
                var diagonal = direction.isDiagonal();
                for (var modifier : MovementModifier.VALUES) {
                    if (diagonal) {
                        for (var side : MovementSide.VALUES) {
                            actions[size++] = registerMovement(blockSubscribers, new PlayerMovement(node, direction, side, modifier));
                        }
                    } else {
                        actions[size++] = registerMovement(blockSubscribers, new PlayerMovement(node, direction, null, modifier));
                    }
                }
            }

            for (var direction : BlockDirection.VALUES) {
                for (var modifier : BlockModifier.VALUES) {
                    actions[size++] = registerBlockPlace(blockSubscribers, new BlockPlaceGraphAction(node, direction, modifier));
                    actions[size++] = registerBlockBreak(blockSubscribers, new BlockBreakGraphAction(node, direction, modifier));
                }
            }

            // log.debug("Block subscribers: {}", blockSubscribers.size());
            // log.debug("Block subscribers values: {}", blockSubscribers.values().stream().mapToInt(ObjectList::size).max());

            blockSubscribers.object2ObjectEntrySet().fastForEach(subscriptionConsumer);
        }

        var results = new GraphInstructions[MAX_ACTIONS];
        {
            var size = 0;
            for (var j = 0; j < MAX_ACTIONS; j++) {
                var movement = actions[j];
                if (movement.isImpossibleToComplete()) {
                    continue;
                }

                results[size++] = movement.getInstructions();
            }
        }

        return results;
    }

    private PlayerMovement registerMovement(Object2ObjectMap<Vector3i, ObjectList<BlockSubscription>> blockSubscribers,
                                            PlayerMovement movement) {
        {
            var freeSubscription = new BlockSubscription(movement, SubscriptionType.MOVEMENT_FREE);
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                        .add(freeSubscription);
            }
        }

        if (movement.isDiagonal()) {
            var addCostIfSolidSubscription = new BlockSubscription(movement, SubscriptionType.MOVEMENT_ADD_CORNER_COST_IF_SOLID);
            for (var addCostIfSolidBlock : movement.listAddCostIfSolidBlocks()) {
                blockSubscribers.computeIfAbsent(addCostIfSolidBlock, CREATE_MISSING_FUNCTION)
                        .add(addCostIfSolidSubscription);
            }
        }

        {
            var solidSubscription = new BlockSubscription(movement, SubscriptionType.MOVEMENT_SOLID);
            var solidBlock = movement.requiredSolidBlock();
            blockSubscribers.computeIfAbsent(solidBlock, CREATE_MISSING_FUNCTION)
                    .add(solidSubscription);
        }

        return movement;
    }

    private BlockPlaceGraphAction registerBlockPlace(Object2ObjectMap<Vector3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                     BlockPlaceGraphAction blockPlace) {
        if (blockPlace.isImpossible()) {
            return blockPlace;
        }

        {
            var replaceableSubscription = new BlockSubscription(blockPlace, SubscriptionType.BLOCK_PLACE_REPLACEABLE);
            var replaceableBlock = blockPlace.requiredReplaceableBlock();
            blockSubscribers.computeIfAbsent(replaceableBlock, CREATE_MISSING_FUNCTION)
                    .add(replaceableSubscription);
        }

        {
            var freeBlock = blockPlace.requiredFreeBlock();
            if (freeBlock.isPresent()) {
                var freeSubscription = new BlockSubscription(blockPlace, SubscriptionType.BLOCK_PLACE_FREE);
                blockSubscribers.computeIfAbsent(freeBlock.get(), CREATE_MISSING_FUNCTION)
                        .add(freeSubscription);
            }
        }

        {
            for (var block : blockPlace.possibleBlocksToPlaceAgainst()) {
                var againstSubscription = new BlockSubscription(blockPlace, SubscriptionType.BLOCK_PLACE_AGAINST_SOLID, block);
                blockSubscribers.computeIfAbsent(block.againstPos(), CREATE_MISSING_FUNCTION)
                        .add(againstSubscription);
            }
        }

        return blockPlace;
    }

    private BlockBreakGraphAction registerBlockBreak(Object2ObjectMap<Vector3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                     BlockBreakGraphAction blockBreak) {
        {
            var solidSubscription = new BlockSubscription(blockBreak, SubscriptionType.BLOCK_BREAK_SOLID_AND_ADD_COST);
            var solidBlock = blockBreak.requiredSolidBlock();
            blockSubscribers.computeIfAbsent(solidBlock, CREATE_MISSING_FUNCTION)
                    .add(solidSubscription);
        }

        {
            var freeBlock = blockBreak.requiredFreeBlock();
            if (freeBlock.isPresent()) {
                var freeSubscription = new BlockSubscription(blockBreak, SubscriptionType.BLOCK_BREAK_FREE);
                blockSubscribers.computeIfAbsent(freeBlock.get(), CREATE_MISSING_FUNCTION)
                        .add(freeSubscription);
            }
        }

        return blockBreak;
    }

    enum SubscriptionType {
        MOVEMENT_FREE,
        MOVEMENT_SOLID,
        MOVEMENT_ADD_CORNER_COST_IF_SOLID,
        BLOCK_PLACE_REPLACEABLE,
        BLOCK_PLACE_FREE,
        BLOCK_PLACE_AGAINST_SOLID,
        BLOCK_BREAK_SOLID_AND_ADD_COST,
        BLOCK_BREAK_FREE
    }

    record BlockSubscription(GraphAction action, SubscriptionType type, Object extraData) {
        BlockSubscription(GraphAction action, SubscriptionType type) {
            this(action, type, null);
        }
    }
}
