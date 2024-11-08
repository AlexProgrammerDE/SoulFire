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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;

public record TagKey<T extends RegistryValue<T>>(ResourceKey<? extends Registry<T>> registry, Key key) {
  public static <T extends RegistryValue<T>> TagKey<T> key(@KeyPattern String key, ResourceKey<?> registry) {
    return key(Key.key(key), registry);
  }

  @SuppressWarnings("unchecked")
  public static <T extends RegistryValue<T>> TagKey<T> key(Key key, ResourceKey<?> registry) {
    return new TagKey<>((ResourceKey<? extends Registry<T>>) registry, key);
  }
}
