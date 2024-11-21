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
public record AttributeType(int id, Key key, double min, double max, double defaultValue,
                            boolean clientSyncable) implements RegistryValue<AttributeType> {
  public static final Registry<AttributeType> REGISTRY = new Registry<>(RegistryKeys.ATTRIBUTE);

  //@formatter:off
  public static final AttributeType ARMOR = register("minecraft:armor");
  public static final AttributeType ARMOR_TOUGHNESS = register("minecraft:armor_toughness");
  public static final AttributeType ATTACK_DAMAGE = register("minecraft:attack_damage");
  public static final AttributeType ATTACK_KNOCKBACK = register("minecraft:attack_knockback");
  public static final AttributeType ATTACK_SPEED = register("minecraft:attack_speed");
  public static final AttributeType BLOCK_BREAK_SPEED = register("minecraft:block_break_speed");
  public static final AttributeType BLOCK_INTERACTION_RANGE = register("minecraft:block_interaction_range");
  public static final AttributeType BURNING_TIME = register("minecraft:burning_time");
  public static final AttributeType EXPLOSION_KNOCKBACK_RESISTANCE = register("minecraft:explosion_knockback_resistance");
  public static final AttributeType ENTITY_INTERACTION_RANGE = register("minecraft:entity_interaction_range");
  public static final AttributeType FALL_DAMAGE_MULTIPLIER = register("minecraft:fall_damage_multiplier");
  public static final AttributeType FLYING_SPEED = register("minecraft:flying_speed");
  public static final AttributeType FOLLOW_RANGE = register("minecraft:follow_range");
  public static final AttributeType GRAVITY = register("minecraft:gravity");
  public static final AttributeType JUMP_STRENGTH = register("minecraft:jump_strength");
  public static final AttributeType KNOCKBACK_RESISTANCE = register("minecraft:knockback_resistance");
  public static final AttributeType LUCK = register("minecraft:luck");
  public static final AttributeType MAX_ABSORPTION = register("minecraft:max_absorption");
  public static final AttributeType MAX_HEALTH = register("minecraft:max_health");
  public static final AttributeType MINING_EFFICIENCY = register("minecraft:mining_efficiency");
  public static final AttributeType MOVEMENT_EFFICIENCY = register("minecraft:movement_efficiency");
  public static final AttributeType MOVEMENT_SPEED = register("minecraft:movement_speed");
  public static final AttributeType OXYGEN_BONUS = register("minecraft:oxygen_bonus");
  public static final AttributeType SAFE_FALL_DISTANCE = register("minecraft:safe_fall_distance");
  public static final AttributeType SCALE = register("minecraft:scale");
  public static final AttributeType SNEAKING_SPEED = register("minecraft:sneaking_speed");
  public static final AttributeType SPAWN_REINFORCEMENTS_CHANCE = register("minecraft:spawn_reinforcements");
  public static final AttributeType STEP_HEIGHT = register("minecraft:step_height");
  public static final AttributeType SUBMERGED_MINING_SPEED = register("minecraft:submerged_mining_speed");
  public static final AttributeType SWEEPING_DAMAGE_RATIO = register("minecraft:sweeping_damage_ratio");
  public static final AttributeType TEMPT_RANGE = register("minecraft:tempt_range");
  public static final AttributeType WATER_MOVEMENT_EFFICIENCY = register("minecraft:water_movement_efficiency");
  //@formatter:on

  public static AttributeType register(String key) {
    var instance =
      GsonDataHelper.fromJson("minecraft/attributes.json", key, AttributeType.class);

    return REGISTRY.register(instance);
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

  @Override
  public Registry<AttributeType> registry() {
    return REGISTRY;
  }
}
