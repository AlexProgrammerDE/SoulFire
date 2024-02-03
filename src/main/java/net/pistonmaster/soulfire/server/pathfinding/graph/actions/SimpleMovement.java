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
package net.pistonmaster.soulfire.server.pathfinding.graph.actions;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.Costs;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.BlockPlaceAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.MovementAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.WorldAction;
import net.pistonmaster.soulfire.server.pathfinding.graph.GraphInstructions;
import net.pistonmaster.soulfire.server.pathfinding.graph.actions.movement.*;
import net.pistonmaster.soulfire.server.protocol.bot.BotActionManager;

import java.util.List;

@Slf4j
public final class SimpleMovement extends GraphAction implements Cloneable {
    private static final SWVec3i FEET_POSITION_RELATIVE_BLOCK = SWVec3i.ZERO;
    private final MovementDirection direction;
    private final MovementSide side;
    private final MovementModifier modifier;
    private final SWVec3i targetFeetBlock;
    @Getter
    private final boolean diagonal;
    @Getter
    private final boolean allowBlockActions;
    @Getter
    private MovementMiningCost[] blockBreakCosts;
    @Getter
    private boolean[] unsafeToBreak;
    @Getter
    private boolean[] noNeedToBreak;
    @Setter
    @Getter
    private BotActionManager.BlockPlaceData blockPlaceData;
    private double cost;
    @Getter
    private boolean appliedCornerCost = false;
    @Setter
    private boolean requiresAgainstBlock = false;

