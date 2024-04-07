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

@SuppressWarnings("unused")
public class RegistryKeys {
  //@formatter:off
  public static final ResourceKey GAME_EVENT = ResourceKey.fromString("minecraft:game_event");
  public static final ResourceKey SOUND_EVENT = ResourceKey.fromString("minecraft:sound_event");
  public static final ResourceKey FLUID = ResourceKey.fromString("minecraft:fluid");
  public static final ResourceKey MOB_EFFECT = ResourceKey.fromString("minecraft:mob_effect");
  public static final ResourceKey BLOCK = ResourceKey.fromString("minecraft:block");
  public static final ResourceKey ENCHANTMENT = ResourceKey.fromString("minecraft:enchantment");
  public static final ResourceKey ENTITY_TYPE = ResourceKey.fromString("minecraft:entity_type");
  public static final ResourceKey ITEM = ResourceKey.fromString("minecraft:item");
  public static final ResourceKey POTION = ResourceKey.fromString("minecraft:potion");
  public static final ResourceKey PARTICLE_TYPE = ResourceKey.fromString("minecraft:particle_type");
  public static final ResourceKey BLOCK_ENTITY_TYPE = ResourceKey.fromString("minecraft:block_entity_type");
  public static final ResourceKey PAINTING_VARIANT = ResourceKey.fromString("minecraft:painting_variant");
  public static final ResourceKey CUSTOM_STAT = ResourceKey.fromString("minecraft:custom_stat");
  public static final ResourceKey CHUNK_STATUS = ResourceKey.fromString("minecraft:chunk_status");
  public static final ResourceKey RULE_TEST = ResourceKey.fromString("minecraft:rule_test");
  public static final ResourceKey RULE_BLOCK_ENTITY_MODIFIER = ResourceKey.fromString("minecraft:rule_block_entity_modifier");
  public static final ResourceKey POS_RULE_TEST = ResourceKey.fromString("minecraft:pos_rule_test");
  public static final ResourceKey MENU = ResourceKey.fromString("minecraft:menu");
  public static final ResourceKey RECIPE_TYPE = ResourceKey.fromString("minecraft:recipe_type");
  public static final ResourceKey RECIPE_SERIALIZER = ResourceKey.fromString("minecraft:recipe_serializer");
  public static final ResourceKey ATTRIBUTE = ResourceKey.fromString("minecraft:attribute");
  public static final ResourceKey POSITION_SOURCE_TYPE = ResourceKey.fromString("minecraft:position_source_type");
  public static final ResourceKey COMMAND_ARGUMENT_TYPE = ResourceKey.fromString("minecraft:command_argument_type");
  public static final ResourceKey STAT_TYPE = ResourceKey.fromString("minecraft:stat_type");
  public static final ResourceKey VILLAGER_TYPE = ResourceKey.fromString("minecraft:villager_type");
  public static final ResourceKey VILLAGER_PROFESSION = ResourceKey.fromString("minecraft:villager_profession");
  public static final ResourceKey POINT_OF_INTEREST_TYPE = ResourceKey.fromString("minecraft:point_of_interest_type");
  public static final ResourceKey MEMORY_MODULE_TYPE = ResourceKey.fromString("minecraft:memory_module_type");
  public static final ResourceKey SENSOR_TYPE = ResourceKey.fromString("minecraft:sensor_type");
  public static final ResourceKey SCHEDULE = ResourceKey.fromString("minecraft:schedule");
  public static final ResourceKey ACTIVITY = ResourceKey.fromString("minecraft:activity");
  public static final ResourceKey LOOT_POOL_ENTRY_TYPE = ResourceKey.fromString("minecraft:loot_pool_entry_type");
  public static final ResourceKey LOOT_FUNCTION_TYPE = ResourceKey.fromString("minecraft:loot_function_type");
  public static final ResourceKey LOOT_CONDITION_TYPE = ResourceKey.fromString("minecraft:loot_condition_type");
  public static final ResourceKey LOOT_NUMBER_PROVIDER_TYPE = ResourceKey.fromString("minecraft:loot_number_provider_type");
  public static final ResourceKey LOOT_NBT_PROVIDER_TYPE = ResourceKey.fromString("minecraft:loot_nbt_provider_type");
  public static final ResourceKey LOOT_SCORE_PROVIDER_TYPE = ResourceKey.fromString("minecraft:loot_score_provider_type");
  public static final ResourceKey FLOAT_PROVIDER_TYPE = ResourceKey.fromString("minecraft:float_provider_type");
  public static final ResourceKey INT_PROVIDER_TYPE = ResourceKey.fromString("minecraft:int_provider_type");
  public static final ResourceKey HEIGHT_PROVIDER_TYPE = ResourceKey.fromString("minecraft:height_provider_type");
  public static final ResourceKey BLOCK_PREDICATE_TYPE = ResourceKey.fromString("minecraft:block_predicate_type");
  public static final ResourceKey WORLDGEN_WITH_CARVER = ResourceKey.fromString("minecraft:worldgen/carver");
  public static final ResourceKey WORLDGEN_WITH_FEATURE = ResourceKey.fromString("minecraft:worldgen/feature");
  public static final ResourceKey WORLDGEN_WITH_STRUCTURE_PLACEMENT = ResourceKey.fromString("minecraft:worldgen/structure_placement");
  public static final ResourceKey WORLDGEN_WITH_STRUCTURE_PIECE = ResourceKey.fromString("minecraft:worldgen/structure_piece");
  public static final ResourceKey WORLDGEN_WITH_STRUCTURE_TYPE = ResourceKey.fromString("minecraft:worldgen/structure_type");
  public static final ResourceKey WORLDGEN_WITH_PLACEMENT_MODIFIER_TYPE = ResourceKey.fromString("minecraft:worldgen/placement_modifier_type");
  public static final ResourceKey WORLDGEN_WITH_BLOCK_STATE_PROVIDER_TYPE = ResourceKey.fromString("minecraft:worldgen/block_state_provider_type");
  public static final ResourceKey WORLDGEN_WITH_FOLIAGE_PLACER_TYPE = ResourceKey.fromString("minecraft:worldgen/foliage_placer_type");
  public static final ResourceKey WORLDGEN_WITH_TRUNK_PLACER_TYPE = ResourceKey.fromString("minecraft:worldgen/trunk_placer_type");
  public static final ResourceKey WORLDGEN_WITH_ROOT_PLACER_TYPE = ResourceKey.fromString("minecraft:worldgen/root_placer_type");
  public static final ResourceKey WORLDGEN_WITH_TREE_DECORATOR_TYPE = ResourceKey.fromString("minecraft:worldgen/tree_decorator_type");
  public static final ResourceKey WORLDGEN_WITH_FEATURE_SIZE_TYPE = ResourceKey.fromString("minecraft:worldgen/feature_size_type");
  public static final ResourceKey WORLDGEN_WITH_BIOME_SOURCE = ResourceKey.fromString("minecraft:worldgen/biome_source");
  public static final ResourceKey WORLDGEN_WITH_CHUNK_GENERATOR = ResourceKey.fromString("minecraft:worldgen/chunk_generator");
  public static final ResourceKey WORLDGEN_WITH_MATERIAL_CONDITION = ResourceKey.fromString("minecraft:worldgen/material_condition");
  public static final ResourceKey WORLDGEN_WITH_MATERIAL_RULE = ResourceKey.fromString("minecraft:worldgen/material_rule");
  public static final ResourceKey WORLDGEN_WITH_DENSITY_FUNCTION_TYPE = ResourceKey.fromString("minecraft:worldgen/density_function_type");
  public static final ResourceKey BLOCK_TYPE = ResourceKey.fromString("minecraft:block_type");
  public static final ResourceKey WORLDGEN_WITH_STRUCTURE_PROCESSOR = ResourceKey.fromString("minecraft:worldgen/structure_processor");
  public static final ResourceKey WORLDGEN_WITH_STRUCTURE_POOL_ELEMENT = ResourceKey.fromString("minecraft:worldgen/structure_pool_element");
  public static final ResourceKey WORLDGEN_WITH_POOL_ALIAS_BINDING = ResourceKey.fromString("minecraft:worldgen/pool_alias_binding");
  public static final ResourceKey CAT_VARIANT = ResourceKey.fromString("minecraft:cat_variant");
  public static final ResourceKey FROG_VARIANT = ResourceKey.fromString("minecraft:frog_variant");
  public static final ResourceKey BANNER_PATTERN = ResourceKey.fromString("minecraft:banner_pattern");
  public static final ResourceKey INSTRUMENT = ResourceKey.fromString("minecraft:instrument");
  public static final ResourceKey DECORATED_POT_PATTERNS = ResourceKey.fromString("minecraft:decorated_pot_patterns");
  public static final ResourceKey CREATIVE_MODE_TAB = ResourceKey.fromString("minecraft:creative_mode_tab");
  public static final ResourceKey TRIGGER_TYPE = ResourceKey.fromString("minecraft:trigger_type");
  public static final ResourceKey NUMBER_FORMAT_TYPE = ResourceKey.fromString("minecraft:number_format_type");
  //@formatter:on

  private RegistryKeys() {}
}
