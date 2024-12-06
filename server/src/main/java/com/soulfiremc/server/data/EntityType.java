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

import net.kyori.adventure.key.Key;

import java.util.List;

@SuppressWarnings("unused")
public record EntityType(
  int id,
  Key key,
  EntityDimensions dimensions,
  int updateInterval,
  int clientTrackingRange,
  String category,
  boolean friendly,
  boolean summonable,
  boolean fireImmune,
  boolean attackable,
  double defaultFollowRange,
  boolean playerEntity,
  boolean livingEntity,
  boolean boatEntity,
  boolean minecartEntity,
  boolean windChargeEntity,
  boolean shulkerEntity,
  List<String> inheritedClasses,
  String defaultEntityMetadata) implements RegistryValue<EntityType> {
  public static final Registry<EntityType> REGISTRY = new Registry<>(RegistryKeys.ENTITY_TYPE);

  //@formatter:off
  public static final EntityType ACACIA_BOAT = register("minecraft:acacia_boat");
  public static final EntityType ACACIA_CHEST_BOAT = register("minecraft:acacia_chest_boat");
  public static final EntityType ALLAY = register("minecraft:allay");
  public static final EntityType AREA_EFFECT_CLOUD = register("minecraft:area_effect_cloud");
  public static final EntityType ARMADILLO = register("minecraft:armadillo");
  public static final EntityType ARMOR_STAND = register("minecraft:armor_stand");
  public static final EntityType ARROW = register("minecraft:arrow");
  public static final EntityType AXOLOTL = register("minecraft:axolotl");
  public static final EntityType BAMBOO_CHEST_RAFT = register("minecraft:bamboo_chest_raft");
  public static final EntityType BAMBOO_RAFT = register("minecraft:bamboo_raft");
  public static final EntityType BAT = register("minecraft:bat");
  public static final EntityType BEE = register("minecraft:bee");
  public static final EntityType BIRCH_BOAT = register("minecraft:birch_boat");
  public static final EntityType BIRCH_CHEST_BOAT = register("minecraft:birch_chest_boat");
  public static final EntityType BLAZE = register("minecraft:blaze");
  public static final EntityType BLOCK_DISPLAY = register("minecraft:block_display");
  public static final EntityType BOGGED = register("minecraft:bogged");
  public static final EntityType BREEZE = register("minecraft:breeze");
  public static final EntityType BREEZE_WIND_CHARGE = register("minecraft:breeze_wind_charge");
  public static final EntityType CAMEL = register("minecraft:camel");
  public static final EntityType CAT = register("minecraft:cat");
  public static final EntityType CAVE_SPIDER = register("minecraft:cave_spider");
  public static final EntityType CHERRY_BOAT = register("minecraft:cherry_boat");
  public static final EntityType CHERRY_CHEST_BOAT = register("minecraft:cherry_chest_boat");
  public static final EntityType CHEST_MINECART = register("minecraft:chest_minecart");
  public static final EntityType CHICKEN = register("minecraft:chicken");
  public static final EntityType COD = register("minecraft:cod");
  public static final EntityType COMMAND_BLOCK_MINECART = register("minecraft:command_block_minecart");
  public static final EntityType COW = register("minecraft:cow");
  public static final EntityType CREAKING = register("minecraft:creaking");
  public static final EntityType CREEPER = register("minecraft:creeper");
  public static final EntityType DARK_OAK_BOAT = register("minecraft:dark_oak_boat");
  public static final EntityType DARK_OAK_CHEST_BOAT = register("minecraft:dark_oak_chest_boat");
  public static final EntityType DOLPHIN = register("minecraft:dolphin");
  public static final EntityType DONKEY = register("minecraft:donkey");
  public static final EntityType DRAGON_FIREBALL = register("minecraft:dragon_fireball");
  public static final EntityType DROWNED = register("minecraft:drowned");
  public static final EntityType EGG = register("minecraft:egg");
  public static final EntityType ELDER_GUARDIAN = register("minecraft:elder_guardian");
  public static final EntityType ENDERMAN = register("minecraft:enderman");
  public static final EntityType ENDERMITE = register("minecraft:endermite");
  public static final EntityType ENDER_DRAGON = register("minecraft:ender_dragon");
  public static final EntityType ENDER_PEARL = register("minecraft:ender_pearl");
  public static final EntityType END_CRYSTAL = register("minecraft:end_crystal");
  public static final EntityType EVOKER = register("minecraft:evoker");
  public static final EntityType EVOKER_FANGS = register("minecraft:evoker_fangs");
  public static final EntityType EXPERIENCE_BOTTLE = register("minecraft:experience_bottle");
  public static final EntityType EXPERIENCE_ORB = register("minecraft:experience_orb");
  public static final EntityType EYE_OF_ENDER = register("minecraft:eye_of_ender");
  public static final EntityType FALLING_BLOCK = register("minecraft:falling_block");
  public static final EntityType FIREBALL = register("minecraft:fireball");
  public static final EntityType FIREWORK_ROCKET = register("minecraft:firework_rocket");
  public static final EntityType FOX = register("minecraft:fox");
  public static final EntityType FROG = register("minecraft:frog");
  public static final EntityType FURNACE_MINECART = register("minecraft:furnace_minecart");
  public static final EntityType GHAST = register("minecraft:ghast");
  public static final EntityType GIANT = register("minecraft:giant");
  public static final EntityType GLOW_ITEM_FRAME = register("minecraft:glow_item_frame");
  public static final EntityType GLOW_SQUID = register("minecraft:glow_squid");
  public static final EntityType GOAT = register("minecraft:goat");
  public static final EntityType GUARDIAN = register("minecraft:guardian");
  public static final EntityType HOGLIN = register("minecraft:hoglin");
  public static final EntityType HOPPER_MINECART = register("minecraft:hopper_minecart");
  public static final EntityType HORSE = register("minecraft:horse");
  public static final EntityType HUSK = register("minecraft:husk");
  public static final EntityType ILLUSIONER = register("minecraft:illusioner");
  public static final EntityType INTERACTION = register("minecraft:interaction");
  public static final EntityType IRON_GOLEM = register("minecraft:iron_golem");
  public static final EntityType ITEM = register("minecraft:item");
  public static final EntityType ITEM_DISPLAY = register("minecraft:item_display");
  public static final EntityType ITEM_FRAME = register("minecraft:item_frame");
  public static final EntityType JUNGLE_BOAT = register("minecraft:jungle_boat");
  public static final EntityType JUNGLE_CHEST_BOAT = register("minecraft:jungle_chest_boat");
  public static final EntityType LEASH_KNOT = register("minecraft:leash_knot");
  public static final EntityType LIGHTNING_BOLT = register("minecraft:lightning_bolt");
  public static final EntityType LLAMA = register("minecraft:llama");
  public static final EntityType LLAMA_SPIT = register("minecraft:llama_spit");
  public static final EntityType MAGMA_CUBE = register("minecraft:magma_cube");
  public static final EntityType MANGROVE_BOAT = register("minecraft:mangrove_boat");
  public static final EntityType MANGROVE_CHEST_BOAT = register("minecraft:mangrove_chest_boat");
  public static final EntityType MARKER = register("minecraft:marker");
  public static final EntityType MINECART = register("minecraft:minecart");
  public static final EntityType MOOSHROOM = register("minecraft:mooshroom");
  public static final EntityType MULE = register("minecraft:mule");
  public static final EntityType OAK_BOAT = register("minecraft:oak_boat");
  public static final EntityType OAK_CHEST_BOAT = register("minecraft:oak_chest_boat");
  public static final EntityType OCELOT = register("minecraft:ocelot");
  public static final EntityType OMINOUS_ITEM_SPAWNER = register("minecraft:ominous_item_spawner");
  public static final EntityType PAINTING = register("minecraft:painting");
  public static final EntityType PALE_OAK_BOAT = register("minecraft:pale_oak_boat");
  public static final EntityType PALE_OAK_CHEST_BOAT = register("minecraft:pale_oak_chest_boat");
  public static final EntityType PANDA = register("minecraft:panda");
  public static final EntityType PARROT = register("minecraft:parrot");
  public static final EntityType PHANTOM = register("minecraft:phantom");
  public static final EntityType PIG = register("minecraft:pig");
  public static final EntityType PIGLIN = register("minecraft:piglin");
  public static final EntityType PIGLIN_BRUTE = register("minecraft:piglin_brute");
  public static final EntityType PILLAGER = register("minecraft:pillager");
  public static final EntityType POLAR_BEAR = register("minecraft:polar_bear");
  public static final EntityType POTION = register("minecraft:potion");
  public static final EntityType PUFFERFISH = register("minecraft:pufferfish");
  public static final EntityType RABBIT = register("minecraft:rabbit");
  public static final EntityType RAVAGER = register("minecraft:ravager");
  public static final EntityType SALMON = register("minecraft:salmon");
  public static final EntityType SHEEP = register("minecraft:sheep");
  public static final EntityType SHULKER = register("minecraft:shulker");
  public static final EntityType SHULKER_BULLET = register("minecraft:shulker_bullet");
  public static final EntityType SILVERFISH = register("minecraft:silverfish");
  public static final EntityType SKELETON = register("minecraft:skeleton");
  public static final EntityType SKELETON_HORSE = register("minecraft:skeleton_horse");
  public static final EntityType SLIME = register("minecraft:slime");
  public static final EntityType SMALL_FIREBALL = register("minecraft:small_fireball");
  public static final EntityType SNIFFER = register("minecraft:sniffer");
  public static final EntityType SNOWBALL = register("minecraft:snowball");
  public static final EntityType SNOW_GOLEM = register("minecraft:snow_golem");
  public static final EntityType SPAWNER_MINECART = register("minecraft:spawner_minecart");
  public static final EntityType SPECTRAL_ARROW = register("minecraft:spectral_arrow");
  public static final EntityType SPIDER = register("minecraft:spider");
  public static final EntityType SPRUCE_BOAT = register("minecraft:spruce_boat");
  public static final EntityType SPRUCE_CHEST_BOAT = register("minecraft:spruce_chest_boat");
  public static final EntityType SQUID = register("minecraft:squid");
  public static final EntityType STRAY = register("minecraft:stray");
  public static final EntityType STRIDER = register("minecraft:strider");
  public static final EntityType TADPOLE = register("minecraft:tadpole");
  public static final EntityType TEXT_DISPLAY = register("minecraft:text_display");
  public static final EntityType TNT = register("minecraft:tnt");
  public static final EntityType TNT_MINECART = register("minecraft:tnt_minecart");
  public static final EntityType TRADER_LLAMA = register("minecraft:trader_llama");
  public static final EntityType TRIDENT = register("minecraft:trident");
  public static final EntityType TROPICAL_FISH = register("minecraft:tropical_fish");
  public static final EntityType TURTLE = register("minecraft:turtle");
  public static final EntityType VEX = register("minecraft:vex");
  public static final EntityType VILLAGER = register("minecraft:villager");
  public static final EntityType VINDICATOR = register("minecraft:vindicator");
  public static final EntityType WANDERING_TRADER = register("minecraft:wandering_trader");
  public static final EntityType WARDEN = register("minecraft:warden");
  public static final EntityType WIND_CHARGE = register("minecraft:wind_charge");
  public static final EntityType WITCH = register("minecraft:witch");
  public static final EntityType WITHER = register("minecraft:wither");
  public static final EntityType WITHER_SKELETON = register("minecraft:wither_skeleton");
  public static final EntityType WITHER_SKULL = register("minecraft:wither_skull");
  public static final EntityType WOLF = register("minecraft:wolf");
  public static final EntityType ZOGLIN = register("minecraft:zoglin");
  public static final EntityType ZOMBIE = register("minecraft:zombie");
  public static final EntityType ZOMBIE_HORSE = register("minecraft:zombie_horse");
  public static final EntityType ZOMBIE_VILLAGER = register("minecraft:zombie_villager");
  public static final EntityType ZOMBIFIED_PIGLIN = register("minecraft:zombified_piglin");
  public static final EntityType PLAYER = register("minecraft:player");
  public static final EntityType FISHING_BOBBER = register("minecraft:fishing_bobber");
  //@formatter:on

  public static EntityType register(String key) {
    var instance =
      GsonDataHelper.fromJson("minecraft/entities.json", key, EntityType.class);

    return REGISTRY.register(instance);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityType other)) {
      return false;
    }
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public Registry<EntityType> registry() {
    return REGISTRY;
  }
}
