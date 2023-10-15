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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

@Slf4j
public final class PlayerMovement implements GraphAction {
    private final Vector3i positionBlock;
    private final MovementDirection direction;
    private final MovementSide side;
    private final MovementModifier modifier;
    private final Vector3i targetBlock;
    @Getter
    private final boolean diagonal;
    private double cost;
    @Getter
    private boolean appliedCornerCost = false;
    @Setter
    @Getter
    private boolean isImpossible = false;

    public PlayerMovement(Vector3i positionBlock, MovementDirection direction, MovementSide side, MovementModifier modifier) {
        this.positionBlock = positionBlock;
        this.direction = direction;
        this.side = side;
        this.modifier = modifier;
        this.diagonal = direction.isDiagonal();

        this.cost = (diagonal ? Costs.DIAGONAL : Costs.STRAIGHT) +
                switch (modifier) { // Add additional "discouraged" costs to prevent the bot from doing too much parkour
                    case NORMAL -> 0;
                    case FALL_1 -> Costs.FALL_1;
                    case FALL_2 -> Costs.FALL_2;
                    case FALL_3 -> Costs.FALL_3;
                    case JUMP -> Costs.JUMP;
                };

        this.targetBlock = modifier.offset(direction.offset(positionBlock));
    }

    private PlayerMovement(PlayerMovement other) {
        this.positionBlock = other.positionBlock;
        this.direction = other.direction;
        this.side = other.side;
        this.modifier = other.modifier;
        this.targetBlock = other.targetBlock;
        this.diagonal = other.diagonal;
        this.cost = other.cost;
        this.appliedCornerCost = other.appliedCornerCost;
        this.isImpossible = other.isImpossible;
    }

    public List<Vector3i> listRequiredFreeBlocks() {
        List<Vector3i> requiredFreeBlocks = new ObjectArrayList<>(2 +
                (diagonal ? 2 : 0) +
                switch (modifier) {
                    case NORMAL -> 0;
                    case FALL_1 -> 1;
                    case FALL_2, JUMP -> 2;
                    case FALL_3 -> 3;
                });
        var fromPosInt = positionBlock;

        var targetEdge = direction.offset(fromPosInt);
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target diagonal and offset for body part
            requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(targetEdge)));
        }

        // Add the blocks that are required to be free for diagonal movement
        if (diagonal) {
            var corner = getCorner(fromPosInt, side);

            for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
                // Apply jump shift to target edge and offset for body part
                requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
            }
        }

        // Require free blocks to fall into the target position
        switch (modifier) {
            case JUMP -> {
                // Make head block free (maybe head block is a slab)
                requiredFreeBlocks.add(fromPosInt.add(0, 1, 0));

                // Make block above head block free
                requiredFreeBlocks.add(fromPosInt.add(0, 2, 0));
            }
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

    private Vector3i getCorner(Vector3i fromPosInt, MovementSide side) {
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

    public List<Vector3i> listAddCostIfSolidBlocks() {
        var list = new ObjectArrayList<Vector3i>(2);

        // If these blocks are solid, the bot moves slower because the bot is running around a corner
        var corner = getCorner(positionBlock, side.opposite());
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target edge and offset for body part
            list.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
        }

        return list;
    }

    public Vector3i requiredSolidBlock() {
        // Floor block
        return targetBlock.sub(0, 1, 0);
    }

    public void addCornerCost() {
        cost += Costs.CORNER_SLIDE;
        appliedCornerCost = true;
    }

    @Override
    public boolean isImpossibleToComplete() {
        return isImpossible;
    }

    @Override
    public GraphInstructions getInstructions(BotEntityState previousEntityState) {
        var realTarget = previousEntityState.positionBlock().add(targetBlock);
        var targetDoublePosition = VectorHelper.middleOfBlockNormalize(realTarget.toDouble());
        return new GraphInstructions(new BotEntityState(
                targetDoublePosition,
                realTarget,
                previousEntityState.levelState(),
                previousEntityState.inventory()
        ), cost, List.of(new MovementAction(targetDoublePosition, diagonal)));
    }

    @Override
    public GraphAction copy(BotEntityState previousEntityState) {
        return new PlayerMovement(this);
    }
}
