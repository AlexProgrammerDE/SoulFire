/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.pathfinding.graph;

import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.util.TriState;
import net.pistonmaster.soulfire.server.data.BlockItems;
import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.pathfinding.graph.actions.*;
import net.pistonmaster.soulfire.server.pathfinding.graph.actions.movement.*;
import net.pistonmaster.soulfire.server.protocol.bot.BotActionManager;
import net.pistonmaster.soulfire.server.protocol.bot.state.TagsState;
import net.pistonmaster.soulfire.server.util.BlockTypeHelper;
import net.pistonmaster.soulfire.server.util.ObjectReference;
import net.pistonmaster.soulfire.server.util.Vec2ObjectOpenHashMap;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public record MinecraftGraph(TagsState tagsState) {
    private static final Object2ObjectFunction<? super SWVec3i, ? extends ObjectList<BlockSubscription>> CREATE_MISSING_FUNCTION =
            k -> new ObjectArrayList<>();
    private static final GraphAction[] ACTIONS_TEMPLATE;
    private static final SWVec3i[] SUBSCRIPTION_KEYS;
    private static final BlockSubscription[][] SUBSCRIPTION_VALUES;

    static {
        var blockSubscribers = new Vec2ObjectOpenHashMap<SWVec3i, ObjectList<BlockSubscription>>();

        var actions = new ObjectArrayList<GraphAction>();
        for (var direction : MovementDirection.VALUES) {
            var diagonal = direction.isDiagonal();
            for (var modifier : MovementModifier.VALUES) {
                if (diagonal) {
                    for (var side : MovementSide.VALUES) {
                        actions.add(registerMovement(
                                blockSubscribers,
                                new SimpleMovement(direction, side, modifier),
                                actions.size()
                        ));
                    }
                } else {
                    actions.add(registerMovement(
                            blockSubscribers,
                            new SimpleMovement(direction, null, modifier),
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

    private static SimpleMovement registerMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                   SimpleMovement movement, int movementIndex) {
        {
            var blockId = 0;
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                movement.subscribe();
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
                    movement.subscribe();
                    blockSubscribers.computeIfAbsent(block.position(), CREATE_MISSING_FUNCTION)
                            .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK, i, block.type()));
                }
            }
        }

        {
            movement.subscribe();
            blockSubscribers.computeIfAbsent(movement.requiredSolidBlock(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_SOLID));
        }

        {
            for (var addCostIfSolidBlock : movement.listAddCostIfSolidBlocks()) {
                movement.subscribe();
                blockSubscribers.computeIfAbsent(addCostIfSolidBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_ADD_CORNER_COST_IF_SOLID));
            }
        }

        {
            for (var againstBlock : movement.possibleBlocksToPlaceAgainst()) {
                movement.subscribe();
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
                movement.subscribe();
                blockSubscribers.computeIfAbsent(freeBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_FREE, blockId++));
            }
        }

        {
            movement.subscribe();
            blockSubscribers.computeIfAbsent(movement.requiredUnsafeBlock(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.PARKOUR_UNSAFE_TO_STAND_ON));
        }

        {
            movement.subscribe();
            blockSubscribers.computeIfAbsent(movement.requiredSolidBlock(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_SOLID));
        }

        return movement;
    }

    private static DownMovement registerDownMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                     DownMovement movement, int movementIndex) {
        {
            for (var safetyBlock : movement.listSafetyCheckBlocks()) {
                movement.subscribe();
                blockSubscribers.computeIfAbsent(safetyBlock, CREATE_MISSING_FUNCTION)
                        .add(new BlockSubscription(movementIndex, SubscriptionType.DOWN_SAFETY_CHECK));
            }
        }

        {
            movement.subscribe();
            blockSubscribers.computeIfAbsent(movement.blockToBreak(), CREATE_MISSING_FUNCTION)
                    .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_FREE));
        }

        {
            var safeBlocks = movement.listCheckSafeMineBlocks();
            for (var i = 0; i < safeBlocks.length; i++) {
                var savedBlock = safeBlocks[i];
                if (savedBlock == null) {
                    continue;
                }

                for (var block : savedBlock) {
                    movement.subscribe();
                    blockSubscribers.computeIfAbsent(block.position(), CREATE_MISSING_FUNCTION).add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK, i, block.type()));
                }
            }
        }

        return movement;
    }

    private static UpMovement registerUpMovement(Object2ObjectMap<SWVec3i, ObjectList<BlockSubscription>> blockSubscribers,
                                                 UpMovement movement, int movementIndex) {
        {
            var blockId = 0;
            for (var freeBlock : movement.listRequiredFreeBlocks()) {
                movement.subscribe();
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
                    movement.subscribe();
                    blockSubscribers.computeIfAbsent(block.position(), CREATE_MISSING_FUNCTION)
                            .add(new BlockSubscription(movementIndex, SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK, i, block.type()));
                }
            }
        }

        return movement;
    }

    private static TriState isBlockFree(BlockState blockState) {
        return TriState.byBoolean(blockState.blockShapeGroup().hasNoCollisions()
                && !blockState.blockType().fluidSource());
    }

    public void insertActions(BotEntityState node, Consumer<GraphInstructions> callback, Predicate<SWVec3i> alreadySeen) {
        log.debug("Inserting actions for node: {}", node.blockPosition());
        calculateActions(node, generateTemplateActions(node), callback, alreadySeen);
    }

    private GraphAction[] generateTemplateActions(BotEntityState node) {
        var actions = new GraphAction[ACTIONS_TEMPLATE.length];
        for (var i = 0; i < ACTIONS_TEMPLATE.length; i++) {
            actions[i] = ACTIONS_TEMPLATE[i].copy(node);
        }

        return actions;
    }

    private void calculateActions(BotEntityState node, GraphAction[] actions, Consumer<GraphInstructions> callback, Predicate<SWVec3i> alreadySeen) {
        for (var i = 0; i < SUBSCRIPTION_KEYS.length; i++) {
            processSubscription(node, actions, callback, i);
        }
    }

    private void processSubscription(BotEntityState node, GraphAction[] actions, Consumer<GraphInstructions> callback, int i) {
        var key = SUBSCRIPTION_KEYS[i];
        var value = SUBSCRIPTION_VALUES[i];

        BlockState blockState = null;
        SWVec3i absolutePositionBlock = null;

        // We cache only this, but not solid because solid will only occur a single time
        var isFreeReference = new ObjectReference<>(TriState.NOT_SET);
        for (var subscriber : value) {
            var action = actions[subscriber.actionIndex];
            if (action == null) {
                continue;
            }

            if (blockState == null) {
                // Lazy calculation to avoid unnecessary calls
                absolutePositionBlock = node.blockPosition().add(key);
                blockState = node.levelState()
                        .getBlockStateAt(absolutePositionBlock);

                if (blockState.blockType() == BlockType.VOID_AIR) {
                    throw new OutOfLevelException();
                }
            }

            switch (processSubscriptionAction(key, subscriber, action, isFreeReference, blockState, absolutePositionBlock, node)) {
                case CONTINUE -> {
                    if (!action.decrementAndIsDone() || action.impossibleToComplete()) {
                        continue;
                    }

                    callback.accept(action.getInstructions(node));
                }
                case IMPOSSIBLE -> actions[subscriber.actionIndex] = null;
            }
        }
    }

    private SubscriptionSingleResult processSubscriptionAction(SWVec3i key, BlockSubscription subscriber, GraphAction action, ObjectReference<TriState> isFreeReference,
                                                               BlockState blockState, SWVec3i absolutePositionBlock,
                                                               BotEntityState node) {
        return switch (action) {
            case SimpleMovement simpleMovement ->
                    processMovementSubscription(subscriber, isFreeReference, blockState, absolutePositionBlock, node, simpleMovement);
            case ParkourMovement ignored -> processParkourSubscription(subscriber, isFreeReference, blockState);
            case DownMovement downMovement ->
                    processDownSubscription(key, subscriber, blockState, absolutePositionBlock, node, downMovement);
            case UpMovement upMovement ->
                    processUpSubscription(subscriber, isFreeReference, blockState, absolutePositionBlock, node, upMovement);
        };
    }

    private SubscriptionSingleResult processMovementSubscription(BlockSubscription subscriber, ObjectReference<TriState> isFreeReference,
                                                                 BlockState blockState, SWVec3i absolutePositionBlock,
                                                                 BotEntityState node, SimpleMovement simpleMovement) {
        return switch (subscriber.type) {
            case MOVEMENT_FREE -> {
                if (isFreeReference.value == TriState.NOT_SET) {
                    // We can walk through blocks like air or grass
                    isFreeReference.value = isBlockFree(blockState);
                }

                if (isFreeReference.value == TriState.TRUE) {
                    if (simpleMovement.allowBlockActions()) {
                        simpleMovement.noNeedToBreak()[subscriber.blockArrayIndex] = true;
                    }

                    yield SubscriptionSingleResult.CONTINUE;
                }

                // Search for a way to break this block
                if (!simpleMovement.allowBlockActions()
                        // Narrow this down to blocks that can be broken
                        || !BlockTypeHelper.isDiggable(blockState.blockType())
                        // Check if we previously found out this block is unsafe to break
                        || simpleMovement.unsafeToBreak()[subscriber.blockArrayIndex]
                        // Narrows the list down to a reasonable size
                        || !BlockItems.hasItemType(blockState.blockType())) {
                    // No way to break this block
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                var cacheableMiningCost = node.inventory()
                        .getMiningCosts(tagsState, blockState);
                // We can mine this block, lets add costs and continue
                simpleMovement.blockBreakCosts()[subscriber.blockArrayIndex] = new MovementMiningCost(
                        absolutePositionBlock,
                        cacheableMiningCost.miningCost(),
                        cacheableMiningCost.willDrop()
                );
                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_BREAK_SAFETY_CHECK -> {
                // There is no need to break this block, so there is no need for safety checks
                if (simpleMovement.noNeedToBreak()[subscriber.blockArrayIndex]) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                // The block was already marked as unsafe
                if (simpleMovement.unsafeToBreak()[subscriber.blockArrayIndex]) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                var unsafe = switch (subscriber.safetyType) {
                    case FALLING_AND_FLUIDS -> blockState.blockType().fluidSource()
                            || blockState.blockType().fallingBlock();
                    case FLUIDS -> blockState.blockType().fluidSource();
                };

                if (!unsafe) {
                    // All good, we can continue
                    yield SubscriptionSingleResult.CONTINUE;
                }

                var currentValue = simpleMovement.blockBreakCosts()[subscriber.blockArrayIndex];

                if (currentValue != null) {
                    // We learned that this block needs to be broken, so we need to set it as impossible
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                // Store for a later time that this is unsafe,
                // so if we check this block,
                // we know it's unsafe
                simpleMovement.unsafeToBreak()[subscriber.blockArrayIndex] = true;

                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_SOLID -> {
                // Block is safe to walk on, no need to check for more
                if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                if (!simpleMovement.allowBlockActions()
                        || node.inventory().hasNoBlocks()
                        || !blockState.blockType().replaceable()) {
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                // We can place a block here, but we need to find a block to place against
                simpleMovement.requiresAgainstBlock(true);
                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_AGAINST_PLACE_SOLID -> {
                // We already found one, no need to check for more
                if (simpleMovement.blockPlaceData() != null) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                // This block should not be placed against
                if (!blockState.blockShapeGroup().isFullBlock()) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                // Fixup the position to be the block we are placing against instead of relative
                simpleMovement.blockPlaceData(new BotActionManager.BlockPlaceData(
                        absolutePositionBlock,
                        subscriber.blockToPlaceAgainst.blockFace()
                ));
                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_ADD_CORNER_COST_IF_SOLID -> {
                // No need to apply the cost multiple times.
                if (simpleMovement.appliedCornerCost()) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                if (blockState.blockShapeGroup().isFullBlock()) {
                    simpleMovement.addCornerCost();
                } else if (BlockTypeHelper.isHurtOnTouchSide(blockState.blockType())) {
                    // Since this is a corner, we can also avoid touching blocks that hurt us, e.g., cacti
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                yield SubscriptionSingleResult.CONTINUE;
            }
            default -> throw new IllegalStateException("Unexpected value: " + subscriber.type);
        };
    }

    private SubscriptionSingleResult processParkourSubscription(BlockSubscription subscriber, ObjectReference<TriState> isFreeReference,
                                                                BlockState blockState) {
        return switch (subscriber.type) {
            case MOVEMENT_FREE -> {
                if (isFreeReference.value == TriState.NOT_SET) {
                    // We can walk through blocks like air or grass
                    isFreeReference.value = isBlockFree(blockState);
                }

                if (isFreeReference.value == TriState.TRUE) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                yield SubscriptionSingleResult.IMPOSSIBLE;
            }
            // We only want to jump over dangerous blocks/gaps
            // So either a non-full-block like water or lava or magma
            // since it hurts to stand on.
            case PARKOUR_UNSAFE_TO_STAND_ON -> {
                if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_SOLID -> {
                // Block is safe to walk on, no need to check for more
                if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                yield SubscriptionSingleResult.IMPOSSIBLE;
            }
            default -> throw new IllegalStateException("Unexpected value: " + subscriber.type);
        };
    }

    private SubscriptionSingleResult processDownSubscription(SWVec3i key, BlockSubscription subscriber,
                                                             BlockState blockState, SWVec3i absolutePositionBlock,
                                                             BotEntityState node, DownMovement downMovement) {
        return switch (subscriber.type) {
            case MOVEMENT_FREE -> {
                if (!BlockTypeHelper.isDiggable(blockState.blockType())
                        // Narrows the list down to a reasonable size
                        || !BlockItems.hasItemType(blockState.blockType())) {
                    // No way to break this block
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                var cacheableMiningCost = node.inventory()
                        .getMiningCosts(tagsState, blockState);
                // We can mine this block, lets add costs and continue
                downMovement.blockBreakCosts(new MovementMiningCost(
                        absolutePositionBlock,
                        cacheableMiningCost.miningCost(),
                        cacheableMiningCost.willDrop()
                ));
                yield SubscriptionSingleResult.CONTINUE;
            }
            case DOWN_SAFETY_CHECK -> {
                var yLevel = key.y;

                if (yLevel < downMovement.closestBlockToFallOn()) {
                    // We already found a block to fall on, above this one
                    yield SubscriptionSingleResult.CONTINUE;
                }

                if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
                    // We found a block to fall on
                    downMovement.closestBlockToFallOn(yLevel);
                }

                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_BREAK_SAFETY_CHECK -> {
                var unsafe = switch (subscriber.safetyType) {
                    case FALLING_AND_FLUIDS ->
                            blockState.blockType().fluidSource() || blockState.blockType().fallingBlock();
                    case FLUIDS -> blockState.blockType().fluidSource();
                };

                if (unsafe) {
                    // We know already WE MUST dig the block below for this action
                    // So if one block around the block below is unsafe, we can't do this action
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                // All good, we can continue
                yield SubscriptionSingleResult.CONTINUE;
            }
            default -> throw new IllegalStateException("Unexpected value: " + subscriber.type);
        };
    }

    private SubscriptionSingleResult processUpSubscription(BlockSubscription subscriber, ObjectReference<TriState> isFreeReference,
                                                           BlockState blockState, SWVec3i absolutePositionBlock,
                                                           BotEntityState node, UpMovement upMovement) {
        return switch (subscriber.type) {
            case MOVEMENT_FREE -> {
                if (isFreeReference.value == TriState.NOT_SET) {
                    // We can walk through blocks like air or grass
                    isFreeReference.value = isBlockFree(blockState);
                }

                if (isFreeReference.value == TriState.TRUE) {
                    upMovement.noNeedToBreak()[subscriber.blockArrayIndex] = true;
                    yield SubscriptionSingleResult.CONTINUE;
                }

                // Search for a way to break this block
                if (!BlockTypeHelper.isDiggable(blockState.blockType())
                        || upMovement.unsafeToBreak()[subscriber.blockArrayIndex]
                        || !BlockItems.hasItemType(blockState.blockType())) {
                    // No way to break this block
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                var cacheableMiningCost = node.inventory()
                        .getMiningCosts(tagsState, blockState);
                // We can mine this block, lets add costs and continue
                upMovement.blockBreakCosts()[subscriber.blockArrayIndex] = new MovementMiningCost(
                        absolutePositionBlock,
                        cacheableMiningCost.miningCost(),
                        cacheableMiningCost.willDrop()
                );
                yield SubscriptionSingleResult.CONTINUE;
            }
            case MOVEMENT_BREAK_SAFETY_CHECK -> {
                // There is no need to break this block, so there is no need for safety checks
                if (upMovement.noNeedToBreak()[subscriber.blockArrayIndex]) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                // The block was already marked as unsafe
                if (upMovement.unsafeToBreak()[subscriber.blockArrayIndex]) {
                    yield SubscriptionSingleResult.CONTINUE;
                }

                var unsafe = switch (subscriber.safetyType) {
                    case FALLING_AND_FLUIDS -> blockState.blockType().fluidSource()
                            || blockState.blockType().fallingBlock();
                    case FLUIDS -> blockState.blockType().fluidSource();
                };

                if (!unsafe) {
                    // All good, we can continue
                    yield SubscriptionSingleResult.CONTINUE;
                }

                var currentValue = upMovement.blockBreakCosts()[subscriber.blockArrayIndex];

                if (currentValue != null) {
                    // We learned that this block needs to be broken, so we need to set it as impossible
                    yield SubscriptionSingleResult.IMPOSSIBLE;
                }

                // Store for a later time that this is unsafe,
                // so if we check this block,
                // we know it's unsafe
                upMovement.unsafeToBreak()[subscriber.blockArrayIndex] = true;

                yield SubscriptionSingleResult.CONTINUE;
            }
            default -> throw new IllegalStateException("Unexpected value: " + subscriber.type);
        };
    }

    private enum SubscriptionSingleResult {
        CONTINUE,
        IMPOSSIBLE
    }

    private enum SubscriptionType {
        MOVEMENT_FREE,
        MOVEMENT_BREAK_SAFETY_CHECK,
        MOVEMENT_SOLID,
        MOVEMENT_ADD_CORNER_COST_IF_SOLID,
        MOVEMENT_AGAINST_PLACE_SOLID,
        DOWN_SAFETY_CHECK,
        PARKOUR_UNSAFE_TO_STAND_ON
    }

    private record BlockSubscription(int actionIndex, SubscriptionType type, int blockArrayIndex,
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
