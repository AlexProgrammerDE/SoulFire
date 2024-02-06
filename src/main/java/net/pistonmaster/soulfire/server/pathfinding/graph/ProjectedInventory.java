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
package net.pistonmaster.soulfire.server.pathfinding.graph;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.Costs;
import net.pistonmaster.soulfire.server.protocol.bot.container.ContainerSlot;
import net.pistonmaster.soulfire.server.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.soulfire.server.protocol.bot.container.SWItemStack;
import net.pistonmaster.soulfire.server.protocol.bot.state.TagsState;
import net.pistonmaster.soulfire.server.util.ItemTypeHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
        this(Arrays.stream(playerInventory.storage())
                .map(ContainerSlot::item)
                .filter(item -> item != null && item.getAmount() > 0)
                .toList());
    }

    public ProjectedInventory(List<SWItemStack> items) {
        var blockItems = 0;
        var usableToolsAndNull = new HashSet<SWItemStack>();

        // Empty slot
        usableToolsAndNull.add(null);

        for (var item : items) {
            if (ItemTypeHelper.isSafeFullBlockItem(item.type())) {
                blockItems += item.getAmount();
            } else if (ItemTypeHelper.isTool(item.type())) {
                usableToolsAndNull.add(item);
            }
        }

        this.usableBlockItems = blockItems;

        this.usableToolsAndNull = usableToolsAndNull.toArray(new SWItemStack[0]);

        this.sharedMiningCosts = new ConcurrentHashMap<>();
    }

    public boolean hasNoBlocks() {
        return usableBlockItems <= 0;
    }

    public ProjectedInventory withOneLessBlock() {
        return new ProjectedInventory(usableBlockItems - 1, usableToolsAndNull, sharedMiningCosts);
    }

    public ProjectedInventory withOneMoreBlock() {
        return new ProjectedInventory(usableBlockItems + 1, usableToolsAndNull, sharedMiningCosts);
    }

    public Costs.BlockMiningCosts getMiningCosts(TagsState tagsState, BlockState blockState) {
        return sharedMiningCosts.computeIfAbsent(blockState.blockType(), type ->
                Costs.calculateBlockBreakCost(tagsState, this, type));
    }
}
