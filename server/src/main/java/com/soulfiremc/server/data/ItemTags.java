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
public class ItemTags {
  public static final List<TagKey<ItemType>> TAGS = new ArrayList<>();

  //@formatter:off
  public static final TagKey<ItemType> WOOL = register("minecraft:wool", TAGS);
  public static final TagKey<ItemType> PLANKS = register("minecraft:planks", TAGS);
  public static final TagKey<ItemType> STONE_BRICKS = register("minecraft:stone_bricks", TAGS);
  public static final TagKey<ItemType> WOODEN_BUTTONS = register("minecraft:wooden_buttons", TAGS);
  public static final TagKey<ItemType> STONE_BUTTONS = register("minecraft:stone_buttons", TAGS);
  public static final TagKey<ItemType> BUTTONS = register("minecraft:buttons", TAGS);
  public static final TagKey<ItemType> WOOL_CARPETS = register("minecraft:wool_carpets", TAGS);
  public static final TagKey<ItemType> WOODEN_DOORS = register("minecraft:wooden_doors", TAGS);
  public static final TagKey<ItemType> WOODEN_STAIRS = register("minecraft:wooden_stairs", TAGS);
  public static final TagKey<ItemType> WOODEN_SLABS = register("minecraft:wooden_slabs", TAGS);
  public static final TagKey<ItemType> WOODEN_FENCES = register("minecraft:wooden_fences", TAGS);
  public static final TagKey<ItemType> FENCE_GATES = register("minecraft:fence_gates", TAGS);
  public static final TagKey<ItemType> WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates", TAGS);
  public static final TagKey<ItemType> WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors", TAGS);
  public static final TagKey<ItemType> DOORS = register("minecraft:doors", TAGS);
  public static final TagKey<ItemType> SAPLINGS = register("minecraft:saplings", TAGS);
  public static final TagKey<ItemType> LOGS_THAT_BURN = register("minecraft:logs_that_burn", TAGS);
  public static final TagKey<ItemType> LOGS = register("minecraft:logs", TAGS);
  public static final TagKey<ItemType> DARK_OAK_LOGS = register("minecraft:dark_oak_logs", TAGS);
  public static final TagKey<ItemType> OAK_LOGS = register("minecraft:oak_logs", TAGS);
  public static final TagKey<ItemType> BIRCH_LOGS = register("minecraft:birch_logs", TAGS);
  public static final TagKey<ItemType> ACACIA_LOGS = register("minecraft:acacia_logs", TAGS);
  public static final TagKey<ItemType> CHERRY_LOGS = register("minecraft:cherry_logs", TAGS);
  public static final TagKey<ItemType> JUNGLE_LOGS = register("minecraft:jungle_logs", TAGS);
  public static final TagKey<ItemType> SPRUCE_LOGS = register("minecraft:spruce_logs", TAGS);
  public static final TagKey<ItemType> MANGROVE_LOGS = register("minecraft:mangrove_logs", TAGS);
  public static final TagKey<ItemType> CRIMSON_STEMS = register("minecraft:crimson_stems", TAGS);
  public static final TagKey<ItemType> WARPED_STEMS = register("minecraft:warped_stems", TAGS);
  public static final TagKey<ItemType> BAMBOO_BLOCKS = register("minecraft:bamboo_blocks", TAGS);
  public static final TagKey<ItemType> WART_BLOCKS = register("minecraft:wart_blocks", TAGS);
  public static final TagKey<ItemType> BANNERS = register("minecraft:banners", TAGS);
  public static final TagKey<ItemType> SAND = register("minecraft:sand", TAGS);
  public static final TagKey<ItemType> SMELTS_TO_GLASS = register("minecraft:smelts_to_glass", TAGS);
  public static final TagKey<ItemType> STAIRS = register("minecraft:stairs", TAGS);
  public static final TagKey<ItemType> SLABS = register("minecraft:slabs", TAGS);
  public static final TagKey<ItemType> WALLS = register("minecraft:walls", TAGS);
  public static final TagKey<ItemType> ANVIL = register("minecraft:anvil", TAGS);
  public static final TagKey<ItemType> RAILS = register("minecraft:rails", TAGS);
  public static final TagKey<ItemType> LEAVES = register("minecraft:leaves", TAGS);
  public static final TagKey<ItemType> TRAPDOORS = register("minecraft:trapdoors", TAGS);
  public static final TagKey<ItemType> SMALL_FLOWERS = register("minecraft:small_flowers", TAGS);
  public static final TagKey<ItemType> BEDS = register("minecraft:beds", TAGS);
  public static final TagKey<ItemType> FENCES = register("minecraft:fences", TAGS);
  public static final TagKey<ItemType> TALL_FLOWERS = register("minecraft:tall_flowers", TAGS);
  public static final TagKey<ItemType> FLOWERS = register("minecraft:flowers", TAGS);
  public static final TagKey<ItemType> PIGLIN_REPELLENTS = register("minecraft:piglin_repellents", TAGS);
  public static final TagKey<ItemType> PIGLIN_LOVED = register("minecraft:piglin_loved", TAGS);
  public static final TagKey<ItemType> IGNORED_BY_PIGLIN_BABIES = register("minecraft:ignored_by_piglin_babies", TAGS);
  public static final TagKey<ItemType> MEAT = register("minecraft:meat", TAGS);
  public static final TagKey<ItemType> SNIFFER_FOOD = register("minecraft:sniffer_food", TAGS);
  public static final TagKey<ItemType> PIGLIN_FOOD = register("minecraft:piglin_food", TAGS);
  public static final TagKey<ItemType> FOX_FOOD = register("minecraft:fox_food", TAGS);
  public static final TagKey<ItemType> COW_FOOD = register("minecraft:cow_food", TAGS);
  public static final TagKey<ItemType> GOAT_FOOD = register("minecraft:goat_food", TAGS);
  public static final TagKey<ItemType> SHEEP_FOOD = register("minecraft:sheep_food", TAGS);
  public static final TagKey<ItemType> WOLF_FOOD = register("minecraft:wolf_food", TAGS);
  public static final TagKey<ItemType> CAT_FOOD = register("minecraft:cat_food", TAGS);
  public static final TagKey<ItemType> HORSE_FOOD = register("minecraft:horse_food", TAGS);
  public static final TagKey<ItemType> HORSE_TEMPT_ITEMS = register("minecraft:horse_tempt_items", TAGS);
  public static final TagKey<ItemType> CAMEL_FOOD = register("minecraft:camel_food", TAGS);
  public static final TagKey<ItemType> ARMADILLO_FOOD = register("minecraft:armadillo_food", TAGS);
  public static final TagKey<ItemType> BEE_FOOD = register("minecraft:bee_food", TAGS);
  public static final TagKey<ItemType> CHICKEN_FOOD = register("minecraft:chicken_food", TAGS);
  public static final TagKey<ItemType> FROG_FOOD = register("minecraft:frog_food", TAGS);
  public static final TagKey<ItemType> HOGLIN_FOOD = register("minecraft:hoglin_food", TAGS);
  public static final TagKey<ItemType> LLAMA_FOOD = register("minecraft:llama_food", TAGS);
  public static final TagKey<ItemType> LLAMA_TEMPT_ITEMS = register("minecraft:llama_tempt_items", TAGS);
  public static final TagKey<ItemType> OCELOT_FOOD = register("minecraft:ocelot_food", TAGS);
  public static final TagKey<ItemType> PANDA_FOOD = register("minecraft:panda_food", TAGS);
  public static final TagKey<ItemType> PIG_FOOD = register("minecraft:pig_food", TAGS);
  public static final TagKey<ItemType> RABBIT_FOOD = register("minecraft:rabbit_food", TAGS);
  public static final TagKey<ItemType> STRIDER_FOOD = register("minecraft:strider_food", TAGS);
  public static final TagKey<ItemType> STRIDER_TEMPT_ITEMS = register("minecraft:strider_tempt_items", TAGS);
  public static final TagKey<ItemType> TURTLE_FOOD = register("minecraft:turtle_food", TAGS);
  public static final TagKey<ItemType> PARROT_FOOD = register("minecraft:parrot_food", TAGS);
  public static final TagKey<ItemType> PARROT_POISONOUS_FOOD = register("minecraft:parrot_poisonous_food", TAGS);
  public static final TagKey<ItemType> AXOLOTL_FOOD = register("minecraft:axolotl_food", TAGS);
  public static final TagKey<ItemType> GOLD_ORES = register("minecraft:gold_ores", TAGS);
  public static final TagKey<ItemType> IRON_ORES = register("minecraft:iron_ores", TAGS);
  public static final TagKey<ItemType> DIAMOND_ORES = register("minecraft:diamond_ores", TAGS);
  public static final TagKey<ItemType> REDSTONE_ORES = register("minecraft:redstone_ores", TAGS);
  public static final TagKey<ItemType> LAPIS_ORES = register("minecraft:lapis_ores", TAGS);
  public static final TagKey<ItemType> COAL_ORES = register("minecraft:coal_ores", TAGS);
  public static final TagKey<ItemType> EMERALD_ORES = register("minecraft:emerald_ores", TAGS);
  public static final TagKey<ItemType> COPPER_ORES = register("minecraft:copper_ores", TAGS);
  public static final TagKey<ItemType> NON_FLAMMABLE_WOOD = register("minecraft:non_flammable_wood", TAGS);
  public static final TagKey<ItemType> SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks", TAGS);
  public static final TagKey<ItemType> CANDLES = register("minecraft:candles", TAGS);
  public static final TagKey<ItemType> DIRT = register("minecraft:dirt", TAGS);
  public static final TagKey<ItemType> TERRACOTTA = register("minecraft:terracotta", TAGS);
  public static final TagKey<ItemType> COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial", TAGS);
  public static final TagKey<ItemType> BOATS = register("minecraft:boats", TAGS);
  public static final TagKey<ItemType> CHEST_BOATS = register("minecraft:chest_boats", TAGS);
  public static final TagKey<ItemType> FISHES = register("minecraft:fishes", TAGS);
  public static final TagKey<ItemType> SIGNS = register("minecraft:signs", TAGS);
  public static final TagKey<ItemType> MUSIC_DISCS = register("minecraft:music_discs", TAGS);
  public static final TagKey<ItemType> CREEPER_DROP_MUSIC_DISCS = register("minecraft:creeper_drop_music_discs", TAGS);
  public static final TagKey<ItemType> COALS = register("minecraft:coals", TAGS);
  public static final TagKey<ItemType> ARROWS = register("minecraft:arrows", TAGS);
  public static final TagKey<ItemType> LECTERN_BOOKS = register("minecraft:lectern_books", TAGS);
  public static final TagKey<ItemType> BOOKSHELF_BOOKS = register("minecraft:bookshelf_books", TAGS);
  public static final TagKey<ItemType> BEACON_PAYMENT_ITEMS = register("minecraft:beacon_payment_items", TAGS);
  public static final TagKey<ItemType> STONE_TOOL_MATERIALS = register("minecraft:stone_tool_materials", TAGS);
  public static final TagKey<ItemType> STONE_CRAFTING_MATERIALS = register("minecraft:stone_crafting_materials", TAGS);
  public static final TagKey<ItemType> FREEZE_IMMUNE_WEARABLES = register("minecraft:freeze_immune_wearables", TAGS);
  public static final TagKey<ItemType> DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations", TAGS);
  public static final TagKey<ItemType> CLUSTER_MAX_HARVESTABLES = register("minecraft:cluster_max_harvestables", TAGS);
  public static final TagKey<ItemType> COMPASSES = register("minecraft:compasses", TAGS);
  public static final TagKey<ItemType> HANGING_SIGNS = register("minecraft:hanging_signs", TAGS);
  public static final TagKey<ItemType> CREEPER_IGNITERS = register("minecraft:creeper_igniters", TAGS);
  public static final TagKey<ItemType> NOTEBLOCK_TOP_INSTRUMENTS = register("minecraft:noteblock_top_instruments", TAGS);
  public static final TagKey<ItemType> FOOT_ARMOR = register("minecraft:foot_armor", TAGS);
  public static final TagKey<ItemType> LEG_ARMOR = register("minecraft:leg_armor", TAGS);
  public static final TagKey<ItemType> CHEST_ARMOR = register("minecraft:chest_armor", TAGS);
  public static final TagKey<ItemType> HEAD_ARMOR = register("minecraft:head_armor", TAGS);
  public static final TagKey<ItemType> SKULLS = register("minecraft:skulls", TAGS);
  public static final TagKey<ItemType> TRIMMABLE_ARMOR = register("minecraft:trimmable_armor", TAGS);
  public static final TagKey<ItemType> TRIM_MATERIALS = register("minecraft:trim_materials", TAGS);
  public static final TagKey<ItemType> TRIM_TEMPLATES = register("minecraft:trim_templates", TAGS);
  public static final TagKey<ItemType> DECORATED_POT_SHERDS = register("minecraft:decorated_pot_sherds", TAGS);
  public static final TagKey<ItemType> DECORATED_POT_INGREDIENTS = register("minecraft:decorated_pot_ingredients", TAGS);
  public static final TagKey<ItemType> SWORDS = register("minecraft:swords", TAGS);
  public static final TagKey<ItemType> AXES = register("minecraft:axes", TAGS);
  public static final TagKey<ItemType> HOES = register("minecraft:hoes", TAGS);
  public static final TagKey<ItemType> PICKAXES = register("minecraft:pickaxes", TAGS);
  public static final TagKey<ItemType> SHOVELS = register("minecraft:shovels", TAGS);
  public static final TagKey<ItemType> BREAKS_DECORATED_POTS = register("minecraft:breaks_decorated_pots", TAGS);
  public static final TagKey<ItemType> VILLAGER_PLANTABLE_SEEDS = register("minecraft:villager_plantable_seeds", TAGS);
  public static final TagKey<ItemType> DYEABLE = register("minecraft:dyeable", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_FOOT_ARMOR = register("minecraft:enchantable/foot_armor", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_LEG_ARMOR = register("minecraft:enchantable/leg_armor", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_CHEST_ARMOR = register("minecraft:enchantable/chest_armor", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_HEAD_ARMOR = register("minecraft:enchantable/head_armor", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_ARMOR = register("minecraft:enchantable/armor", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_SWORD = register("minecraft:enchantable/sword", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_FIRE_ASPECT = register("minecraft:enchantable/fire_aspect", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_SHARP_WEAPON = register("minecraft:enchantable/sharp_weapon", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_WEAPON = register("minecraft:enchantable/weapon", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_MINING = register("minecraft:enchantable/mining", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_MINING_LOOT = register("minecraft:enchantable/mining_loot", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_FISHING = register("minecraft:enchantable/fishing", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_TRIDENT = register("minecraft:enchantable/trident", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_DURABILITY = register("minecraft:enchantable/durability", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_BOW = register("minecraft:enchantable/bow", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_EQUIPPABLE = register("minecraft:enchantable/equippable", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_CROSSBOW = register("minecraft:enchantable/crossbow", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_VANISHING = register("minecraft:enchantable/vanishing", TAGS);
  public static final TagKey<ItemType> ENCHANTABLE_WITH_MACE = register("minecraft:enchantable/mace", TAGS);
  //@formatter:on

  private ItemTags() {}

  public static <T extends RegistryValue<T>> TagKey<T> register(@KeyPattern String key, List<TagKey<T>> values) {
    var resourceKey = TagKey.<T>key(key, RegistryKeys.ITEM);
    values.add(resourceKey);
    return resourceKey;
  }
}
