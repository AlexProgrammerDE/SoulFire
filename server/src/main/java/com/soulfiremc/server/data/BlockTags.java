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
public class BlockTags {
  public static final List<TagKey<BlockType>> TAGS = new ArrayList<>();

  //@formatter:off
  public static final TagKey<BlockType> WOOL = register("minecraft:wool", TAGS);
  public static final TagKey<BlockType> PLANKS = register("minecraft:planks", TAGS);
  public static final TagKey<BlockType> STONE_BRICKS = register("minecraft:stone_bricks", TAGS);
  public static final TagKey<BlockType> WOODEN_BUTTONS = register("minecraft:wooden_buttons", TAGS);
  public static final TagKey<BlockType> STONE_BUTTONS = register("minecraft:stone_buttons", TAGS);
  public static final TagKey<BlockType> BUTTONS = register("minecraft:buttons", TAGS);
  public static final TagKey<BlockType> WOOL_CARPETS = register("minecraft:wool_carpets", TAGS);
  public static final TagKey<BlockType> WOODEN_DOORS = register("minecraft:wooden_doors", TAGS);
  public static final TagKey<BlockType> WOODEN_STAIRS = register("minecraft:wooden_stairs", TAGS);
  public static final TagKey<BlockType> WOODEN_SLABS = register("minecraft:wooden_slabs", TAGS);
  public static final TagKey<BlockType> WOODEN_FENCES = register("minecraft:wooden_fences", TAGS);
  public static final TagKey<BlockType> PRESSURE_PLATES = register("minecraft:pressure_plates", TAGS);
  public static final TagKey<BlockType> WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates", TAGS);
  public static final TagKey<BlockType> STONE_PRESSURE_PLATES = register("minecraft:stone_pressure_plates", TAGS);
  public static final TagKey<BlockType> WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors", TAGS);
  public static final TagKey<BlockType> DOORS = register("minecraft:doors", TAGS);
  public static final TagKey<BlockType> SAPLINGS = register("minecraft:saplings", TAGS);
  public static final TagKey<BlockType> LOGS_THAT_BURN = register("minecraft:logs_that_burn", TAGS);
  public static final TagKey<BlockType> OVERWORLD_NATURAL_LOGS = register("minecraft:overworld_natural_logs", TAGS);
  public static final TagKey<BlockType> LOGS = register("minecraft:logs", TAGS);
  public static final TagKey<BlockType> DARK_OAK_LOGS = register("minecraft:dark_oak_logs", TAGS);
  public static final TagKey<BlockType> OAK_LOGS = register("minecraft:oak_logs", TAGS);
  public static final TagKey<BlockType> BIRCH_LOGS = register("minecraft:birch_logs", TAGS);
  public static final TagKey<BlockType> ACACIA_LOGS = register("minecraft:acacia_logs", TAGS);
  public static final TagKey<BlockType> CHERRY_LOGS = register("minecraft:cherry_logs", TAGS);
  public static final TagKey<BlockType> JUNGLE_LOGS = register("minecraft:jungle_logs", TAGS);
  public static final TagKey<BlockType> SPRUCE_LOGS = register("minecraft:spruce_logs", TAGS);
  public static final TagKey<BlockType> MANGROVE_LOGS = register("minecraft:mangrove_logs", TAGS);
  public static final TagKey<BlockType> CRIMSON_STEMS = register("minecraft:crimson_stems", TAGS);
  public static final TagKey<BlockType> WARPED_STEMS = register("minecraft:warped_stems", TAGS);
  public static final TagKey<BlockType> BAMBOO_BLOCKS = register("minecraft:bamboo_blocks", TAGS);
  public static final TagKey<BlockType> WART_BLOCKS = register("minecraft:wart_blocks", TAGS);
  public static final TagKey<BlockType> BANNERS = register("minecraft:banners", TAGS);
  public static final TagKey<BlockType> SAND = register("minecraft:sand", TAGS);
  public static final TagKey<BlockType> SMELTS_TO_GLASS = register("minecraft:smelts_to_glass", TAGS);
  public static final TagKey<BlockType> STAIRS = register("minecraft:stairs", TAGS);
  public static final TagKey<BlockType> SLABS = register("minecraft:slabs", TAGS);
  public static final TagKey<BlockType> WALLS = register("minecraft:walls", TAGS);
  public static final TagKey<BlockType> ANVIL = register("minecraft:anvil", TAGS);
  public static final TagKey<BlockType> RAILS = register("minecraft:rails", TAGS);
  public static final TagKey<BlockType> LEAVES = register("minecraft:leaves", TAGS);
  public static final TagKey<BlockType> TRAPDOORS = register("minecraft:trapdoors", TAGS);
  public static final TagKey<BlockType> SMALL_FLOWERS = register("minecraft:small_flowers", TAGS);
  public static final TagKey<BlockType> BEDS = register("minecraft:beds", TAGS);
  public static final TagKey<BlockType> FENCES = register("minecraft:fences", TAGS);
  public static final TagKey<BlockType> TALL_FLOWERS = register("minecraft:tall_flowers", TAGS);
  public static final TagKey<BlockType> FLOWERS = register("minecraft:flowers", TAGS);
  public static final TagKey<BlockType> PIGLIN_REPELLENTS = register("minecraft:piglin_repellents", TAGS);
  public static final TagKey<BlockType> GOLD_ORES = register("minecraft:gold_ores", TAGS);
  public static final TagKey<BlockType> IRON_ORES = register("minecraft:iron_ores", TAGS);
  public static final TagKey<BlockType> DIAMOND_ORES = register("minecraft:diamond_ores", TAGS);
  public static final TagKey<BlockType> REDSTONE_ORES = register("minecraft:redstone_ores", TAGS);
  public static final TagKey<BlockType> LAPIS_ORES = register("minecraft:lapis_ores", TAGS);
  public static final TagKey<BlockType> COAL_ORES = register("minecraft:coal_ores", TAGS);
  public static final TagKey<BlockType> EMERALD_ORES = register("minecraft:emerald_ores", TAGS);
  public static final TagKey<BlockType> COPPER_ORES = register("minecraft:copper_ores", TAGS);
  public static final TagKey<BlockType> CANDLES = register("minecraft:candles", TAGS);
  public static final TagKey<BlockType> DIRT = register("minecraft:dirt", TAGS);
  public static final TagKey<BlockType> TERRACOTTA = register("minecraft:terracotta", TAGS);
  public static final TagKey<BlockType> BADLANDS_TERRACOTTA = register("minecraft:badlands_terracotta", TAGS);
  public static final TagKey<BlockType> CONCRETE_POWDER = register("minecraft:concrete_powder", TAGS);
  public static final TagKey<BlockType> COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial", TAGS);
  public static final TagKey<BlockType> FLOWER_POTS = register("minecraft:flower_pots", TAGS);
  public static final TagKey<BlockType> ENDERMAN_HOLDABLE = register("minecraft:enderman_holdable", TAGS);
  public static final TagKey<BlockType> ICE = register("minecraft:ice", TAGS);
  public static final TagKey<BlockType> VALID_SPAWN = register("minecraft:valid_spawn", TAGS);
  public static final TagKey<BlockType> IMPERMEABLE = register("minecraft:impermeable", TAGS);
  public static final TagKey<BlockType> UNDERWATER_BONEMEALS = register("minecraft:underwater_bonemeals", TAGS);
  public static final TagKey<BlockType> CORAL_BLOCKS = register("minecraft:coral_blocks", TAGS);
  public static final TagKey<BlockType> WALL_CORALS = register("minecraft:wall_corals", TAGS);
  public static final TagKey<BlockType> CORAL_PLANTS = register("minecraft:coral_plants", TAGS);
  public static final TagKey<BlockType> CORALS = register("minecraft:corals", TAGS);
  public static final TagKey<BlockType> BAMBOO_PLANTABLE_ON = register("minecraft:bamboo_plantable_on", TAGS);
  public static final TagKey<BlockType> STANDING_SIGNS = register("minecraft:standing_signs", TAGS);
  public static final TagKey<BlockType> WALL_SIGNS = register("minecraft:wall_signs", TAGS);
  public static final TagKey<BlockType> SIGNS = register("minecraft:signs", TAGS);
  public static final TagKey<BlockType> CEILING_HANGING_SIGNS = register("minecraft:ceiling_hanging_signs", TAGS);
  public static final TagKey<BlockType> WALL_HANGING_SIGNS = register("minecraft:wall_hanging_signs", TAGS);
  public static final TagKey<BlockType> ALL_HANGING_SIGNS = register("minecraft:all_hanging_signs", TAGS);
  public static final TagKey<BlockType> ALL_SIGNS = register("minecraft:all_signs", TAGS);
  public static final TagKey<BlockType> DRAGON_IMMUNE = register("minecraft:dragon_immune", TAGS);
  public static final TagKey<BlockType> DRAGON_TRANSPARENT = register("minecraft:dragon_transparent", TAGS);
  public static final TagKey<BlockType> WITHER_IMMUNE = register("minecraft:wither_immune", TAGS);
  public static final TagKey<BlockType> WITHER_SUMMON_BASE_BLOCKS = register("minecraft:wither_summon_base_blocks", TAGS);
  public static final TagKey<BlockType> BEEHIVES = register("minecraft:beehives", TAGS);
  public static final TagKey<BlockType> CROPS = register("minecraft:crops", TAGS);
  public static final TagKey<BlockType> BEE_GROWABLES = register("minecraft:bee_growables", TAGS);
  public static final TagKey<BlockType> PORTALS = register("minecraft:portals", TAGS);
  public static final TagKey<BlockType> FIRE = register("minecraft:fire", TAGS);
  public static final TagKey<BlockType> NYLIUM = register("minecraft:nylium", TAGS);
  public static final TagKey<BlockType> BEACON_BASE_BLOCKS = register("minecraft:beacon_base_blocks", TAGS);
  public static final TagKey<BlockType> SOUL_SPEED_BLOCKS = register("minecraft:soul_speed_blocks", TAGS);
  public static final TagKey<BlockType> WALL_POST_OVERRIDE = register("minecraft:wall_post_override", TAGS);
  public static final TagKey<BlockType> CLIMBABLE = register("minecraft:climbable", TAGS);
  public static final TagKey<BlockType> FALL_DAMAGE_RESETTING = register("minecraft:fall_damage_resetting", TAGS);
  public static final TagKey<BlockType> SHULKER_BOXES = register("minecraft:shulker_boxes", TAGS);
  public static final TagKey<BlockType> HOGLIN_REPELLENTS = register("minecraft:hoglin_repellents", TAGS);
  public static final TagKey<BlockType> SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks", TAGS);
  public static final TagKey<BlockType> STRIDER_WARM_BLOCKS = register("minecraft:strider_warm_blocks", TAGS);
  public static final TagKey<BlockType> CAMPFIRES = register("minecraft:campfires", TAGS);
  public static final TagKey<BlockType> GUARDED_BY_PIGLINS = register("minecraft:guarded_by_piglins", TAGS);
  public static final TagKey<BlockType> PREVENT_MOB_SPAWNING_INSIDE = register("minecraft:prevent_mob_spawning_inside", TAGS);
  public static final TagKey<BlockType> FENCE_GATES = register("minecraft:fence_gates", TAGS);
  public static final TagKey<BlockType> UNSTABLE_BOTTOM_CENTER = register("minecraft:unstable_bottom_center", TAGS);
  public static final TagKey<BlockType> MUSHROOM_GROW_BLOCK = register("minecraft:mushroom_grow_block", TAGS);
  public static final TagKey<BlockType> INFINIBURN_OVERWORLD = register("minecraft:infiniburn_overworld", TAGS);
  public static final TagKey<BlockType> INFINIBURN_NETHER = register("minecraft:infiniburn_nether", TAGS);
  public static final TagKey<BlockType> INFINIBURN_END = register("minecraft:infiniburn_end", TAGS);
  public static final TagKey<BlockType> BASE_STONE_OVERWORLD = register("minecraft:base_stone_overworld", TAGS);
  public static final TagKey<BlockType> STONE_ORE_REPLACEABLES = register("minecraft:stone_ore_replaceables", TAGS);
  public static final TagKey<BlockType> DEEPSLATE_ORE_REPLACEABLES = register("minecraft:deepslate_ore_replaceables", TAGS);
  public static final TagKey<BlockType> BASE_STONE_NETHER = register("minecraft:base_stone_nether", TAGS);
  public static final TagKey<BlockType> OVERWORLD_CARVER_REPLACEABLES = register("minecraft:overworld_carver_replaceables", TAGS);
  public static final TagKey<BlockType> NETHER_CARVER_REPLACEABLES = register("minecraft:nether_carver_replaceables", TAGS);
  public static final TagKey<BlockType> CANDLE_CAKES = register("minecraft:candle_cakes", TAGS);
  public static final TagKey<BlockType> CAULDRONS = register("minecraft:cauldrons", TAGS);
  public static final TagKey<BlockType> CRYSTAL_SOUND_BLOCKS = register("minecraft:crystal_sound_blocks", TAGS);
  public static final TagKey<BlockType> INSIDE_STEP_SOUND_BLOCKS = register("minecraft:inside_step_sound_blocks", TAGS);
  public static final TagKey<BlockType> COMBINATION_STEP_SOUND_BLOCKS = register("minecraft:combination_step_sound_blocks", TAGS);
  public static final TagKey<BlockType> CAMEL_SAND_STEP_SOUND_BLOCKS = register("minecraft:camel_sand_step_sound_blocks", TAGS);
  public static final TagKey<BlockType> OCCLUDES_VIBRATION_SIGNALS = register("minecraft:occludes_vibration_signals", TAGS);
  public static final TagKey<BlockType> DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations", TAGS);
  public static final TagKey<BlockType> DRIPSTONE_REPLACEABLE_BLOCKS = register("minecraft:dripstone_replaceable_blocks", TAGS);
  public static final TagKey<BlockType> CAVE_VINES = register("minecraft:cave_vines", TAGS);
  public static final TagKey<BlockType> MOSS_REPLACEABLE = register("minecraft:moss_replaceable", TAGS);
  public static final TagKey<BlockType> LUSH_GROUND_REPLACEABLE = register("minecraft:lush_ground_replaceable", TAGS);
  public static final TagKey<BlockType> AZALEA_ROOT_REPLACEABLE = register("minecraft:azalea_root_replaceable", TAGS);
  public static final TagKey<BlockType> SMALL_DRIPLEAF_PLACEABLE = register("minecraft:small_dripleaf_placeable", TAGS);
  public static final TagKey<BlockType> BIG_DRIPLEAF_PLACEABLE = register("minecraft:big_dripleaf_placeable", TAGS);
  public static final TagKey<BlockType> SNOW = register("minecraft:snow", TAGS);
  public static final TagKey<BlockType> MINEABLE_WITH_AXE = register("minecraft:mineable/axe", TAGS);
  public static final TagKey<BlockType> MINEABLE_WITH_HOE = register("minecraft:mineable/hoe", TAGS);
  public static final TagKey<BlockType> MINEABLE_WITH_PICKAXE = register("minecraft:mineable/pickaxe", TAGS);
  public static final TagKey<BlockType> MINEABLE_WITH_SHOVEL = register("minecraft:mineable/shovel", TAGS);
  public static final TagKey<BlockType> SWORD_EFFICIENT = register("minecraft:sword_efficient", TAGS);
  public static final TagKey<BlockType> NEEDS_DIAMOND_TOOL = register("minecraft:needs_diamond_tool", TAGS);
  public static final TagKey<BlockType> NEEDS_IRON_TOOL = register("minecraft:needs_iron_tool", TAGS);
  public static final TagKey<BlockType> NEEDS_STONE_TOOL = register("minecraft:needs_stone_tool", TAGS);
  public static final TagKey<BlockType> INCORRECT_FOR_NETHERITE_TOOL = register("minecraft:incorrect_for_netherite_tool", TAGS);
  public static final TagKey<BlockType> INCORRECT_FOR_DIAMOND_TOOL = register("minecraft:incorrect_for_diamond_tool", TAGS);
  public static final TagKey<BlockType> INCORRECT_FOR_IRON_TOOL = register("minecraft:incorrect_for_iron_tool", TAGS);
  public static final TagKey<BlockType> INCORRECT_FOR_STONE_TOOL = register("minecraft:incorrect_for_stone_tool", TAGS);
  public static final TagKey<BlockType> INCORRECT_FOR_GOLD_TOOL = register("minecraft:incorrect_for_gold_tool", TAGS);
  public static final TagKey<BlockType> INCORRECT_FOR_WOODEN_TOOL = register("minecraft:incorrect_for_wooden_tool", TAGS);
  public static final TagKey<BlockType> FEATURES_CANNOT_REPLACE = register("minecraft:features_cannot_replace", TAGS);
  public static final TagKey<BlockType> LAVA_POOL_STONE_CANNOT_REPLACE = register("minecraft:lava_pool_stone_cannot_replace", TAGS);
  public static final TagKey<BlockType> GEODE_INVALID_BLOCKS = register("minecraft:geode_invalid_blocks", TAGS);
  public static final TagKey<BlockType> FROG_PREFER_JUMP_TO = register("minecraft:frog_prefer_jump_to", TAGS);
  public static final TagKey<BlockType> SCULK_REPLACEABLE = register("minecraft:sculk_replaceable", TAGS);
  public static final TagKey<BlockType> SCULK_REPLACEABLE_WORLD_GEN = register("minecraft:sculk_replaceable_world_gen", TAGS);
  public static final TagKey<BlockType> ANCIENT_CITY_REPLACEABLE = register("minecraft:ancient_city_replaceable", TAGS);
  public static final TagKey<BlockType> VIBRATION_RESONATORS = register("minecraft:vibration_resonators", TAGS);
  public static final TagKey<BlockType> ANIMALS_SPAWNABLE_ON = register("minecraft:animals_spawnable_on", TAGS);
  public static final TagKey<BlockType> ARMADILLO_SPAWNABLE_ON = register("minecraft:armadillo_spawnable_on", TAGS);
  public static final TagKey<BlockType> AXOLOTLS_SPAWNABLE_ON = register("minecraft:axolotls_spawnable_on", TAGS);
  public static final TagKey<BlockType> GOATS_SPAWNABLE_ON = register("minecraft:goats_spawnable_on", TAGS);
  public static final TagKey<BlockType> MOOSHROOMS_SPAWNABLE_ON = register("minecraft:mooshrooms_spawnable_on", TAGS);
  public static final TagKey<BlockType> PARROTS_SPAWNABLE_ON = register("minecraft:parrots_spawnable_on", TAGS);
  public static final TagKey<BlockType> POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = register("minecraft:polar_bears_spawnable_on_alternate", TAGS);
  public static final TagKey<BlockType> RABBITS_SPAWNABLE_ON = register("minecraft:rabbits_spawnable_on", TAGS);
  public static final TagKey<BlockType> FOXES_SPAWNABLE_ON = register("minecraft:foxes_spawnable_on", TAGS);
  public static final TagKey<BlockType> WOLVES_SPAWNABLE_ON = register("minecraft:wolves_spawnable_on", TAGS);
  public static final TagKey<BlockType> FROGS_SPAWNABLE_ON = register("minecraft:frogs_spawnable_on", TAGS);
  public static final TagKey<BlockType> AZALEA_GROWS_ON = register("minecraft:azalea_grows_on", TAGS);
  public static final TagKey<BlockType> CONVERTABLE_TO_MUD = register("minecraft:convertable_to_mud", TAGS);
  public static final TagKey<BlockType> MANGROVE_LOGS_CAN_GROW_THROUGH = register("minecraft:mangrove_logs_can_grow_through", TAGS);
  public static final TagKey<BlockType> MANGROVE_ROOTS_CAN_GROW_THROUGH = register("minecraft:mangrove_roots_can_grow_through", TAGS);
  public static final TagKey<BlockType> DEAD_BUSH_MAY_PLACE_ON = register("minecraft:dead_bush_may_place_on", TAGS);
  public static final TagKey<BlockType> SNAPS_GOAT_HORN = register("minecraft:snaps_goat_horn", TAGS);
  public static final TagKey<BlockType> REPLACEABLE_BY_TREES = register("minecraft:replaceable_by_trees", TAGS);
  public static final TagKey<BlockType> SNOW_LAYER_CANNOT_SURVIVE_ON = register("minecraft:snow_layer_cannot_survive_on", TAGS);
  public static final TagKey<BlockType> SNOW_LAYER_CAN_SURVIVE_ON = register("minecraft:snow_layer_can_survive_on", TAGS);
  public static final TagKey<BlockType> INVALID_SPAWN_INSIDE = register("minecraft:invalid_spawn_inside", TAGS);
  public static final TagKey<BlockType> SNIFFER_DIGGABLE_BLOCK = register("minecraft:sniffer_diggable_block", TAGS);
  public static final TagKey<BlockType> SNIFFER_EGG_HATCH_BOOST = register("minecraft:sniffer_egg_hatch_boost", TAGS);
  public static final TagKey<BlockType> TRAIL_RUINS_REPLACEABLE = register("minecraft:trail_ruins_replaceable", TAGS);
  public static final TagKey<BlockType> REPLACEABLE = register("minecraft:replaceable", TAGS);
  public static final TagKey<BlockType> ENCHANTMENT_POWER_PROVIDER = register("minecraft:enchantment_power_provider", TAGS);
  public static final TagKey<BlockType> ENCHANTMENT_POWER_TRANSMITTER = register("minecraft:enchantment_power_transmitter", TAGS);
  public static final TagKey<BlockType> MAINTAINS_FARMLAND = register("minecraft:maintains_farmland", TAGS);
  public static final TagKey<BlockType> BLOCKS_WIND_CHARGE_EXPLOSIONS = register("minecraft:blocks_wind_charge_explosions", TAGS);
  public static final TagKey<BlockType> DOES_NOT_BLOCK_HOPPERS = register("minecraft:does_not_block_hoppers", TAGS);
  //@formatter:on

  private BlockTags() {}

  public static <T extends RegistryValue<T>> TagKey<T> register(@KeyPattern String key, List<TagKey<T>> values) {
    var resourceKey = TagKey.<T>key(key, RegistryKeys.BLOCK);
    values.add(resourceKey);
    return resourceKey;
  }
}
