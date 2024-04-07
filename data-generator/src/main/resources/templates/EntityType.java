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

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

@SuppressWarnings("unused")
public record EntityType(
  int id,
  ResourceKey key,
  float width,
  float height,
  String category,
  boolean friendly,
  boolean summonable,
  boolean attackable) {
  public static final Int2ReferenceMap<EntityType> FROM_ID = new Int2ReferenceOpenHashMap<>();

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static EntityType register(String key) {
    var instance =
      GsonDataHelper.fromJson("/minecraft/entities.json", key, EntityType.class);

    FROM_ID.put(instance.id(), instance);
    return instance;
  }

  public static EntityType getById(int id) {
    return FROM_ID.get(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
