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
public class ItemTags {
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
  public static final Key FENCE_GATES = register(Key.key("minecraft:fence_gates"));
  public static final Key WOODEN_PRESSURE_PLATES = register(Key.key("minecraft:wooden_pressure_plates"));
  public static final Key WOODEN_TRAPDOORS = register(Key.key("minecraft:wooden_trapdoors"));
  public static final Key DOORS = register(Key.key("minecraft:doors"));
  public static final Key SAPLINGS = register(Key.key("minecraft:saplings"));
  public static final Key LOGS_THAT_BURN = register(Key.key("minecraft:logs_that_burn"));
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
  public static final Key PIGLIN_LOVED = register(Key.key("minecraft:piglin_loved"));
  public static final Key IGNORED_BY_PIGLIN_BABIES = register(Key.key("minecraft:ignored_by_piglin_babies"));
  public static final Key MEAT = register(Key.key("minecraft:meat"));
  public static final Key SNIFFER_FOOD = register(Key.key("minecraft:sniffer_food"));
  public static final Key PIGLIN_FOOD = register(Key.key("minecraft:piglin_food"));
  public static final Key FOX_FOOD = register(Key.key("minecraft:fox_food"));
  public static final Key COW_FOOD = register(Key.key("minecraft:cow_food"));
  public static final Key GOAT_FOOD = register(Key.key("minecraft:goat_food"));
  public static final Key SHEEP_FOOD = register(Key.key("minecraft:sheep_food"));
  public static final Key WOLF_FOOD = register(Key.key("minecraft:wolf_food"));
  public static final Key CAT_FOOD = register(Key.key("minecraft:cat_food"));
  public static final Key HORSE_FOOD = register(Key.key("minecraft:horse_food"));
  public static final Key HORSE_TEMPT_ITEMS = register(Key.key("minecraft:horse_tempt_items"));
  public static final Key CAMEL_FOOD = register(Key.key("minecraft:camel_food"));
  public static final Key ARMADILLO_FOOD = register(Key.key("minecraft:armadillo_food"));
  public static final Key BEE_FOOD = register(Key.key("minecraft:bee_food"));
  public static final Key CHICKEN_FOOD = register(Key.key("minecraft:chicken_food"));
  public static final Key FROG_FOOD = register(Key.key("minecraft:frog_food"));
  public static final Key HOGLIN_FOOD = register(Key.key("minecraft:hoglin_food"));
  public static final Key LLAMA_FOOD = register(Key.key("minecraft:llama_food"));
  public static final Key LLAMA_TEMPT_ITEMS = register(Key.key("minecraft:llama_tempt_items"));
  public static final Key OCELOT_FOOD = register(Key.key("minecraft:ocelot_food"));
  public static final Key PANDA_FOOD = register(Key.key("minecraft:panda_food"));
  public static final Key PIG_FOOD = register(Key.key("minecraft:pig_food"));
  public static final Key RABBIT_FOOD = register(Key.key("minecraft:rabbit_food"));
  public static final Key STRIDER_FOOD = register(Key.key("minecraft:strider_food"));
  public static final Key STRIDER_TEMPT_ITEMS = register(Key.key("minecraft:strider_tempt_items"));
  public static final Key TURTLE_FOOD = register(Key.key("minecraft:turtle_food"));
  public static final Key PARROT_FOOD = register(Key.key("minecraft:parrot_food"));
  public static final Key PARROT_POISONOUS_FOOD = register(Key.key("minecraft:parrot_poisonous_food"));
  public static final Key AXOLOTL_FOOD = register(Key.key("minecraft:axolotl_food"));
  public static final Key GOLD_ORES = register(Key.key("minecraft:gold_ores"));
  public static final Key IRON_ORES = register(Key.key("minecraft:iron_ores"));
  public static final Key DIAMOND_ORES = register(Key.key("minecraft:diamond_ores"));
  public static final Key REDSTONE_ORES = register(Key.key("minecraft:redstone_ores"));
  public static final Key LAPIS_ORES = register(Key.key("minecraft:lapis_ores"));
  public static final Key COAL_ORES = register(Key.key("minecraft:coal_ores"));
  public static final Key EMERALD_ORES = register(Key.key("minecraft:emerald_ores"));
  public static final Key COPPER_ORES = register(Key.key("minecraft:copper_ores"));
  public static final Key NON_FLAMMABLE_WOOD = register(Key.key("minecraft:non_flammable_wood"));
  public static final Key SOUL_FIRE_BASE_BLOCKS = register(Key.key("minecraft:soul_fire_base_blocks"));
  public static final Key CANDLES = register(Key.key("minecraft:candles"));
  public static final Key DIRT = register(Key.key("minecraft:dirt"));
  public static final Key TERRACOTTA = register(Key.key("minecraft:terracotta"));
  public static final Key COMPLETES_FIND_TREE_TUTORIAL = register(Key.key("minecraft:completes_find_tree_tutorial"));
  public static final Key BOATS = register(Key.key("minecraft:boats"));
  public static final Key CHEST_BOATS = register(Key.key("minecraft:chest_boats"));
  public static final Key FISHES = register(Key.key("minecraft:fishes"));
  public static final Key SIGNS = register(Key.key("minecraft:signs"));
  public static final Key MUSIC_DISCS = register(Key.key("minecraft:music_discs"));
  public static final Key CREEPER_DROP_MUSIC_DISCS = register(Key.key("minecraft:creeper_drop_music_discs"));
  public static final Key COALS = register(Key.key("minecraft:coals"));
  public static final Key ARROWS = register(Key.key("minecraft:arrows"));
  public static final Key LECTERN_BOOKS = register(Key.key("minecraft:lectern_books"));
  public static final Key BOOKSHELF_BOOKS = register(Key.key("minecraft:bookshelf_books"));
  public static final Key BEACON_PAYMENT_ITEMS = register(Key.key("minecraft:beacon_payment_items"));
  public static final Key STONE_TOOL_MATERIALS = register(Key.key("minecraft:stone_tool_materials"));
  public static final Key STONE_CRAFTING_MATERIALS = register(Key.key("minecraft:stone_crafting_materials"));
  public static final Key FREEZE_IMMUNE_WEARABLES = register(Key.key("minecraft:freeze_immune_wearables"));
  public static final Key DAMPENS_VIBRATIONS = register(Key.key("minecraft:dampens_vibrations"));
  public static final Key CLUSTER_MAX_HARVESTABLES = register(Key.key("minecraft:cluster_max_harvestables"));
  public static final Key COMPASSES = register(Key.key("minecraft:compasses"));
  public static final Key HANGING_SIGNS = register(Key.key("minecraft:hanging_signs"));
  public static final Key CREEPER_IGNITERS = register(Key.key("minecraft:creeper_igniters"));
  public static final Key NOTEBLOCK_TOP_INSTRUMENTS = register(Key.key("minecraft:noteblock_top_instruments"));
  public static final Key FOOT_ARMOR = register(Key.key("minecraft:foot_armor"));
  public static final Key LEG_ARMOR = register(Key.key("minecraft:leg_armor"));
  public static final Key CHEST_ARMOR = register(Key.key("minecraft:chest_armor"));
  public static final Key HEAD_ARMOR = register(Key.key("minecraft:head_armor"));
  public static final Key SKULLS = register(Key.key("minecraft:skulls"));
  public static final Key TRIMMABLE_ARMOR = register(Key.key("minecraft:trimmable_armor"));
  public static final Key TRIM_MATERIALS = register(Key.key("minecraft:trim_materials"));
  public static final Key TRIM_TEMPLATES = register(Key.key("minecraft:trim_templates"));
  public static final Key DECORATED_POT_SHERDS = register(Key.key("minecraft:decorated_pot_sherds"));
  public static final Key DECORATED_POT_INGREDIENTS = register(Key.key("minecraft:decorated_pot_ingredients"));
  public static final Key SWORDS = register(Key.key("minecraft:swords"));
  public static final Key AXES = register(Key.key("minecraft:axes"));
  public static final Key HOES = register(Key.key("minecraft:hoes"));
  public static final Key PICKAXES = register(Key.key("minecraft:pickaxes"));
  public static final Key SHOVELS = register(Key.key("minecraft:shovels"));
  public static final Key BREAKS_DECORATED_POTS = register(Key.key("minecraft:breaks_decorated_pots"));
  public static final Key VILLAGER_PLANTABLE_SEEDS = register(Key.key("minecraft:villager_plantable_seeds"));
  public static final Key DYEABLE = register(Key.key("minecraft:dyeable"));
  public static final Key ENCHANTABLE_WITH_FOOT_ARMOR = register(Key.key("minecraft:enchantable/foot_armor"));
  public static final Key ENCHANTABLE_WITH_LEG_ARMOR = register(Key.key("minecraft:enchantable/leg_armor"));
  public static final Key ENCHANTABLE_WITH_CHEST_ARMOR = register(Key.key("minecraft:enchantable/chest_armor"));
  public static final Key ENCHANTABLE_WITH_HEAD_ARMOR = register(Key.key("minecraft:enchantable/head_armor"));
  public static final Key ENCHANTABLE_WITH_ARMOR = register(Key.key("minecraft:enchantable/armor"));
  public static final Key ENCHANTABLE_WITH_SWORD = register(Key.key("minecraft:enchantable/sword"));
  public static final Key ENCHANTABLE_WITH_FIRE_ASPECT = register(Key.key("minecraft:enchantable/fire_aspect"));
  public static final Key ENCHANTABLE_WITH_SHARP_WEAPON = register(Key.key("minecraft:enchantable/sharp_weapon"));
  public static final Key ENCHANTABLE_WITH_WEAPON = register(Key.key("minecraft:enchantable/weapon"));
  public static final Key ENCHANTABLE_WITH_MINING = register(Key.key("minecraft:enchantable/mining"));
  public static final Key ENCHANTABLE_WITH_MINING_LOOT = register(Key.key("minecraft:enchantable/mining_loot"));
  public static final Key ENCHANTABLE_WITH_FISHING = register(Key.key("minecraft:enchantable/fishing"));
  public static final Key ENCHANTABLE_WITH_TRIDENT = register(Key.key("minecraft:enchantable/trident"));
  public static final Key ENCHANTABLE_WITH_DURABILITY = register(Key.key("minecraft:enchantable/durability"));
  public static final Key ENCHANTABLE_WITH_BOW = register(Key.key("minecraft:enchantable/bow"));
  public static final Key ENCHANTABLE_WITH_EQUIPPABLE = register(Key.key("minecraft:enchantable/equippable"));
  public static final Key ENCHANTABLE_WITH_CROSSBOW = register(Key.key("minecraft:enchantable/crossbow"));
  public static final Key ENCHANTABLE_WITH_VANISHING = register(Key.key("minecraft:enchantable/vanishing"));
  public static final Key ENCHANTABLE_WITH_MACE = register(Key.key("minecraft:enchantable/mace"));
  //@formatter:on

  private ItemTags() {}

  public static Key register(@KeyPattern String key) {
    var resourceKey = Key.key(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
