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

import java.util.List;

@SuppressWarnings("unused")
public record EntityType(
  int id,
  Key key,
  EntityDimensions dimensions,
  int updateInterval,
  int clientTrackingRange,
  String category,
  boolean friendly,
  boolean summonable,
  boolean fireImmune,
  boolean attackable,
  double defaultFollowRange,
  boolean playerEntity,
  boolean livingEntity,
  boolean boatEntity,
  boolean minecartEntity,
  boolean windChargeEntity,
  boolean shulkerEntity,
  List<String> inheritedClasses,
  String defaultEntityMetadata) implements RegistryValue<EntityType> {
  public static final Registry<EntityType> REGISTRY = new Registry<>(RegistryKeys.ENTITY_TYPE);

  //@formatter:off
  // VALUES REPLACE
  //@formatter:on

  public static EntityType register(String key) {
    var instance =
      GsonDataHelper.fromJson("minecraft/entities.json", key, EntityType.class);

    return REGISTRY.register(instance);
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

  @Override
  public Registry<EntityType> registry() {
    return REGISTRY;
  }
}
