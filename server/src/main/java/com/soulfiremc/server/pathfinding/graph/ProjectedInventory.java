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
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.protocol.bot.container.ContainerSlot;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.util.ItemTypeHelper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * An immutable representation of a player inventory. This takes an inventory and projects changes
 * onto it. This way we calculate the way we can do actions after a block was broken/placed.
 */
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class ProjectedInventory {
  @Getter
  @ToString.Include
  private final int usableBlockItems;
  @Getter
  @ToString.Include
  private final SFItemStack[] usableToolsAndNull;
  private final Map<BlockType, Costs.BlockMiningCosts> sharedMiningCosts;
  private final ClientEntity entity;
  private final PlayerInventoryContainer playerInventory;

  public ProjectedInventory(PlayerInventoryContainer playerInventory, ClientEntity entity,
                            Predicate<SFItemStack> isPlaceable, Predicate<SFItemStack> isTool) {
    this(
      Arrays.stream(playerInventory.storage())
        .map(ContainerSlot::item)
        .filter(item -> item != null && item.getAmount() > 0)
        .toList(), entity, playerInventory,
      isPlaceable, isTool);
  }

  public ProjectedInventory(List<SFItemStack> items) {
    this(items, null, null, ItemTypeHelper::isSafeFullBlockItem, ItemTypeHelper::isTool);
  }

  public ProjectedInventory(List<SFItemStack> items, ClientEntity entity, PlayerInventoryContainer playerInventory,
                            Predicate<SFItemStack> isPlaceable, Predicate<SFItemStack> isTool) {
    this.entity = entity;
    this.playerInventory = playerInventory;

    var blockItems = 0;
    var usableToolsAndNull = new HashSet<SFItemStack>();

    // Empty slot
    usableToolsAndNull.add(null);

    for (var item : items) {
      if (isPlaceable.test(item)) {
        blockItems += item.getAmount();
      } else if (isTool.test(item)) {
        usableToolsAndNull.add(item);
      }
    }

    this.usableBlockItems = blockItems;
    this.usableToolsAndNull = usableToolsAndNull.toArray(new SFItemStack[0]);
    this.sharedMiningCosts = new ConcurrentHashMap<>();
  }

  public boolean creativeModeBreak() {
    return entity != null && entity.abilities().creativeModeBreak();
  }

  public Costs.BlockMiningCosts getMiningCosts(TagsState tagsState, BlockState blockState) {
    return sharedMiningCosts.computeIfAbsent(
      blockState.blockType(), type -> Costs.calculateBlockBreakCost(tagsState, entity, playerInventory, this, type));
  }
}
