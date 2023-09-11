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

import net.pistonmaster.serverwrecker.data.BlockShapeType;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
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
    private static final Optional<List<WorldAction>> EMPTY_LIST = Optional.of(Collections.emptyList());

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

    private Optional<List<WorldAction>> requireFreeBlocks(ProjectedLevelState level, ProjectedInventory inventory) {
        List<WorldAction> actions = new ArrayList<>();
        Vector3i fromPosInt = previousEntityState.position().toInt();

        if (modifier == MovementModifier.JUMP) {
            // Make head block free (maybe head block is a slab)
            if (requireFreeHelper(fromPosInt.add(0, 1, 0), level, inventory, actions)) {
                return Optional.empty();
            }

            // Make block above head block free
            if (requireFreeHelper(fromPosInt.add(0, 2, 0), level, inventory, actions)) {
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

            for (Vector3i bodyOffset : BodyPart.BODY_PARTS) {
                // Apply jump shift to target edge and offset for body part
                if (requireFreeHelper(applyJumpShift(corner, modifier).add(bodyOffset), level, inventory, actions)) {
                    return Optional.empty();
                }
            }
        }

        Vector3i targetEdge = applyDirection(fromPosInt, direction);

        for (Vector3i bodyOffset : BodyPart.BODY_PARTS) {
            // Apply jump shift to target diagonal and offset for body part
            if (requireFreeHelper(applyJumpShift(targetEdge, modifier).add(bodyOffset), level, inventory, actions)) {
                return Optional.empty();
            }
        }

        // Require free blocks to fall into the target position
        switch (modifier) {
            case FALL_1 -> {
                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_1), level, inventory, actions)) {
                    return Optional.empty();
                }
            }
            case FALL_2 -> {
                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_1), level, inventory, actions)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_2), level, inventory, actions)) {
                    return Optional.empty();
                }
            }
            case FALL_3 -> {
                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_1), level, inventory, actions)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_2), level, inventory, actions)) {
                    return Optional.empty();
                }

                if (requireFreeHelper(applyModifier(targetEdge, MovementModifier.FALL_3), level, inventory, actions)) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(actions);
    }

    private boolean requireFreeHelper(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory, List<WorldAction> actions) {
        Optional<List<WorldAction>> blockActions = requireFreeBlock(block, level, inventory);
        if (blockActions.isEmpty()) {
            return true;
        } else {
            actions.addAll(blockActions.get());
            return false;
        }
    }

    private Optional<List<WorldAction>> requireFreeBlock(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory) {
        BlockShapeType blockShapeType = getBlockShapeType(level, block);

        // Collision block like stone
        if (blockShapeType != null && !blockShapeType.hasNoCollisions()) {
            // In the future, add cost of breaking here
            return Optional.empty();
        }

        return EMPTY_LIST;
    }

    private Optional<List<WorldAction>> requireSolidBlocks(ProjectedLevelState level, ProjectedInventory inventory) {
        List<WorldAction> actions = new ArrayList<>();
        Vector3i fromPosInt = previousEntityState.position().toInt();
        Vector3i floorPos = fromPosInt.add(0, -1, 0);

        // Add the block that is required to be solid for straight movement
        if (requireSolidHelper(applyModifier(applyDirection(floorPos, direction), modifier), level, inventory, actions)) {
            return Optional.empty();
        }

        return Optional.of(actions);
    }

    private boolean requireSolidHelper(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory, List<WorldAction> actions) {
        Optional<List<WorldAction>> blockActions = requireSolidBlock(block, level, inventory);
        if (blockActions.isEmpty()) {
            return true;
        } else {
            actions.addAll(blockActions.get());
            return false;
        }
    }

    private Optional<List<WorldAction>> requireSolidBlock(Vector3i block, ProjectedLevelState level, ProjectedInventory inventory) {
        BlockShapeType blockShapeType = getBlockShapeType(level, block);

        // Empty block like air or grass
        if (blockShapeType == null) {
            // In the future, add cost of placing here if block is replaceable (like air)
            return Optional.empty();
        }

        // Block with a current state that has no collision (Like open fence gate)
        if (blockShapeType.hasNoCollisions()) {
            // Could destroy and place block here, but that's too much work
            return Optional.empty();
        }

        // Prevent walking over cake, fences, etc.
        if (!blockShapeType.isFullBlock()) {
            // Could destroy and place block here, but that's too much work
            return Optional.empty();
        }

        return EMPTY_LIST;
    }

    private BlockShapeType getBlockShapeType(ProjectedLevelState level, Vector3i block) {
        Optional<BlockStateMeta> blockType = level.getBlockStateAt(block);
        if (blockType.isEmpty()) {
            // Out of level, so we can't go there, so we'll recalculate
            throw new OutOfLevelException();
        }

        return blockType.get().blockShapeType();
    }

    @Override
    public GraphInstructions getInstructions() {
        double cost = switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Costs.STRAIGHT;
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> Costs.DIAGONAL;
        };
        List<WorldAction> actions = new ArrayList<>();
        ProjectedLevelState projectedLevelState = previousEntityState.levelState();
        ProjectedInventory projectedInventory = previousEntityState.inventory();

        Optional<List<WorldAction>> freeActions = requireFreeBlocks(projectedLevelState, projectedInventory);
        if (freeActions.isEmpty()) {
            return GraphInstructions.IMPOSSIBLE;
        } else {
            actions.addAll(freeActions.get());
        }

        Optional<List<WorldAction>> solidActions = requireSolidBlocks(projectedLevelState, projectedInventory);
        if (solidActions.isEmpty()) {
            return GraphInstructions.IMPOSSIBLE;
        } else {
            actions.addAll(solidActions.get());
        }

        Vector3d targetPosition = applyModifier(applyDirection(previousEntityState.position(), direction), modifier);

        int yawOffset = 0;
        if (side != null) {
            yawOffset = switch (side) {
                case LEFT -> 10;
                case RIGHT -> -10;
            };
        }

        actions.add(new MovementAction(targetPosition, yawOffset));

        return new GraphInstructions(new BotEntityState(
                targetPosition,
                projectedLevelState,
                projectedInventory
        ), cost, actions);
    }
}
