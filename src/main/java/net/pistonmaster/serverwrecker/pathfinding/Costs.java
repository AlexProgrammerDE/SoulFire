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
package net.pistonmaster.serverwrecker.pathfinding;

import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.data.ItemType;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Costs {
    public static final double STRAIGHT = 1;
    public static final double DIAGONAL = 1.4142135623730951;
    public static final double JUMP = 0.3;
    public static final double FALL_1 = 0.1;
    public static final double FALL_2 = 0.2;
    public static final double FALL_3 = 0.3;
    public static final double DIG_BLOCK_WITHOUT_TOOL = 10;
    public static final double DIG_BLOCK_WITH_TOOL = 5;
    public static final double PLACE_BLOCK = 10;

    private Costs() {
    }

    public static Optional<BlockMiningCosts> calculateBlockCost(PlayerInventoryContainer inventory, BlockStateMeta blockStateMeta) {
        BlockType blockType = blockStateMeta.blockType();

        // Don't try to find a way to dig bedrock
        if (!blockType.diggable()) {
            return Optional.empty();
        }

        // We only want to dig full blocks (not slabs, stairs, etc.), removes a lot of edge cases
        if (!blockStateMeta.blockShapeType().isFullBlock()) {
            return Optional.empty();
        }

        if (blockType.tools().isEmpty()) {
            return Optional.of(new BlockMiningCosts(DIG_BLOCK_WITHOUT_TOOL, null));
        }

        for (ContainerSlot slot : inventory.getStorage()) {
            if (slot.item() == null) {
                continue;
            }

            if (blockType.tools().contains(slot.item().getType())) {
                return Optional.of(new BlockMiningCosts(DIG_BLOCK_WITH_TOOL, slot.item().getType()));
            }
        }

        return Optional.of(new BlockMiningCosts(DIG_BLOCK_WITHOUT_TOOL, null));
    }

    public record BlockMiningCosts(double miningCost, @Nullable ItemType toolType) {
    }
}
