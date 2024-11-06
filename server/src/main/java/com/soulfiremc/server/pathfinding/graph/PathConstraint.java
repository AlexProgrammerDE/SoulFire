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
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.LevelHeightAccessor;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.ItemTypeHelper;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@RequiredArgsConstructor
public class PathConstraint {
  private static final boolean ALLOW_BREAKING_UNDIGGABLE = Boolean.getBoolean("sf.pathfinding-allow-breaking-undiggable");
  private final ClientEntity entity;
  private final LevelHeightAccessor levelHeightAccessor;

  public PathConstraint(BotConnection botConnection) {
    this(botConnection.dataManager().clientEntity(), botConnection.dataManager().currentLevel());
  }

  public boolean doUsableBlocksDecreaseWhenPlaced() {
    return entity == null || !entity.abilitiesData().instabuild();
  }

  public boolean isPlaceable(SFItemStack item) {
    return ItemTypeHelper.isSafeFullBlockItem(item);
  }

  public boolean isTool(SFItemStack item) {
    return ItemTypeHelper.isTool(item);
  }

  public boolean isOutOfLevel(BlockState blockState, SFVec3i pos) {
    return blockState.blockType() == BlockType.VOID_AIR && !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  public boolean canBreakBlockPos(SFVec3i pos) {
    return !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  public boolean canPlaceBlockPos(SFVec3i pos) {
    return !levelHeightAccessor.isOutsideBuildHeight(pos.y);
  }

  public boolean canBreakBlockType(BlockType blockType) {
    if (ALLOW_BREAKING_UNDIGGABLE) {
      return true;
    }

    return BlockTypeHelper.isDiggable(blockType);
  }
}
