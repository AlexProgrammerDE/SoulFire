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

import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record PlayerMovement(BotEntityState previousEntityState, MovementDirection direction, MovementModifier modifier,
                             MovementSide side) implements GraphAction {
    @Override
    public BotEntityState getTargetState() {
        // Make sure we are in the middle of the block
        Vector3d position = applyModifier(applyDirection(previousEntityState.position(), direction), modifier);

        return new BotEntityState(position, previousEntityState.levelState());
    }

    @Override
    public double getActionCost() {
        double cost = switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Costs.STRAIGHT;
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> Costs.DIAGONAL;
        };

        ProjectedLevelState projectedLevelState = previousEntityState.levelState();

        for (Vector3i requiredFreeBlock : requiredFreeBlocks()) {
            BlockType blockType = projectedLevelState.getBlockTypeAt(requiredFreeBlock);
            if (BlockTypeHelper.isSolid(blockType)) {
                // In the future, add cost of placing here
                return Double.POSITIVE_INFINITY;
            }
        }

        for (Vector3i requiredSolidBlock : requiredSolidBlocks()) {
            BlockType blockType = projectedLevelState.getBlockTypeAt(requiredSolidBlock);

            if (!BlockTypeHelper.isSolid(blockType)) {
                // In the future, add cost of digging here
                return Double.POSITIVE_INFINITY;
            }
        }

        return cost;
    }

    public Set<Vector3i> requiredFreeBlocks() {
        Set<Vector3i> requiredFreeBlocks = new HashSet<>();
        Vector3i fromPosInt = previousEntityState.position().toInt();

        // Add the block that is required to be free for straight movement
        Vector3i targetEdge = applyDirection(fromPosInt, direction);
        requiredFreeBlocks.add(targetEdge);

        // Add the blocks that are required to be free for diagonal movement
        switch (direction) {
            case NORTH_EAST -> {
                switch (side) {
                    case LEFT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.NORTH));
                    case RIGHT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.EAST));
                }
            }
            case NORTH_WEST -> {
                switch (side) {
                    case LEFT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.NORTH));
                    case RIGHT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.WEST));
                }
            }
            case SOUTH_EAST -> {
                switch (side) {
                    case LEFT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.SOUTH));
                    case RIGHT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.EAST));
                }
            }
            case SOUTH_WEST -> {
                switch (side) {
                    case LEFT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.SOUTH));
                    case RIGHT -> requiredFreeBlocks.add(applyDirection(fromPosInt, MovementDirection.WEST));
                }
            }
        }

        // Add the blocks that are required to be free for the head of the player
        for (Vector3i requiredFreeBlock : Set.copyOf(requiredFreeBlocks)) {
            requiredFreeBlocks.add(requiredFreeBlock.add(0, 1, 0));
        }

        switch (modifier) {
            case FALL_1 -> requiredFreeBlocks.add(applyModifier(targetEdge, modifier));
            case FALL_2 -> {
                requiredFreeBlocks.add(applyModifier(targetEdge, MovementModifier.FALL_1));
                requiredFreeBlocks.add(applyModifier(targetEdge, modifier));
            }
            case FALL_3 -> {
                requiredFreeBlocks.add(applyModifier(targetEdge, MovementModifier.FALL_1));
                requiredFreeBlocks.add(applyModifier(targetEdge, MovementModifier.FALL_2));
                requiredFreeBlocks.add(applyModifier(targetEdge, modifier));
            }
            case JUMP -> {
                // Shift the blocks up by one
                requiredFreeBlocks = requiredFreeBlocks.stream()
                        .map(block -> block.add(0, 1, 0))
                        .collect(Collectors.toSet());

                // You need to have a block above you free to jump
                requiredFreeBlocks.add(fromPosInt.add(0, 1, 0));
            }
        }

        return requiredFreeBlocks;
    }

    public Set<Vector3i> requiredSolidBlocks() {
        Set<Vector3i> requiredSolidBlocks = new HashSet<>();
        Vector3i fromPosInt = previousEntityState.position().toInt();

        Vector3i floorPos = fromPosInt.add(0, -1, 0);

        // Add the block that is required to be solid for straight movement
        requiredSolidBlocks.add(applyModifier(applyDirection(floorPos, direction), modifier));

        return requiredSolidBlocks;
    }

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

    private static Vector3d applyModifier(Vector3d pos, MovementModifier modifier) {
        return switch (modifier) {
            case FALL_1 -> pos.add(0, -1, 0);
            case FALL_2 -> pos.add(0, -2, 0);
            case FALL_3 -> pos.add(0, -3, 0);
            case JUMP -> pos.add(0, 1, 0);
            default -> pos;
        };
    }
}
