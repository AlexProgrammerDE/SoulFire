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
package net.pistonmaster.serverwrecker.server.pathfinding.graph;

import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.server.data.BlockItems;
import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.actions.*;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.actions.movement.*;
import net.pistonmaster.serverwrecker.server.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final Object2ObjectFunction<? super SWVec3i, ? extends ObjectList<BlockSubscription>> CREATE_MISSING_FUNCTION =
            k -> new ObjectArrayList<>();
    private static final GraphAction[] ACTIONS_TEMPLATE;
    private static final SWVec3i[] SUBSCRIPTION_KEYS;
    private static final BlockSubscription[][] SUBSCRIPTION_VALUES;

    static {
        var blockSubscribers = new Object2ObjectArrayMap<SWVec3i, ObjectList<BlockSubscription>>();

        var actions = new ObjectArrayList<GraphAction>();
        for (var direction : MovementDirection.VALUES) {
            var diagonal = direction.isDiagonal();
            for (var modifier : MovementModifier.VALUES) {
                if (diagonal) {
                    for (var side : MovementSide.VALUES) {
                        actions.add(registerMovement(
                                blockSubscribers,
                                new PlayerMovement(direction, side, modifier),
                                actions.size()
                        ));
                    }
                } else {
                    actions.add(registerMovement(
                            blockSubscribers,
                            new PlayerMovement(direction, null, modifier),
                            actions.size()
                    ));
                }
            }
        }

        for (var direction : ParkourDirection.VALUES) {
            actions.add(registerParkourMovement(
                    blockSubscribers,
                    new ParkourMovement(direction),
                    actions.size()
            ));
        }

        actions.add(registerDownMovement(
                blockSubscribers,
                new DownMovement(),
                actions.size()
        ));

        actions.add(registerUpMovement(
                blockSubscribers,
                new UpMovement(),
                actions.size()
        ));

        ACTIONS_TEMPLATE = actions.toArray(new GraphAction[0]);
        SUBSCRIPTION_KEYS = new SWVec3i[blockSubscribers.size()];
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

    private static PlayerMovement registerMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                   PlayerMovement movement, int movementIndex) {
        {
            var blockId = 0;
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_FREE, blockId++));
            }
        }

        {
            var safeBlocks = movement.listCheckSafeMineBlocks();
            for (var i = 0; i < safeBlocks.length; i++) {
                var savedBlock = safeBlocks[i];
                if (savedBlock == null) {
                    continue;
                }

                for (var block : savedBlock) {
                    blockSubscribers.computeIfAbsent(block.position(), CREATE_MISSING_FUNCTION)
                            .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK, i, block.type()));
                }
            }
        }

        {
            blockSubscribers.computeIfAbsent(movement.requiredSolidBlock(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_SOLID));
        }

        {
            for (var addCostIfSolidBlock : movement.listAddCostIfSolidBlocks()) {
                blockSubscribers.computeIfAbsent(addCostIfSolidBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_ADD_CORNER_COST_IF_SOLID));
            }
        }

        {
            for (var againstBlock : movement.possibleBlocksToPlaceAgainst()) {
                blockSubscribers.computeIfAbsent(againstBlock.againstPos(), CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_AGAINST_PLACE_SOLID, againstBlock));
            }
        }

        return movement;
    }

    private static ParkourMovement registerParkourMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                           ParkourMovement movement, int movementIndex) {
        {
            var blockId = 0;
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_FREE, blockId++));
            }
        }

        {
            blockSubscribers.computeIfAbsent(movement.requiredUnsafeBlock(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.PARKOUR_UNSAFE_TO_STAND_ON));
        }

        {
            blockSubscribers.computeIfAbsent(movement.requiredSolidBlock(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_SOLID));
        }

        return movement;
    }

    private static DownMovement registerDownMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                     DownMovement movement, int movementIndex) {
        {
            for (var safetyBlock : movement.listSafetyCheckBlocks()) {
                blockSubscribers.computeIfAbsent(safetyBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.DOWN_SAFETY_CHECK));
            }
        }

        {
            blockSubscribers.computeIfAbsent(movement.blockToBreak(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_FREE));
        }

        return movement;
    }

    private static UpMovement registerUpMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                 UpMovement movement, int movementIndex) {
        {
            var blockId = 0;
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_FREE, blockId++));
            }
        }

        {
            var safeBlocks = movement.listCheckSafeMineBlocks();
            for (var i = 0; i < safeBlocks.length; i++) {
                var savedBlock = safeBlocks[i];
                if (savedBlock == null) {
                    continue;
                }

                for (var block : savedBlock) {
                    blockSubscribers.computeIfAbsent(block.position(), CREATE_MISSING_FUNCTION)
                            .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK, i, block.type()));
                }
            }
        }

        return movement;
    }

    public GraphInstructions[] getActions(BotEntityState node) {
        var actions = new GraphAction[ACTIONS_TEMPLATE.length];

        fillTemplateActions(node, actions);

        calculateActions(node, actions);

        var results = new GraphInstructions[ACTIONS_TEMPLATE.length];
        convertToInstructions(node, actions, results);

        return results;
    }

    private void fillTemplateActions(BotEntityState node, GraphAction[] actions) {
        for (var i = 0; i < ACTIONS_TEMPLATE.length; i++) {
            actions[i] = ACTIONS_TEMPLATE[i].copy(node);
        }
    }

    private void calculateActions(BotEntityState node, GraphAction[] actions) {
        for (var i = 0; i < SUBSCRIPTION_KEYS.length; i++) {
            var key = SUBSCRIPTION_KEYS[i];
            var value = SUBSCRIPTION_VALUES[i];

            BlockStateMeta blockState = null;
            SWVec3i absolutePositionBlock = null;

            // We cache only this, but not solid because solid will only occur a single time
            var calculatedFree = false;
            var isFree = false;
            for (var subscriber : value) {
                var action = actions[subscriber.actionIndex];
                if (action == null) {
                    continue;
                }

                if (action.impossible()) {
                    // Calling isImpossible can waste seconds of execution time
                    // Calling an interface method is expensive!
                    actions[subscriber.actionIndex] = null;
                    continue;
                }

                if (blockState == null) {
                    // Lazy calculation to avoid unnecessary calls
                    absolutePositionBlock = node.positionBlock().add(key);
                    blockState = node.levelState()
                            .getBlockStateAt(absolutePositionBlock);

                    if (blockState.blockType() == BlockType.VOID_AIR) {
                        throw new OutOfLevelException();
                    }
                }

                switch (action) {
                    case PlayerMovement playerMovement -> {
                        switch (subscriber.type) {
                            case MOVEMENT_FREE -> {
                                if (!calculatedFree) {
                                    // We can walk through blocks like air or grass
                                    isFree = blockState.blockShapeType().hasNoCollisions()
                                            && !BlockTypeHelper.isFluid(blockState.blockType());
                                    calculatedFree = true;
                                }

                                if (isFree) {
                                    if (playerMovement.allowBlockActions()) {
                                        playerMovement.noNeedToBreak()[subscriber.blockArrayIndex] = true;
                                    }

                                    continue;
                                }

                                // Search for a way to break this block
                                if (playerMovement.allowBlockActions()
                                        // Narrow this down to blocks that can be broken
                                        && blockState.blockType().diggable()
                                        // Check if we previously found out this block is unsafe to break
                                        && !playerMovement.unsafeToBreak()[subscriber.blockArrayIndex]
                                        // Narrows the list down to a reasonable size
                                        && BlockItems.hasItemType(blockState.blockType())) {
                                    var cacheableMiningCost = node.inventory()
                                            .getMiningCosts(tagsState, blockState);
                                    // We can mine this block, lets add costs and continue
                                    playerMovement.blockBreakCosts()[subscriber.blockArrayIndex] = new MovementMiningCost(
                                            absolutePositionBlock,
                                            cacheableMiningCost.miningCost(),
                                            cacheableMiningCost.willDrop()
                                    );
                                } else {
                                    // No way to break this block
                                    playerMovement.impossible(true);
                                }
                            }
                            case MOVEMENT_BREAK_SAFETY_CHECK -> {
                                // There is no need to break this block, so there is no need for safety checks
                                if (playerMovement.noNeedToBreak()[subscriber.blockArrayIndex]) {
                                    continue;
                                }

                                // The block was already marked as unsafe
                                if (playerMovement.unsafeToBreak()[subscriber.blockArrayIndex]) {
                                    continue;
                                }

                                var unsafe = switch (subscriber.safetyType) {
                                    case FALLING_AND_FLUIDS -> BlockTypeHelper.isFluid(blockState.blockType())
                                            || blockState.blockType().blockProperties().fallingBlock();
                                    case FLUIDS -> BlockTypeHelper.isFluid(blockState.blockType());
                                };

                                if (unsafe) {
                                    var currentValue = playerMovement.blockBreakCosts()[subscriber.blockArrayIndex];

                                    if (currentValue == null) {
                                        // Store for a later time that this is unsafe,
                                        // so if we check this block,
                                        // we know it's unsafe
                                        playerMovement.unsafeToBreak()[subscriber.blockArrayIndex] = true;
                                    } else {
                                        // We learned that this block needs to be broken, so we need to set it as impossible
                                        playerMovement.impossible(true);
                                    }
                                }
                            }
                            case MOVEMENT_SOLID -> {
                                // Block is safe to walk on, no need to check for more
                                if (blockState.blockShapeType().isFullBlock()) {
                                    continue;
                                }

                                if (playerMovement.allowBlockActions()
                                        && node.inventory().hasBlockToPlace()
                                        && blockState.blockType().blockProperties().replaceable()) {
                                    // We can place a block here, but we need to find a block to place against
                                    playerMovement.requiresAgainstBlock(true);
                                } else {
                                    playerMovement.impossible(true);
                                }
                            }
                            case MOVEMENT_AGAINST_PLACE_SOLID -> {
                                // We already found one, no need to check for more
                                if (playerMovement.blockPlaceData() != null) {
                                    continue;
                                }

                                // This block should not be placed against
                                if (!blockState.blockShapeType().isFullBlock()) {
                                    continue;
                                }

                                // Fixup the position to be the block we are placing against instead of relative
                                playerMovement.blockPlaceData(new BotActionManager.BlockPlaceData(
                                        absolutePositionBlock,
                                        subscriber.blockToPlaceAgainst.blockFace()
                                ));
                            }
                            case MOVEMENT_ADD_CORNER_COST_IF_SOLID -> {
                                // No need to apply the cost multiple times.
                                if (playerMovement.appliedCornerCost()) {
                                    continue;
                                }

                                if (blockState.blockShapeType().isFullBlock()) {
                                    playerMovement.addCornerCost();
                                } else if (BlockTypeHelper.isHurtOnTouchSide(blockState.blockType())) {
                                    // Since this is a corner, we can also avoid touching blocks that hurt us, e.g., cacti
                                    playerMovement.impossible(true);
                                }
                            }
                        }
                    }
                    case ParkourMovement parkourMovement -> {
                        switch (subscriber.type) {
                            case MOVEMENT_FREE -> {
                                if (!calculatedFree) {
                                    // We can walk through blocks like air or grass
                                    isFree = blockState.blockShapeType().hasNoCollisions()
                                            && !BlockTypeHelper.isFluid(blockState.blockType());
                                    calculatedFree = true;
                                }

                                if (isFree) {
                                    continue;
                                }

                                parkourMovement.impossible(true);
                            }
                            // We only want to jump over dangerous blocks/gaps
                            // So either a non-full-block like water or lava or magma
                            // since it hurts to stand on.
                            case PARKOUR_UNSAFE_TO_STAND_ON -> {
                                if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
                                    parkourMovement.impossible(true);
                                }
                            }
                            case MOVEMENT_SOLID -> {
                                // Block is safe to walk on, no need to check for more
                                if (blockState.blockShapeType().isFullBlock()) {
                                    continue;
                                }

                                parkourMovement.impossible(true);
                            }
                        }
                    }
                    case DownMovement downMovement -> {
                        switch (subscriber.type) {
                            case MOVEMENT_FREE -> {
                                if (blockState.blockType().diggable()
                                        // Narrows the list down to a reasonable size
                                        && BlockItems.hasItemType(blockState.blockType())) {
                                    var cacheableMiningCost = node.inventory()
                                            .getMiningCosts(tagsState, blockState);
                                    // We can mine this block, lets add costs and continue
                                    downMovement.blockBreakCosts(new MovementMiningCost(
                                            absolutePositionBlock,
                                            cacheableMiningCost.miningCost(),
                                            cacheableMiningCost.willDrop()
                                    ));
                                } else {
                                    // No way to break this block
                                    downMovement.impossible(true);
                                }
                            }
                            case DOWN_SAFETY_CHECK -> {
                                var yLevel = key.y;

                                if (yLevel < downMovement.closestBlockToFallOn()) {
                                    // We already found a block to fall on, above this one
                                    continue;
                                }

                                if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
                                    // We found a block to fall on
                                    downMovement.closestBlockToFallOn(yLevel);
                                }
                            }
                        }
                    }
                    case UpMovement upMovement -> {
                        switch (subscriber.type) {
                            case MOVEMENT_FREE -> {
                                if (!calculatedFree) {
                                    // We can walk through blocks like air or grass
                                    isFree = blockState.blockShapeType().hasNoCollisions()
                                            && !BlockTypeHelper.isFluid(blockState.blockType());
                                    calculatedFree = true;
                                }

                                if (isFree) {
                                    upMovement.noNeedToBreak()[subscriber.blockArrayIndex] = true;
                                    continue;
                                }

                                // Search for a way to break this block
                                if (blockState.blockType().diggable()
                                        && !upMovement.unsafeToBreak()[subscriber.blockArrayIndex]
                                        && BlockItems.hasItemType(blockState.blockType())) {
                                    var cacheableMiningCost = node.inventory()
                                            .getMiningCosts(tagsState, blockState);
                                    // We can mine this block, lets add costs and continue
                                    upMovement.blockBreakCosts()[subscriber.blockArrayIndex] = new MovementMiningCost(
                                            absolutePositionBlock,
                                            cacheableMiningCost.miningCost(),
                                            cacheableMiningCost.willDrop()
                                    );
                                } else {
                                    // No way to break this block
                                    upMovement.impossible(true);
                                }
                            }
                            case MOVEMENT_BREAK_SAFETY_CHECK -> {
                                // There is no need to break this block, so there is no need for safety checks
                                if (upMovement.noNeedToBreak()[subscriber.blockArrayIndex]) {
                                    continue;
                                }

                                // The block was already marked as unsafe
                                if (upMovement.unsafeToBreak()[subscriber.blockArrayIndex]) {
                                    continue;
                                }

                                var unsafe = switch (subscriber.safetyType) {
                                    case FALLING_AND_FLUIDS -> BlockTypeHelper.isFluid(blockState.blockType())
                                            || blockState.blockType().blockProperties().fallingBlock();
                                    case FLUIDS -> BlockTypeHelper.isFluid(blockState.blockType());
                                };

                                if (unsafe) {
                                    var currentValue = upMovement.blockBreakCosts()[subscriber.blockArrayIndex];

                                    if (currentValue == null) {
                                        // Store for a later time that this is unsafe,
                                        // so if we check this block,
                                        // we know it's unsafe
                                        upMovement.unsafeToBreak()[subscriber.blockArrayIndex] = true;
                                    } else {
                                        // We learned that this block needs to be broken, so we need to set it as impossible
                                        upMovement.impossible(true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void convertToInstructions(BotEntityState node, GraphAction[] actions, GraphInstructions[] results) {
        var size = 0;
        for (var i = 0; i < ACTIONS_TEMPLATE.length; i++) {
            var movement = actions[i];
            if (movement == null) {
                continue;
            }

            if (movement.impossibleToComplete()) {
                continue;
            }

            results[size++] = movement.getInstructions(node);
        }
    }

    enum SubscriptionType {
        MOVEMENT_FREE,
        MOVEMENT_BREAK_SAFETY_CHECK,
        MOVEMENT_SOLID,
        MOVEMENT_ADD_CORNER_COST_IF_SOLID,
        MOVEMENT_AGAINST_PLACE_SOLID,
        DOWN_SAFETY_CHECK,
        PARKOUR_UNSAFE_TO_STAND_ON
    }

    record BlockSubscription(int actionIndex, SubscriptionType type, int blockArrayIndex,
                             BotActionManager.BlockPlaceData blockToPlaceAgainst,
                             BlockSafetyData.BlockSafetyType safetyType) {
        BlockSubscription(int movementIndex, SubscriptionType type) {
            this(movementIndex, type, -1, null, null);
        }

        BlockSubscription(int movementIndex, SubscriptionType type, int blockArrayIndex) {
            this(movementIndex, type, blockArrayIndex, null, null);
        }

        BlockSubscription(int movementIndex, SubscriptionType type, BotActionManager.BlockPlaceData blockToPlaceAgainst) {
            this(movementIndex, type, -1, blockToPlaceAgainst, null);
        }

        BlockSubscription(int movementIndex, SubscriptionType subscriptionType, int i, BlockSafetyData.BlockSafetyType type) {
            this(movementIndex, subscriptionType, i, null, type);
        }
    }
}
