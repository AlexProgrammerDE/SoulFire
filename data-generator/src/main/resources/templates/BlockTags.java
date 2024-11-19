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

import net.kyori.adventure.key.KeyPattern;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class BlockTags {
  public static final List<TagKey<BlockType>> TAGS = new ArrayList<>();

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  private BlockTags() {}

  public static TagKey<BlockType> register(@KeyPattern String key) {
    var resourceKey = TagKey.<BlockType>key(key, RegistryKeys.BLOCK);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
