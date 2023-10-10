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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions;

import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

@Slf4j
public final class PlayerMovement implements GraphAction {
    private final BotEntityState previousEntityState;
    private final MovementDirection direction;
    private final MovementSide side;
    private final MovementModifier modifier;
    private final AtomicDouble cost;
    private final Vector3i targetBlock;
    @Setter
    @Getter
    private boolean isImpossible = false;

    public PlayerMovement(BotEntityState previousEntityState, MovementDirection direction, MovementSide side, MovementModifier modifier) {
        this.previousEntityState = previousEntityState;
        this.direction = direction;
        this.side = side;
        this.modifier = modifier;

        this.cost = new AtomicDouble(switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Costs.STRAIGHT;
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> Costs.DIAGONAL;
        } + switch (modifier) { // Add additional "discouraged" costs to prevent the bot from doing too much parkour
            case NORMAL -> 0;
            case FALL_1 -> Costs.FALL_1;
            case FALL_2 -> Costs.FALL_2;
            case FALL_3 -> Costs.FALL_3;
            case JUMP -> Costs.JUMP;
        });

        this.targetBlock = modifier.offset(direction.offset(previousEntityState.positionBlock()));
    }

    public List<Vector3i> listRequiredFreeBlocks() {
        List<Vector3i> requiredFreeBlocks = new ObjectArrayList<>();
        var fromPosInt = previousEntityState.positionBlock();

        if (modifier == MovementModifier.JUMP) {
            // Make head block free (maybe head block is a slab)
            requiredFreeBlocks.add(fromPosInt.add(0, 1, 0));

            // Make block above head block free
            requiredFreeBlocks.add(fromPosInt.add(0, 2, 0));
        }

        // Add the blocks that are required to be free for diagonal movement
        if (direction.isDiagonal()) {
            var corner = getCorner(fromPosInt);

            for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
                // Apply jump shift to target edge and offset for body part
                requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
            }
        }

        var targetEdge = direction.offset(fromPosInt);
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target diagonal and offset for body part
            requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(targetEdge)));
        }

        // Require free blocks to fall into the target position
        switch (modifier) {
            case FALL_1 -> {
                requiredFreeBlocks.add(MovementModifier.FALL_1.offset(targetEdge));
            }
            case FALL_2 -> {
                requiredFreeBlocks.add(MovementModifier.FALL_1.offset(targetEdge));
                requiredFreeBlocks.add(MovementModifier.FALL_2.offset(targetEdge));
            }
            case FALL_3 -> {
                requiredFreeBlocks.add(MovementModifier.FALL_1.offset(targetEdge));
                requiredFreeBlocks.add(MovementModifier.FALL_2.offset(targetEdge));
                requiredFreeBlocks.add(MovementModifier.FALL_3.offset(targetEdge));
            }
        }

        return requiredFreeBlocks;
    }

    private Vector3i getCorner(Vector3i fromPosInt) {
        return (switch (direction) {
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
        }).offset(fromPosInt);
    }

    public Vector3i requiredSolidBlock() {
        // Floor block
        return targetBlock.sub(0, 1, 0);
    }

    @Override
    public GraphInstructions getInstructions() {
        var targetDoublePosition = VectorHelper.middleOfBlockNormalize(targetBlock.toDouble());
        return new GraphInstructions(new BotEntityState(
                targetDoublePosition,
                targetBlock,
                previousEntityState.levelState(),
                previousEntityState.inventory()
        ), cost.get(), List.of(new MovementAction(targetDoublePosition, side != null)));
    }
}
