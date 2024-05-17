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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;

@SuppressWarnings("unused")
public class EntityTypeTags {
  public static final List<Key> TAGS = new ArrayList<>();

  //@formatter:off
  public static final Key SKELETONS = register(Key.key("minecraft:skeletons"));
  public static final Key ZOMBIES = register(Key.key("minecraft:zombies"));
  public static final Key RAIDERS = register(Key.key("minecraft:raiders"));
  public static final Key UNDEAD = register(Key.key("minecraft:undead"));
  public static final Key BEEHIVE_INHABITORS = register(Key.key("minecraft:beehive_inhabitors"));
  public static final Key ARROWS = register(Key.key("minecraft:arrows"));
  public static final Key IMPACT_PROJECTILES = register(Key.key("minecraft:impact_projectiles"));
  public static final Key POWDER_SNOW_WALKABLE_MOBS = register(Key.key("minecraft:powder_snow_walkable_mobs"));
  public static final Key AXOLOTL_ALWAYS_HOSTILES = register(Key.key("minecraft:axolotl_always_hostiles"));
  public static final Key AXOLOTL_HUNT_TARGETS = register(Key.key("minecraft:axolotl_hunt_targets"));
  public static final Key FREEZE_IMMUNE_ENTITY_TYPES = register(Key.key("minecraft:freeze_immune_entity_types"));
  public static final Key FREEZE_HURTS_EXTRA_TYPES = register(Key.key("minecraft:freeze_hurts_extra_types"));
  public static final Key CAN_BREATHE_UNDER_WATER = register(Key.key("minecraft:can_breathe_under_water"));
  public static final Key FROG_FOOD = register(Key.key("minecraft:frog_food"));
  public static final Key FALL_DAMAGE_IMMUNE = register(Key.key("minecraft:fall_damage_immune"));
  public static final Key DISMOUNTS_UNDERWATER = register(Key.key("minecraft:dismounts_underwater"));
  public static final Key NON_CONTROLLING_RIDER = register(Key.key("minecraft:non_controlling_rider"));
  public static final Key DEFLECTS_PROJECTILES = register(Key.key("minecraft:deflects_projectiles"));
  public static final Key CAN_TURN_IN_BOATS = register(Key.key("minecraft:can_turn_in_boats"));
  public static final Key ILLAGER = register(Key.key("minecraft:illager"));
  public static final Key AQUATIC = register(Key.key("minecraft:aquatic"));
  public static final Key ARTHROPOD = register(Key.key("minecraft:arthropod"));
  public static final Key IGNORES_POISON_AND_REGEN = register(Key.key("minecraft:ignores_poison_and_regen"));
  public static final Key INVERTED_HEALING_AND_HARM = register(Key.key("minecraft:inverted_healing_and_harm"));
  public static final Key WITHER_FRIENDS = register(Key.key("minecraft:wither_friends"));
  public static final Key ILLAGER_FRIENDS = register(Key.key("minecraft:illager_friends"));
  public static final Key NOT_SCARY_FOR_PUFFERFISH = register(Key.key("minecraft:not_scary_for_pufferfish"));
  public static final Key SENSITIVE_TO_IMPALING = register(Key.key("minecraft:sensitive_to_impaling"));
  public static final Key SENSITIVE_TO_BANE_OF_ARTHROPODS = register(Key.key("minecraft:sensitive_to_bane_of_arthropods"));
  public static final Key SENSITIVE_TO_SMITE = register(Key.key("minecraft:sensitive_to_smite"));
  public static final Key NO_ANGER_FROM_WIND_CHARGE = register(Key.key("minecraft:no_anger_from_wind_charge"));
  public static final Key IMMUNE_TO_OOZING = register(Key.key("minecraft:immune_to_oozing"));
  public static final Key IMMUNE_TO_INFESTED = register(Key.key("minecraft:immune_to_infested"));
  public static final Key REDIRECTABLE_PROJECTILE = register(Key.key("minecraft:redirectable_projectile"));
  //@formatter:on

  private EntityTypeTags() {}

  public static Key register(@KeyPattern String key) {
    var resourceKey = Key.key(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
