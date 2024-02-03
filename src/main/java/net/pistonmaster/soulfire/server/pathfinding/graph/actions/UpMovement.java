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
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.Costs;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.JumpAndPlaceBelowAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.WorldAction;
import net.pistonmaster.soulfire.server.pathfinding.graph.GraphInstructions;
import net.pistonmaster.soulfire.server.pathfinding.graph.actions.movement.BlockDirection;
import net.pistonmaster.soulfire.server.pathfinding.graph.actions.movement.BlockSafetyData;
import net.pistonmaster.soulfire.server.pathfinding.graph.actions.movement.MovementMiningCost;
import net.pistonmaster.soulfire.server.protocol.bot.BotActionManager;

import java.util.List;

@Slf4j
public final class UpMovement extends GraphAction implements Cloneable {
    private static final SWVec3i FEET_POSITION_RELATIVE_BLOCK = SWVec3i.ZERO;
    private final SWVec3i targetFeetBlock;
    @Getter
    private MovementMiningCost[] blockBreakCosts;
    @Getter
    private boolean[] unsafeToBreak;
    @Getter
    private boolean[] noNeedToBreak;

    public UpMovement() {
        this.targetFeetBlock = FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0);

        this.blockBreakCosts = new MovementMiningCost[freeCapacity()];
        this.unsafeToBreak = new boolean[freeCapacity()];
        this.noNeedToBreak = new boolean[freeCapacity()];
    }

    private int freeCapacity() {
        return 1;
    }

    public List<SWVec3i> listRequiredFreeBlocks() {
        var requiredFreeBlocks = new ObjectArrayList<SWVec3i>(freeCapacity());

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
        levelState = levelState.withChangeToSolidBlock(previousEntityState.blockPosition());

        var absoluteTargetFeetBlock = previousEntityState.blockPosition().add(targetFeetBlock);

        // Where we are standing right now, we'll place the target block below us after jumping
        actions.add(new JumpAndPlaceBelowAction(previousEntityState.blockPosition(), new BotActionManager.BlockPlaceData(
                previousEntityState.blockPosition().sub(0, 1, 0),
                Direction.UP
        )));

        return new GraphInstructions(new BotEntityState(
                absoluteTargetFeetBlock,
                levelState,
                inventory
        ), cost, actions);
    }

    @Override
    public UpMovement copy(BotEntityState previousEntityState) {
        // Skip calculations since we have no blocks to place
        if (previousEntityState.inventory().hasNoBlocks()) {
            return null;
        }

        return this.clone();
    }

    @Override
    public UpMovement clone() {
        try {
            var c = (UpMovement) super.clone();

            c.blockBreakCosts = this.blockBreakCosts == null ? null : new MovementMiningCost[this.blockBreakCosts.length];
            c.unsafeToBreak = this.unsafeToBreak == null ? null : new boolean[this.unsafeToBreak.length];
            c.noNeedToBreak = this.noNeedToBreak == null ? null : new boolean[this.noNeedToBreak.length];

            return c;
        } catch (CloneNotSupportedException cantHappen) {
            throw new InternalError();
        }
    }
}
