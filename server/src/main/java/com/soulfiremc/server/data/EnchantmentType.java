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
  public static final EnchantmentType PROTECTION = register("minecraft:protection");
  public static final EnchantmentType FIRE_PROTECTION = register("minecraft:fire_protection");
  public static final EnchantmentType FEATHER_FALLING = register("minecraft:feather_falling");
  public static final EnchantmentType BLAST_PROTECTION = register("minecraft:blast_protection");
  public static final EnchantmentType PROJECTILE_PROTECTION = register("minecraft:projectile_protection");
  public static final EnchantmentType RESPIRATION = register("minecraft:respiration");
  public static final EnchantmentType AQUA_AFFINITY = register("minecraft:aqua_affinity");
  public static final EnchantmentType THORNS = register("minecraft:thorns");
  public static final EnchantmentType DEPTH_STRIDER = register("minecraft:depth_strider");
  public static final EnchantmentType FROST_WALKER = register("minecraft:frost_walker");
  public static final EnchantmentType BINDING_CURSE = register("minecraft:binding_curse");
  public static final EnchantmentType SOUL_SPEED = register("minecraft:soul_speed");
  public static final EnchantmentType SWIFT_SNEAK = register("minecraft:swift_sneak");
  public static final EnchantmentType SHARPNESS = register("minecraft:sharpness");
  public static final EnchantmentType SMITE = register("minecraft:smite");
  public static final EnchantmentType BANE_OF_ARTHROPODS = register("minecraft:bane_of_arthropods");
  public static final EnchantmentType KNOCKBACK = register("minecraft:knockback");
  public static final EnchantmentType FIRE_ASPECT = register("minecraft:fire_aspect");
  public static final EnchantmentType LOOTING = register("minecraft:looting");
  public static final EnchantmentType SWEEPING_EDGE = register("minecraft:sweeping_edge");
  public static final EnchantmentType EFFICIENCY = register("minecraft:efficiency");
  public static final EnchantmentType SILK_TOUCH = register("minecraft:silk_touch");
  public static final EnchantmentType UNBREAKING = register("minecraft:unbreaking");
  public static final EnchantmentType FORTUNE = register("minecraft:fortune");
  public static final EnchantmentType POWER = register("minecraft:power");
  public static final EnchantmentType PUNCH = register("minecraft:punch");
  public static final EnchantmentType FLAME = register("minecraft:flame");
  public static final EnchantmentType INFINITY = register("minecraft:infinity");
  public static final EnchantmentType LUCK_OF_THE_SEA = register("minecraft:luck_of_the_sea");
  public static final EnchantmentType LURE = register("minecraft:lure");
  public static final EnchantmentType LOYALTY = register("minecraft:loyalty");
  public static final EnchantmentType IMPALING = register("minecraft:impaling");
  public static final EnchantmentType RIPTIDE = register("minecraft:riptide");
  public static final EnchantmentType CHANNELING = register("minecraft:channeling");
  public static final EnchantmentType MULTISHOT = register("minecraft:multishot");
  public static final EnchantmentType QUICK_CHARGE = register("minecraft:quick_charge");
  public static final EnchantmentType PIERCING = register("minecraft:piercing");
  public static final EnchantmentType DENSITY = register("minecraft:density");
  public static final EnchantmentType BREACH = register("minecraft:breach");
  public static final EnchantmentType WIND_BURST = register("minecraft:wind_burst");
  public static final EnchantmentType MENDING = register("minecraft:mending");
  public static final EnchantmentType VANISHING_CURSE = register("minecraft:vanishing_curse");
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
