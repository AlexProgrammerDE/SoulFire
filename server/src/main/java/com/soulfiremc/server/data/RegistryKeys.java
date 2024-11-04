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
  public static final ResourceKey<?> ACTIVITY = ResourceKey.key("minecraft:activity");
  public static final ResourceKey<?> ATTRIBUTE = ResourceKey.key("minecraft:attribute");
  public static final ResourceKey<?> BANNER_PATTERN = ResourceKey.key("minecraft:banner_pattern");
  public static final ResourceKey<?> BIOME_SOURCE = ResourceKey.key("minecraft:worldgen/biome_source");
  public static final ResourceKey<?> BLOCK = ResourceKey.key("minecraft:block");
  public static final ResourceKey<?> BLOCK_TYPE = ResourceKey.key("minecraft:block_type");
  public static final ResourceKey<?> BLOCK_ENTITY_TYPE = ResourceKey.key("minecraft:block_entity_type");
  public static final ResourceKey<?> BLOCK_PREDICATE_TYPE = ResourceKey.key("minecraft:block_predicate_type");
  public static final ResourceKey<?> BLOCK_STATE_PROVIDER_TYPE = ResourceKey.key("minecraft:worldgen/block_state_provider_type");
  public static final ResourceKey<?> CARVER = ResourceKey.key("minecraft:worldgen/carver");
  public static final ResourceKey<?> CAT_VARIANT = ResourceKey.key("minecraft:cat_variant");
  public static final ResourceKey<?> WOLF_VARIANT = ResourceKey.key("minecraft:wolf_variant");
  public static final ResourceKey<?> CHUNK_GENERATOR = ResourceKey.key("minecraft:worldgen/chunk_generator");
  public static final ResourceKey<?> CHUNK_STATUS = ResourceKey.key("minecraft:chunk_status");
  public static final ResourceKey<?> COMMAND_ARGUMENT_TYPE = ResourceKey.key("minecraft:command_argument_type");
  public static final ResourceKey<?> CREATIVE_MODE_TAB = ResourceKey.key("minecraft:creative_mode_tab");
  public static final ResourceKey<?> CUSTOM_STAT = ResourceKey.key("minecraft:custom_stat");
  public static final ResourceKey<?> DAMAGE_TYPE = ResourceKey.key("minecraft:damage_type");
  public static final ResourceKey<?> DENSITY_FUNCTION_TYPE = ResourceKey.key("minecraft:worldgen/density_function_type");
  public static final ResourceKey<?> ENCHANTMENT_ENTITY_EFFECT_TYPE = ResourceKey.key("minecraft:enchantment_entity_effect_type");
  public static final ResourceKey<?> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = ResourceKey.key("minecraft:enchantment_level_based_value_type");
  public static final ResourceKey<?> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = ResourceKey.key("minecraft:enchantment_location_based_effect_type");
  public static final ResourceKey<?> ENCHANTMENT_PROVIDER_TYPE = ResourceKey.key("minecraft:enchantment_provider_type");
  public static final ResourceKey<?> ENCHANTMENT_VALUE_EFFECT_TYPE = ResourceKey.key("minecraft:enchantment_value_effect_type");
  public static final ResourceKey<?> ENTITY_TYPE = ResourceKey.key("minecraft:entity_type");
  public static final ResourceKey<?> FEATURE = ResourceKey.key("minecraft:worldgen/feature");
  public static final ResourceKey<?> FEATURE_SIZE_TYPE = ResourceKey.key("minecraft:worldgen/feature_size_type");
  public static final ResourceKey<?> FLOAT_PROVIDER_TYPE = ResourceKey.key("minecraft:float_provider_type");
  public static final ResourceKey<?> FLUID = ResourceKey.key("minecraft:fluid");
  public static final ResourceKey<?> FOLIAGE_PLACER_TYPE = ResourceKey.key("minecraft:worldgen/foliage_placer_type");
  public static final ResourceKey<?> FROG_VARIANT = ResourceKey.key("minecraft:frog_variant");
  public static final ResourceKey<?> GAME_EVENT = ResourceKey.key("minecraft:game_event");
  public static final ResourceKey<?> HEIGHT_PROVIDER_TYPE = ResourceKey.key("minecraft:height_provider_type");
  public static final ResourceKey<?> INSTRUMENT = ResourceKey.key("minecraft:instrument");
  public static final ResourceKey<?> INT_PROVIDER_TYPE = ResourceKey.key("minecraft:int_provider_type");
  public static final ResourceKey<?> ITEM = ResourceKey.key("minecraft:item");
  public static final ResourceKey<?> JUKEBOX_SONG = ResourceKey.key("minecraft:jukebox_song");
  public static final ResourceKey<?> LOOT_CONDITION_TYPE = ResourceKey.key("minecraft:loot_condition_type");
  public static final ResourceKey<?> LOOT_FUNCTION_TYPE = ResourceKey.key("minecraft:loot_function_type");
  public static final ResourceKey<?> LOOT_NBT_PROVIDER_TYPE = ResourceKey.key("minecraft:loot_nbt_provider_type");
  public static final ResourceKey<?> LOOT_NUMBER_PROVIDER_TYPE = ResourceKey.key("minecraft:loot_number_provider_type");
  public static final ResourceKey<?> LOOT_POOL_ENTRY_TYPE = ResourceKey.key("minecraft:loot_pool_entry_type");
  public static final ResourceKey<?> LOOT_SCORE_PROVIDER_TYPE = ResourceKey.key("minecraft:loot_score_provider_type");
  public static final ResourceKey<?> MATERIAL_CONDITION = ResourceKey.key("minecraft:worldgen/material_condition");
  public static final ResourceKey<?> MATERIAL_RULE = ResourceKey.key("minecraft:worldgen/material_rule");
  public static final ResourceKey<?> MEMORY_MODULE_TYPE = ResourceKey.key("minecraft:memory_module_type");
  public static final ResourceKey<?> MENU = ResourceKey.key("minecraft:menu");
  public static final ResourceKey<?> MOB_EFFECT = ResourceKey.key("minecraft:mob_effect");
  public static final ResourceKey<?> PAINTING_VARIANT = ResourceKey.key("minecraft:painting_variant");
  public static final ResourceKey<?> PARTICLE_TYPE = ResourceKey.key("minecraft:particle_type");
  public static final ResourceKey<?> PLACEMENT_MODIFIER_TYPE = ResourceKey.key("minecraft:worldgen/placement_modifier_type");
  public static final ResourceKey<?> POINT_OF_INTEREST_TYPE = ResourceKey.key("minecraft:point_of_interest_type");
  public static final ResourceKey<?> POSITION_SOURCE_TYPE = ResourceKey.key("minecraft:position_source_type");
  public static final ResourceKey<?> POS_RULE_TEST = ResourceKey.key("minecraft:pos_rule_test");
  public static final ResourceKey<?> POTION = ResourceKey.key("minecraft:potion");
  public static final ResourceKey<?> RECIPE_SERIALIZER = ResourceKey.key("minecraft:recipe_serializer");
  public static final ResourceKey<?> RECIPE_TYPE = ResourceKey.key("minecraft:recipe_type");
  public static final ResourceKey<?> ROOT_PLACER_TYPE = ResourceKey.key("minecraft:worldgen/root_placer_type");
  public static final ResourceKey<?> RULE_TEST = ResourceKey.key("minecraft:rule_test");
  public static final ResourceKey<?> RULE_BLOCK_ENTITY_MODIFIER = ResourceKey.key("minecraft:rule_block_entity_modifier");
  public static final ResourceKey<?> SCHEDULE = ResourceKey.key("minecraft:schedule");
  public static final ResourceKey<?> SENSOR_TYPE = ResourceKey.key("minecraft:sensor_type");
  public static final ResourceKey<?> SOUND_EVENT = ResourceKey.key("minecraft:sound_event");
  public static final ResourceKey<?> STAT_TYPE = ResourceKey.key("minecraft:stat_type");
  public static final ResourceKey<?> STRUCTURE_PIECE = ResourceKey.key("minecraft:worldgen/structure_piece");
  public static final ResourceKey<?> STRUCTURE_PLACEMENT = ResourceKey.key("minecraft:worldgen/structure_placement");
  public static final ResourceKey<?> STRUCTURE_POOL_ELEMENT = ResourceKey.key("minecraft:worldgen/structure_pool_element");
  public static final ResourceKey<?> POOL_ALIAS_BINDING = ResourceKey.key("minecraft:worldgen/pool_alias_binding");
  public static final ResourceKey<?> STRUCTURE_PROCESSOR = ResourceKey.key("minecraft:worldgen/structure_processor");
  public static final ResourceKey<?> STRUCTURE_TYPE = ResourceKey.key("minecraft:worldgen/structure_type");
  public static final ResourceKey<?> TREE_DECORATOR_TYPE = ResourceKey.key("minecraft:worldgen/tree_decorator_type");
  public static final ResourceKey<?> TRUNK_PLACER_TYPE = ResourceKey.key("minecraft:worldgen/trunk_placer_type");
  public static final ResourceKey<?> VILLAGER_PROFESSION = ResourceKey.key("minecraft:villager_profession");
  public static final ResourceKey<?> VILLAGER_TYPE = ResourceKey.key("minecraft:villager_type");
  public static final ResourceKey<?> DECORATED_POT_PATTERN = ResourceKey.key("minecraft:decorated_pot_pattern");
  public static final ResourceKey<?> NUMBER_FORMAT_TYPE = ResourceKey.key("minecraft:number_format_type");
  public static final ResourceKey<?> DATA_COMPONENT_TYPE = ResourceKey.key("minecraft:data_component_type");
  public static final ResourceKey<?> ENTITY_SUB_PREDICATE_TYPE = ResourceKey.key("minecraft:entity_sub_predicate_type");
  public static final ResourceKey<?> ITEM_SUB_PREDICATE_TYPE = ResourceKey.key("minecraft:item_sub_predicate_type");
  public static final ResourceKey<?> MAP_DECORATION_TYPE = ResourceKey.key("minecraft:map_decoration_type");
  public static final ResourceKey<?> ENCHANTMENT_EFFECT_COMPONENT_TYPE = ResourceKey.key("minecraft:enchantment_effect_component_type");
  public static final ResourceKey<?> CONSUME_EFFECT_TYPE = ResourceKey.key("minecraft:consume_effect_type");
  public static final ResourceKey<?> RECIPE_DISPLAY = ResourceKey.key("minecraft:recipe_display");
  public static final ResourceKey<?> SLOT_DISPLAY = ResourceKey.key("minecraft:slot_display");
  public static final ResourceKey<?> RECIPE_BOOK_CATEGORY = ResourceKey.key("minecraft:recipe_book_category");
  public static final ResourceKey<?> BIOME = ResourceKey.key("minecraft:worldgen/biome");
  public static final ResourceKey<?> CHAT_TYPE = ResourceKey.key("minecraft:chat_type");
  public static final ResourceKey<?> CONFIGURED_CARVER = ResourceKey.key("minecraft:worldgen/configured_carver");
  public static final ResourceKey<?> CONFIGURED_FEATURE = ResourceKey.key("minecraft:worldgen/configured_feature");
  public static final ResourceKey<?> DENSITY_FUNCTION = ResourceKey.key("minecraft:worldgen/density_function");
  public static final ResourceKey<?> DIMENSION_TYPE = ResourceKey.key("minecraft:dimension_type");
  public static final ResourceKey<?> ENCHANTMENT = ResourceKey.key("minecraft:enchantment");
  public static final ResourceKey<?> ENCHANTMENT_PROVIDER = ResourceKey.key("minecraft:enchantment_provider");
  public static final ResourceKey<?> FLAT_LEVEL_GENERATOR_PRESET = ResourceKey.key("minecraft:worldgen/flat_level_generator_preset");
  public static final ResourceKey<?> NOISE_SETTINGS = ResourceKey.key("minecraft:worldgen/noise_settings");
  public static final ResourceKey<?> NOISE = ResourceKey.key("minecraft:worldgen/noise");
  public static final ResourceKey<?> PLACED_FEATURE = ResourceKey.key("minecraft:worldgen/placed_feature");
  public static final ResourceKey<?> STRUCTURE = ResourceKey.key("minecraft:worldgen/structure");
  public static final ResourceKey<?> PROCESSOR_LIST = ResourceKey.key("minecraft:worldgen/processor_list");
  public static final ResourceKey<?> STRUCTURE_SET = ResourceKey.key("minecraft:worldgen/structure_set");
  public static final ResourceKey<?> TEMPLATE_POOL = ResourceKey.key("minecraft:worldgen/template_pool");
  public static final ResourceKey<?> TRIGGER_TYPE = ResourceKey.key("minecraft:trigger_type");
  public static final ResourceKey<?> TRIM_MATERIAL = ResourceKey.key("minecraft:trim_material");
  public static final ResourceKey<?> TRIM_PATTERN = ResourceKey.key("minecraft:trim_pattern");
  public static final ResourceKey<?> WORLD_PRESET = ResourceKey.key("minecraft:worldgen/world_preset");
  public static final ResourceKey<?> MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST = ResourceKey.key("minecraft:worldgen/multi_noise_biome_source_parameter_list");
  public static final ResourceKey<?> TRIAL_SPAWNER_CONFIG = ResourceKey.key("minecraft:trial_spawner");
  public static final ResourceKey<?> DIMENSION = ResourceKey.key("minecraft:dimension");
  public static final ResourceKey<?> LEVEL_STEM = ResourceKey.key("minecraft:dimension");
  public static final ResourceKey<?> LOOT_TABLE = ResourceKey.key("minecraft:loot_table");
  public static final ResourceKey<?> ITEM_MODIFIER = ResourceKey.key("minecraft:item_modifier");
  public static final ResourceKey<?> PREDICATE = ResourceKey.key("minecraft:predicate");
  public static final ResourceKey<?> ADVANCEMENT = ResourceKey.key("minecraft:advancement");
  public static final ResourceKey<?> RECIPE = ResourceKey.key("minecraft:recipe");
  //@formatter:on

  private RegistryKeys() {}
}
