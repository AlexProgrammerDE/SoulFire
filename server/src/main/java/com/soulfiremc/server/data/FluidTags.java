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
public class FluidTags {
  public static final List<TagKey<FluidType>> TAGS = new ArrayList<>();

  //@formatter:off
  public static final TagKey<FluidType> WATER = register("minecraft:water");
  public static final TagKey<FluidType> LAVA = register("minecraft:lava");
  //@formatter:on

  private FluidTags() {}

  public static TagKey<FluidType> register(@KeyPattern String key) {
    var resourceKey = TagKey.<FluidType>key(key, RegistryKeys.FLUID);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