    public SimpleMovement(MovementDirection direction, MovementSide side, MovementModifier modifier) {
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

        this.targetFeetBlock = modifier.offset(direction.offset(FEET_POSITION_RELATIVE_BLOCK));
        this.allowBlockActions = !diagonal && (
                modifier == MovementModifier.JUMP
                        || modifier == MovementModifier.NORMAL
                        || modifier == MovementModifier.FALL_1
        );

        if (allowBlockActions) {
            blockBreakCosts = new MovementMiningCost[freeCapacity()];
            unsafeToBreak = new boolean[freeCapacity()];
            noNeedToBreak = new boolean[freeCapacity()];
        } else {
            blockBreakCosts = null;
            unsafeToBreak = null;
            noNeedToBreak = null;
        }
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

    public List<SWVec3i> listRequiredFreeBlocks() {
        var requiredFreeBlocks = new ObjectArrayList<SWVec3i>(freeCapacity());

        if (modifier == MovementModifier.JUMP) {
            // Make head block free (maybe head block is a slab)
            requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0));

            // Make block above the head block free for jump
            requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0));
        }

        // Add the blocks that are required to be free for diagonal movement
        if (diagonal) {
            var corner = getCorner(side);

            for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
                // Apply jump shift to target edge and offset for body part
                requiredFreeBlocks.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
            }
        }

        var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
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

    private SWVec3i getCorner(MovementSide side) {
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
        }).offset(FEET_POSITION_RELATIVE_BLOCK);
    }

    public List<SWVec3i> listAddCostIfSolidBlocks() {
        if (!diagonal) {
            return List.of();
        }

        var list = new ObjectArrayList<SWVec3i>(2);

        // If these blocks are solid, the bot moves slower because the bot is running around a corner
        var corner = getCorner(side.opposite());
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target edge and offset for body part
            list.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
        }

        return list;
    }

    public SWVec3i requiredSolidBlock() {
        // Floor block
        return targetFeetBlock.sub(0, 1, 0);
    }

    public BlockSafetyData[][] listCheckSafeMineBlocks() {
        if (!allowBlockActions) {
            return new BlockSafetyData[0][];
        }

        var requiredFreeBlocks = listRequiredFreeBlocks();
        var results = new BlockSafetyData[requiredFreeBlocks.size()][];

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

        if (modifier == MovementModifier.JUMP) {
            var aboveHead = FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0);
            results[requiredFreeBlocks.indexOf(aboveHead)] = new BlockSafetyData[]{
                    new BlockSafetyData(aboveHead.add(0, 1, 0), BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS),
                    new BlockSafetyData(oppositeDirection.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
                    new BlockSafetyData(leftDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
                    new BlockSafetyData(rightDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS)
            };
        }

        var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
        for (var bodyOffset : BodyPart.BODY_PARTS_REVERSE) {
            // Apply jump shift to target diagonal and offset for body part
            var block = bodyOffset.offset(modifier.offsetIfJump(targetEdge));
            var index = requiredFreeBlocks.indexOf(block);

            if (bodyOffset == BodyPart.HEAD) {
                results[index] = new BlockSafetyData[]{
                        new BlockSafetyData(block.add(0, 1, 0), BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS),
                        new BlockSafetyData(direction.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
                        new BlockSafetyData(leftDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
                        new BlockSafetyData(rightDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS)
                };
            } else {
                results[index] = new BlockSafetyData[]{
                        new BlockSafetyData(direction.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
                        new BlockSafetyData(leftDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
                        new BlockSafetyData(rightDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS)
                };
            }
        }

        // Require free blocks to fall into the target position
        if (modifier == MovementModifier.FALL_1) {
            var fallFree = MovementModifier.FALL_1.offset(targetEdge);
            results[requiredFreeBlocks.indexOf(fallFree)] = new BlockSafetyData[]{
                    new BlockSafetyData(direction.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS),
                    new BlockSafetyData(leftDirectionSide.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS),
                    new BlockSafetyData(rightDirectionSide.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS)
            };
        }

        return results;
    }

    public List<BotActionManager.BlockPlaceData> possibleBlocksToPlaceAgainst() {
        if (!allowBlockActions) {
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
                            new BotActionManager.BlockPlaceData(blockDirection.offset(floorBlock), oppositeDirection.direction()),
                            // Scaffolding
                            new BotActionManager.BlockPlaceData(oppositeDirection.offset(floorBlock), blockDirection.direction()),
                            // Left side
                            new BotActionManager.BlockPlaceData(leftDirectionSide.offset(floorBlock), rightDirectionSide.direction()),
                            // Right side
                            new BotActionManager.BlockPlaceData(rightDirectionSide.offset(floorBlock), leftDirectionSide.direction())
                    );
            case JUMP, FALL_1 -> // 4 - no scaffolding
                    List.of(
                            // Below
                            new BotActionManager.BlockPlaceData(floorBlock.sub(0, 1, 0), Direction.UP),
                            // In front
                            new BotActionManager.BlockPlaceData(blockDirection.offset(floorBlock), oppositeDirection.direction()),
                            // Left side
                            new BotActionManager.BlockPlaceData(leftDirectionSide.offset(floorBlock), rightDirectionSide.direction()),
                            // Right side
                            new BotActionManager.BlockPlaceData(rightDirectionSide.offset(floorBlock), leftDirectionSide.direction())
                    );
            default -> throw new IllegalStateException("Unexpected value: " + modifier);
        };
    }

    public void addCornerCost() {
        cost += Costs.CORNER_SLIDE;
        appliedCornerCost = true;
    }

    @Override
    public boolean impossibleToComplete() {
        return requiresAgainstBlock && blockPlaceData == null;
    }

    @Override
    public GraphInstructions getInstructions(BotEntityState previousEntityState) {
        var inventory = previousEntityState.inventory();
        var levelState = previousEntityState.levelState();
        var cost = this.cost;

        var blocksToBreak = blockBreakCosts == null ? 0 : blockBreakCosts.length;
        var blockToBreakArray = blocksToBreak > 0 ? new SWVec3i[blocksToBreak] : null;
        var blockToPlace = requiresAgainstBlock ? 1 : 0;
        SWVec3i blockToPlacePosition = null;

        var actions = new ObjectArrayList<WorldAction>(1 + blocksToBreak + blockToPlace);
        if (blockBreakCosts != null) {
            for (var i = 0; i < blockBreakCosts.length; i++) {
                var breakCost = blockBreakCosts[i];
                if (breakCost == null) {
                    continue;
                }

                cost += breakCost.miningCost();
                actions.add(new BlockBreakAction(breakCost.block()));
                if (breakCost.willDrop()) {
                    inventory = inventory.withOneMoreBlock();
                }

                blockToBreakArray[i] = breakCost.block();
            }
        }

        var absoluteTargetFeetBlock = previousEntityState.blockPosition().add(targetFeetBlock);

        if (requiresAgainstBlock) {
            var floorBlock = absoluteTargetFeetBlock.sub(0, 1, 0);
            cost += Costs.PLACE_BLOCK;
            actions.add(new BlockPlaceAction(floorBlock, blockPlaceData));
            inventory = inventory.withOneLessBlock();

            blockToPlacePosition = floorBlock;
        }

        if (blockToBreakArray != null || blockToPlacePosition != null) {
            levelState = levelState.withChanges(blockToBreakArray, blockToPlacePosition);
        }

        actions.add(new MovementAction(absoluteTargetFeetBlock, diagonal));

        return new GraphInstructions(new BotEntityState(
                absoluteTargetFeetBlock,
                levelState,
                inventory
        ), cost, actions);
    }

    @Override
    public SimpleMovement copy(BotEntityState previousEntityState) {
        return this.clone();
    }

    @Override
    public SimpleMovement clone() {
        try {
            var c = (SimpleMovement) super.clone();

            c.blockBreakCosts = this.blockBreakCosts == null ? null : new MovementMiningCost[this.blockBreakCosts.length];
            c.unsafeToBreak = this.unsafeToBreak == null ? null : new boolean[this.unsafeToBreak.length];
            c.noNeedToBreak = this.noNeedToBreak == null ? null : new boolean[this.noNeedToBreak.length];

            return c;
        } catch (CloneNotSupportedException cantHappen) {
            throw new InternalError();
        }
    }
}
