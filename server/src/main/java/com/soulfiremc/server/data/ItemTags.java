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

@SuppressWarnings("unused")
public class ItemTags {
  public static final List<ResourceKey> TAGS = new ArrayList<>();

  //@formatter:off
  public static final ResourceKey WOOL = register("minecraft:wool");
  public static final ResourceKey PLANKS = register("minecraft:planks");
  public static final ResourceKey STONE_BRICKS = register("minecraft:stone_bricks");
  public static final ResourceKey WOODEN_BUTTONS = register("minecraft:wooden_buttons");
  public static final ResourceKey STONE_BUTTONS = register("minecraft:stone_buttons");
  public static final ResourceKey BUTTONS = register("minecraft:buttons");
  public static final ResourceKey WOOL_CARPETS = register("minecraft:wool_carpets");
  public static final ResourceKey WOODEN_DOORS = register("minecraft:wooden_doors");
  public static final ResourceKey WOODEN_STAIRS = register("minecraft:wooden_stairs");
  public static final ResourceKey WOODEN_SLABS = register("minecraft:wooden_slabs");
  public static final ResourceKey WOODEN_FENCES = register("minecraft:wooden_fences");
  public static final ResourceKey FENCE_GATES = register("minecraft:fence_gates");
  public static final ResourceKey WOODEN_PRESSURE_PLATES = register("minecraft:wooden_pressure_plates");
  public static final ResourceKey WOODEN_TRAPDOORS = register("minecraft:wooden_trapdoors");
  public static final ResourceKey DOORS = register("minecraft:doors");
  public static final ResourceKey SAPLINGS = register("minecraft:saplings");
  public static final ResourceKey LOGS_THAT_BURN = register("minecraft:logs_that_burn");
  public static final ResourceKey LOGS = register("minecraft:logs");
  public static final ResourceKey DARK_OAK_LOGS = register("minecraft:dark_oak_logs");
  public static final ResourceKey OAK_LOGS = register("minecraft:oak_logs");
  public static final ResourceKey BIRCH_LOGS = register("minecraft:birch_logs");
  public static final ResourceKey ACACIA_LOGS = register("minecraft:acacia_logs");
  public static final ResourceKey CHERRY_LOGS = register("minecraft:cherry_logs");
  public static final ResourceKey JUNGLE_LOGS = register("minecraft:jungle_logs");
  public static final ResourceKey SPRUCE_LOGS = register("minecraft:spruce_logs");
  public static final ResourceKey MANGROVE_LOGS = register("minecraft:mangrove_logs");
  public static final ResourceKey CRIMSON_STEMS = register("minecraft:crimson_stems");
  public static final ResourceKey WARPED_STEMS = register("minecraft:warped_stems");
  public static final ResourceKey BAMBOO_BLOCKS = register("minecraft:bamboo_blocks");
  public static final ResourceKey WART_BLOCKS = register("minecraft:wart_blocks");
  public static final ResourceKey BANNERS = register("minecraft:banners");
  public static final ResourceKey SAND = register("minecraft:sand");
  public static final ResourceKey SMELTS_TO_GLASS = register("minecraft:smelts_to_glass");
  public static final ResourceKey STAIRS = register("minecraft:stairs");
  public static final ResourceKey SLABS = register("minecraft:slabs");
  public static final ResourceKey WALLS = register("minecraft:walls");
  public static final ResourceKey ANVIL = register("minecraft:anvil");
  public static final ResourceKey RAILS = register("minecraft:rails");
  public static final ResourceKey LEAVES = register("minecraft:leaves");
  public static final ResourceKey TRAPDOORS = register("minecraft:trapdoors");
  public static final ResourceKey SMALL_FLOWERS = register("minecraft:small_flowers");
  public static final ResourceKey BEDS = register("minecraft:beds");
  public static final ResourceKey FENCES = register("minecraft:fences");
  public static final ResourceKey TALL_FLOWERS = register("minecraft:tall_flowers");
  public static final ResourceKey FLOWERS = register("minecraft:flowers");
  public static final ResourceKey PIGLIN_REPELLENTS = register("minecraft:piglin_repellents");
  public static final ResourceKey PIGLIN_LOVED = register("minecraft:piglin_loved");
  public static final ResourceKey IGNORED_BY_PIGLIN_BABIES = register("minecraft:ignored_by_piglin_babies");
  public static final ResourceKey PIGLIN_FOOD = register("minecraft:piglin_food");
  public static final ResourceKey FOX_FOOD = register("minecraft:fox_food");
  public static final ResourceKey GOLD_ORES = register("minecraft:gold_ores");
  public static final ResourceKey IRON_ORES = register("minecraft:iron_ores");
  public static final ResourceKey DIAMOND_ORES = register("minecraft:diamond_ores");
  public static final ResourceKey REDSTONE_ORES = register("minecraft:redstone_ores");
  public static final ResourceKey LAPIS_ORES = register("minecraft:lapis_ores");
  public static final ResourceKey COAL_ORES = register("minecraft:coal_ores");
  public static final ResourceKey EMERALD_ORES = register("minecraft:emerald_ores");
  public static final ResourceKey COPPER_ORES = register("minecraft:copper_ores");
  public static final ResourceKey NON_FLAMMABLE_WOOD = register("minecraft:non_flammable_wood");
  public static final ResourceKey SOUL_FIRE_BASE_BLOCKS = register("minecraft:soul_fire_base_blocks");
  public static final ResourceKey CANDLES = register("minecraft:candles");
  public static final ResourceKey DIRT = register("minecraft:dirt");
  public static final ResourceKey TERRACOTTA = register("minecraft:terracotta");
  public static final ResourceKey COMPLETES_FIND_TREE_TUTORIAL = register("minecraft:completes_find_tree_tutorial");
  public static final ResourceKey BOATS = register("minecraft:boats");
  public static final ResourceKey CHEST_BOATS = register("minecraft:chest_boats");
  public static final ResourceKey FISHES = register("minecraft:fishes");
  public static final ResourceKey SIGNS = register("minecraft:signs");
  public static final ResourceKey MUSIC_DISCS = register("minecraft:music_discs");
  public static final ResourceKey CREEPER_DROP_MUSIC_DISCS = register("minecraft:creeper_drop_music_discs");
  public static final ResourceKey COALS = register("minecraft:coals");
  public static final ResourceKey ARROWS = register("minecraft:arrows");
  public static final ResourceKey LECTERN_BOOKS = register("minecraft:lectern_books");
  public static final ResourceKey BOOKSHELF_BOOKS = register("minecraft:bookshelf_books");
  public static final ResourceKey BEACON_PAYMENT_ITEMS = register("minecraft:beacon_payment_items");
  public static final ResourceKey STONE_TOOL_MATERIALS = register("minecraft:stone_tool_materials");
  public static final ResourceKey STONE_CRAFTING_MATERIALS = register("minecraft:stone_crafting_materials");
  public static final ResourceKey FREEZE_IMMUNE_WEARABLES = register("minecraft:freeze_immune_wearables");
  public static final ResourceKey AXOLOTL_TEMPT_ITEMS = register("minecraft:axolotl_tempt_items");
  public static final ResourceKey DAMPENS_VIBRATIONS = register("minecraft:dampens_vibrations");
  public static final ResourceKey CLUSTER_MAX_HARVESTABLES = register("minecraft:cluster_max_harvestables");
  public static final ResourceKey COMPASSES = register("minecraft:compasses");
  public static final ResourceKey HANGING_SIGNS = register("minecraft:hanging_signs");
  public static final ResourceKey CREEPER_IGNITERS = register("minecraft:creeper_igniters");
  public static final ResourceKey NOTEBLOCK_TOP_INSTRUMENTS = register("minecraft:noteblock_top_instruments");
  public static final ResourceKey TRIMMABLE_ARMOR = register("minecraft:trimmable_armor");
  public static final ResourceKey TRIM_MATERIALS = register("minecraft:trim_materials");
  public static final ResourceKey TRIM_TEMPLATES = register("minecraft:trim_templates");
  public static final ResourceKey SNIFFER_FOOD = register("minecraft:sniffer_food");
  public static final ResourceKey DECORATED_POT_SHERDS = register("minecraft:decorated_pot_sherds");
  public static final ResourceKey DECORATED_POT_INGREDIENTS = register("minecraft:decorated_pot_ingredients");
  public static final ResourceKey SWORDS = register("minecraft:swords");
  public static final ResourceKey AXES = register("minecraft:axes");
  public static final ResourceKey HOES = register("minecraft:hoes");
  public static final ResourceKey PICKAXES = register("minecraft:pickaxes");
  public static final ResourceKey SHOVELS = register("minecraft:shovels");
  public static final ResourceKey TOOLS = register("minecraft:tools");
  public static final ResourceKey BREAKS_DECORATED_POTS = register("minecraft:breaks_decorated_pots");
  public static final ResourceKey VILLAGER_PLANTABLE_SEEDS = register("minecraft:villager_plantable_seeds");
  //@formatter:on

  private ItemTags() {}

  public static ResourceKey register(String key) {
    var resourceKey = ResourceKey.fromString(key);
    TAGS.add(resourceKey);
    return resourceKey;
  }
}
