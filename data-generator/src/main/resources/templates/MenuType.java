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

import java.util.Map;

@SuppressWarnings("unused")
public record MenuType(
  int id,
  Key key,
  int slots,
  Map<Integer, Integer> playerInventory,
  Map<Integer, Integer> maxStackSize) implements RegistryValue<MenuType> {
  public static final Registry<MenuType> REGISTRY = new Registry<>(RegistryKeys.MENU);

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static MenuType register(String key) {
    var instance =
      GsonDataHelper.fromJson("minecraft/menus.json", key, MenuType.class);

    return REGISTRY.register(instance);
  }

  public static MenuType createWithoutRegistry(String key) {
    return GsonDataHelper.fromJson("minecraft/menus.json", key, MenuType.class);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MenuType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public Registry<MenuType> registry() {
    return REGISTRY;
  }
}
