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

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.AccessLevel;
import lombok.With;

@SuppressWarnings("unused")
@With(value = AccessLevel.PRIVATE)
public record AttributeType(int id, ResourceKey key, double min, double max, double defaultValue) {
  public static final Object2ReferenceMap<ResourceKey, AttributeType> FROM_KEY =
    new Object2ReferenceOpenHashMap<>();

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static AttributeType register(String key) {
    var instance =
      GsonDataHelper.fromJson("/minecraft/attributes.json", key, AttributeType.class);

    FROM_KEY.put(instance.key(), instance);
    return instance;
  }

  public static AttributeType getByKey(ResourceKey key) {
    return FROM_KEY.get(key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AttributeType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
