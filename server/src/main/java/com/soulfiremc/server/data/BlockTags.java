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

import net.kyori.adventure.key.KeyPattern;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class BlockTags {
  public static final List<TagKey<BlockType>> TAGS = new ArrayList<>();

  //@formatter:off
  public static final TagKey<BlockType> WOOL = register("minecraft:wool");
  public static final TagKey<BlockType> PLANKS = register("minecraft:planks");
  public static final TagKey<BlockType> STONE_BRICKS = register("minecraft:stone_bricks");
  public static final TagKey<BlockType> WOODEN_BUTTONS = register("minecraft:wooden_buttons");
  public static final TagKey<BlockType> STONE_BUTTONS = register("minecraft:stone_buttons");
  public static final TagKey<BlockType> BUTTONS = register("minecraft:buttons");
  public static final TagKey<BlockType> WOOL_CARPETS = register("minecraft:wool_carpets");
  public static final TagKey<BlockType> WOODEN_DOORS = register("minecraft:wooden_doors");
  public static final TagKey<BlockType> MOB_INTERACTABLE_DOORS = register("minecraft:mob_interactable_doors");
  public static final TagKey<BlockType> WOODEN_STAIRS = register("minecraft:wooden_stairs");
  public static final TagKey<BlockType> WOODEN_SLABS = register("minecraft:wooden_slabs");
  public static final TagKey<BlockType> WOODEN_FENCES = register("minecraft:wooden_fences");
  public static final TagKey<BlockType> PRESSURE_PLATES = register("minecraft:pressure_plates");
  public static final TagKey<BlockType> WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates");
  public static final TagKey<BlockType> STONE_PRESSURE_PLATES = register("minecraft:stone_pressure_plates");
  public static final TagKey<BlockType> WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors");
  public static final TagKey<BlockType> DOORS = register("minecraft:doors");
  public static final TagKey<BlockType> SAPLINGS = register("minecraft:saplings");
  public static final TagKey<BlockType> LOGS_THAT_BURN = register("minecraft:logs_that_burn");
  public static final TagKey<BlockType> OVERWORLD_NATURAL_LOGS = register("minecraft:overworld_natural_logs");
  public static final TagKey<BlockType> LOGS = register("minecraft:logs");
  public static final TagKey<BlockType> DARK_OAK_LOGS = register("minecraft:dark_oak_logs");
  public static final TagKey<BlockType> PALE_OAK_LOGS = register("minecraft:pale_oak_logs");
  public static final TagKey<BlockType> OAK_LOGS = register("minecraft:oak_logs");
  public static final TagKey<BlockType> BIRCH_LOGS = register("minecraft:birch_logs");
  public static final TagKey<BlockType> ACACIA_LOGS = register("minecraft:acacia_logs");
  public static final TagKey<BlockType> CHERRY_LOGS = register("minecraft:cherry_logs");
  public static final TagKey<BlockType> JUNGLE_LOGS = register("minecraft:jungle_logs");
  public static final TagKey<BlockType> SPRUCE_LOGS = register("minecraft:spruce_logs");
  public static final TagKey<BlockType> MANGROVE_LOGS = register("minecraft:mangrove_logs");
  public static final TagKey<BlockType> CRIMSON_STEMS = register("minecraft:crimson_stems");
  public static final TagKey<BlockType> WARPED_STEMS = register("minecraft:warped_stems");
  public static final TagKey<BlockType> BAMBOO_BLOCKS = register("minecraft:bamboo_blocks");
  public static final TagKey<BlockType> WART_BLOCKS = register("minecraft:wart_blocks");
  public static final TagKey<BlockType> BANNERS = register("minecraft:banners");
  public static final TagKey<BlockType> SAND = register("minecraft:sand");
  public static final TagKey<BlockType> SMELTS_TO_GLASS = register("minecraft:smelts_to_glass");
  public static final TagKey<BlockType> STAIRS = register("minecraft:stairs");
  public static final TagKey<BlockType> SLABS = register("minecraft:slabs");
  public static final TagKey<BlockType> WALLS = register("minecraft:walls");
  public static final TagKey<BlockType> ANVIL = register("minecraft:anvil");
  public static final TagKey<BlockType> RAILS = register("minecraft:rails");
  public static final TagKey<BlockType> LEAVES = register("minecraft:leaves");
  public static final TagKey<BlockType> TRAPDOORS = register("minecraft:trapdoors");
  public static final TagKey<BlockType> SMALL_FLOWERS = register("minecraft:small_flowers");
  public static final TagKey<BlockType> BEDS = register("minecraft:beds");
  public static final TagKey<BlockType> FENCES = register("minecraft:fences");
  public static final TagKey<BlockType> FLOWERS = register("minecraft:flowers");
  public static final TagKey<BlockType> BEE_ATTRACTIVE = register("minecraft:bee_attractive");
  public static final TagKey<BlockType> PIGLIN_REPELLENTS = register("minecraft:piglin_repellents");
  public static final TagKey<BlockType> GOLD_ORES = register("minecraft:gold_ores");
  public static final TagKey<BlockType> IRON_ORES = register("minecraft:iron_ores");
  public static final TagKey<BlockType> DIAMOND_ORES = register("minecraft:diamond_ores");
  public static final TagKey<BlockType> REDSTONE_ORES = register("minecraft:redstone_ores");
  public static final TagKey<BlockType> LAPIS_ORES = register("minecraft:lapis_ores");
  public static final TagKey<BlockType> COAL_ORES = register("minecraft:coal_ores");
  public static final TagKey<BlockType> EMERALD_ORES = register("minecraft:emerald_ores");
  public static final TagKey<BlockType> COPPER_ORES = register("minecraft:copper_ores");
  public static final TagKey<BlockType> CANDLES = register("minecraft:candles");
  public static final TagKey<BlockType> DIRT = register("minecraft:dirt");
  public static final TagKey<BlockType> TERRACOTTA = register("minecraft:terracotta");
  public static final TagKey<BlockType> BADLANDS_TERRACOTTA = register("minecraft:badlands_terracotta");
  public static final TagKey<BlockType> CONCRETE_POWDER = register("minecraft:concrete_powder");
  public static final TagKey<BlockType> COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial");
  public static final TagKey<BlockType> SHULKER_BOXES = register("minecraft:shulker_boxes");
  public static final TagKey<BlockType> FLOWER_POTS = register("minecraft:flower_pots");
  public static final TagKey<BlockType> ENDERMAN_HOLDABLE = register("minecraft:enderman_holdable");
  public static final TagKey<BlockType> ICE = register("minecraft:ice");
  public static final TagKey<BlockType> VALID_SPAWN = register("minecraft:valid_spawn");
  public static final TagKey<BlockType> IMPERMEABLE = register("minecraft:impermeable");
  public static final TagKey<BlockType> UNDERWATER_BONEMEALS = register("minecraft:underwater_bonemeals");
  public static final TagKey<BlockType> CORAL_BLOCKS = register("minecraft:coral_blocks");
  public static final TagKey<BlockType> WALL_CORALS = register("minecraft:wall_corals");
  public static final TagKey<BlockType> CORAL_PLANTS = register("minecraft:coral_plants");
  public static final TagKey<BlockType> CORALS = register("minecraft:corals");
  public static final TagKey<BlockType> BAMBOO_PLANTABLE_ON = register("minecraft:bamboo_plantable_on");
  public static final TagKey<BlockType> STANDING_SIGNS = register("minecraft:standing_signs");
  public static final TagKey<BlockType> WALL_SIGNS = register("minecraft:wall_signs");
  public static final TagKey<BlockType> SIGNS = register("minecraft:signs");
  public static final TagKey<BlockType> CEILING_HANGING_SIGNS = register("minecraft:ceiling_hanging_signs");
  public static final TagKey<BlockType> WALL_HANGING_SIGNS = register("minecraft:wall_hanging_signs");
  public static final TagKey<BlockType> ALL_HANGING_SIGNS = register("minecraft:all_hanging_signs");
  public static final TagKey<BlockType> ALL_SIGNS = register("minecraft:all_signs");
  public static final TagKey<BlockType> DRAGON_IMMUNE = register("minecraft:dragon_immune");
  public static final TagKey<BlockType> DRAGON_TRANSPARENT = register("minecraft:dragon_transparent");
  public static final TagKey<BlockType> WITHER_IMMUNE = register("minecraft:wither_immune");
  public static final TagKey<BlockType> WITHER_SUMMON_BASE_BLOCKS = register("minecraft:wither_summon_base_blocks");
  public static final TagKey<BlockType> BEEHIVES = register("minecraft:beehives");
  public static final TagKey<BlockType> CROPS = register("minecraft:crops");
  public static final TagKey<BlockType> BEE_GROWABLES = register("minecraft:bee_growables");
  public static final TagKey<BlockType> PORTALS = register("minecraft:portals");
  public static final TagKey<BlockType> FIRE = register("minecraft:fire");
  public static final TagKey<BlockType> NYLIUM = register("minecraft:nylium");
  public static final TagKey<BlockType> BEACON_BASE_BLOCKS = register("minecraft:beacon_base_blocks");
  public static final TagKey<BlockType> SOUL_SPEED_BLOCKS = register("minecraft:soul_speed_blocks");
  public static final TagKey<BlockType> WALL_POST_OVERRIDE = register("minecraft:wall_post_override");
  public static final TagKey<BlockType> CLIMBABLE = register("minecraft:climbable");
  public static final TagKey<BlockType> FALL_DAMAGE_RESETTING = register("minecraft:fall_damage_resetting");
  public static final TagKey<BlockType> HOGLIN_REPELLENTS = register("minecraft:hoglin_repellents");
  public static final TagKey<BlockType> SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks");
  public static final TagKey<BlockType> STRIDER_WARM_BLOCKS = register("minecraft:strider_warm_blocks");
  public static final TagKey<BlockType> CAMPFIRES = register("minecraft:campfires");
  public static final TagKey<BlockType> GUARDED_BY_PIGLINS = register("minecraft:guarded_by_piglins");
  public static final TagKey<BlockType> PREVENT_MOB_SPAWNING_INSIDE = register("minecraft:prevent_mob_spawning_inside");
  public static final TagKey<BlockType> FENCE_GATES = register("minecraft:fence_gates");
  public static final TagKey<BlockType> UNSTABLE_BOTTOM_CENTER = register("minecraft:unstable_bottom_center");
  public static final TagKey<BlockType> MUSHROOM_GROW_BLOCK = register("minecraft:mushroom_grow_block");
  public static final TagKey<BlockType> INFINIBURN_OVERWORLD = register("minecraft:infiniburn_overworld");
  public static final TagKey<BlockType> INFINIBURN_NETHER = register("minecraft:infiniburn_nether");
  public static final TagKey<BlockType> INFINIBURN_END = register("minecraft:infiniburn_end");
  public static final TagKey<BlockType> BASE_STONE_OVERWORLD = register("minecraft:base_stone_overworld");
  public static final TagKey<BlockType> STONE_ORE_REPLACEABLES = register("minecraft:stone_ore_replaceables");
  public static final TagKey<BlockType> DEEPSLATE_ORE_REPLACEABLES = register("minecraft:deepslate_ore_replaceables");
  public static final TagKey<BlockType> BASE_STONE_NETHER = register("minecraft:base_stone_nether");
  public static final TagKey<BlockType> OVERWORLD_CARVER_REPLACEABLES = register("minecraft:overworld_carver_replaceables");
  public static final TagKey<BlockType> NETHER_CARVER_REPLACEABLES = register("minecraft:nether_carver_replaceables");
  public static final TagKey<BlockType> CANDLE_CAKES = register("minecraft:candle_cakes");
  public static final TagKey<BlockType> CAULDRONS = register("minecraft:cauldrons");
  public static final TagKey<BlockType> CRYSTAL_SOUND_BLOCKS = register("minecraft:crystal_sound_blocks");
  public static final TagKey<BlockType> INSIDE_STEP_SOUND_BLOCKS = register("minecraft:inside_step_sound_blocks");
  public static final TagKey<BlockType> COMBINATION_STEP_SOUND_BLOCKS = register("minecraft:combination_step_sound_blocks");
  public static final TagKey<BlockType> CAMEL_SAND_STEP_SOUND_BLOCKS = register("minecraft:camel_sand_step_sound_blocks");
  public static final TagKey<BlockType> OCCLUDES_VIBRATION_SIGNALS = register("minecraft:occludes_vibration_signals");
  public static final TagKey<BlockType> DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations");
  public static final TagKey<BlockType> DRIPSTONE_REPLACEABLE = register("minecraft:dripstone_replaceable_blocks");
  public static final TagKey<BlockType> CAVE_VINES = register("minecraft:cave_vines");
  public static final TagKey<BlockType> MOSS_REPLACEABLE = register("minecraft:moss_replaceable");
  public static final TagKey<BlockType> LUSH_GROUND_REPLACEABLE = register("minecraft:lush_ground_replaceable");
  public static final TagKey<BlockType> AZALEA_ROOT_REPLACEABLE = register("minecraft:azalea_root_replaceable");
  public static final TagKey<BlockType> SMALL_DRIPLEAF_PLACEABLE = register("minecraft:small_dripleaf_placeable");
  public static final TagKey<BlockType> BIG_DRIPLEAF_PLACEABLE = register("minecraft:big_dripleaf_placeable");
  public static final TagKey<BlockType> SNOW = register("minecraft:snow");
  public static final TagKey<BlockType> MINEABLE_WITH_AXE = register("minecraft:mineable/axe");
  public static final TagKey<BlockType> MINEABLE_WITH_HOE = register("minecraft:mineable/hoe");
  public static final TagKey<BlockType> MINEABLE_WITH_PICKAXE = register("minecraft:mineable/pickaxe");
  public static final TagKey<BlockType> MINEABLE_WITH_SHOVEL = register("minecraft:mineable/shovel");
  public static final TagKey<BlockType> SWORD_EFFICIENT = register("minecraft:sword_efficient");
  public static final TagKey<BlockType> NEEDS_DIAMOND_TOOL = register("minecraft:needs_diamond_tool");
  public static final TagKey<BlockType> NEEDS_IRON_TOOL = register("minecraft:needs_iron_tool");
  public static final TagKey<BlockType> NEEDS_STONE_TOOL = register("minecraft:needs_stone_tool");
  public static final TagKey<BlockType> INCORRECT_FOR_NETHERITE_TOOL = register("minecraft:incorrect_for_netherite_tool");
  public static final TagKey<BlockType> INCORRECT_FOR_DIAMOND_TOOL = register("minecraft:incorrect_for_diamond_tool");
  public static final TagKey<BlockType> INCORRECT_FOR_IRON_TOOL = register("minecraft:incorrect_for_iron_tool");
  public static final TagKey<BlockType> INCORRECT_FOR_STONE_TOOL = register("minecraft:incorrect_for_stone_tool");
  public static final TagKey<BlockType> INCORRECT_FOR_GOLD_TOOL = register("minecraft:incorrect_for_gold_tool");
  public static final TagKey<BlockType> INCORRECT_FOR_WOODEN_TOOL = register("minecraft:incorrect_for_wooden_tool");
  public static final TagKey<BlockType> FEATURES_CANNOT_REPLACE = register("minecraft:features_cannot_replace");
  public static final TagKey<BlockType> LAVA_POOL_STONE_CANNOT_REPLACE = register("minecraft:lava_pool_stone_cannot_replace");
  public static final TagKey<BlockType> GEODE_INVALID_BLOCKS = register("minecraft:geode_invalid_blocks");
  public static final TagKey<BlockType> FROG_PREFER_JUMP_TO = register("minecraft:frog_prefer_jump_to");
  public static final TagKey<BlockType> SCULK_REPLACEABLE = register("minecraft:sculk_replaceable");
  public static final TagKey<BlockType> SCULK_REPLACEABLE_WORLD_GEN = register("minecraft:sculk_replaceable_world_gen");
  public static final TagKey<BlockType> ANCIENT_CITY_REPLACEABLE = register("minecraft:ancient_city_replaceable");
  public static final TagKey<BlockType> VIBRATION_RESONATORS = register("minecraft:vibration_resonators");
  public static final TagKey<BlockType> ANIMALS_SPAWNABLE_ON = register("minecraft:animals_spawnable_on");
  public static final TagKey<BlockType> ARMADILLO_SPAWNABLE_ON = register("minecraft:armadillo_spawnable_on");
  public static final TagKey<BlockType> AXOLOTLS_SPAWNABLE_ON = register("minecraft:axolotls_spawnable_on");
  public static final TagKey<BlockType> GOATS_SPAWNABLE_ON = register("minecraft:goats_spawnable_on");
  public static final TagKey<BlockType> MOOSHROOMS_SPAWNABLE_ON = register("minecraft:mooshrooms_spawnable_on");
  public static final TagKey<BlockType> PARROTS_SPAWNABLE_ON = register("minecraft:parrots_spawnable_on");
  public static final TagKey<BlockType> POLAR_BEARS_SPAWNABLE_ON_ALTERNATE = register("minecraft:polar_bears_spawnable_on_alternate");
  public static final TagKey<BlockType> RABBITS_SPAWNABLE_ON = register("minecraft:rabbits_spawnable_on");
  public static final TagKey<BlockType> FOXES_SPAWNABLE_ON = register("minecraft:foxes_spawnable_on");
  public static final TagKey<BlockType> WOLVES_SPAWNABLE_ON = register("minecraft:wolves_spawnable_on");
  public static final TagKey<BlockType> FROGS_SPAWNABLE_ON = register("minecraft:frogs_spawnable_on");
  public static final TagKey<BlockType> BATS_SPAWNABLE_ON = register("minecraft:bats_spawnable_on");
  public static final TagKey<BlockType> AZALEA_GROWS_ON = register("minecraft:azalea_grows_on");
  public static final TagKey<BlockType> CONVERTABLE_TO_MUD = register("minecraft:convertable_to_mud");
  public static final TagKey<BlockType> MANGROVE_LOGS_CAN_GROW_THROUGH = register("minecraft:mangrove_logs_can_grow_through");
  public static final TagKey<BlockType> MANGROVE_ROOTS_CAN_GROW_THROUGH = register("minecraft:mangrove_roots_can_grow_through");
  public static final TagKey<BlockType> DEAD_BUSH_MAY_PLACE_ON = register("minecraft:dead_bush_may_place_on");
  public static final TagKey<BlockType> SNAPS_GOAT_HORN = register("minecraft:snaps_goat_horn");
  public static final TagKey<BlockType> REPLACEABLE_BY_TREES = register("minecraft:replaceable_by_trees");
  public static final TagKey<BlockType> SNOW_LAYER_CANNOT_SURVIVE_ON = register("minecraft:snow_layer_cannot_survive_on");
  public static final TagKey<BlockType> SNOW_LAYER_CAN_SURVIVE_ON = register("minecraft:snow_layer_can_survive_on");
  public static final TagKey<BlockType> INVALID_SPAWN_INSIDE = register("minecraft:invalid_spawn_inside");
  public static final TagKey<BlockType> SNIFFER_DIGGABLE_BLOCK = register("minecraft:sniffer_diggable_block");
  public static final TagKey<BlockType> SNIFFER_EGG_HATCH_BOOST = register("minecraft:sniffer_egg_hatch_boost");
  public static final TagKey<BlockType> TRAIL_RUINS_REPLACEABLE = register("minecraft:trail_ruins_replaceable");
  public static final TagKey<BlockType> REPLACEABLE = register("minecraft:replaceable");
  public static final TagKey<BlockType> ENCHANTMENT_POWER_PROVIDER = register("minecraft:enchantment_power_provider");
  public static final TagKey<BlockType> ENCHANTMENT_POWER_TRANSMITTER = register("minecraft:enchantment_power_transmitter");
  public static final TagKey<BlockType> MAINTAINS_FARMLAND = register("minecraft:maintains_farmland");
  public static final TagKey<BlockType> BLOCKS_WIND_CHARGE_EXPLOSIONS = register("minecraft:blocks_wind_charge_explosions");
  public static final TagKey<BlockType> DOES_NOT_BLOCK_HOPPERS = register("minecraft:does_not_block_hoppers");
  public static final TagKey<BlockType> AIR = register("minecraft:air");
  //@formatter:on

  private BlockTags() {}

  public static TagKey<BlockType> register(@KeyPattern String key) {
    var resourceKey = TagKey.<BlockType>key(key, RegistryKeys.BLOCK);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
