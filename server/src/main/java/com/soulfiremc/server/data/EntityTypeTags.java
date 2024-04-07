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
public class EntityTypeTags {
  public static final List<ResourceKey> TAGS = new ArrayList<>();

  //@formatter:off
  public static final ResourceKey SKELETONS = register("minecraft:skeletons");
  public static final ResourceKey ZOMBIES = register("minecraft:zombies");
  public static final ResourceKey RAIDERS = register("minecraft:raiders");
  public static final ResourceKey UNDEAD = register("minecraft:undead");
  public static final ResourceKey BEEHIVE_INHABITORS = register("minecraft:beehive_inhabitors");
  public static final ResourceKey ARROWS = register("minecraft:arrows");
  public static final ResourceKey IMPACT_PROJECTILES = register("minecraft:impact_projectiles");
  public static final ResourceKey POWDER_SNOW_WALKABLE_MOBS = register("minecraft:powder_snow_walkable_mobs");
  public static final ResourceKey AXOLOTL_ALWAYS_HOSTILES = register("minecraft:axolotl_always_hostiles");
  public static final ResourceKey AXOLOTL_HUNT_TARGETS = register("minecraft:axolotl_hunt_targets");
  public static final ResourceKey FREEZE_IMMUNE_ENTITY_TYPES = register("minecraft:freeze_immune_entity_types");
  public static final ResourceKey FREEZE_HURTS_EXTRA_TYPES = register("minecraft:freeze_hurts_extra_types");
  public static final ResourceKey CAN_BREATHE_UNDER_WATER = register("minecraft:can_breathe_under_water");
  public static final ResourceKey FROG_FOOD = register("minecraft:frog_food");
  public static final ResourceKey FALL_DAMAGE_IMMUNE = register("minecraft:fall_damage_immune");
  public static final ResourceKey DISMOUNTS_UNDERWATER = register("minecraft:dismounts_underwater");
  public static final ResourceKey NON_CONTROLLING_RIDER = register("minecraft:non_controlling_rider");
  public static final ResourceKey DEFLECTS_ARROWS = register("minecraft:deflects_arrows");
  public static final ResourceKey DEFLECTS_TRIDENTS = register("minecraft:deflects_tridents");
  public static final ResourceKey CAN_TURN_IN_BOATS = register("minecraft:can_turn_in_boats");
  //@formatter:on

  private EntityTypeTags() {}

  public static ResourceKey register(String key) {
    var resourceKey = ResourceKey.fromString(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
