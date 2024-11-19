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
public record NamedEntityData(String key, int networkId, String entityClass) {
  public static final List<NamedEntityData> VALUES = new ArrayList<>();

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static NamedEntityData register(String key, int networkId, String entityClass) {
    var instance = new NamedEntityData(key, networkId, entityClass);
    VALUES.add(instance);
    return instance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NamedEntityData other)) {
      return false;
    }
    return key.equals(other.key) && entityClass.equals(other.entityClass);
  }

  @Override
  public int hashCode() {
    return key.hashCode() + entityClass.hashCode();
  }
}
