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

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockPlaceAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import net.pistonmaster.serverwrecker.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

@Slf4j
public final class PlayerMovement implements GraphAction {
    private static final Vector3i feetPositionRelativeBlock = Vector3i.ZERO;
    private final MovementDirection direction;
    private final MovementSide side;
    private final MovementModifier modifier;
    private final Vector3i targetFeetBlock;
    @Getter
    private final boolean diagonal;
    @Getter
    private final boolean allowsBlockActions;
    @Getter
    private final MovementMiningCost[] blockBreakCosts;
    @Setter
    @Getter
    private BotActionManager.BlockPlaceData blockPlaceData;
    private double cost;
    @Getter
    private boolean appliedCornerCost = false;
    @Setter
    @Getter
    private boolean isImpossible = false;
    @Setter
    private boolean requiresAgainstBlock = false;

    public PlayerMovement(MovementDirection direction, MovementSide side, MovementModifier modifier) {
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

        this.targetFeetBlock = modifier.offset(direction.offset(feetPositionRelativeBlock));
        this.allowsBlockActions = !diagonal && (
                modifier == MovementModifier.JUMP
                        || modifier == MovementModifier.NORMAL
                        || modifier == MovementModifier.FALL_1
        );

        if (allowsBlockActions) {
            blockBreakCosts = new MovementMiningCost[freeCapacity()];
        } else {
            blockBreakCosts = null;
        }
    }

    private PlayerMovement(PlayerMovement other) {
        this.direction = other.direction;
        this.side = other.side;
        this.modifier = other.modifier;
        this.targetFeetBlock = other.targetFeetBlock;
        this.diagonal = other.diagonal;
        this.cost = other.cost;
        this.appliedCornerCost = other.appliedCornerCost;
        this.isImpossible = other.isImpossible;
        this.allowsBlockActions = other.allowsBlockActions;
        this.blockBreakCosts = other.blockBreakCosts == null ? null : new MovementMiningCost[other.blockBreakCosts.length];
        this.blockPlaceData = other.blockPlaceData;
        this.requiresAgainstBlock = other.requiresAgainstBlock;
    }

    private int freeCapacity() {
        return 2 + (diagonal ? 2 : 0) +
                switch (modifier) {
                    case NORMAL -> 0;
                    case FALL_1 -> 1;
                    case FALL_2, JUMP -> 2;
                    case FALL_3 -> 3;
                };
    }

    public List<Vector3i> listRequiredFreeBlocks() {
        List<Vector3i> requiredFreeBlocks = new ObjectArrayList<>();

        if (modifier == MovementModifier.JUMP) {
            // Make head block free (maybe head block is a slab)
            requiredFreeBlocks.add(feetPositionRelativeBlock.add(0, 1, 0));

            // Make block above head block free
            requiredFreeBlocks.add(feetPositionRelativeBlock.add(0, 2, 0));
        }

        // Add the blocks that are required to be free for diagonal movement
        if (diagonal) {
            var corner = getCorner(side);

            for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
                // Apply jump shift to target edge and offset for body part
                requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
            }
        }

