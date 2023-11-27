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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement.MovementMiningCost;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import net.pistonmaster.serverwrecker.pathfinding.SWVec3i;

import java.util.List;

public class DownMovement implements GraphAction {
    private static final SWVec3i FEET_POSITION_RELATIVE_BLOCK = SWVec3i.ZERO;
    private final SWVec3i targetToMineBlock;
    @Getter
    @Setter
    private MovementMiningCost blockBreakCosts;
    @Setter
    @Getter
    private boolean isImpossible = false;
    @Getter
    @Setter
    private int closestBlockToFallOn = Integer.MIN_VALUE;

    public DownMovement() {
        this.targetToMineBlock = FEET_POSITION_RELATIVE_BLOCK.sub(0, 1, 0);
    }

    private DownMovement(DownMovement other) {
        this.targetToMineBlock = other.targetToMineBlock;
        this.isImpossible = other.isImpossible;
        this.blockBreakCosts = other.blockBreakCosts;
        this.closestBlockToFallOn = other.closestBlockToFallOn;
    }

    public SWVec3i blockToBreak() {
        return targetToMineBlock;
    }

    public List<SWVec3i> listSafetyCheckBlocks() {
        List<SWVec3i> requiredFreeBlocks = new ObjectArrayList<>();

        // Falls one block
        requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0));

        // Falls two blocks
        requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0));

        // Falls three blocks
        requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 4, 0));

        return requiredFreeBlocks;
    }

    @Override
    public boolean isImpossibleToComplete() {
        return isImpossible || closestBlockToFallOn == Integer.MIN_VALUE;
    }

    @Override
    public GraphInstructions getInstructions(BotEntityState previousEntityState) {
        var inventory = previousEntityState.inventory();
        var levelState = previousEntityState.levelState();
        var cost = 0D;

        cost += switch (closestBlockToFallOn) {
            case -2 -> Costs.FALL_1;
            case -3 -> Costs.FALL_2;
            case -4 -> Costs.FALL_3;
            default -> throw new IllegalStateException("Unexpected value: " + closestBlockToFallOn);
        };

        cost += blockBreakCosts.miningCost();
        if (blockBreakCosts.willDrop()) {
            inventory = inventory.withOneMoreBlock();
        }

        levelState = levelState.withChangeToAir(blockBreakCosts.block());

        var absoluteMinedBlock = previousEntityState.positionBlock().add(targetToMineBlock);
        var absoluteTargetFeetBlock = previousEntityState.positionBlock().add(0, closestBlockToFallOn + 1, 0);
        var targetFeetDoublePosition = VectorHelper.middleOfBlockNormalize(absoluteTargetFeetBlock.toVector3d());

        return new GraphInstructions(new BotEntityState(
                targetFeetDoublePosition,
                absoluteTargetFeetBlock,
                levelState,
                inventory
        ), cost, List.of(new BlockBreakAction(absoluteMinedBlock)));
    }

    @Override
    public DownMovement copy(BotEntityState previousEntityState) {
        return new DownMovement(this);
    }
}
