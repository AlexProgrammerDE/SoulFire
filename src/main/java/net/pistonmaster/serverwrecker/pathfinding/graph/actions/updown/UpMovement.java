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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions.updown;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.JumpAndPlaceBelowAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.BlockDirection;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.BlockSafetyData;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.MovementMiningCost;
import net.pistonmaster.serverwrecker.protocol.bot.BotActionManager;
import net.pistonmaster.serverwrecker.util.VectorHelper;

import java.util.List;

@Slf4j
public final class UpMovement implements GraphAction {
    private static final SWVec3i FEET_POSITION_RELATIVE_BLOCK = SWVec3i.ZERO;
    private final SWVec3i targetFeetBlock;
    @Getter
    private final MovementMiningCost[] blockBreakCosts;
    @Getter
    private final boolean[] unsafeToBreak;
    @Getter
    private final boolean[] noNeedToBreak;
    @Setter
    @Getter
    private boolean isImpossible = false;
    @Setter
    private boolean requiresAgainstBlock = false;

    public UpMovement() {
        this.targetFeetBlock = FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0);

        blockBreakCosts = new MovementMiningCost[freeCapacity()];
        unsafeToBreak = new boolean[freeCapacity()];
        noNeedToBreak = new boolean[freeCapacity()];
    }

    private UpMovement(UpMovement other) {
        this.targetFeetBlock = other.targetFeetBlock;
        this.isImpossible = other.isImpossible;
        this.blockBreakCosts = new MovementMiningCost[other.blockBreakCosts.length];
        this.unsafeToBreak = new boolean[other.unsafeToBreak.length];
        this.noNeedToBreak = new boolean[other.noNeedToBreak.length];
        this.requiresAgainstBlock = other.requiresAgainstBlock;
    }

    private int freeCapacity() {
        return 1;
    }

    public List<SWVec3i> listRequiredFreeBlocks() {
        List<SWVec3i> requiredFreeBlocks = new ObjectArrayList<>(freeCapacity());

        // The one above the head to jump
        requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0));

        return requiredFreeBlocks;
    }

    public BlockSafetyData[][] listCheckSafeMineBlocks() {
        var requiredFreeBlocks = listRequiredFreeBlocks();
        var results = new BlockSafetyData[requiredFreeBlocks.size()][];

        var firstDirection = BlockDirection.NORTH;
        var oppositeDirection = firstDirection.opposite();
        var leftDirectionSide = firstDirection.leftSide();
        var rightDirectionSide = firstDirection.rightSide();

        var aboveHead = FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0);
        results[requiredFreeBlocks.indexOf(aboveHead)] = new BlockSafetyData[]{
                new BlockSafetyData(aboveHead.add(0, 1, 0), BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS),
                new BlockSafetyData(oppositeDirection.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
                new BlockSafetyData(leftDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
                new BlockSafetyData(rightDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS)
        };

        return results;
    }

    @Override
    public boolean isImpossibleToComplete() {
        return isImpossible;
    }

    @Override
    public GraphInstructions getInstructions(BotEntityState previousEntityState) {
        var actions = new ObjectArrayList<WorldAction>();
        var inventory = previousEntityState.inventory();
        var levelState = previousEntityState.levelState();
        var cost = Costs.JUMP_UP_AND_PLACE_BELOW;

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

        // Change values for block we're going to place and stand on
        inventory = inventory.withOneLessBlock();
        levelState = levelState.withChangeToSolidBlock(previousEntityState.positionBlock());

        var absoluteTargetFeetBlock = previousEntityState.positionBlock().add(targetFeetBlock);
        var targetFeetDoublePosition = VectorHelper.middleOfBlockNormalize(absoluteTargetFeetBlock.toVector3d());

        // Where we are standing right now, we'll place the target block below us after jumping
        actions.add(new JumpAndPlaceBelowAction(previousEntityState.positionBlock(), new BotActionManager.BlockPlaceData(
                previousEntityState.positionBlock().sub(0, 1, 0),
                Direction.UP
        )));

        return new GraphInstructions(new BotEntityState(
                targetFeetDoublePosition,
                absoluteTargetFeetBlock,
                levelState,
                inventory
        ), cost, actions);
    }

    @Override
    public UpMovement copy(BotEntityState previousEntityState) {
        var upMovement = new UpMovement(this);
        upMovement.isImpossible = !previousEntityState.inventory().hasBlockToPlace();
        return upMovement;
    }
}
