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
package com.soulfiremc.server.data;

import com.soulfiremc.server.protocol.bot.state.TagsState;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MineableType {
  PICKAXE(ItemTags.PICKAXES, BlockTags.MINEABLE_WITH_PICKAXE),
  SHOVEL(ItemTags.SHOVELS, BlockTags.MINEABLE_WITH_SHOVEL),
  AXE(ItemTags.AXES, BlockTags.MINEABLE_WITH_AXE),
  HOE(ItemTags.HOES, BlockTags.MINEABLE_WITH_HOE);

  public static MineableType[] VALUES = values();
  private final ResourceKey itemTagKey;
  @Getter
  private final ResourceKey blockTagKey;

  public static Optional<MineableType> getFromTool(TagsState tagsState, ItemType itemType) {
    for (var mineableType : VALUES) {
      if (tagsState.isItemInTag(itemType, mineableType.itemTagKey)) {
        return Optional.of(mineableType);
      }
    }

    return Optional.empty();
  }
}
