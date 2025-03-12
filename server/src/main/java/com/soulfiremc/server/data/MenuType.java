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
  Map<Integer, Integer> playerInventory) implements RegistryValue<MenuType> {
  public static final Registry<MenuType> REGISTRY = new Registry<>(RegistryKeys.MENU);

  //@formatter:off
  public static final MenuType GENERIC_9x1 = register("minecraft:generic_9x1");
  public static final MenuType GENERIC_9x2 = register("minecraft:generic_9x2");
  public static final MenuType GENERIC_9x3 = register("minecraft:generic_9x3");
  public static final MenuType GENERIC_9x4 = register("minecraft:generic_9x4");
  public static final MenuType GENERIC_9x5 = register("minecraft:generic_9x5");
  public static final MenuType GENERIC_9x6 = register("minecraft:generic_9x6");
  public static final MenuType GENERIC_3x3 = register("minecraft:generic_3x3");
  public static final MenuType CRAFTER_3x3 = register("minecraft:crafter_3x3");
  public static final MenuType ANVIL = register("minecraft:anvil");
  public static final MenuType BEACON = register("minecraft:beacon");
  public static final MenuType BLAST_FURNACE = register("minecraft:blast_furnace");
  public static final MenuType BREWING_STAND = register("minecraft:brewing_stand");
  public static final MenuType CRAFTING = register("minecraft:crafting");
  public static final MenuType ENCHANTMENT = register("minecraft:enchantment");
  public static final MenuType FURNACE = register("minecraft:furnace");
  public static final MenuType GRINDSTONE = register("minecraft:grindstone");
  public static final MenuType HOPPER = register("minecraft:hopper");
  public static final MenuType LECTERN = register("minecraft:lectern");
  public static final MenuType LOOM = register("minecraft:loom");
  public static final MenuType MERCHANT = register("minecraft:merchant");
  public static final MenuType SHULKER_BOX = register("minecraft:shulker_box");
  public static final MenuType SMITHING = register("minecraft:smithing");
  public static final MenuType SMOKER = register("minecraft:smoker");
  public static final MenuType CARTOGRAPHY_TABLE = register("minecraft:cartography_table");
  public static final MenuType STONECUTTER = register("minecraft:stonecutter");
  public static final MenuType SOULFIRE_INVENTORY_MENU = createWithoutRegistry("soulfire:inventory_menu");
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
