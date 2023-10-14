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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions.block;

import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockBreakAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;

public class BlockBreakGraphAction implements GraphAction {
    @Getter
    private final BotEntityState previousEntityState;
    private final BlockModifier modifier;
    private final Vector3i targetWithoutModifier;
    private final Vector3i targetBlock;
    @Setter
    private Costs.BlockMiningCosts costs;
    @Setter
    @Getter
    private boolean isImpossible = false;

    public BlockBreakGraphAction(BotEntityState previousEntityState, BlockDirection direction, BlockModifier modifier) {
        this.previousEntityState = previousEntityState;
        this.modifier = modifier;
        this.targetWithoutModifier = direction.offset(previousEntityState.positionBlock());
        this.targetBlock = modifier.offset(targetWithoutModifier);
    }

    public Vector3i requiredSolidBlock() {
        return targetBlock;
    }

    public Optional<Vector3i> requiredFreeBlock() {
        if (modifier == BlockModifier.FLOOR) {
            return Optional.of(targetWithoutModifier);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public GraphInstructions getInstructions() {
        var inventory = previousEntityState.inventory();

        if (costs.willDrop()) {
            inventory = inventory.withOneMoreBlock();
        }

        return new GraphInstructions(new BotEntityState(
                previousEntityState.position(),
                previousEntityState.positionBlock(),
                previousEntityState.levelState().withChangeToAir(targetBlock),
                inventory
        ), costs.miningCost(), List.of(new BlockBreakAction(targetBlock)));
    }

    @Override
    public boolean isImpossibleToComplete() {
        return isImpossible;
    }
}
