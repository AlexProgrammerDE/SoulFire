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
  public static final Key FENCE_GATES = register("minecraft:fence_gates");
  public static final Key WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates");
  public static final Key WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors");
  public static final Key DOORS = register("minecraft:doors");
  public static final Key SAPLINGS = register("minecraft:saplings");
  public static final Key LOGS_THAT_BURN = register("minecraft:logs_that_burn");
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
  public static final Key PIGLIN_LOVED = register("minecraft:piglin_loved");
  public static final Key IGNORED_BY_PIGLIN_BABIES = register("minecraft:ignored_by_piglin_babies");
  public static final Key MEAT = register("minecraft:meat");
  public static final Key SNIFFER_FOOD = register("minecraft:sniffer_food");
  public static final Key PIGLIN_FOOD = register("minecraft:piglin_food");
  public static final Key FOX_FOOD = register("minecraft:fox_food");
  public static final Key COW_FOOD = register("minecraft:cow_food");
  public static final Key GOAT_FOOD = register("minecraft:goat_food");
  public static final Key SHEEP_FOOD = register("minecraft:sheep_food");
  public static final Key WOLF_FOOD = register("minecraft:wolf_food");
  public static final Key CAT_FOOD = register("minecraft:cat_food");
  public static final Key HORSE_FOOD = register("minecraft:horse_food");
  public static final Key HORSE_TEMPT_ITEMS = register("minecraft:horse_tempt_items");
  public static final Key CAMEL_FOOD = register("minecraft:camel_food");
  public static final Key ARMADILLO_FOOD = register("minecraft:armadillo_food");
  public static final Key BEE_FOOD = register("minecraft:bee_food");
  public static final Key CHICKEN_FOOD = register("minecraft:chicken_food");
  public static final Key FROG_FOOD = register("minecraft:frog_food");
  public static final Key HOGLIN_FOOD = register("minecraft:hoglin_food");
  public static final Key LLAMA_FOOD = register("minecraft:llama_food");
  public static final Key LLAMA_TEMPT_ITEMS = register("minecraft:llama_tempt_items");
  public static final Key OCELOT_FOOD = register("minecraft:ocelot_food");
  public static final Key PANDA_FOOD = register("minecraft:panda_food");
  public static final Key PIG_FOOD = register("minecraft:pig_food");
  public static final Key RABBIT_FOOD = register("minecraft:rabbit_food");
  public static final Key STRIDER_FOOD = register("minecraft:strider_food");
  public static final Key STRIDER_TEMPT_ITEMS = register("minecraft:strider_tempt_items");
  public static final Key TURTLE_FOOD = register("minecraft:turtle_food");
  public static final Key PARROT_FOOD = register("minecraft:parrot_food");
  public static final Key PARROT_POISONOUS_FOOD = register("minecraft:parrot_poisonous_food");
  public static final Key AXOLOTL_FOOD = register("minecraft:axolotl_food");
  public static final Key GOLD_ORES = register("minecraft:gold_ores");
  public static final Key IRON_ORES = register("minecraft:iron_ores");
  public static final Key DIAMOND_ORES = register("minecraft:diamond_ores");
  public static final Key REDSTONE_ORES = register("minecraft:redstone_ores");
  public static final Key LAPIS_ORES = register("minecraft:lapis_ores");
  public static final Key COAL_ORES = register("minecraft:coal_ores");
  public static final Key EMERALD_ORES = register("minecraft:emerald_ores");
  public static final Key COPPER_ORES = register("minecraft:copper_ores");
  public static final Key NON_FLAMMABLE_WOOD = register("minecraft:non_flammable_wood");
  public static final Key SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks");
  public static final Key CANDLES = register("minecraft:candles");
  public static final Key DIRT = register("minecraft:dirt");
  public static final Key TERRACOTTA = register("minecraft:terracotta");
  public static final Key COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial");
  public static final Key BOATS = register("minecraft:boats");
  public static final Key CHEST_BOATS = register("minecraft:chest_boats");
  public static final Key FISHES = register("minecraft:fishes");
  public static final Key SIGNS = register("minecraft:signs");
  public static final Key MUSIC_DISCS = register("minecraft:music_discs");
  public static final Key CREEPER_DROP_MUSIC_DISCS = register("minecraft:creeper_drop_music_discs");
  public static final Key COALS = register("minecraft:coals");
  public static final Key ARROWS = register("minecraft:arrows");
  public static final Key LECTERN_BOOKS = register("minecraft:lectern_books");
  public static final Key BOOKSHELF_BOOKS = register("minecraft:bookshelf_books");
  public static final Key BEACON_PAYMENT_ITEMS = register("minecraft:beacon_payment_items");
  public static final Key STONE_TOOL_MATERIALS = register("minecraft:stone_tool_materials");
  public static final Key STONE_CRAFTING_MATERIALS = register("minecraft:stone_crafting_materials");
  public static final Key FREEZE_IMMUNE_WEARABLES = register("minecraft:freeze_immune_wearables");
  public static final Key DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations");
  public static final Key CLUSTER_MAX_HARVESTABLES = register("minecraft:cluster_max_harvestables");
  public static final Key COMPASSES = register("minecraft:compasses");
  public static final Key HANGING_SIGNS = register("minecraft:hanging_signs");
  public static final Key CREEPER_IGNITERS = register("minecraft:creeper_igniters");
  public static final Key NOTEBLOCK_TOP_INSTRUMENTS = register("minecraft:noteblock_top_instruments");
  public static final Key FOOT_ARMOR = register("minecraft:foot_armor");
  public static final Key LEG_ARMOR = register("minecraft:leg_armor");
  public static final Key CHEST_ARMOR = register("minecraft:chest_armor");
  public static final Key HEAD_ARMOR = register("minecraft:head_armor");
  public static final Key SKULLS = register("minecraft:skulls");
  public static final Key TRIMMABLE_ARMOR = register("minecraft:trimmable_armor");
  public static final Key TRIM_MATERIALS = register("minecraft:trim_materials");
  public static final Key TRIM_TEMPLATES = register("minecraft:trim_templates");
  public static final Key DECORATED_POT_SHERDS = register("minecraft:decorated_pot_sherds");
  public static final Key DECORATED_POT_INGREDIENTS = register("minecraft:decorated_pot_ingredients");
  public static final Key SWORDS = register("minecraft:swords");
  public static final Key AXES = register("minecraft:axes");
  public static final Key HOES = register("minecraft:hoes");
  public static final Key PICKAXES = register("minecraft:pickaxes");
  public static final Key SHOVELS = register("minecraft:shovels");
  public static final Key BREAKS_DECORATED_POTS = register("minecraft:breaks_decorated_pots");
  public static final Key VILLAGER_PLANTABLE_SEEDS = register("minecraft:villager_plantable_seeds");
  public static final Key DYEABLE = register("minecraft:dyeable");
  public static final Key ENCHANTABLE_WITH_FOOT_ARMOR = register("minecraft:enchantable/foot_armor");
  public static final Key ENCHANTABLE_WITH_LEG_ARMOR = register("minecraft:enchantable/leg_armor");
  public static final Key ENCHANTABLE_WITH_CHEST_ARMOR = register("minecraft:enchantable/chest_armor");
  public static final Key ENCHANTABLE_WITH_HEAD_ARMOR = register("minecraft:enchantable/head_armor");
  public static final Key ENCHANTABLE_WITH_ARMOR = register("minecraft:enchantable/armor");
  public static final Key ENCHANTABLE_WITH_SWORD = register("minecraft:enchantable/sword");
  public static final Key ENCHANTABLE_WITH_FIRE_ASPECT = register("minecraft:enchantable/fire_aspect");
  public static final Key ENCHANTABLE_WITH_SHARP_WEAPON = register("minecraft:enchantable/sharp_weapon");
  public static final Key ENCHANTABLE_WITH_WEAPON = register("minecraft:enchantable/weapon");
  public static final Key ENCHANTABLE_WITH_MINING = register("minecraft:enchantable/mining");
  public static final Key ENCHANTABLE_WITH_MINING_LOOT = register("minecraft:enchantable/mining_loot");
  public static final Key ENCHANTABLE_WITH_FISHING = register("minecraft:enchantable/fishing");
  public static final Key ENCHANTABLE_WITH_TRIDENT = register("minecraft:enchantable/trident");
  public static final Key ENCHANTABLE_WITH_DURABILITY = register("minecraft:enchantable/durability");
  public static final Key ENCHANTABLE_WITH_BOW = register("minecraft:enchantable/bow");
  public static final Key ENCHANTABLE_WITH_EQUIPPABLE = register("minecraft:enchantable/equippable");
  public static final Key ENCHANTABLE_WITH_CROSSBOW = register("minecraft:enchantable/crossbow");
  public static final Key ENCHANTABLE_WITH_VANISHING = register("minecraft:enchantable/vanishing");
  public static final Key ENCHANTABLE_WITH_MACE = register("minecraft:enchantable/mace");
  //@formatter:on

  private ItemTags() {}

  public static Key register(@KeyPattern String key) {
    var resourceKey = Key.key(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
