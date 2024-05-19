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
package com.soulfiremc.data;

import java.util.List;
import lombok.AccessLevel;
import lombok.With;
import net.kyori.adventure.key.Key;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record EnchantmentType(
  int id,
  Key key,
  int minLevel,
  int maxLevel,
  List<Key> incompatible,
  Key supportedItems,
  boolean tradeable,
  boolean discoverable,
  boolean curse,
  boolean treasureOnly,
  List<EquipmentSlot> slots) implements RegistryValue<EnchantmentType> {
  public static final Registry<EnchantmentType> REGISTRY = new Registry<>(RegistryKeys.ENCHANTMENT);

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static EnchantmentType register(String key) {
    var instance =
      GsonDataHelper.fromJson("/minecraft/enchantments.json", key, EnchantmentType.class);

    return REGISTRY.register(instance);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EnchantmentType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
