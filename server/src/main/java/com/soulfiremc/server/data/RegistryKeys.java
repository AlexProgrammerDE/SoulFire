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

@SuppressWarnings("unused")
public class RegistryKeys {
  //@formatter:off
  public static final Key ACTIVITY = Key.key("minecraft:activity");
  public static final Key ATTRIBUTE = Key.key("minecraft:attribute");
  public static final Key BANNER_PATTERN = Key.key("minecraft:banner_pattern");
  public static final Key BIOME_SOURCE = Key.key("minecraft:worldgen/biome_source");
  public static final Key BLOCK = Key.key("minecraft:block");
  public static final Key BLOCK_TYPE = Key.key("minecraft:block_type");
  public static final Key BLOCK_ENTITY_TYPE = Key.key("minecraft:block_entity_type");
  public static final Key BLOCK_PREDICATE_TYPE = Key.key("minecraft:block_predicate_type");
  public static final Key BLOCK_STATE_PROVIDER_TYPE = Key.key("minecraft:worldgen/block_state_provider_type");
  public static final Key CARVER = Key.key("minecraft:worldgen/carver");
  public static final Key CAT_VARIANT = Key.key("minecraft:cat_variant");
  public static final Key WOLF_VARIANT = Key.key("minecraft:wolf_variant");
  public static final Key CHUNK_GENERATOR = Key.key("minecraft:worldgen/chunk_generator");
  public static final Key CHUNK_STATUS = Key.key("minecraft:chunk_status");
  public static final Key COMMAND_ARGUMENT_TYPE = Key.key("minecraft:command_argument_type");
  public static final Key CREATIVE_MODE_TAB = Key.key("minecraft:creative_mode_tab");
  public static final Key CUSTOM_STAT = Key.key("minecraft:custom_stat");
  public static final Key DAMAGE_TYPE = Key.key("minecraft:damage_type");
  public static final Key DENSITY_FUNCTION_TYPE = Key.key("minecraft:worldgen/density_function_type");
  public static final Key ENCHANTMENT = Key.key("minecraft:enchantment");
  public static final Key ENTITY_TYPE = Key.key("minecraft:entity_type");
  public static final Key FEATURE = Key.key("minecraft:worldgen/feature");
  public static final Key FEATURE_SIZE_TYPE = Key.key("minecraft:worldgen/feature_size_type");
  public static final Key FLOAT_PROVIDER_TYPE = Key.key("minecraft:float_provider_type");
  public static final Key FLUID = Key.key("minecraft:fluid");
  public static final Key FOLIAGE_PLACER_TYPE = Key.key("minecraft:worldgen/foliage_placer_type");
  public static final Key FROG_VARIANT = Key.key("minecraft:frog_variant");
  public static final Key GAME_EVENT = Key.key("minecraft:game_event");
  public static final Key HEIGHT_PROVIDER_TYPE = Key.key("minecraft:height_provider_type");
  public static final Key INSTRUMENT = Key.key("minecraft:instrument");
  public static final Key INT_PROVIDER_TYPE = Key.key("minecraft:int_provider_type");
  public static final Key ITEM = Key.key("minecraft:item");
  public static final Key LOOT_CONDITION_TYPE = Key.key("minecraft:loot_condition_type");
  public static final Key LOOT_FUNCTION_TYPE = Key.key("minecraft:loot_function_type");
  public static final Key LOOT_NBT_PROVIDER_TYPE = Key.key("minecraft:loot_nbt_provider_type");
  public static final Key LOOT_NUMBER_PROVIDER_TYPE = Key.key("minecraft:loot_number_provider_type");
  public static final Key LOOT_POOL_ENTRY_TYPE = Key.key("minecraft:loot_pool_entry_type");
  public static final Key LOOT_SCORE_PROVIDER_TYPE = Key.key("minecraft:loot_score_provider_type");
  public static final Key MATERIAL_CONDITION = Key.key("minecraft:worldgen/material_condition");
  public static final Key MATERIAL_RULE = Key.key("minecraft:worldgen/material_rule");
  public static final Key MEMORY_MODULE_TYPE = Key.key("minecraft:memory_module_type");
  public static final Key MENU = Key.key("minecraft:menu");
  public static final Key MOB_EFFECT = Key.key("minecraft:mob_effect");
  public static final Key PAINTING_VARIANT = Key.key("minecraft:painting_variant");
  public static final Key PARTICLE_TYPE = Key.key("minecraft:particle_type");
  public static final Key PLACEMENT_MODIFIER_TYPE = Key.key("minecraft:worldgen/placement_modifier_type");
  public static final Key POINT_OF_INTEREST_TYPE = Key.key("minecraft:point_of_interest_type");
  public static final Key POSITION_SOURCE_TYPE = Key.key("minecraft:position_source_type");
  public static final Key POS_RULE_TEST = Key.key("minecraft:pos_rule_test");
  public static final Key POTION = Key.key("minecraft:potion");
  public static final Key RECIPE_SERIALIZER = Key.key("minecraft:recipe_serializer");
  public static final Key RECIPE_TYPE = Key.key("minecraft:recipe_type");
  public static final Key ROOT_PLACER_TYPE = Key.key("minecraft:worldgen/root_placer_type");
  public static final Key RULE_TEST = Key.key("minecraft:rule_test");
  public static final Key RULE_BLOCK_ENTITY_MODIFIER = Key.key("minecraft:rule_block_entity_modifier");
  public static final Key SCHEDULE = Key.key("minecraft:schedule");
  public static final Key SENSOR_TYPE = Key.key("minecraft:sensor_type");
  public static final Key SOUND_EVENT = Key.key("minecraft:sound_event");
  public static final Key STAT_TYPE = Key.key("minecraft:stat_type");
  public static final Key STRUCTURE_PIECE = Key.key("minecraft:worldgen/structure_piece");
  public static final Key STRUCTURE_PLACEMENT = Key.key("minecraft:worldgen/structure_placement");
  public static final Key STRUCTURE_POOL_ELEMENT = Key.key("minecraft:worldgen/structure_pool_element");
  public static final Key POOL_ALIAS_BINDING = Key.key("minecraft:worldgen/pool_alias_binding");
  public static final Key STRUCTURE_PROCESSOR = Key.key("minecraft:worldgen/structure_processor");
  public static final Key STRUCTURE_TYPE = Key.key("minecraft:worldgen/structure_type");
  public static final Key TREE_DECORATOR_TYPE = Key.key("minecraft:worldgen/tree_decorator_type");
  public static final Key TRUNK_PLACER_TYPE = Key.key("minecraft:worldgen/trunk_placer_type");
  public static final Key VILLAGER_PROFESSION = Key.key("minecraft:villager_profession");
  public static final Key VILLAGER_TYPE = Key.key("minecraft:villager_type");
  public static final Key DECORATED_POT_PATTERNS = Key.key("minecraft:decorated_pot_patterns");
  public static final Key NUMBER_FORMAT_TYPE = Key.key("minecraft:number_format_type");
  public static final Key ARMOR_MATERIAL = Key.key("minecraft:armor_material");
  public static final Key DATA_COMPONENT_TYPE = Key.key("minecraft:data_component_type");
  public static final Key ENTITY_SUB_PREDICATE_TYPE = Key.key("minecraft:entity_sub_predicate_type");
  public static final Key ITEM_SUB_PREDICATE_TYPE = Key.key("minecraft:item_sub_predicate_type");
  public static final Key MAP_DECORATION_TYPE = Key.key("minecraft:map_decoration_type");
  public static final Key BIOME = Key.key("minecraft:worldgen/biome");
  public static final Key CHAT_TYPE = Key.key("minecraft:chat_type");
  public static final Key CONFIGURED_CARVER = Key.key("minecraft:worldgen/configured_carver");
  public static final Key CONFIGURED_FEATURE = Key.key("minecraft:worldgen/configured_feature");
  public static final Key DENSITY_FUNCTION = Key.key("minecraft:worldgen/density_function");
  public static final Key DIMENSION_TYPE = Key.key("minecraft:dimension_type");
  public static final Key FLAT_LEVEL_GENERATOR_PRESET = Key.key("minecraft:worldgen/flat_level_generator_preset");
  public static final Key NOISE_SETTINGS = Key.key("minecraft:worldgen/noise_settings");
  public static final Key NOISE = Key.key("minecraft:worldgen/noise");
  public static final Key PLACED_FEATURE = Key.key("minecraft:worldgen/placed_feature");
  public static final Key STRUCTURE = Key.key("minecraft:worldgen/structure");
  public static final Key PROCESSOR_LIST = Key.key("minecraft:worldgen/processor_list");
  public static final Key STRUCTURE_SET = Key.key("minecraft:worldgen/structure_set");
  public static final Key TEMPLATE_POOL = Key.key("minecraft:worldgen/template_pool");
  public static final Key TRIGGER_TYPE = Key.key("minecraft:trigger_type");
  public static final Key TRIM_MATERIAL = Key.key("minecraft:trim_material");
  public static final Key TRIM_PATTERN = Key.key("minecraft:trim_pattern");
  public static final Key WORLD_PRESET = Key.key("minecraft:worldgen/world_preset");
  public static final Key MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST = Key.key("minecraft:worldgen/multi_noise_biome_source_parameter_list");
  public static final Key DIMENSION = Key.key("minecraft:dimension");
  public static final Key LEVEL_STEM = Key.key("minecraft:dimension");
  public static final Key LOOT_TABLE = Key.key("minecraft:loot_table");
  public static final Key ITEM_MODIFIER = Key.key("minecraft:item_modifier");
  public static final Key PREDICATE = Key.key("minecraft:predicate");
  //@formatter:on

  private RegistryKeys() {}
}
