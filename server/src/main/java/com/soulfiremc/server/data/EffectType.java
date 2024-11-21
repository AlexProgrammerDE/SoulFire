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
  public static final EffectType MOVEMENT_SPEED = register("minecraft:speed");
  public static final EffectType MOVEMENT_SLOWDOWN = register("minecraft:slowness");
  public static final EffectType DIG_SPEED = register("minecraft:haste");
  public static final EffectType DIG_SLOWDOWN = register("minecraft:mining_fatigue");
  public static final EffectType DAMAGE_BOOST = register("minecraft:strength");
  public static final EffectType HEAL = register("minecraft:instant_health");
  public static final EffectType HARM = register("minecraft:instant_damage");
  public static final EffectType JUMP = register("minecraft:jump_boost");
  public static final EffectType CONFUSION = register("minecraft:nausea");
  public static final EffectType REGENERATION = register("minecraft:regeneration");
  public static final EffectType DAMAGE_RESISTANCE = register("minecraft:resistance");
  public static final EffectType FIRE_RESISTANCE = register("minecraft:fire_resistance");
  public static final EffectType WATER_BREATHING = register("minecraft:water_breathing");
  public static final EffectType INVISIBILITY = register("minecraft:invisibility");
  public static final EffectType BLINDNESS = register("minecraft:blindness");
  public static final EffectType NIGHT_VISION = register("minecraft:night_vision");
  public static final EffectType HUNGER = register("minecraft:hunger");
  public static final EffectType WEAKNESS = register("minecraft:weakness");
  public static final EffectType POISON = register("minecraft:poison");
  public static final EffectType WITHER = register("minecraft:wither");
  public static final EffectType HEALTH_BOOST = register("minecraft:health_boost");
  public static final EffectType ABSORPTION = register("minecraft:absorption");
  public static final EffectType SATURATION = register("minecraft:saturation");
  public static final EffectType GLOWING = register("minecraft:glowing");
  public static final EffectType LEVITATION = register("minecraft:levitation");
  public static final EffectType LUCK = register("minecraft:luck");
  public static final EffectType UNLUCK = register("minecraft:unluck");
  public static final EffectType SLOW_FALLING = register("minecraft:slow_falling");
  public static final EffectType CONDUIT_POWER = register("minecraft:conduit_power");
  public static final EffectType DOLPHINS_GRACE = register("minecraft:dolphins_grace");
  public static final EffectType BAD_OMEN = register("minecraft:bad_omen");
  public static final EffectType HERO_OF_THE_VILLAGE = register("minecraft:hero_of_the_village");
  public static final EffectType DARKNESS = register("minecraft:darkness");
  public static final EffectType TRIAL_OMEN = register("minecraft:trial_omen");
  public static final EffectType RAID_OMEN = register("minecraft:raid_omen");
  public static final EffectType WIND_CHARGED = register("minecraft:wind_charged");
  public static final EffectType WEAVING = register("minecraft:weaving");
  public static final EffectType OOZING = register("minecraft:oozing");
  public static final EffectType INFESTED = register("minecraft:infested");
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
