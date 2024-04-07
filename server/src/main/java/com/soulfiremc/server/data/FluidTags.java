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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class FluidTags {
  public static final List<ResourceKey> TAGS = new ArrayList<>();

  //@formatter:off
  public static final ResourceKey WATER = register("minecraft:water");
  public static final ResourceKey LAVA = register("minecraft:lava");
  //@formatter:on

  private FluidTags() {}

  public static ResourceKey register(String key) {
    var resourceKey = ResourceKey.fromString(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