        var targetEdge = direction.offset(feetPositionRelativeBlock);
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target diagonal and offset for body part
            requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(targetEdge)));
        }

        // Require free blocks to fall into the target position
        switch (modifier) {
            case FALL_1 -> requiredFreeBlocks.add(MovementModifier.FALL_1.offset(targetEdge));
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

    private Vector3i getCorner(MovementSide side) {
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
        }).offset(feetPositionRelativeBlock);
    }

    public List<Vector3i> listAddCostIfSolidBlocks() {
        if (!diagonal) {
            return List.of();
        }

        var list = new ObjectArrayList<Vector3i>(2);

        // If these blocks are solid, the bot moves slower because the bot is running around a corner
        var corner = getCorner(side.opposite());
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target edge and offset for body part
            list.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
        }

        return list;
    }

    public Vector3i requiredSolidBlock() {
        // Floor block
        return targetFeetBlock.sub(0, 1, 0);
    }

    public List<BotActionManager.BlockPlaceData> possibleBlocksToPlaceAgainst() {
        if (!allowsBlockActions) {
            return List.of();
        }

        var blockDirection = switch (direction) {
            case NORTH -> BlockDirection.NORTH;
            case SOUTH -> BlockDirection.SOUTH;
            case EAST -> BlockDirection.EAST;
            case WEST -> BlockDirection.WEST;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        };

        var oppositeDirection = blockDirection.opposite();
        var leftDirectionSide = blockDirection.leftSide();
        var rightDirectionSide = blockDirection.rightSide();

        var floorBlock = targetFeetBlock.sub(0, 1, 0);
        return switch (modifier) {
            case NORMAL -> // 5
                    List.of(
                            // Below
                            new BotActionManager.BlockPlaceData(floorBlock.sub(0, 1, 0), Direction.UP),
                            // In front
                            new BotActionManager.BlockPlaceData(blockDirection.offset(floorBlock), oppositeDirection.getDirection()),
                            // Scaffolding
                            new BotActionManager.BlockPlaceData(oppositeDirection.offset(floorBlock), blockDirection.getDirection()),
                            // Left side
                            new BotActionManager.BlockPlaceData(leftDirectionSide.offset(floorBlock), rightDirectionSide.getDirection()),
                            // Right side
                            new BotActionManager.BlockPlaceData(rightDirectionSide.offset(floorBlock), leftDirectionSide.getDirection())
                    );
            case JUMP, FALL_1 -> // 4 - no scaffolding
                    List.of(
                            // Below
                            new BotActionManager.BlockPlaceData(floorBlock.sub(0, 1, 0), Direction.UP),
                            // In front
                            new BotActionManager.BlockPlaceData(blockDirection.offset(floorBlock), oppositeDirection.getDirection()),
                            // Left side
                            new BotActionManager.BlockPlaceData(leftDirectionSide.offset(floorBlock), rightDirectionSide.getDirection()),
                            // Right side
                            new BotActionManager.BlockPlaceData(rightDirectionSide.offset(floorBlock), leftDirectionSide.getDirection())
                    );
            default -> throw new IllegalStateException("Unexpected value: " + modifier);
        };
    }

    public void addCornerCost() {
        cost += Costs.CORNER_SLIDE;
        appliedCornerCost = true;
    }

    @Override
    public boolean isImpossibleToComplete() {
        return isImpossible || (requiresAgainstBlock && blockPlaceData == null);
    }

    @Override
    public GraphInstructions getInstructions(BotEntityState previousEntityState) {
        var actions = new ObjectArrayList<WorldAction>();
        var inventory = previousEntityState.inventory();
        var levelState = previousEntityState.levelState();
        var cost = this.cost;
        if (blockBreakCosts != null) {
            for (var breakCost : blockBreakCosts) {
                if (breakCost == null) {
                    continue;
                }

                cost += breakCost.miningCost();
                actions.add(new BlockBreakAction(breakCost.block()));
                if (breakCost.willDrop()) {
                    inventory = inventory.withOneMoreBlock();
                }

                levelState = levelState.withChangeToAir(breakCost.block());
            }
        }

        var realTarget = previousEntityState.positionBlock().add(targetFeetBlock);

        if (blockPlaceData != null) {
            var floorBlock = realTarget.sub(0, 1, 0);
            cost += Costs.PLACE_BLOCK;
            actions.add(new BlockPlaceAction(floorBlock, blockPlaceData));
            inventory = inventory.withOneLessBlock();
            levelState = levelState.withChangeToSolidBlock(floorBlock);
        }

        var targetDoublePosition = VectorHelper.middleOfBlockNormalize(realTarget.toDouble());
        actions.add(new MovementAction(targetDoublePosition, diagonal));

        return new GraphInstructions(new BotEntityState(
                targetDoublePosition,
                realTarget,
                levelState,
                inventory
        ), cost, actions);
    }

    @Override
    public GraphAction copy(BotEntityState previousEntityState) {
        return new PlayerMovement(this);
    }
}
