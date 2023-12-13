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
package net.pistonmaster.serverwrecker.pathfinding.graph;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.container.SWItemStack;
import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;
import net.pistonmaster.serverwrecker.util.ItemTypeHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An immutable representation of a player inventory.
 * This takes an inventory and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedInventory {
    private final int usableBlockItems;
    @Getter
    private final SWItemStack[] usableToolsAndNull;
    private final Map<BlockType, Costs.BlockMiningCosts> sharedMiningCosts;

    public ProjectedInventory(PlayerInventoryContainer playerInventory) {
        var blockItems = 0;
        var usableToolsAndNull = new HashSet<SWItemStack>();

        // Empty slot
        usableToolsAndNull.add(null);

        for (var slot : playerInventory.storage()) {
            if (slot.item() == null) {
                continue;
            }

            if (ItemTypeHelper.isSafeFullBlockItem(slot.item().type())) {
                blockItems += slot.item().getAmount();
            } else if (ItemTypeHelper.isTool(slot.item().type())) {
                usableToolsAndNull.add(slot.item());
            }
        }

        this.usableBlockItems = blockItems;

        this.usableToolsAndNull = usableToolsAndNull.toArray(new SWItemStack[0]);

        this.sharedMiningCosts = new ConcurrentHashMap<>();
    }

    public boolean hasBlockToPlace() {
        return usableBlockItems > 0;
    }

    public ProjectedInventory withOneLessBlock() {
        return new ProjectedInventory(usableBlockItems - 1, usableToolsAndNull, sharedMiningCosts);
    }

    public ProjectedInventory withOneMoreBlock() {
        return new ProjectedInventory(usableBlockItems + 1, usableToolsAndNull, sharedMiningCosts);
    }

    public Costs.BlockMiningCosts getMiningCosts(TagsState tagsState, BlockStateMeta blockStateMeta) {
        return sharedMiningCosts.computeIfAbsent(blockStateMeta.blockType(), type ->
                Costs.calculateBlockBreakCost(tagsState, this, type));
    }
}
