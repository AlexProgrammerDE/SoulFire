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

import com.google.common.util.concurrent.AtomicDouble;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
public record PlayerMovement(BotEntityState previousEntityState, MovementDirection direction, MovementModifier modifier,
                             MovementSide side) implements GraphAction {
    // Optional.of() takes a few milliseconds, so we'll just cache it
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final Optional<ActionCosts> EMPTY_COST = Optional.of(new ActionCosts(0, Collections.emptyList()));
    private static final boolean ALLOW_BLOCK_ACTIONS = false;

    private static Vector3i applyDirection(Vector3i pos, MovementDirection direction) {
        return switch (direction) {
            case NORTH -> pos.add(0, 0, -1);
            case SOUTH -> pos.add(0, 0, 1);
            case EAST -> pos.add(1, 0, 0);
            case WEST -> pos.add(-1, 0, 0);
            case NORTH_EAST -> pos.add(1, 0, -1);
            case NORTH_WEST -> pos.add(-1, 0, -1);
            case SOUTH_EAST -> pos.add(1, 0, 1);
            case SOUTH_WEST -> pos.add(-1, 0, 1);
        };
    }

    private static Vector3d applyDirection(Vector3d pos, MovementDirection direction) {
        return switch (direction) {
            case NORTH -> pos.add(0, 0, -1);
            case SOUTH -> pos.add(0, 0, 1);
            case EAST -> pos.add(1, 0, 0);
            case WEST -> pos.add(-1, 0, 0);
            case NORTH_EAST -> pos.add(1, 0, -1);
            case NORTH_WEST -> pos.add(-1, 0, -1);
            case SOUTH_EAST -> pos.add(1, 0, 1);
            case SOUTH_WEST -> pos.add(-1, 0, 1);
        };
    }

    private static Vector3i applyModifier(Vector3i pos, MovementModifier modifier) {
        return switch (modifier) {
            case FALL_1 -> pos.add(0, -1, 0);
            case FALL_2 -> pos.add(0, -2, 0);
            case FALL_3 -> pos.add(0, -3, 0);
            case JUMP -> pos.add(0, 1, 0);
            default -> pos;
        };
    }

    private static Vector3i applyJumpShift(Vector3i pos, MovementModifier modifier) {
        if (modifier == MovementModifier.JUMP) {
            return pos.add(0, 1, 0);
        } else {
            return pos;
        }
    }

    private static Vector3d applyModifier(Vector3d pos, MovementModifier modifier) {
        return switch (modifier) {
            case FALL_1 -> pos.add(0, -1, 0);
            case FALL_2 -> pos.add(0, -2, 0);
            case FALL_3 -> pos.add(0, -3, 0);
            case JUMP -> pos.add(0, 1, 0);
            default -> pos;
        };
    }

    private Optional<ActionCosts> requireFreeBlocks(ProjectedLevelState level, ProjectedInventory inventory) {
        List<WorldAction> actions = new ArrayList<>();
        var cost = new AtomicDouble();
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
            Vector3i corner = null;
            switch (direction) {
                case NORTH_EAST -> {
                    switch (side) {
                        case LEFT -> corner = applyDirection(fromPosInt, MovementDirection.NORTH);
                        case RIGHT -> corner = applyDirection(fromPosInt, MovementDirection.EAST);
                    }
                }
                case NORTH_WEST -> {
                    switch (side) {
                        case LEFT -> corner = applyDirection(fromPosInt, MovementDirection.NORTH);
                        case RIGHT -> corner = applyDirection(fromPosInt, MovementDirection.WEST);
                    }
                }
                case SOUTH_EAST -> {
                    switch (side) {
                        case LEFT -> corner = applyDirection(fromPosInt, MovementDirection.SOUTH);
                        case RIGHT -> corner = applyDirection(fromPosInt, MovementDirection.EAST);
                    }
                }
                case SOUTH_WEST -> {
                    switch (side) {
                        case LEFT -> corner = applyDirection(fromPosInt, MovementDirection.SOUTH);
                        case RIGHT -> corner = applyDirection(fromPosInt, MovementDirection.WEST);
                    }
                }
            }

            // This should never happen
            assert corner != null;

            for (var bodyOffset : BodyPart.BODY_PARTS) {
                // Apply jump shift to target edge and offset for body part
                if (requireFreeHelper(applyJumpShift(corner, modifier).add(bodyOffset), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
        }

        var targetEdge = applyDirection(fromPosInt, direction);

        for (var bodyOffset : BodyPart.BODY_PARTS) {
            // Apply jump shift to target diagonal and offset for body part
            if (requireFreeHelper(applyJumpShift(targetEdge, modifier).add(bodyOffset), level, inventory, actions, cost)) {
                return Optional.empty();
            }
        }

        // Require free blocks to fall into the target position
        switch (modifier) {
            case FALL_1 -> {
                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_1), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
            case FALL_2 -> {
                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_1), level, inventory, actions, cost)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_2), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
            case FALL_3 -> {
                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_1), level, inventory, actions, cost)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_2), level, inventory, actions, cost)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_3), level, inventory, actions, cost)) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new ActionCosts(cost.get(), actions));
    }

    private boolean requireFreeHelper(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory, List<WorldAction> actions, AtomicDouble cost) {
        var blockActions = requireFreeBlock(block, level, inventory);
        if (blockActions.isEmpty()) {
            return true;
        } else {
            actions.addAll(blockActions.get().actions());
            cost.addAndGet(blockActions.get().cost());
            return false;
        }
    }

    private Optional<ActionCosts> requireFreeBlock(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory) {
        var blockStateMeta = getBlockShapeType(level, block);

        // No need to break blocks like air or grass
        if (blockStateMeta.blockShapeType().hasNoCollisions()) {
            return EMPTY_COST;
        }

        if (!ALLOW_BLOCK_ACTIONS) {
            return Optional.empty();
        }

        var blockMiningCosts = Costs.calculateBlockBreakCost(inventory, blockStateMeta);

        // No way to break block
        if (blockMiningCosts.isEmpty()) {
            return Optional.empty();
        }

        var costs = blockMiningCosts.get();

        // Add cost of breaking block
        return Optional.of(new ActionCosts(costs.miningCost(), List.of(new BlockBreakAction(block, costs.toolType()))));
    }

    private Optional<ActionCosts> requireSolidBlocks(ProjectedLevelState level, ProjectedInventory inventory) {
        var actions = new ArrayList<WorldAction>();
        var cost = new AtomicDouble();
        var fromPosInt = previousEntityState.position().toInt();
        var floorPos = fromPosInt.add(0, -1, 0);

        // Add the block that is required to be solid for straight movement
        if (requireSolidHelper(applyModifier(applyDirection(floorPos, direction), modifier), level, inventory, actions, cost)) {
            return Optional.empty();
        }

        return Optional.of(new ActionCosts(cost.get(), actions));
    }

    private boolean requireSolidHelper(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory, List<WorldAction> actions, AtomicDouble cost) {
        var blockActions = requireSolidBlock(block, level, inventory);
        if (blockActions.isEmpty()) {
            return true;
        } else {
            actions.addAll(blockActions.get().actions());
            cost.addAndGet(blockActions.get().cost());
            return false;
        }
    }

    private Optional<ActionCosts> requireSolidBlock(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory) {
        var blockShapeType = getBlockShapeType(level, block).blockShapeType();

        // Block with a current state that has no collision (Like air, grass, open fence)
        if (blockShapeType.hasNoCollisions()) {
            if (!ALLOW_BLOCK_ACTIONS) {
                return Optional.empty();
            }

            // Could destroy and place block here, but that's too much work
            return Optional.empty();
        }

        // Prevent walking over cake, slabs, fences, etc.
        if (!blockShapeType.isFullBlock()) {
            // Could destroy and place block here, but that's too much work
            return Optional.empty();
        }

        return EMPTY_COST;
    }

    private BlockStateMeta getBlockShapeType(ProjectedLevelState level, Vector3i block) {
        // If out of level, we can't go there, so we'll recalculate
        return level.getBlockStateAt(block).orElseThrow(OutOfLevelException::new);
    }

    @Override
    public GraphInstructions getInstructions() {
        var cost = switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Costs.STRAIGHT;
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> Costs.DIAGONAL;
        };
        List<WorldAction> actions = new ArrayList<>();
        var projectedLevelState = previousEntityState.levelState();
        var projectedInventory = previousEntityState.inventory();

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

        var targetPosition = applyModifier(applyDirection(previousEntityState.position(), direction), modifier);

        var yawOffset = 0;
        if (side != null) {
            yawOffset = switch (side) {
                case LEFT -> 10;
                case RIGHT -> -10;
            };
        }

        actions.add(new MovementAction(targetPosition, yawOffset));

        // Add additional "discouraged" costs to prevent the bot from doing too much parkour when it's not needed
        switch (modifier) {
            case FALL_1 -> cost += Costs.FALL_1;
            case FALL_2 -> cost += Costs.FALL_2;
            case FALL_3 -> cost += Costs.FALL_3;
            case JUMP -> cost += Costs.JUMP;
        }

        return new GraphInstructions(new BotEntityState(
                targetPosition,
                projectedLevelState,
                projectedInventory
        ), cost, actions);
    }

    private record ActionCosts(double cost, List<WorldAction> actions) {
    }
}
