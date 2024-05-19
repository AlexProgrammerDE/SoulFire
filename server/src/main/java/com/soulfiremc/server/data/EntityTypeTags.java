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
import net.kyori.adventure.key.KeyPattern;

@SuppressWarnings("unused")
public class EntityTypeTags {
  public static final List<TagKey<EntityType>> TAGS = new ArrayList<>();

  //@formatter:off
  public static final TagKey<EntityType> SKELETONS = register("minecraft:skeletons", TAGS);
  public static final TagKey<EntityType> ZOMBIES = register("minecraft:zombies", TAGS);
  public static final TagKey<EntityType> RAIDERS = register("minecraft:raiders", TAGS);
  public static final TagKey<EntityType> UNDEAD = register("minecraft:undead", TAGS);
  public static final TagKey<EntityType> BEEHIVE_INHABITORS = register("minecraft:beehive_inhabitors", TAGS);
  public static final TagKey<EntityType> ARROWS = register("minecraft:arrows", TAGS);
  public static final TagKey<EntityType> IMPACT_PROJECTILES = register("minecraft:impact_projectiles", TAGS);
  public static final TagKey<EntityType> POWDER_SNOW_WALKABLE_MOBS = register("minecraft:powder_snow_walkable_mobs", TAGS);
  public static final TagKey<EntityType> AXOLOTL_ALWAYS_HOSTILES = register("minecraft:axolotl_always_hostiles", TAGS);
  public static final TagKey<EntityType> AXOLOTL_HUNT_TARGETS = register("minecraft:axolotl_hunt_targets", TAGS);
  public static final TagKey<EntityType> FREEZE_IMMUNE_ENTITY_TYPES = register("minecraft:freeze_immune_entity_types", TAGS);
  public static final TagKey<EntityType> FREEZE_HURTS_EXTRA_TYPES = register("minecraft:freeze_hurts_extra_types", TAGS);
  public static final TagKey<EntityType> CAN_BREATHE_UNDER_WATER = register("minecraft:can_breathe_under_water", TAGS);
  public static final TagKey<EntityType> FROG_FOOD = register("minecraft:frog_food", TAGS);
  public static final TagKey<EntityType> FALL_DAMAGE_IMMUNE = register("minecraft:fall_damage_immune", TAGS);
  public static final TagKey<EntityType> DISMOUNTS_UNDERWATER = register("minecraft:dismounts_underwater", TAGS);
  public static final TagKey<EntityType> NON_CONTROLLING_RIDER = register("minecraft:non_controlling_rider", TAGS);
  public static final TagKey<EntityType> DEFLECTS_PROJECTILES = register("minecraft:deflects_projectiles", TAGS);
  public static final TagKey<EntityType> CAN_TURN_IN_BOATS = register("minecraft:can_turn_in_boats", TAGS);
  public static final TagKey<EntityType> ILLAGER = register("minecraft:illager", TAGS);
  public static final TagKey<EntityType> AQUATIC = register("minecraft:aquatic", TAGS);
  public static final TagKey<EntityType> ARTHROPOD = register("minecraft:arthropod", TAGS);
  public static final TagKey<EntityType> IGNORES_POISON_AND_REGEN = register("minecraft:ignores_poison_and_regen", TAGS);
  public static final TagKey<EntityType> INVERTED_HEALING_AND_HARM = register("minecraft:inverted_healing_and_harm", TAGS);
  public static final TagKey<EntityType> WITHER_FRIENDS = register("minecraft:wither_friends", TAGS);
  public static final TagKey<EntityType> ILLAGER_FRIENDS = register("minecraft:illager_friends", TAGS);
  public static final TagKey<EntityType> NOT_SCARY_FOR_PUFFERFISH = register("minecraft:not_scary_for_pufferfish", TAGS);
  public static final TagKey<EntityType> SENSITIVE_TO_IMPALING = register("minecraft:sensitive_to_impaling", TAGS);
  public static final TagKey<EntityType> SENSITIVE_TO_BANE_OF_ARTHROPODS = register("minecraft:sensitive_to_bane_of_arthropods", TAGS);
  public static final TagKey<EntityType> SENSITIVE_TO_SMITE = register("minecraft:sensitive_to_smite", TAGS);
  public static final TagKey<EntityType> NO_ANGER_FROM_WIND_CHARGE = register("minecraft:no_anger_from_wind_charge", TAGS);
  public static final TagKey<EntityType> IMMUNE_TO_OOZING = register("minecraft:immune_to_oozing", TAGS);
  public static final TagKey<EntityType> IMMUNE_TO_INFESTED = register("minecraft:immune_to_infested", TAGS);
  public static final TagKey<EntityType> REDIRECTABLE_PROJECTILE = register("minecraft:redirectable_projectile", TAGS);
  //@formatter:on

  private EntityTypeTags() {}

  public static <T extends RegistryValue<T>> TagKey<T> register(@KeyPattern String key, List<TagKey<T>> values) {
    var resourceKey = TagKey.<T>key(key, RegistryKeys.ENTITY_TYPE);
    values.add(resourceKey);
    return resourceKey;
  }
}
