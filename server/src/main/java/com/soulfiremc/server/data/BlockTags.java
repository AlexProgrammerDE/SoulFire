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
public class BlockTags {
  public static final List<Key> TAGS = new ArrayList<>();

  //@formatter:off
  public static final Key WOOL = register(Key.key("minecraft:wool"));
  public static final Key PLANKS = register(Key.key("minecraft:planks"));
  public static final Key STONE_BRICKS = register(Key.key("minecraft:stone_bricks"));
  public static final Key WOODEN_BUTTONS = register(Key.key("minecraft:wooden_buttons"));
  public static final Key STONE_BUTTONS = register(Key.key("minecraft:stone_buttons"));
  public static final Key BUTTONS = register(Key.key("minecraft:buttons"));
  public static final Key WOOL_CARPETS = register(Key.key("minecraft:wool_carpets"));
  public static final Key WOODEN_DOORS = register(Key.key("minecraft:wooden_doors"));
  public static final Key WOODEN_STAIRS = register(Key.key("minecraft:wooden_stairs"));
  public static final Key WOODEN_SLABS = register(Key.key("minecraft:wooden_slabs"));
  public static final Key WOODEN_FENCES = register(Key.key("minecraft:wooden_fences"));
  public static final Key PRESSURE_PLATES = register(Key.key("minecraft:pressure_plates"));
  public static final Key WOODEN_PRESSURE_PLATES = register(Key.key("minecraft:wooden_pressure_plates"));
  public static final Key STONE_PRESSURE_PLATES = register(Key.key("minecraft:stone_pressure_plates"));
  public static final Key WOODEN_TRAPDOORS = register(Key.key("minecraft:wooden_trapdoors"));
  public static final Key DOORS = register(Key.key("minecraft:doors"));
  public static final Key SAPLINGS = register(Key.key("minecraft:saplings"));
  public static final Key LOGS_THAT_BURN = register(Key.key("minecraft:logs_that_burn"));
  public static final Key OVERWORLD_NATURAL_LOGS = register(Key.key("minecraft:overworld_natural_logs"));
  public static final Key LOGS = register(Key.key("minecraft:logs"));
  public static final Key DARK_OAK_LOGS = register(Key.key("minecraft:dark_oak_logs"));
  public static final Key OAK_LOGS = register(Key.key("minecraft:oak_logs"));
  public static final Key BIRCH_LOGS = register(Key.key("minecraft:birch_logs"));
  public static final Key ACACIA_LOGS = register(Key.key("minecraft:acacia_logs"));
  public static final Key CHERRY_LOGS = register(Key.key("minecraft:cherry_logs"));
  public static final Key JUNGLE_LOGS = register(Key.key("minecraft:jungle_logs"));
  public static final Key SPRUCE_LOGS = register(Key.key("minecraft:spruce_logs"));
  public static final Key MANGROVE_LOGS = register(Key.key("minecraft:mangrove_logs"));
  public static final Key CRIMSON_STEMS = register(Key.key("minecraft:crimson_stems"));
  public static final Key WARPED_STEMS = register(Key.key("minecraft:warped_stems"));
  public static final Key BAMBOO_BLOCKS = register(Key.key("minecraft:bamboo_blocks"));
  public static final Key WART_BLOCKS = register(Key.key("minecraft:wart_blocks"));
  public static final Key BANNERS = register(Key.key("minecraft:banners"));
  public static final Key SAND = register(Key.key("minecraft:sand"));
  public static final Key SMELTS_TO_GLASS = register(Key.key("minecraft:smelts_to_glass"));
  public static final Key STAIRS = register(Key.key("minecraft:stairs"));
  public static final Key SLABS = register(Key.key("minecraft:slabs"));
  public static final Key WALLS = register(Key.key("minecraft:walls"));
  public static final Key ANVIL = register(Key.key("minecraft:anvil"));
  public static final Key RAILS = register(Key.key("minecraft:rails"));
  public static final Key LEAVES = register(Key.key("minecraft:leaves"));
  public static final Key TRAPDOORS = register(Key.key("minecraft:trapdoors"));
  public static final Key SMALL_FLOWERS = register(Key.key("minecraft:small_flowers"));
  public static final Key BEDS = register(Key.key("minecraft:beds"));
  public static final Key FENCES = register(Key.key("minecraft:fences"));
  public static final Key TALL_FLOWERS = register(Key.key("minecraft:tall_flowers"));
  public static final Key FLOWERS = register(Key.key("minecraft:flowers"));
  public static final Key PIGLIN_REPELLENTS = register(Key.key("minecraft:piglin_repellents"));
  public static final Key GOLD_ORES = register(Key.key("minecraft:gold_ores"));
  public static final Key IRON_ORES = register(Key.key("minecraft:iron_ores"));
  public static final Key DIAMOND_ORES = register(Key.key("minecraft:diamond_ores"));
  public static final Key REDSTONE_ORES = register(Key.key("minecraft:redstone_ores"));
  public static final Key LAPIS_ORES = register(Key.key("minecraft:lapis_ores"));
  public static final Key COAL_ORES = register(Key.key("minecraft:coal_ores"));
  public static final Key EMERALD_ORES = register(Key.key("minecraft:emerald_ores"));
  public static final Key COPPER_ORES = register(Key.key("minecraft:copper_ores"));
  public static final Key CANDLES = register(Key.key("minecraft:candles"));
  public static final Key DIRT = register(Key.key("minecraft:dirt"));
  public static final Key TERRACOTTA = register(Key.key("minecraft:terracotta"));
  public static final Key BADLANDS_TERRACOTTA = register(Key.key("minecraft:badlands_terracotta"));
  public static final Key CONCRETE_POWDER = register(Key.key("minecraft:concrete_powder"));
  public static final Key COMPLETES_FIND_TREE_TUTORIAL = register(Key.key("minecraft:completes_find_tree_tutorial"));
  public static final Key FLOWER_POTS = register(Key.key("minecraft:flower_pots"));
  public static final Key ENDERMAN_HOLDABLE = register(Key.key("minecraft:enderman_holdable"));
  public static final Key ICE = register(Key.key("minecraft:ice"));
  public static final Key VALID_SPAWN = register(Key.key("minecraft:valid_spawn"));
  public static final Key IMPERMEABLE = register(Key.key("minecraft:impermeable"));
  public static final Key UNDERWATER_BONEMEALS = register(Key.key("minecraft:underwater_bonemeals"));
  public static final Key CORAL_BLOCKS = register(Key.key("minecraft:coral_blocks"));
  public static final Key WALL_CORALS = register(Key.key("minecraft:wall_corals"));
  public static final Key CORAL_PLANTS = register(Key.key("minecraft:coral_plants"));
  public static final Key CORALS = register(Key.key("minecraft:corals"));
  public static final Key BAMBOO_PLANTABLE_ON = register(Key.key("minecraft:bamboo_plantable_on"));
  public static final Key STANDING_SIGNS = register(Key.key("minecraft:standing_signs"));
  public static final Key WALL_SIGNS = register(Key.key("minecraft:wall_signs"));
  public static final Key SIGNS = register(Key.key("minecraft:signs"));
  public static final Key CEILING_HANGING_SIGNS = register(Key.key("minecraft:ceiling_hanging_signs"));
  public static final Key WALL_HANGING_SIGNS = register(Key.key("minecraft:wall_hanging_signs"));
  public static final Key ALL_HANGING_SIGNS = register(Key.key("minecraft:all_hanging_signs"));
  public static final Key ALL_SIGNS = register(Key.key("minecraft:all_signs"));
  public static final Key DRAGON_IMMUNE = register(Key.key("minecraft:dragon_immune"));
  public static final Key DRAGON_TRANSPARENT = register(Key.key("minecraft:dragon_transparent"));
  public static final Key WITHER_IMMUNE = register(Key.key("minecraft:wither_immune"));
  public static final Key WITHER_SUMMON_BASE_BLOCKS = register(Key.key("minecraft:wither_summon_base_blocks"));
  public static final Key BEEHIVES = register(Key.key("minecraft:beehives"));
  public static final Key CROPS = register(Key.key("minecraft:crops"));
  public static final Key BEE_GROWABLES = register(Key.key("minecraft:bee_growables"));
  public static final Key PORTALS = register(Key.key("minecraft:portals"));
  public static final Key FIRE = register(Key.key("minecraft:fire"));
  public static final Key NYLIUM = register(Key.key("minecraft:nylium"));
  public static final Key BEACON_BASE_BLOCKS = register(Key.key("minecraft:beacon_base_blocks"));
  public static final Key SOUL_SPEED_BLOCKS = register(Key.key("minecraft:soul_speed_blocks"));
  public static final Key WALL_POST_OVERRIDE = register(Key.key("minecraft:wall_post_override"));
  public static final Key CLIMBABLE = register(Key.key("minecraft:climbable"));
  public static final Key FALL_DAMAGE_RESETTING = register(Key.key("minecraft:fall_damage_resetting"));
  public static final Key SHULKER_BOXES = register(Key.key("minecraft:shulker_boxes"));
  public static final Key HOGLIN_REPELLENTS = register(Key.key("minecraft:hoglin_repellents"));
  public static final Key SOUL_FIRE_BASE_BLOCKS = register(Key.key("minecraft:soul_fire_base_blocks"));
  public static final Key STRIDER_WARM_BLOCKS = register(Key.key("minecraft:strider_warm_blocks"));
  public static final Key CAMPFIRES = register(Key.key("minecraft:campfires"));
  public static final Key GUARDED_BY_PIGLINS = register(Key.key("minecraft:guarded_by_piglins"));
  public static final Key PREVENT_MOB_SPAWNING_INSIDE = register(Key.key("minecraft:prevent_mob_spawning_inside"));
  public static final Key FENCE_GATES = register(Key.key("minecraft:fence_gates"));
  public static final Key UNSTABLE_BOTTOM_CENTER = register(Key.key("minecraft:unstable_bottom_center"));
  public static final Key MUSHROOM_GROW_BLOCK = register(Key.key("minecraft:mushroom_grow_block"));
  public static final Key INFINIBURN_OVERWORLD = register(Key.key("minecraft:infiniburn_overworld"));
  public static final Key INFINIBURN_NETHER = register(Key.key("minecraft:infiniburn_nether"));
  public static final Key INFINIBURN_END = register(Key.key("minecraft:infiniburn_end"));
  public static final Key BASE_STONE_OVERWORLD = register(Key.key("minecraft:base_stone_overworld"));
  public static final Key STONE_ORE_REPLACEABLES = register(Key.key("minecraft:stone_ore_replaceables"));
  public static final Key DEEPSLATE_ORE_REPLACEABLES = register(Key.key("minecraft:deepslate_ore_replaceables"));
  public static final Key BASE_STONE_NETHER = register(Key.key("minecraft:base_stone_nether"));
  public static final Key OVERWORLD_CARVER_REPLACEABLES = register(Key.key("minecraft:overworld_carver_replaceables"));
  public static final Key NETHER_CARVER_REPLACEABLES = register(Key.key("minecraft:nether_carver_replaceables"));
  public static final Key CANDLE_CAKES = register(Key.key("minecraft:candle_cakes"));
  public static final Key CAULDRONS = register(Key.key("minecraft:cauldrons"));
  public static final Key CRYSTAL_SOUND_BLOCKS = register(Key.key("minecraft:crystal_sound_blocks"));
  public static final Key INSIDE_STEP_SOUND_BLOCKS = register(Key.key("minecraft:inside_step_sound_blocks"));
  public static final Key COMBINATION_STEP_SOUND_BLOCKS = register(Key.key("minecraft:combination_step_sound_blocks"));
  public static final Key CAMEL_SAND_STEP_SOUND_BLOCKS = register(Key.key("minecraft:camel_sand_step_sound_blocks"));
  public static final Key OCCLUDES_VIBRATION_SIGNALS = register(Key.key("minecraft:occludes_vibration_signals"));
  public static final Key DAMPENS_VIBRATIONS = register(Key.key("minecraft:dampens_vibrations"));
  public static final Key DRIPSTONE_REPLACEABLE_BLOCKS = register(Key.key("minecraft:dripstone_replaceable_blocks"));
  public static final Key CAVE_VINES = register(Key.key("minecraft:cave_vines"));
  public static final Key MOSS_REPLACEABLE = register(Key.key("minecraft:moss_replaceable"));
  public static final Key LUSH_GROUND_REPLACEABLE = register(Key.key("minecraft:lush_ground_replaceable"));
  public static final Key AZALEA_ROOT_REPLACEABLE = register(Key.key("minecraft:azalea_root_replaceable"));
  public static final Key SMALL_DRIPLEAF_PLACEABLE = register(Key.key("minecraft:small_dripleaf_placeable"));
  public static final Key BIG_DRIPLEAF_PLACEABLE = register(Key.key("minecraft:big_dripleaf_placeable"));
  public static final Key SNOW = register(Key.key("minecraft:snow"));
  public static final Key MINEABLE_WITH_AXE = register(Key.key("minecraft:mineable/axe"));
  public static final Key MINEABLE_WITH_HOE = register(Key.key("minecraft:mineable/hoe"));
  public static final Key MINEABLE_WITH_PICKAXE = register(Key.key("minecraft:mineable/pickaxe"));
  public static final Key MINEABLE_WITH_SHOVEL = register(Key.key("minecraft:mineable/shovel"));
  public static final Key SWORD_EFFICIENT = register(Key.key("minecraft:sword_efficient"));
  public static final Key NEEDS_DIAMOND_TOOL = register(Key.key("minecraft:needs_diamond_tool"));
  public static final Key NEEDS_IRON_TOOL = register(Key.key("minecraft:needs_iron_tool"));
  public static final Key NEEDS_STONE_TOOL = register(Key.key("minecraft:needs_stone_tool"));
  public static final Key INCORRECT_FOR_NETHERITE_TOOL = register(Key.key("minecraft:incorrect_for_netherite_tool"));
  public static final Key INCORRECT_FOR_DIAMOND_TOOL = register(Key.key("minecraft:incorrect_for_diamond_tool"));
  public static final Key INCORRECT_FOR_IRON_TOOL = register(Key.key("minecraft:incorrect_for_iron_tool"));
  public static final Key INCORRECT_FOR_STONE_TOOL = register(Key.key("minecraft:incorrect_for_stone_tool"));
  public static final Key INCORRECT_FOR_GOLD_TOOL = register(Key.key("minecraft:incorrect_for_gold_tool"));
  public static final Key INCORRECT_FOR_WOODEN_TOOL = register(Key.key("minecraft:incorrect_for_wooden_tool"));
  public static final Key FEATURES_CANNOT_REPLACE = register(Key.key("minecraft:features_cannot_replace"));
  public static final Key LAVA_POOL_STONE_CANNOT_REPLACE = register(Key.key("minecraft:lava_pool_stone_cannot_replace"));
  public static final Key GEODE_INVALID_BLOCKS = register(Key.key("minecraft:geode_invalid_blocks"));
  public static final Key FROG_PREFER_JUMP_TO = register(Key.key("minecraft:frog_prefer_jump_to"));
  public static final Key SCULK_REPLACEABLE = register(Key.key("minecraft:sculk_replaceable"));
  public static final Key SCULK_REPLACEABLE_WORLD_GEN = register(Key.key("minecraft:sculk_replaceable_world_gen"));
  public static final Key ANCIENT_CITY_REPLACEABLE = register(Key.key("minecraft:ancient_city_replaceable"));
  public static final Key VIBRATION_RESONATORS = register(Key.key("minecraft:vibration_resonators"));
  public static final Key ANIMALS_SPAWNABLE_ON = register(Key.key("minecraft:animals_spawnable_on"));
  public static final Key ARMADILLO_SPAWNABLE_ON = register(Key.key("minecraft:armadillo_spawnable_on"));
  public static final Key AXOLOTLS_SPAWNABLE_ON = register(Key.key("minecraft:axolotls_spawnable_on"));
  public static final Key GOATS_SPAWNABLE_ON = register(Key.key("minecraft:goats_spawnable_on"));
  public static final Key MOOSHROOMS_SPAWNABLE_ON = register(Key.key("minecraft:mooshrooms_spawnable_on"));
  public static final Key PARROTS_SPAWNABLE_ON = register(Key.key("minecraft:parrots_spawnable_on"));
  public static final Key POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = register(Key.key("minecraft:polar_bears_spawnable_on_alternate"));
  public static final Key RABBITS_SPAWNABLE_ON = register(Key.key("minecraft:rabbits_spawnable_on"));
  public static final Key FOXES_SPAWNABLE_ON = register(Key.key("minecraft:foxes_spawnable_on"));
  public static final Key WOLVES_SPAWNABLE_ON = register(Key.key("minecraft:wolves_spawnable_on"));
  public static final Key FROGS_SPAWNABLE_ON = register(Key.key("minecraft:frogs_spawnable_on"));
  public static final Key AZALEA_GROWS_ON = register(Key.key("minecraft:azalea_grows_on"));
  public static final Key CONVERTABLE_TO_MUD = register(Key.key("minecraft:convertable_to_mud"));
  public static final Key MANGROVE_LOGS_CAN_GROW_THROUGH = register(Key.key("minecraft:mangrove_logs_can_grow_through"));
  public static final Key MANGROVE_ROOTS_CAN_GROW_THROUGH = register(Key.key("minecraft:mangrove_roots_can_grow_through"));
  public static final Key DEAD_BUSH_MAY_PLACE_ON = register(Key.key("minecraft:dead_bush_may_place_on"));
  public static final Key SNAPS_GOAT_HORN = register(Key.key("minecraft:snaps_goat_horn"));
  public static final Key REPLACEABLE_BY_TREES = register(Key.key("minecraft:replaceable_by_trees"));
  public static final Key SNOW_LAYER_CANNOT_SURVIVE_ON = register(Key.key("minecraft:snow_layer_cannot_survive_on"));
  public static final Key SNOW_LAYER_CAN_SURVIVE_ON = register(Key.key("minecraft:snow_layer_can_survive_on"));
  public static final Key INVALID_SPAWN_INSIDE = register(Key.key("minecraft:invalid_spawn_inside"));
  public static final Key SNIFFER_DIGGABLE_BLOCK = register(Key.key("minecraft:sniffer_diggable_block"));
  public static final Key SNIFFER_EGG_HATCH_BOOST = register(Key.key("minecraft:sniffer_egg_hatch_boost"));
  public static final Key TRAIL_RUINS_REPLACEABLE = register(Key.key("minecraft:trail_ruins_replaceable"));
  public static final Key REPLACEABLE = register(Key.key("minecraft:replaceable"));
  public static final Key ENCHANTMENT_POWER_PROVIDER = register(Key.key("minecraft:enchantment_power_provider"));
  public static final Key ENCHANTMENT_POWER_TRANSMITTER = register(Key.key("minecraft:enchantment_power_transmitter"));
  public static final Key MAINTAINS_FARMLAND = register(Key.key("minecraft:maintains_farmland"));
  public static final Key BLOCKS_WIND_CHARGE_EXPLOSIONS = register(Key.key("minecraft:blocks_wind_charge_explosions"));
  public static final Key DOES_NOT_BLOCK_HOPPERS = register(Key.key("minecraft:does_not_block_hoppers"));
  //@formatter:on

  private BlockTags() {}

  public static Key register(@KeyPattern String key) {
    var resourceKey = Key.key(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
