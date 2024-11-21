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

import lombok.AccessLevel;
import lombok.With;
import net.kyori.adventure.key.Key;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record EffectType(int id, Key key, EffectCategory category, boolean beneficial, boolean instantenous) implements RegistryValue<EffectType> {
  public static final Registry<EffectType> REGISTRY = new Registry<>(RegistryKeys.MOB_EFFECT);

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static EffectType register(String key) {
    var instance =
      GsonDataHelper.fromJson("minecraft/effects.json", key, EffectType.class);

    return REGISTRY.register(instance);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EffectType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public Registry<EffectType> registry() {
    return REGISTRY;
  }

  public enum EffectCategory {
    BENEFICIAL,
    HARMFUL,
    NEUTRAL
  }
}
