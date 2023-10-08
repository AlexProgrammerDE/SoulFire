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
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.data.BlockItems;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockPlaceAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import net.pistonmaster.serverwrecker.util.reference.ReferenceDouble;
import net.pistonmaster.serverwrecker.util.reference.ReferenceObject;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public record PlayerMovement(TagsState tagsState, BotEntityState previousEntityState, MovementDirection direction,
                             MovementModifier modifier, MovementSide side,
                             Map<Vector3i, Optional<BlockStateMeta>> blockCache) implements GraphAction {
    // Optional.of() takes a few milliseconds, so we'll just cache it
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<ActionCosts> NO_COST_RESULT = Optional.of(new ActionCosts(0, ObjectLists.emptyList()));
    private static final boolean BREAK_BLOCKS = false;
    private static final boolean PLACE_BLOCKS = false;

    private Optional<ActionCosts> requireFreeBlocks(ReferenceObject<ProjectedLevelState> level,
                                                    ReferenceObject<ProjectedInventory> inventory) {
        var actions = new ObjectArrayList<WorldAction>();
        var cost = new ReferenceDouble();
        var fromPosInt = previousEntityState.position().toInt();

        if (modifier == MovementModifier.JUMP) {
            // Make head block free (maybe head block is a slab)
            if (requireFreeHelper(fromPosInt.add(0, 1, 0), level, inventory, actions, cost)) {
                return Optional.empty();
            }

            // Make block above head block free
            if (requireFreeHelper(fromPosInt.add(0, 2, 0), level, inventory, actions, cost)) {
                return Optional.empty();
            }
        }

        // Add the blocks that are required to be free for diagonal movement
        if (direction.isDiagonal()) {
            var cornerDirection = switch (direction) {
                case NORTH_EAST -> switch (side) {
                    case LEFT -> MovementDirection.NORTH;
                    case RIGHT -> MovementDirection.EAST;
                };
                case NORTH_WEST -> switch (side) {
                    case LEFT -> MovementDirection.NORTH;
                    case RIGHT -> MovementDirection.WEST;
                };
                case SOUTH_EAST -> switch (side) {
                    case LEFT -> MovementDirection.SOUTH;
                    case RIGHT -> MovementDirection.EAST;
                };
                case SOUTH_WEST -> switch (side) {
                    case LEFT -> MovementDirection.SOUTH;
                    case RIGHT -> MovementDirection.WEST;
                };
                default -> throw new IllegalStateException("Unexpected value: " + direction);
            };
            var corner = cornerDirection.offset(fromPosInt);

            for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
                // Apply jump shift to target edge and offset for body part
                if (requireFreeHelper(bodyOffset.offset(modifier.offsetIfJump(corner)), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
        }

        var targetEdge = direction.offset(fromPosInt);
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target diagonal and offset for body part
            if (requireFreeHelper(bodyOffset.offset(modifier.offsetIfJump(targetEdge)), level, inventory, actions, cost)) {
                return Optional.empty();
            }
        }

        // Require free blocks to fall into the target position
        switch (modifier) {
            case FALL_1 -> {
                if (requireFreeHelper(MovementModifier.FALL_1.offset(targetEdge), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
            case FALL_2 -> {
                if (requireFreeHelper(MovementModifier.FALL_1.offset(targetEdge), level, inventory, actions, cost)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(MovementModifier.FALL_2.offset(targetEdge), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
            case FALL_3 -> {
                if (requireFreeHelper(MovementModifier.FALL_1.offset(targetEdge), level, inventory, actions, cost)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(MovementModifier.FALL_2.offset(targetEdge), level, inventory, actions, cost)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(MovementModifier.FALL_3.offset(targetEdge), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new ActionCosts(cost.get(), actions));
    }

    private boolean requireFreeHelper(Vector3i block, ReferenceObject<ProjectedLevelState> level,
                                      ReferenceObject<ProjectedInventory> inventory, List<WorldAction> actions, ReferenceDouble cost) {
        var blockActions = requireFreeBlock(block, level, inventory);
        if (blockActions.isEmpty()) {
            log.debug("Block at {} is not free", block);
            return true;
        } else {
            actions.addAll(blockActions.get().actions());
            cost.add(blockActions.get().cost());
            return false;
        }
    }

    private Optional<ActionCosts> requireFreeBlock(Vector3i block, ReferenceObject<ProjectedLevelState> level,
                                                   ReferenceObject<ProjectedInventory> inventory) {
        var resolvedLevel = level.get();
        var resolvedInventory = inventory.get();
        var blockStateMeta = getBlockStateMeta(resolvedLevel, block);

        // No need to break blocks like air or grass
        if (blockStateMeta.blockShapeType().hasNoCollisions()) {
            return NO_COST_RESULT;
        }

        if (!BREAK_BLOCKS) {
            return Optional.empty();
        }

        var blockMiningCosts = Costs.calculateBlockBreakCost(tagsState, resolvedInventory, blockStateMeta);

        // No way to break block
        if (blockMiningCosts.isEmpty()) {
            return Optional.empty();
        }

        var costs = blockMiningCosts.get();

        level.set(resolvedLevel.withChange(block, new BlockStateMeta(BlockType.AIR)));

        if (costs.willDrop()) {
            var itemDrop = BlockItems.getItemType(blockStateMeta.blockType());
            itemDrop.ifPresent(itemType -> inventory.set(resolvedInventory.withItemPickup(itemType)));
        }

        // Add cost of breaking block
        return Optional.of(new ActionCosts(costs.miningCost(), ObjectLists.singleton(new BlockBreakAction(block))));
    }

    private Optional<ActionCosts> requireSolidBlocks(ReferenceObject<ProjectedLevelState> level,
                                                     ReferenceObject<ProjectedInventory> inventory) {
        var actions = new ObjectArrayList<WorldAction>();
        var cost = new ReferenceDouble();
        var fromPosInt = previousEntityState.position().toInt();
        var floorPos = fromPosInt.add(0, -1, 0);

        // Add the block that is required to be solid for straight movement
        if (requireSolidHelper(modifier.offset(direction.offset(floorPos)), level, inventory, actions, cost)) {
            return Optional.empty();
        }

        return Optional.of(new ActionCosts(cost.get(), actions));
    }

    private boolean requireSolidHelper(Vector3i block, ReferenceObject<ProjectedLevelState> level,
                                       ReferenceObject<ProjectedInventory> inventory, List<WorldAction> actions, ReferenceDouble cost) {
        var blockActions = requireSolidBlock(block, level, inventory);
        if (blockActions.isEmpty()) {
            log.debug("Block at {} is not solid", block);
            return true;
        } else {
            actions.addAll(blockActions.get().actions());
            cost.add(blockActions.get().cost());
            return false;
        }
    }

    private Optional<ActionCosts> requireSolidBlock(Vector3i block, ReferenceObject<ProjectedLevelState> level,
                                                    ReferenceObject<ProjectedInventory> inventory) {
        var resolvedLevel = level.get();
        var resolvedInventory = inventory.get();
        var blockStateMeta = getBlockStateMeta(resolvedLevel, block);

        // We have found a replaceable block, so we can try to replace it
        if (PLACE_BLOCKS && BlockTypeHelper.isReplaceable(blockStateMeta.blockType())) {
            var blockPlaceInfo = BotActionManager.findBlockToPlaceAgainst(blockCache,
                    resolvedLevel, block, List.of(previousEntityState.position().toInt()));

            // No way to place a block against a block, fail
            if (blockPlaceInfo.isEmpty()) {
                return Optional.empty();
            }

            for (var slot : resolvedInventory.getStorage()) {
                if (slot.item() == null) {
                    continue;
                }

                var itemType = slot.item().getType();
                var blockItem = BlockItems.getBlockType(itemType);

                // We found an item we can place, so it's a valid action
                if (blockItem.isPresent()) {
                    level.set(resolvedLevel.withChange(block, new BlockStateMeta(blockItem.get())));

                    if (slot.item().getAmount() > 1) {
                        inventory.set(resolvedInventory.withChange(slot.slot(), slot.item().withAmount(slot.item().getAmount() - 1)));
                    } else {
                        inventory.set(resolvedInventory.withChange(slot.slot(), null));
                    }

                    return Optional.of(new ActionCosts(
                            Costs.PLACE_BLOCK,
                            ObjectLists.singleton(new BlockPlaceAction(block, blockPlaceInfo.get()))
                    ));
                }
            }

            // Found no item to place, fail
            return Optional.empty();
        }

        var blockShapeType = blockStateMeta.blockShapeType();

        // Block with a current state that has no collision (Like grass, open fence)
        if (blockShapeType.hasNoCollisions()) {
            return Optional.empty();
        }

        // Prevent walking over cake, slabs, fences, etc.
        if (!blockShapeType.isFullBlock()) {
            // Could destroy and place block here, but that's too much work
            return Optional.empty();
        }

        // There already is a full block there
        return NO_COST_RESULT;
    }

    private BlockStateMeta getBlockStateMeta(ProjectedLevelState level, Vector3i block) {
        // If out of level, we can't go there, so we'll recalculate once we get there
        return level.getCachedBlockStateAt(blockCache, block).orElseThrow(OutOfLevelException::new);
    }

    @Override
    public GraphInstructions getInstructions() {
        log.debug("Calculating instructions for {}", this);
        var cost = switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Costs.STRAIGHT;
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> Costs.DIAGONAL;
        };
        var actions = new ObjectArrayList<WorldAction>();
        var projectedLevelState = new ReferenceObject<>(previousEntityState.levelState());
        var projectedInventory = new ReferenceObject<>(previousEntityState.inventory());

        var freeActions = requireFreeBlocks(projectedLevelState, projectedInventory);
        if (freeActions.isEmpty()) {
            return GraphInstructions.IMPOSSIBLE;
        } else {
            actions.addAll(freeActions.get().actions());
            cost += freeActions.get().cost();
        }

        var solidActions = requireSolidBlocks(projectedLevelState, projectedInventory);
        if (solidActions.isEmpty()) {
            return GraphInstructions.IMPOSSIBLE;
        } else {
            actions.addAll(solidActions.get().actions());
            cost += solidActions.get().cost();
        }

        var targetPosition = modifier.offset(direction.offset(previousEntityState.position()));

        actions.add(new MovementAction(targetPosition, side != null));

        // Add additional "discouraged" costs to prevent the bot from doing too much parkour when it's not needed
        switch (modifier) {
            case FALL_1 -> cost += Costs.FALL_1;
            case FALL_2 -> cost += Costs.FALL_2;
            case FALL_3 -> cost += Costs.FALL_3;
            case JUMP -> cost += Costs.JUMP;
        }

        return new GraphInstructions(new BotEntityState(
                targetPosition,
                projectedLevelState.get(),
                projectedInventory.get()
        ), cost, actions);
    }

    @Override
    public String toString() {
        return "PlayerMovement{" +
                "previousPosition=" + previousEntityState.position() +
                ", direction=" + direction +
                ", modifier=" + modifier +
                ", side=" + side +
                '}';
    }

    private record ActionCosts(double cost, ObjectList<WorldAction> actions) {
    }
}
