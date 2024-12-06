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
public class ItemTags {
  public static final List<TagKey<ItemType>> TAGS = new ArrayList<>();

  //@formatter:off
  public static final TagKey<ItemType> WOOL = register("minecraft:wool");
  public static final TagKey<ItemType> PLANKS = register("minecraft:planks");
  public static final TagKey<ItemType> STONE_BRICKS = register("minecraft:stone_bricks");
  public static final TagKey<ItemType> WOODEN_BUTTONS = register("minecraft:wooden_buttons");
  public static final TagKey<ItemType> STONE_BUTTONS = register("minecraft:stone_buttons");
  public static final TagKey<ItemType> BUTTONS = register("minecraft:buttons");
  public static final TagKey<ItemType> WOOL_CARPETS = register("minecraft:wool_carpets");
  public static final TagKey<ItemType> WOODEN_DOORS = register("minecraft:wooden_doors");
  public static final TagKey<ItemType> WOODEN_STAIRS = register("minecraft:wooden_stairs");
  public static final TagKey<ItemType> WOODEN_SLABS = register("minecraft:wooden_slabs");
  public static final TagKey<ItemType> WOODEN_FENCES = register("minecraft:wooden_fences");
  public static final TagKey<ItemType> FENCE_GATES = register("minecraft:fence_gates");
  public static final TagKey<ItemType> WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates");
  public static final TagKey<ItemType> WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors");
  public static final TagKey<ItemType> DOORS = register("minecraft:doors");
  public static final TagKey<ItemType> SAPLINGS = register("minecraft:saplings");
  public static final TagKey<ItemType> LOGS_THAT_BURN = register("minecraft:logs_that_burn");
  public static final TagKey<ItemType> LOGS = register("minecraft:logs");
  public static final TagKey<ItemType> DARK_OAK_LOGS = register("minecraft:dark_oak_logs");
  public static final TagKey<ItemType> PALE_OAK_LOGS = register("minecraft:pale_oak_logs");
  public static final TagKey<ItemType> OAK_LOGS = register("minecraft:oak_logs");
  public static final TagKey<ItemType> BIRCH_LOGS = register("minecraft:birch_logs");
  public static final TagKey<ItemType> ACACIA_LOGS = register("minecraft:acacia_logs");
  public static final TagKey<ItemType> CHERRY_LOGS = register("minecraft:cherry_logs");
  public static final TagKey<ItemType> JUNGLE_LOGS = register("minecraft:jungle_logs");
  public static final TagKey<ItemType> SPRUCE_LOGS = register("minecraft:spruce_logs");
  public static final TagKey<ItemType> MANGROVE_LOGS = register("minecraft:mangrove_logs");
  public static final TagKey<ItemType> CRIMSON_STEMS = register("minecraft:crimson_stems");
  public static final TagKey<ItemType> WARPED_STEMS = register("minecraft:warped_stems");
  public static final TagKey<ItemType> BAMBOO_BLOCKS = register("minecraft:bamboo_blocks");
  public static final TagKey<ItemType> WART_BLOCKS = register("minecraft:wart_blocks");
  public static final TagKey<ItemType> BANNERS = register("minecraft:banners");
  public static final TagKey<ItemType> SAND = register("minecraft:sand");
  public static final TagKey<ItemType> SMELTS_TO_GLASS = register("minecraft:smelts_to_glass");
  public static final TagKey<ItemType> STAIRS = register("minecraft:stairs");
  public static final TagKey<ItemType> SLABS = register("minecraft:slabs");
  public static final TagKey<ItemType> WALLS = register("minecraft:walls");
  public static final TagKey<ItemType> ANVIL = register("minecraft:anvil");
  public static final TagKey<ItemType> RAILS = register("minecraft:rails");
  public static final TagKey<ItemType> LEAVES = register("minecraft:leaves");
  public static final TagKey<ItemType> TRAPDOORS = register("minecraft:trapdoors");
  public static final TagKey<ItemType> SMALL_FLOWERS = register("minecraft:small_flowers");
  public static final TagKey<ItemType> BEDS = register("minecraft:beds");
  public static final TagKey<ItemType> FENCES = register("minecraft:fences");
  public static final TagKey<ItemType> PIGLIN_REPELLENTS = register("minecraft:piglin_repellents");
  public static final TagKey<ItemType> PIGLIN_LOVED = register("minecraft:piglin_loved");
  public static final TagKey<ItemType> IGNORED_BY_PIGLIN_BABIES = register("minecraft:ignored_by_piglin_babies");
  public static final TagKey<ItemType> PIGLIN_SAFE_ARMOR = register("minecraft:piglin_safe_armor");
  public static final TagKey<ItemType> DUPLICATES_ALLAYS = register("minecraft:duplicates_allays");
  public static final TagKey<ItemType> BREWING_FUEL = register("minecraft:brewing_fuel");
  public static final TagKey<ItemType> SHULKER_BOXES = register("minecraft:shulker_boxes");
  public static final TagKey<ItemType> MEAT = register("minecraft:meat");
  public static final TagKey<ItemType> SNIFFER_FOOD = register("minecraft:sniffer_food");
  public static final TagKey<ItemType> PIGLIN_FOOD = register("minecraft:piglin_food");
  public static final TagKey<ItemType> FOX_FOOD = register("minecraft:fox_food");
  public static final TagKey<ItemType> COW_FOOD = register("minecraft:cow_food");
  public static final TagKey<ItemType> GOAT_FOOD = register("minecraft:goat_food");
  public static final TagKey<ItemType> SHEEP_FOOD = register("minecraft:sheep_food");
  public static final TagKey<ItemType> WOLF_FOOD = register("minecraft:wolf_food");
  public static final TagKey<ItemType> CAT_FOOD = register("minecraft:cat_food");
  public static final TagKey<ItemType> HORSE_FOOD = register("minecraft:horse_food");
  public static final TagKey<ItemType> HORSE_TEMPT_ITEMS = register("minecraft:horse_tempt_items");
  public static final TagKey<ItemType> CAMEL_FOOD = register("minecraft:camel_food");
  public static final TagKey<ItemType> ARMADILLO_FOOD = register("minecraft:armadillo_food");
  public static final TagKey<ItemType> BEE_FOOD = register("minecraft:bee_food");
  public static final TagKey<ItemType> CHICKEN_FOOD = register("minecraft:chicken_food");
  public static final TagKey<ItemType> FROG_FOOD = register("minecraft:frog_food");
  public static final TagKey<ItemType> HOGLIN_FOOD = register("minecraft:hoglin_food");
  public static final TagKey<ItemType> LLAMA_FOOD = register("minecraft:llama_food");
  public static final TagKey<ItemType> LLAMA_TEMPT_ITEMS = register("minecraft:llama_tempt_items");
  public static final TagKey<ItemType> OCELOT_FOOD = register("minecraft:ocelot_food");
  public static final TagKey<ItemType> PANDA_FOOD = register("minecraft:panda_food");
  public static final TagKey<ItemType> PANDA_EATS_FROM_GROUND = register("minecraft:panda_eats_from_ground");
  public static final TagKey<ItemType> PIG_FOOD = register("minecraft:pig_food");
  public static final TagKey<ItemType> RABBIT_FOOD = register("minecraft:rabbit_food");
  public static final TagKey<ItemType> STRIDER_FOOD = register("minecraft:strider_food");
  public static final TagKey<ItemType> STRIDER_TEMPT_ITEMS = register("minecraft:strider_tempt_items");
  public static final TagKey<ItemType> TURTLE_FOOD = register("minecraft:turtle_food");
  public static final TagKey<ItemType> PARROT_FOOD = register("minecraft:parrot_food");
  public static final TagKey<ItemType> PARROT_POISONOUS_FOOD = register("minecraft:parrot_poisonous_food");
  public static final TagKey<ItemType> AXOLOTL_FOOD = register("minecraft:axolotl_food");
  public static final TagKey<ItemType> GOLD_ORES = register("minecraft:gold_ores");
  public static final TagKey<ItemType> IRON_ORES = register("minecraft:iron_ores");
  public static final TagKey<ItemType> DIAMOND_ORES = register("minecraft:diamond_ores");
  public static final TagKey<ItemType> REDSTONE_ORES = register("minecraft:redstone_ores");
  public static final TagKey<ItemType> LAPIS_ORES = register("minecraft:lapis_ores");
  public static final TagKey<ItemType> COAL_ORES = register("minecraft:coal_ores");
  public static final TagKey<ItemType> EMERALD_ORES = register("minecraft:emerald_ores");
  public static final TagKey<ItemType> COPPER_ORES = register("minecraft:copper_ores");
  public static final TagKey<ItemType> NON_FLAMMABLE_WOOD = register("minecraft:non_flammable_wood");
  public static final TagKey<ItemType> SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks");
  public static final TagKey<ItemType> CANDLES = register("minecraft:candles");
  public static final TagKey<ItemType> DIRT = register("minecraft:dirt");
  public static final TagKey<ItemType> TERRACOTTA = register("minecraft:terracotta");
  public static final TagKey<ItemType> COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial");
  public static final TagKey<ItemType> BOATS = register("minecraft:boats");
  public static final TagKey<ItemType> CHEST_BOATS = register("minecraft:chest_boats");
  public static final TagKey<ItemType> FISHES = register("minecraft:fishes");
  public static final TagKey<ItemType> SIGNS = register("minecraft:signs");
  public static final TagKey<ItemType> CREEPER_DROP_MUSIC_DISCS = register("minecraft:creeper_drop_music_discs");
  public static final TagKey<ItemType> COALS = register("minecraft:coals");
  public static final TagKey<ItemType> ARROWS = register("minecraft:arrows");
  public static final TagKey<ItemType> LECTERN_BOOKS = register("minecraft:lectern_books");
  public static final TagKey<ItemType> BOOKSHELF_BOOKS = register("minecraft:bookshelf_books");
  public static final TagKey<ItemType> BEACON_PAYMENT_ITEMS = register("minecraft:beacon_payment_items");
  public static final TagKey<ItemType> WOODEN_TOOL_MATERIALS = register("minecraft:wooden_tool_materials");
  public static final TagKey<ItemType> STONE_TOOL_MATERIALS = register("minecraft:stone_tool_materials");
  public static final TagKey<ItemType> IRON_TOOL_MATERIALS = register("minecraft:iron_tool_materials");
  public static final TagKey<ItemType> GOLD_TOOL_MATERIALS = register("minecraft:gold_tool_materials");
  public static final TagKey<ItemType> DIAMOND_TOOL_MATERIALS = register("minecraft:diamond_tool_materials");
  public static final TagKey<ItemType> NETHERITE_TOOL_MATERIALS = register("minecraft:netherite_tool_materials");
  public static final TagKey<ItemType> REPAIRS_LEATHER_ARMOR = register("minecraft:repairs_leather_armor");
  public static final TagKey<ItemType> REPAIRS_CHAIN_ARMOR = register("minecraft:repairs_chain_armor");
  public static final TagKey<ItemType> REPAIRS_IRON_ARMOR = register("minecraft:repairs_iron_armor");
  public static final TagKey<ItemType> REPAIRS_GOLD_ARMOR = register("minecraft:repairs_gold_armor");
  public static final TagKey<ItemType> REPAIRS_DIAMOND_ARMOR = register("minecraft:repairs_diamond_armor");
  public static final TagKey<ItemType> REPAIRS_NETHERITE_ARMOR = register("minecraft:repairs_netherite_armor");
  public static final TagKey<ItemType> REPAIRS_TURTLE_HELMET = register("minecraft:repairs_turtle_helmet");
  public static final TagKey<ItemType> REPAIRS_WOLF_ARMOR = register("minecraft:repairs_wolf_armor");
  public static final TagKey<ItemType> STONE_CRAFTING_MATERIALS = register("minecraft:stone_crafting_materials");
  public static final TagKey<ItemType> FREEZE_IMMUNE_WEARABLES = register("minecraft:freeze_immune_wearables");
  public static final TagKey<ItemType> DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations");
  public static final TagKey<ItemType> CLUSTER_MAX_HARVESTABLES = register("minecraft:cluster_max_harvestables");
  public static final TagKey<ItemType> COMPASSES = register("minecraft:compasses");
  public static final TagKey<ItemType> HANGING_SIGNS = register("minecraft:hanging_signs");
  public static final TagKey<ItemType> CREEPER_IGNITERS = register("minecraft:creeper_igniters");
  public static final TagKey<ItemType> NOTE_BLOCK_TOP_INSTRUMENTS = register("minecraft:noteblock_top_instruments");
  public static final TagKey<ItemType> FOOT_ARMOR = register("minecraft:foot_armor");
  public static final TagKey<ItemType> LEG_ARMOR = register("minecraft:leg_armor");
  public static final TagKey<ItemType> CHEST_ARMOR = register("minecraft:chest_armor");
  public static final TagKey<ItemType> HEAD_ARMOR = register("minecraft:head_armor");
  public static final TagKey<ItemType> SKULLS = register("minecraft:skulls");
  public static final TagKey<ItemType> TRIMMABLE_ARMOR = register("minecraft:trimmable_armor");
  public static final TagKey<ItemType> TRIM_MATERIALS = register("minecraft:trim_materials");
  public static final TagKey<ItemType> DECORATED_POT_SHERDS = register("minecraft:decorated_pot_sherds");
  public static final TagKey<ItemType> DECORATED_POT_INGREDIENTS = register("minecraft:decorated_pot_ingredients");
  public static final TagKey<ItemType> SWORDS = register("minecraft:swords");
  public static final TagKey<ItemType> AXES = register("minecraft:axes");
  public static final TagKey<ItemType> HOES = register("minecraft:hoes");
  public static final TagKey<ItemType> PICKAXES = register("minecraft:pickaxes");
  public static final TagKey<ItemType> SHOVELS = register("minecraft:shovels");
  public static final TagKey<ItemType> BREAKS_DECORATED_POTS = register("minecraft:breaks_decorated_pots");
  public static final TagKey<ItemType> VILLAGER_PLANTABLE_SEEDS = register("minecraft:villager_plantable_seeds");
  public static final TagKey<ItemType> VILLAGER_PICKS_UP = register("minecraft:villager_picks_up");
  public static final TagKey<ItemType> DYEABLE = register("minecraft:dyeable");
  public static final TagKey<ItemType> FURNACE_MINECART_FUEL = register("minecraft:furnace_minecart_fuel");
  public static final TagKey<ItemType> BUNDLES = register("minecraft:bundles");
  public static final TagKey<ItemType> SKELETON_PREFERRED_WEAPONS = register("minecraft:skeleton_preferred_weapons");
  public static final TagKey<ItemType> DROWNED_PREFERRED_WEAPONS = register("minecraft:drowned_preferred_weapons");
  public static final TagKey<ItemType> PIGLIN_PREFERRED_WEAPONS = register("minecraft:piglin_preferred_weapons");
  public static final TagKey<ItemType> PILLAGER_PREFERRED_WEAPONS = register("minecraft:pillager_preferred_weapons");
  public static final TagKey<ItemType> WITHER_SKELETON_DISLIKED_WEAPONS = register("minecraft:wither_skeleton_disliked_weapons");
  public static final TagKey<ItemType> FOOT_ARMOR_ENCHANTABLE = register("minecraft:enchantable/foot_armor");
  public static final TagKey<ItemType> LEG_ARMOR_ENCHANTABLE = register("minecraft:enchantable/leg_armor");
  public static final TagKey<ItemType> CHEST_ARMOR_ENCHANTABLE = register("minecraft:enchantable/chest_armor");
  public static final TagKey<ItemType> HEAD_ARMOR_ENCHANTABLE = register("minecraft:enchantable/head_armor");
  public static final TagKey<ItemType> ARMOR_ENCHANTABLE = register("minecraft:enchantable/armor");
  public static final TagKey<ItemType> SWORD_ENCHANTABLE = register("minecraft:enchantable/sword");
  public static final TagKey<ItemType> FIRE_ASPECT_ENCHANTABLE = register("minecraft:enchantable/fire_aspect");
  public static final TagKey<ItemType> SHARP_WEAPON_ENCHANTABLE = register("minecraft:enchantable/sharp_weapon");
  public static final TagKey<ItemType> WEAPON_ENCHANTABLE = register("minecraft:enchantable/weapon");
  public static final TagKey<ItemType> MINING_ENCHANTABLE = register("minecraft:enchantable/mining");
  public static final TagKey<ItemType> MINING_LOOT_ENCHANTABLE = register("minecraft:enchantable/mining_loot");
  public static final TagKey<ItemType> FISHING_ENCHANTABLE = register("minecraft:enchantable/fishing");
  public static final TagKey<ItemType> TRIDENT_ENCHANTABLE = register("minecraft:enchantable/trident");
  public static final TagKey<ItemType> DURABILITY_ENCHANTABLE = register("minecraft:enchantable/durability");
  public static final TagKey<ItemType> BOW_ENCHANTABLE = register("minecraft:enchantable/bow");
  public static final TagKey<ItemType> EQUIPPABLE_ENCHANTABLE = register("minecraft:enchantable/equippable");
  public static final TagKey<ItemType> CROSSBOW_ENCHANTABLE = register("minecraft:enchantable/crossbow");
  public static final TagKey<ItemType> VANISHING_ENCHANTABLE = register("minecraft:enchantable/vanishing");
  public static final TagKey<ItemType> MACE_ENCHANTABLE = register("minecraft:enchantable/mace");
  public static final TagKey<ItemType> MAP_INVISIBILITY_EQUIPMENT = register("minecraft:map_invisibility_equipment");
  public static final TagKey<ItemType> GAZE_DISGUISE_EQUIPMENT = register("minecraft:gaze_disguise_equipment");
  //@formatter:on

  private ItemTags() {}

  public static TagKey<ItemType> register(@KeyPattern String key) {
    var resourceKey = TagKey.<ItemType>key(key, RegistryKeys.ITEM);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
