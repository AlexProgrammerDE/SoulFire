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

@SuppressWarnings("unused")
public class BlockTags {
  public static final List<Key> TAGS = new ArrayList<>();

  //@formatter:off
  public static final Key WOOL = register("minecraft:wool");
  public static final Key PLANKS = register("minecraft:planks");
  public static final Key STONE_BRICKS = register("minecraft:stone_bricks");
  public static final Key WOODEN_BUTTONS = register("minecraft:wooden_buttons");
  public static final Key STONE_BUTTONS = register("minecraft:stone_buttons");
  public static final Key BUTTONS = register("minecraft:buttons");
  public static final Key WOOL_CARPETS = register("minecraft:wool_carpets");
  public static final Key WOODEN_DOORS = register("minecraft:wooden_doors");
  public static final Key WOODEN_STAIRS = register("minecraft:wooden_stairs");
  public static final Key WOODEN_SLABS = register("minecraft:wooden_slabs");
  public static final Key WOODEN_FENCES = register("minecraft:wooden_fences");
  public static final Key PRESSURE_PLATES = register("minecraft:pressure_plates");
  public static final Key WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates");
  public static final Key STONE_PRESSURE_PLATES = register("minecraft:stone_pressure_plates");
  public static final Key WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors");
  public static final Key DOORS = register("minecraft:doors");
  public static final Key SAPLINGS = register("minecraft:saplings");
  public static final Key LOGS_THAT_BURN = register("minecraft:logs_that_burn");
  public static final Key OVERWORLD_NATURAL_LOGS = register("minecraft:overworld_natural_logs");
  public static final Key LOGS = register("minecraft:logs");
  public static final Key DARK_OAK_LOGS = register("minecraft:dark_oak_logs");
  public static final Key OAK_LOGS = register("minecraft:oak_logs");
  public static final Key BIRCH_LOGS = register("minecraft:birch_logs");
  public static final Key ACACIA_LOGS = register("minecraft:acacia_logs");
  public static final Key CHERRY_LOGS = register("minecraft:cherry_logs");
  public static final Key JUNGLE_LOGS = register("minecraft:jungle_logs");
  public static final Key SPRUCE_LOGS = register("minecraft:spruce_logs");
  public static final Key MANGROVE_LOGS = register("minecraft:mangrove_logs");
  public static final Key CRIMSON_STEMS = register("minecraft:crimson_stems");
  public static final Key WARPED_STEMS = register("minecraft:warped_stems");
  public static final Key BAMBOO_BLOCKS = register("minecraft:bamboo_blocks");
  public static final Key WART_BLOCKS = register("minecraft:wart_blocks");
  public static final Key BANNERS = register("minecraft:banners");
  public static final Key SAND = register("minecraft:sand");
  public static final Key SMELTS_TO_GLASS = register("minecraft:smelts_to_glass");
  public static final Key STAIRS = register("minecraft:stairs");
  public static final Key SLABS = register("minecraft:slabs");
  public static final Key WALLS = register("minecraft:walls");
  public static final Key ANVIL = register("minecraft:anvil");
  public static final Key RAILS = register("minecraft:rails");
  public static final Key LEAVES = register("minecraft:leaves");
  public static final Key TRAPDOORS = register("minecraft:trapdoors");
  public static final Key SMALL_FLOWERS = register("minecraft:small_flowers");
  public static final Key BEDS = register("minecraft:beds");
  public static final Key FENCES = register("minecraft:fences");
  public static final Key TALL_FLOWERS = register("minecraft:tall_flowers");
  public static final Key FLOWERS = register("minecraft:flowers");
  public static final Key PIGLIN_REPELLENTS = register("minecraft:piglin_repellents");
  public static final Key GOLD_ORES = register("minecraft:gold_ores");
  public static final Key IRON_ORES = register("minecraft:iron_ores");
  public static final Key DIAMOND_ORES = register("minecraft:diamond_ores");
  public static final Key REDSTONE_ORES = register("minecraft:redstone_ores");
  public static final Key LAPIS_ORES = register("minecraft:lapis_ores");
  public static final Key COAL_ORES = register("minecraft:coal_ores");
  public static final Key EMERALD_ORES = register("minecraft:emerald_ores");
  public static final Key COPPER_ORES = register("minecraft:copper_ores");
  public static final Key CANDLES = register("minecraft:candles");
  public static final Key DIRT = register("minecraft:dirt");
  public static final Key TERRACOTTA = register("minecraft:terracotta");
  public static final Key BADLANDS_TERRACOTTA = register("minecraft:badlands_terracotta");
  public static final Key CONCRETE_POWDER = register("minecraft:concrete_powder");
  public static final Key COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial");
  public static final Key FLOWER_POTS = register("minecraft:flower_pots");
  public static final Key ENDERMAN_HOLDABLE = register("minecraft:enderman_holdable");
  public static final Key ICE = register("minecraft:ice");
  public static final Key VALID_SPAWN = register("minecraft:valid_spawn");
  public static final Key IMPERMEABLE = register("minecraft:impermeable");
  public static final Key UNDERWATER_BONEMEALS = register("minecraft:underwater_bonemeals");
  public static final Key CORAL_BLOCKS = register("minecraft:coral_blocks");
  public static final Key WALL_CORALS = register("minecraft:wall_corals");
  public static final Key CORAL_PLANTS = register("minecraft:coral_plants");
  public static final Key CORALS = register("minecraft:corals");
  public static final Key BAMBOO_PLANTABLE_ON = register("minecraft:bamboo_plantable_on");
  public static final Key STANDING_SIGNS = register("minecraft:standing_signs");
  public static final Key WALL_SIGNS = register("minecraft:wall_signs");
  public static final Key SIGNS = register("minecraft:signs");
  public static final Key CEILING_HANGING_SIGNS = register("minecraft:ceiling_hanging_signs");
  public static final Key WALL_HANGING_SIGNS = register("minecraft:wall_hanging_signs");
  public static final Key ALL_HANGING_SIGNS = register("minecraft:all_hanging_signs");
  public static final Key ALL_SIGNS = register("minecraft:all_signs");
  public static final Key DRAGON_IMMUNE = register("minecraft:dragon_immune");
  public static final Key DRAGON_TRANSPARENT = register("minecraft:dragon_transparent");
  public static final Key WITHER_IMMUNE = register("minecraft:wither_immune");
  public static final Key WITHER_SUMMON_BASE_BLOCKS = register("minecraft:wither_summon_base_blocks");
  public static final Key BEEHIVES = register("minecraft:beehives");
  public static final Key CROPS = register("minecraft:crops");
  public static final Key BEE_GROWABLES = register("minecraft:bee_growables");
  public static final Key PORTALS = register("minecraft:portals");
  public static final Key FIRE = register("minecraft:fire");
  public static final Key NYLIUM = register("minecraft:nylium");
  public static final Key BEACON_BASE_BLOCKS = register("minecraft:beacon_base_blocks");
  public static final Key SOUL_SPEED_BLOCKS = register("minecraft:soul_speed_blocks");
  public static final Key WALL_POST_OVERRIDE = register("minecraft:wall_post_override");
  public static final Key CLIMBABLE = register("minecraft:climbable");
  public static final Key FALL_DAMAGE_RESETTING = register("minecraft:fall_damage_resetting");
  public static final Key SHULKER_BOXES = register("minecraft:shulker_boxes");
  public static final Key HOGLIN_REPELLENTS = register("minecraft:hoglin_repellents");
  public static final Key SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks");
  public static final Key STRIDER_WARM_BLOCKS = register("minecraft:strider_warm_blocks");
  public static final Key CAMPFIRES = register("minecraft:campfires");
  public static final Key GUARDED_BY_PIGLINS = register("minecraft:guarded_by_piglins");
  public static final Key PREVENT_MOB_SPAWNING_INSIDE = register("minecraft:prevent_mob_spawning_inside");
  public static final Key FENCE_GATES = register("minecraft:fence_gates");
  public static final Key UNSTABLE_BOTTOM_CENTER = register("minecraft:unstable_bottom_center");
  public static final Key MUSHROOM_GROW_BLOCK = register("minecraft:mushroom_grow_block");
  public static final Key INFINIBURN_OVERWORLD = register("minecraft:infiniburn_overworld");
  public static final Key INFINIBURN_NETHER = register("minecraft:infiniburn_nether");
  public static final Key INFINIBURN_END = register("minecraft:infiniburn_end");
  public static final Key BASE_STONE_OVERWORLD = register("minecraft:base_stone_overworld");
  public static final Key STONE_ORE_REPLACEABLES = register("minecraft:stone_ore_replaceables");
  public static final Key DEEPSLATE_ORE_REPLACEABLES = register("minecraft:deepslate_ore_replaceables");
  public static final Key BASE_STONE_NETHER = register("minecraft:base_stone_nether");
  public static final Key OVERWORLD_CARVER_REPLACEABLES = register("minecraft:overworld_carver_replaceables");
  public static final Key NETHER_CARVER_REPLACEABLES = register("minecraft:nether_carver_replaceables");
  public static final Key CANDLE_CAKES = register("minecraft:candle_cakes");
  public static final Key CAULDRONS = register("minecraft:cauldrons");
  public static final Key CRYSTAL_SOUND_BLOCKS = register("minecraft:crystal_sound_blocks");
  public static final Key INSIDE_STEP_SOUND_BLOCKS = register("minecraft:inside_step_sound_blocks");
  public static final Key COMBINATION_STEP_SOUND_BLOCKS = register("minecraft:combination_step_sound_blocks");
  public static final Key CAMEL_SAND_STEP_SOUND_BLOCKS = register("minecraft:camel_sand_step_sound_blocks");
  public static final Key OCCLUDES_VIBRATION_SIGNALS = register("minecraft:occludes_vibration_signals");
  public static final Key DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations");
  public static final Key DRIPSTONE_REPLACEABLE_BLOCKS = register("minecraft:dripstone_replaceable_blocks");
  public static final Key CAVE_VINES = register("minecraft:cave_vines");
  public static final Key MOSS_REPLACEABLE = register("minecraft:moss_replaceable");
  public static final Key LUSH_GROUND_REPLACEABLE = register("minecraft:lush_ground_replaceable");
  public static final Key AZALEA_ROOT_REPLACEABLE = register("minecraft:azalea_root_replaceable");
  public static final Key SMALL_DRIPLEAF_PLACEABLE = register("minecraft:small_dripleaf_placeable");
  public static final Key BIG_DRIPLEAF_PLACEABLE = register("minecraft:big_dripleaf_placeable");
  public static final Key SNOW = register("minecraft:snow");
  public static final Key MINEABLE_WITH_AXE = register("minecraft:mineable/axe");
  public static final Key MINEABLE_WITH_HOE = register("minecraft:mineable/hoe");
  public static final Key MINEABLE_WITH_PICKAXE = register("minecraft:mineable/pickaxe");
  public static final Key MINEABLE_WITH_SHOVEL = register("minecraft:mineable/shovel");
  public static final Key SWORD_EFFICIENT = register("minecraft:sword_efficient");
  public static final Key NEEDS_DIAMOND_TOOL = register("minecraft:needs_diamond_tool");
  public static final Key NEEDS_IRON_TOOL = register("minecraft:needs_iron_tool");
  public static final Key NEEDS_STONE_TOOL = register("minecraft:needs_stone_tool");
  public static final Key INCORRECT_FOR_NETHERITE_TOOL = register("minecraft:incorrect_for_netherite_tool");
  public static final Key INCORRECT_FOR_DIAMOND_TOOL = register("minecraft:incorrect_for_diamond_tool");
  public static final Key INCORRECT_FOR_IRON_TOOL = register("minecraft:incorrect_for_iron_tool");
  public static final Key INCORRECT_FOR_STONE_TOOL = register("minecraft:incorrect_for_stone_tool");
  public static final Key INCORRECT_FOR_GOLD_TOOL = register("minecraft:incorrect_for_gold_tool");
  public static final Key INCORRECT_FOR_WOODEN_TOOL = register("minecraft:incorrect_for_wooden_tool");
  public static final Key FEATURES_CANNOT_REPLACE = register("minecraft:features_cannot_replace");
  public static final Key LAVA_POOL_STONE_CANNOT_REPLACE = register("minecraft:lava_pool_stone_cannot_replace");
  public static final Key GEODE_INVALID_BLOCKS = register("minecraft:geode_invalid_blocks");
  public static final Key FROG_PREFER_JUMP_TO = register("minecraft:frog_prefer_jump_to");
  public static final Key SCULK_REPLACEABLE = register("minecraft:sculk_replaceable");
  public static final Key SCULK_REPLACEABLE_WORLD_GEN = register("minecraft:sculk_replaceable_world_gen");
  public static final Key ANCIENT_CITY_REPLACEABLE = register("minecraft:ancient_city_replaceable");
  public static final Key VIBRATION_RESONATORS = register("minecraft:vibration_resonators");
  public static final Key ANIMALS_SPAWNABLE_ON = register("minecraft:animals_spawnable_on");
  public static final Key ARMADILLO_SPAWNABLE_ON = register("minecraft:armadillo_spawnable_on");
  public static final Key AXOLOTLS_SPAWNABLE_ON = register("minecraft:axolotls_spawnable_on");
  public static final Key GOATS_SPAWNABLE_ON = register("minecraft:goats_spawnable_on");
  public static final Key MOOSHROOMS_SPAWNABLE_ON = register("minecraft:mooshrooms_spawnable_on");
  public static final Key PARROTS_SPAWNABLE_ON = register("minecraft:parrots_spawnable_on");
  public static final Key POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = register("minecraft:polar_bears_spawnable_on_alternate");
  public static final Key RABBITS_SPAWNABLE_ON = register("minecraft:rabbits_spawnable_on");
  public static final Key FOXES_SPAWNABLE_ON = register("minecraft:foxes_spawnable_on");
  public static final Key WOLVES_SPAWNABLE_ON = register("minecraft:wolves_spawnable_on");
  public static final Key FROGS_SPAWNABLE_ON = register("minecraft:frogs_spawnable_on");
  public static final Key AZALEA_GROWS_ON = register("minecraft:azalea_grows_on");
  public static final Key CONVERTABLE_TO_MUD = register("minecraft:convertable_to_mud");
  public static final Key MANGROVE_LOGS_CAN_GROW_THROUGH = register("minecraft:mangrove_logs_can_grow_through");
  public static final Key MANGROVE_ROOTS_CAN_GROW_THROUGH = register("minecraft:mangrove_roots_can_grow_through");
  public static final Key DEAD_BUSH_MAY_PLACE_ON = register("minecraft:dead_bush_may_place_on");
  public static final Key SNAPS_GOAT_HORN = register("minecraft:snaps_goat_horn");
  public static final Key REPLACEABLE_BY_TREES = register("minecraft:replaceable_by_trees");
  public static final Key SNOW_LAYER_CANNOT_SURVIVE_ON = register("minecraft:snow_layer_cannot_survive_on");
  public static final Key SNOW_LAYER_CAN_SURVIVE_ON = register("minecraft:snow_layer_can_survive_on");
  public static final Key INVALID_SPAWN_INSIDE = register("minecraft:invalid_spawn_inside");
  public static final Key SNIFFER_DIGGABLE_BLOCK = register("minecraft:sniffer_diggable_block");
  public static final Key SNIFFER_EGG_HATCH_BOOST = register("minecraft:sniffer_egg_hatch_boost");
  public static final Key TRAIL_RUINS_REPLACEABLE = register("minecraft:trail_ruins_replaceable");
  public static final Key REPLACEABLE = register("minecraft:replaceable");
  public static final Key ENCHANTMENT_POWER_PROVIDER = register("minecraft:enchantment_power_provider");
  public static final Key ENCHANTMENT_POWER_TRANSMITTER = register("minecraft:enchantment_power_transmitter");
  public static final Key MAINTAINS_FARMLAND = register("minecraft:maintains_farmland");
  public static final Key BLOCKS_WIND_CHARGE_EXPLOSIONS = register("minecraft:blocks_wind_charge_explosions");
  public static final Key DOES_NOT_BLOCK_HOPPERS = register("minecraft:does_not_block_hoppers");
  //@formatter:on

  private BlockTags() {}

  public static Key register(String key) {
    var resourceKey = Key.key(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
