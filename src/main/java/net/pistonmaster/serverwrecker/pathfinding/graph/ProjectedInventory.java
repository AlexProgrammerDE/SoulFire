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

import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.container.SWItemStack;
import net.pistonmaster.serverwrecker.util.ItemTypeHelper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable representation of a player inventory.
 * This takes an inventory and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedInventory {
    private final int usableBlockItems;
    private final Set<SWItemStack> usableTools;
    private final int changeHash;

    public ProjectedInventory(PlayerInventoryContainer playerInventory) {
        var blockItems = 0;
        var usableTools = new HashSet<SWItemStack>();
        for (var slot : playerInventory.getStorage()) {
            if (slot.item() == null) {
                continue;
            }

            if (ItemTypeHelper.isFullBlockItem(slot.item().getType())) {
                blockItems += slot.item().getAmount();
            } else if (ItemTypeHelper.isTool(slot.item().getType())) {
                usableTools.add(slot.item());
            }
        }

        this.usableBlockItems = blockItems;
        if (usableTools.isEmpty()) {
            this.usableTools = Set.of();
        } else {
            this.usableTools = usableTools;
        }

        this.changeHash = Objects.hash(usableBlockItems, usableTools);
    }

    public boolean hasBlockToPlace() {
        return usableBlockItems > 0;
    }

    public ProjectedInventory withOneLessBlock() {
        return new ProjectedInventory(usableBlockItems - 1, usableTools, changeHash);
    }

    public ProjectedInventory withOneMoreBlock() {
        return new ProjectedInventory(usableBlockItems + 1, usableTools, changeHash);
    }

    public SWItemStack[] getToolAndNull() {
        var array = new SWItemStack[usableTools.size() + 1];

        var i = 0;
        for (var tool : usableTools) {
            array[i] = tool;
            i++;
        }

        array[i] = null;

        return array;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ProjectedInventory) o;
        return changeHash == that.changeHash;
    }

    @Override
    public int hashCode() {
        return changeHash;
    }
}
