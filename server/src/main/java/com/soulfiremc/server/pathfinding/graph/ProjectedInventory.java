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
import com.soulfiremc.server.data.BlockTags;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.protocol.bot.container.ContainerSlot;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.structs.IDBooleanMap;
import com.soulfiremc.server.util.structs.IDMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * An immutable representation of a player inventory. This takes an inventory and projects places/breaks
 * onto it. This way we calculate the way we can do actions after a block was broken/placed.
 */
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public final class ProjectedInventory {
  @Getter
  @ToString.Include
  private final int usableBlockItems;
  @Getter
  @ToString.Include
  private final SFItemStack[] usableToolsAndNull;
  private final IDMap<BlockType, Costs.BlockMiningCosts> sharedMiningCosts;
  private final IDBooleanMap<BlockState> stairsBlockToStandOn;

  public ProjectedInventory(PlayerInventoryContainer playerInventory, LocalPlayer entity, TagsState tagsState, PathConstraint pathConstraint) {
    this(
      Arrays.stream(playerInventory.storage())
        .map(ContainerSlot::item)
        .filter(item -> item != null && item.getAmount() > 0)
        .toList(),
      entity,
      tagsState,
      pathConstraint);
  }

  public ProjectedInventory(List<SFItemStack> items, LocalPlayer entity, TagsState tagsState, PathConstraint pathConstraint) {
    var blockItems = 0;
    var usableToolsAndNull = new HashSet<SFItemStack>();

    // Empty slot
    usableToolsAndNull.add(null);

    for (var item : items) {
      if (pathConstraint.isPlaceable(item)) {
        blockItems += item.getAmount();
      } else if (pathConstraint.isTool(item)) {
        usableToolsAndNull.add(item);
      }
    }

    this.usableBlockItems = blockItems;
    this.usableToolsAndNull = usableToolsAndNull.toArray(new SFItemStack[0]);
    this.sharedMiningCosts = new IDMap<>(BlockType.REGISTRY.values(),
      blockType -> Costs.calculateBlockBreakCost(tagsState, entity, this, blockType));
    this.stairsBlockToStandOn = new IDBooleanMap<>(BlockType.REGISTRY.values()
      .stream()
      .flatMap(blockType -> blockType.statesData().possibleStates().stream())
      .toList(),
      state -> tagsState.is(state.blockType(), BlockTags.STAIRS) && !SFBlockHelpers.isHurtWhenStoodOn(state));
  }

  @VisibleForTesting
  public static ProjectedInventory forUnitTest(List<SFItemStack> items, TagsState tagsState, PathConstraint pathConstraint) {
    return new ProjectedInventory(items, null, tagsState, pathConstraint);
  }

  public Costs.BlockMiningCosts getMiningCosts(BlockState blockState) {
    return Objects.requireNonNull(sharedMiningCosts.get(blockState.blockType()), "BlockType not destructible");
  }

  public boolean isStairsBlockToStandOn(BlockState blockState) {
    return stairsBlockToStandOn.get(blockState);
  }
}
