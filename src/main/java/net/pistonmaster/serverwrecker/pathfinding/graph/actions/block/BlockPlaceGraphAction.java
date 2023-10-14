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

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.execution.BlockPlaceAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphAction;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import net.pistonmaster.serverwrecker.protocol.bot.BotActionManager;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

public class BlockPlaceGraphAction implements GraphAction {
    @Getter
    private final BotEntityState previousEntityState;
    private final BlockDirection direction;
    private final BlockModifier modifier;
    private final Vector3i targetBlock;
    @Getter
    @Setter
    private BotActionManager.BlockPlaceData blockToPlaceAgainst = null;
    @Setter
    @Getter
    private boolean isImpossible;

    public BlockPlaceGraphAction(BotEntityState previousEntityState, BlockDirection direction, BlockModifier modifier) {
        this.previousEntityState = previousEntityState;
        this.direction = direction;
        this.modifier = modifier;

        // No block to place means instant failure
        this.isImpossible = !previousEntityState.inventory().hasBlockToPlace();
        this.targetBlock = modifier.offset(direction.offset(previousEntityState.positionBlock()));
    }

    public Vector3i requiredReplaceableBlock() {
        return targetBlock;
    }

    public List<BotActionManager.BlockPlaceData> possibleBlocksToPlaceAgainst() {
        var oppositeDirection = direction.opposite().getDirection();
        var leftDirectionSide = direction.leftSide();
        var rightDirectionSide = direction.rightSide();

        return switch (modifier) {
            case HEAD -> // 5
                    List.of(
                            new BotActionManager.BlockPlaceData(targetBlock.sub(0, 1, 0), Direction.UP),
                            new BotActionManager.BlockPlaceData(direction.offset(targetBlock), oppositeDirection),
                            new BotActionManager.BlockPlaceData(leftDirectionSide.offset(targetBlock), rightDirectionSide.getDirection()),
                            new BotActionManager.BlockPlaceData(rightDirectionSide.offset(targetBlock), leftDirectionSide.getDirection()),
                            new BotActionManager.BlockPlaceData(targetBlock.add(0, 1, 0), Direction.DOWN)
                    );
            case FEET, FLOOR -> // 4
                    List.of(
                            new BotActionManager.BlockPlaceData(targetBlock.sub(0, 1, 0), Direction.UP),
                            new BotActionManager.BlockPlaceData(direction.offset(targetBlock), oppositeDirection),
                            new BotActionManager.BlockPlaceData(leftDirectionSide.offset(targetBlock), rightDirectionSide.getDirection()),
                            new BotActionManager.BlockPlaceData(rightDirectionSide.offset(targetBlock), leftDirectionSide.getDirection())
                    );
        };
    }

    @Override
    public GraphInstructions getInstructions() {
        return new GraphInstructions(new BotEntityState(
                previousEntityState.position(),
                previousEntityState.positionBlock(),
                previousEntityState.levelState().withChangeToSolidBlock(targetBlock),
                previousEntityState.inventory().withOneLessBlock()
        ), Costs.PLACE_BLOCK, List.of(new BlockPlaceAction(targetBlock, blockToPlaceAgainst)));
    }

    @Override
    public boolean isImpossibleToComplete() {
        return isImpossible || blockToPlaceAgainst == null;
    }
}
