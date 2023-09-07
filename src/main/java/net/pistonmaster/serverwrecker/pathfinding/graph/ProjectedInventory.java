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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.container.SWItemStack;

import java.util.Optional;

/**
 * An immutable representation of a player inventory.
 * This takes an inventory and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedInventory {
    private final PlayerInventoryContainer playerInventory;
    private final Int2ObjectMap<ContainerSlot> slotChanges;
    private final int slotChangesHash;

    public ProjectedInventory(PlayerInventoryContainer playerInventory) {
        Int2ObjectMap<ContainerSlot> slotChanges = new Int2ObjectOpenHashMap<>();
        this.playerInventory = playerInventory;
        this.slotChanges = slotChanges;
        this.slotChangesHash = slotChanges.hashCode();
    }

    public ProjectedInventory withChange(int slot, SWItemStack itemStack) {
        Int2ObjectMap<ContainerSlot> slotChanges = new Int2ObjectOpenHashMap<>(this.slotChanges);
        slotChanges.put(slot, new ContainerSlot(slot, itemStack));

        return new ProjectedInventory(playerInventory, slotChanges, slotChanges.hashCode());
    }

    public ContainerSlot[] getStorage() {
        ContainerSlot[] storage = playerInventory.getStorage();

        for (int i = 0; i < storage.length; i++) {
            Optional<ContainerSlot> cachedSlot = populateCache(storage[i]);
            if (cachedSlot.isPresent()) {
                storage[i] = cachedSlot.get();
            }
        }

        return storage;
    }

    private Optional<ContainerSlot> populateCache(ContainerSlot slot) {
        ContainerSlot cachedSlot = slotChanges.get(slot.slot());
        if (cachedSlot != null) {
            return Optional.of(cachedSlot);
        }

        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectedInventory that = (ProjectedInventory) o;
        return slotChangesHash == that.slotChangesHash;
    }

    @Override
    public int hashCode() {
        return slotChangesHash;
    }
}
