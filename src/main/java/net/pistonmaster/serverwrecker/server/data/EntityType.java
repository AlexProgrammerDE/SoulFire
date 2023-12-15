/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.server.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record EntityType(int id, int internalId, String name, String displayName, String type,
                         double width, double height, int length, int offset, String category) {
    public static final List<EntityType> VALUES = new ArrayList<>();

    public static final EntityType ALLAY = register(new EntityType(0, 0, "allay", "Allay", "mob", 0.35, 0.6, -1, -1, "Passive mobs"));
    public static final EntityType AREA_EFFECT_CLOUD = register(new EntityType(1, 1, "area_effect_cloud", "Area Effect Cloud", "other", 6, 0.5, -1, -1, "UNKNOWN"));
    public static final EntityType ARMOR_STAND = register(new EntityType(2, 2, "armor_stand", "Armor Stand", "living", 0.5, 1.975, -1, -1, "Immobile"));
    public static final EntityType ARROW = register(new EntityType(3, 3, "arrow", "Arrow", "projectile", 0.5, 0.5, -1, -1, "Projectiles"));
    public static final EntityType AXOLOTL = register(new EntityType(4, 4, "axolotl", "Axolotl", "animal", 0.75, 0.42, -1, -1, "Passive mobs"));
    public static final EntityType BAT = register(new EntityType(5, 5, "bat", "Bat", "ambient", 0.5, 0.9, -1, -1, "Passive mobs"));
    public static final EntityType BEE = register(new EntityType(6, 6, "bee", "Bee", "animal", 0.7, 0.6, -1, -1, "Passive mobs"));
    public static final EntityType BLAZE = register(new EntityType(7, 7, "blaze", "Blaze", "hostile", 0.6, 1.8, -1, -1, "Hostile mobs"));
    public static final EntityType BLOCK_DISPLAY = register(new EntityType(8, 8, "block_display", "Block Display", "other", 0, 0, -1, -1, "Immobile"));
    public static final EntityType BOAT = register(new EntityType(9, 9, "boat", "Boat", "other", 1.375, 0.5625, -1, -1, "Vehicles"));
    public static final EntityType CAMEL = register(new EntityType(10, 10, "camel", "Camel", "animal", 1.7, 2.375, -1, -1, "Passive mobs"));
    public static final EntityType CAT = register(new EntityType(11, 11, "cat", "Cat", "animal", 0.6, 0.7, -1, -1, "Passive mobs"));
    public static final EntityType CAVE_SPIDER = register(new EntityType(12, 12, "cave_spider", "Cave Spider", "hostile", 0.7, 0.5, -1, -1, "Hostile mobs"));
    public static final EntityType CHEST_BOAT = register(new EntityType(13, 13, "chest_boat", "Boat with Chest", "other", 1.375, 0.5625, -1, -1, "Vehicles"));
    public static final EntityType CHEST_MINECART = register(new EntityType(14, 14, "chest_minecart", "Minecart with Chest", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType CHICKEN = register(new EntityType(15, 15, "chicken", "Chicken", "animal", 0.4, 0.7, -1, -1, "Passive mobs"));
    public static final EntityType COD = register(new EntityType(16, 16, "cod", "Cod", "water_creature", 0.5, 0.3, -1, -1, "Passive mobs"));
    public static final EntityType COMMAND_BLOCK_MINECART = register(new EntityType(17, 17, "command_block_minecart", "Minecart with Command Block", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType COW = register(new EntityType(18, 18, "cow", "Cow", "animal", 0.9, 1.4, -1, -1, "Passive mobs"));
    public static final EntityType CREEPER = register(new EntityType(19, 19, "creeper", "Creeper", "hostile", 0.6, 1.7, -1, -1, "Hostile mobs"));
    public static final EntityType DOLPHIN = register(new EntityType(20, 20, "dolphin", "Dolphin", "water_creature", 0.9, 0.6, -1, -1, "Passive mobs"));
    public static final EntityType DONKEY = register(new EntityType(21, 21, "donkey", "Donkey", "animal", 1.3964844, 1.5, -1, -1, "Passive mobs"));
    public static final EntityType DRAGON_FIREBALL = register(new EntityType(22, 22, "dragon_fireball", "Dragon Fireball", "projectile", 1, 1, -1, -1, "Projectiles"));
    public static final EntityType DROWNED = register(new EntityType(23, 23, "drowned", "Drowned", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType EGG = register(new EntityType(24, 24, "egg", "Thrown Egg", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType ELDER_GUARDIAN = register(new EntityType(25, 25, "elder_guardian", "Elder Guardian", "hostile", 1.9975, 1.9975, -1, -1, "Hostile mobs"));
    public static final EntityType END_CRYSTAL = register(new EntityType(26, 26, "end_crystal", "End Crystal", "other", 2, 2, -1, -1, "Immobile"));
    public static final EntityType ENDER_DRAGON = register(new EntityType(27, 27, "ender_dragon", "Ender Dragon", "mob", 16, 8, -1, -1, "Hostile mobs"));
    public static final EntityType ENDER_PEARL = register(new EntityType(28, 28, "ender_pearl", "Thrown Ender Pearl", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType ENDERMAN = register(new EntityType(29, 29, "enderman", "Enderman", "hostile", 0.6, 2.9, -1, -1, "Hostile mobs"));
    public static final EntityType ENDERMITE = register(new EntityType(30, 30, "endermite", "Endermite", "hostile", 0.4, 0.3, -1, -1, "Hostile mobs"));
    public static final EntityType EVOKER = register(new EntityType(31, 31, "evoker", "Evoker", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType EVOKER_FANGS = register(new EntityType(32, 32, "evoker_fangs", "Evoker Fangs", "other", 0.5, 0.8, -1, -1, "Hostile mobs"));
    public static final EntityType EXPERIENCE_BOTTLE = register(new EntityType(33, 33, "experience_bottle", "Thrown Bottle o' Enchanting", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType EXPERIENCE_ORB = register(new EntityType(34, 34, "experience_orb", "Experience Orb", "other", 0.5, 0.5, -1, -1, "UNKNOWN"));
    public static final EntityType EYE_OF_ENDER = register(new EntityType(35, 35, "eye_of_ender", "Eye of Ender", "other", 0.25, 0.25, -1, -1, "UNKNOWN"));
    public static final EntityType FALLING_BLOCK = register(new EntityType(36, 36, "falling_block", "Falling Block", "other", 0.98, 0.98, -1, -1, "UNKNOWN"));
    public static final EntityType FIREWORK_ROCKET = register(new EntityType(37, 37, "firework_rocket", "Firework Rocket", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType FOX = register(new EntityType(38, 38, "fox", "Fox", "animal", 0.6, 0.7, -1, -1, "Passive mobs"));
    public static final EntityType FROG = register(new EntityType(39, 39, "frog", "Frog", "animal", 0.5, 0.5, -1, -1, "Passive mobs"));
    public static final EntityType FURNACE_MINECART = register(new EntityType(40, 40, "furnace_minecart", "Minecart with Furnace", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType GHAST = register(new EntityType(41, 41, "ghast", "Ghast", "mob", 4, 4, -1, -1, "Hostile mobs"));
    public static final EntityType GIANT = register(new EntityType(42, 42, "giant", "Giant", "hostile", 3.6, 12, -1, -1, "Hostile mobs"));
    public static final EntityType GLOW_ITEM_FRAME = register(new EntityType(43, 43, "glow_item_frame", "Glow Item Frame", "other", 0.5, 0.5, -1, -1, "Immobile"));
    public static final EntityType GLOW_SQUID = register(new EntityType(44, 44, "glow_squid", "Glow Squid", "water_creature", 0.8, 0.8, -1, -1, "Passive mobs"));
    public static final EntityType GOAT = register(new EntityType(45, 45, "goat", "Goat", "animal", 0.9, 1.3, -1, -1, "Passive mobs"));
    public static final EntityType GUARDIAN = register(new EntityType(46, 46, "guardian", "Guardian", "hostile", 0.85, 0.85, -1, -1, "Hostile mobs"));
    public static final EntityType HOGLIN = register(new EntityType(47, 47, "hoglin", "Hoglin", "animal", 1.3964844, 1.4, -1, -1, "Hostile mobs"));
    public static final EntityType HOPPER_MINECART = register(new EntityType(48, 48, "hopper_minecart", "Minecart with Hopper", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType HORSE = register(new EntityType(49, 49, "horse", "Horse", "animal", 1.3964844, 1.6, -1, -1, "Passive mobs"));
    public static final EntityType HUSK = register(new EntityType(50, 50, "husk", "Husk", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType ILLUSIONER = register(new EntityType(51, 51, "illusioner", "Illusioner", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType INTERACTION = register(new EntityType(52, 52, "interaction", "Interaction", "other", 0, 0, -1, -1, "Immobile"));
    public static final EntityType IRON_GOLEM = register(new EntityType(53, 53, "iron_golem", "Iron Golem", "mob", 1.4, 2.7, -1, -1, "Passive mobs"));
    public static final EntityType ITEM = register(new EntityType(54, 54, "item", "Item", "other", 0.25, 0.25, -1, -1, "UNKNOWN"));
    public static final EntityType ITEM_DISPLAY = register(new EntityType(55, 55, "item_display", "Item Display", "other", 0, 0, -1, -1, "Immobile"));
    public static final EntityType ITEM_FRAME = register(new EntityType(56, 56, "item_frame", "Item Frame", "other", 0.5, 0.5, -1, -1, "Immobile"));
    public static final EntityType FIREBALL = register(new EntityType(57, 57, "fireball", "Fireball", "projectile", 1, 1, -1, -1, "Projectiles"));
    public static final EntityType LEASH_KNOT = register(new EntityType(58, 58, "leash_knot", "Leash Knot", "other", 0.375, 0.5, -1, -1, "Immobile"));
    public static final EntityType LIGHTNING_BOLT = register(new EntityType(59, 59, "lightning_bolt", "Lightning Bolt", "other", 0, 0, -1, -1, "UNKNOWN"));
    public static final EntityType LLAMA = register(new EntityType(60, 60, "llama", "Llama", "animal", 0.9, 1.87, -1, -1, "Passive mobs"));
    public static final EntityType LLAMA_SPIT = register(new EntityType(61, 61, "llama_spit", "Llama Spit", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType MAGMA_CUBE = register(new EntityType(62, 62, "magma_cube", "Magma Cube", "mob", 2.04, 2.04, -1, -1, "Hostile mobs"));
    public static final EntityType MARKER = register(new EntityType(63, 63, "marker", "Marker", "other", 0, 0, -1, -1, "UNKNOWN"));
    public static final EntityType MINECART = register(new EntityType(64, 64, "minecart", "Minecart", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType MOOSHROOM = register(new EntityType(65, 65, "mooshroom", "Mooshroom", "animal", 0.9, 1.4, -1, -1, "Passive mobs"));
    public static final EntityType MULE = register(new EntityType(66, 66, "mule", "Mule", "animal", 1.3964844, 1.6, -1, -1, "Passive mobs"));
    public static final EntityType OCELOT = register(new EntityType(67, 67, "ocelot", "Ocelot", "animal", 0.6, 0.7, -1, -1, "Passive mobs"));
    public static final EntityType PAINTING = register(new EntityType(68, 68, "painting", "Painting", "other", 0.5, 0.5, -1, -1, "Immobile"));
    public static final EntityType PANDA = register(new EntityType(69, 69, "panda", "Panda", "animal", 1.3, 1.25, -1, -1, "Passive mobs"));
    public static final EntityType PARROT = register(new EntityType(70, 70, "parrot", "Parrot", "animal", 0.5, 0.9, -1, -1, "Passive mobs"));
    public static final EntityType PHANTOM = register(new EntityType(71, 71, "phantom", "Phantom", "mob", 0.9, 0.5, -1, -1, "Hostile mobs"));
    public static final EntityType PIG = register(new EntityType(72, 72, "pig", "Pig", "animal", 0.9, 0.9, -1, -1, "Passive mobs"));
    public static final EntityType PIGLIN = register(new EntityType(73, 73, "piglin", "Piglin", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType PIGLIN_BRUTE = register(new EntityType(74, 74, "piglin_brute", "Piglin Brute", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType PILLAGER = register(new EntityType(75, 75, "pillager", "Pillager", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType POLAR_BEAR = register(new EntityType(76, 76, "polar_bear", "Polar Bear", "animal", 1.4, 1.4, -1, -1, "Passive mobs"));
    public static final EntityType POTION = register(new EntityType(77, 77, "potion", "Potion", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType PUFFERFISH = register(new EntityType(78, 78, "pufferfish", "Pufferfish", "water_creature", 0.7, 0.7, -1, -1, "Passive mobs"));
    public static final EntityType RABBIT = register(new EntityType(79, 79, "rabbit", "Rabbit", "animal", 0.4, 0.5, -1, -1, "Passive mobs"));
    public static final EntityType RAVAGER = register(new EntityType(80, 80, "ravager", "Ravager", "hostile", 1.95, 2.2, -1, -1, "Hostile mobs"));
    public static final EntityType SALMON = register(new EntityType(81, 81, "salmon", "Salmon", "water_creature", 0.7, 0.4, -1, -1, "Passive mobs"));
    public static final EntityType SHEEP = register(new EntityType(82, 82, "sheep", "Sheep", "animal", 0.9, 1.3, -1, -1, "Passive mobs"));
    public static final EntityType SHULKER = register(new EntityType(83, 83, "shulker", "Shulker", "mob", 1, 1, -1, -1, "Hostile mobs"));
    public static final EntityType SHULKER_BULLET = register(new EntityType(84, 84, "shulker_bullet", "Shulker Bullet", "projectile", 0.3125, 0.3125, -1, -1, "Projectiles"));
    public static final EntityType SILVERFISH = register(new EntityType(85, 85, "silverfish", "Silverfish", "hostile", 0.4, 0.3, -1, -1, "Hostile mobs"));
    public static final EntityType SKELETON = register(new EntityType(86, 86, "skeleton", "Skeleton", "hostile", 0.6, 1.99, -1, -1, "Hostile mobs"));
    public static final EntityType SKELETON_HORSE = register(new EntityType(87, 87, "skeleton_horse", "Skeleton Horse", "animal", 1.3964844, 1.6, -1, -1, "Hostile mobs"));
    public static final EntityType SLIME = register(new EntityType(88, 88, "slime", "Slime", "mob", 2.04, 2.04, -1, -1, "Hostile mobs"));
    public static final EntityType SMALL_FIREBALL = register(new EntityType(89, 89, "small_fireball", "Small Fireball", "projectile", 0.3125, 0.3125, -1, -1, "Projectiles"));
    public static final EntityType SNIFFER = register(new EntityType(90, 90, "sniffer", "Sniffer", "animal", 1.9, 1.75, -1, -1, "Passive mobs"));
    public static final EntityType SNOW_GOLEM = register(new EntityType(91, 91, "snow_golem", "Snow Golem", "mob", 0.7, 1.9, -1, -1, "Passive mobs"));
    public static final EntityType SNOWBALL = register(new EntityType(92, 92, "snowball", "Snowball", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));
    public static final EntityType SPAWNER_MINECART = register(new EntityType(93, 93, "spawner_minecart", "Minecart with Monster Spawner", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType SPECTRAL_ARROW = register(new EntityType(94, 94, "spectral_arrow", "Spectral Arrow", "projectile", 0.5, 0.5, -1, -1, "Projectiles"));
    public static final EntityType SPIDER = register(new EntityType(95, 95, "spider", "Spider", "hostile", 1.4, 0.9, -1, -1, "Hostile mobs"));
    public static final EntityType SQUID = register(new EntityType(96, 96, "squid", "Squid", "water_creature", 0.8, 0.8, -1, -1, "Passive mobs"));
    public static final EntityType STRAY = register(new EntityType(97, 97, "stray", "Stray", "hostile", 0.6, 1.99, -1, -1, "Hostile mobs"));
    public static final EntityType STRIDER = register(new EntityType(98, 98, "strider", "Strider", "animal", 0.9, 1.7, -1, -1, "Passive mobs"));
    public static final EntityType TADPOLE = register(new EntityType(99, 99, "tadpole", "Tadpole", "water_creature", 0.4, 0.3, -1, -1, "Passive mobs"));
    public static final EntityType TEXT_DISPLAY = register(new EntityType(100, 100, "text_display", "Text Display", "other", 0, 0, -1, -1, "Immobile"));
    public static final EntityType TNT = register(new EntityType(101, 101, "tnt", "Primed TNT", "other", 0.98, 0.98, -1, -1, "UNKNOWN"));
    public static final EntityType TNT_MINECART = register(new EntityType(102, 102, "tnt_minecart", "Minecart with TNT", "other", 0.98, 0.7, -1, -1, "Vehicles"));
    public static final EntityType TRADER_LLAMA = register(new EntityType(103, 103, "trader_llama", "Trader Llama", "animal", 0.9, 1.87, -1, -1, "Passive mobs"));
    public static final EntityType TRIDENT = register(new EntityType(104, 104, "trident", "Trident", "projectile", 0.5, 0.5, -1, -1, "Projectiles"));
    public static final EntityType TROPICAL_FISH = register(new EntityType(105, 105, "tropical_fish", "Tropical Fish", "water_creature", 0.5, 0.4, -1, -1, "Passive mobs"));
    public static final EntityType TURTLE = register(new EntityType(106, 106, "turtle", "Turtle", "animal", 1.2, 0.4, -1, -1, "Passive mobs"));
    public static final EntityType VEX = register(new EntityType(107, 107, "vex", "Vex", "hostile", 0.4, 0.8, -1, -1, "Hostile mobs"));
    public static final EntityType VILLAGER = register(new EntityType(108, 108, "villager", "Villager", "passive", 0.6, 1.95, -1, -1, "Passive mobs"));
    public static final EntityType VINDICATOR = register(new EntityType(109, 109, "vindicator", "Vindicator", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType WANDERING_TRADER = register(new EntityType(110, 110, "wandering_trader", "Wandering Trader", "passive", 0.6, 1.95, -1, -1, "Passive mobs"));
    public static final EntityType WARDEN = register(new EntityType(111, 111, "warden", "Warden", "hostile", 0.9, 2.9, -1, -1, "Hostile mobs"));
    public static final EntityType WITCH = register(new EntityType(112, 112, "witch", "Witch", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType WITHER = register(new EntityType(113, 113, "wither", "Wither", "hostile", 0.9, 3.5, -1, -1, "Hostile mobs"));
    public static final EntityType WITHER_SKELETON = register(new EntityType(114, 114, "wither_skeleton", "Wither Skeleton", "hostile", 0.7, 2.4, -1, -1, "Hostile mobs"));
    public static final EntityType WITHER_SKULL = register(new EntityType(115, 115, "wither_skull", "Wither Skull", "projectile", 0.3125, 0.3125, -1, -1, "Projectiles"));
    public static final EntityType WOLF = register(new EntityType(116, 116, "wolf", "Wolf", "animal", 0.6, 0.85, -1, -1, "Passive mobs"));
    public static final EntityType ZOGLIN = register(new EntityType(117, 117, "zoglin", "Zoglin", "hostile", 1.3964844, 1.4, -1, -1, "Hostile mobs"));
    public static final EntityType ZOMBIE = register(new EntityType(118, 118, "zombie", "Zombie", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType ZOMBIE_HORSE = register(new EntityType(119, 119, "zombie_horse", "Zombie Horse", "animal", 1.3964844, 1.6, -1, -1, "Hostile mobs"));
    public static final EntityType ZOMBIE_VILLAGER = register(new EntityType(120, 120, "zombie_villager", "Zombie Villager", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType ZOMBIFIED_PIGLIN = register(new EntityType(121, 121, "zombified_piglin", "Zombified Piglin", "hostile", 0.6, 1.95, -1, -1, "Hostile mobs"));
    public static final EntityType PLAYER = register(new EntityType(122, 122, "player", "Player", "player", 0.6, 1.8, -1, -1, "UNKNOWN"));
    public static final EntityType FISHING_BOBBER = register(new EntityType(123, 123, "fishing_bobber", "Fishing Bobber", "projectile", 0.25, 0.25, -1, -1, "Projectiles"));

    public static EntityType register(EntityType entityType) {
        VALUES.add(entityType);
        return entityType;
    }

    public static EntityType getById(int id) {
        for (var entityId : VALUES) {
            if (entityId.id() == id) {
                return entityId;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityType entityType)) return false;
        return id == entityType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
