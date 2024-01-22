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
package net.pistonmaster.soulfire.server.data;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

@SuppressWarnings("unused")
public record EntityType(int id, String name, float width, float height,
                         String category, boolean friendly) {
    public static final Int2ReferenceMap<EntityType> FROM_ID = new Int2ReferenceOpenHashMap<>();

    public static final EntityType ALLAY = register("allay");
    public static final EntityType AREA_EFFECT_CLOUD = register("area_effect_cloud");
    public static final EntityType ARMOR_STAND = register("armor_stand");
    public static final EntityType ARROW = register("arrow");
    public static final EntityType AXOLOTL = register("axolotl");
    public static final EntityType BAT = register("bat");
    public static final EntityType BEE = register("bee");
    public static final EntityType BLAZE = register("blaze");
    public static final EntityType BLOCK_DISPLAY = register("block_display");
    public static final EntityType BOAT = register("boat");
    public static final EntityType BREEZE = register("breeze");
    public static final EntityType CAMEL = register("camel");
    public static final EntityType CAT = register("cat");
    public static final EntityType CAVE_SPIDER = register("cave_spider");
    public static final EntityType CHEST_BOAT = register("chest_boat");
    public static final EntityType CHEST_MINECART = register("chest_minecart");
    public static final EntityType CHICKEN = register("chicken");
    public static final EntityType COD = register("cod");
    public static final EntityType COMMAND_BLOCK_MINECART = register("command_block_minecart");
    public static final EntityType COW = register("cow");
    public static final EntityType CREEPER = register("creeper");
    public static final EntityType DOLPHIN = register("dolphin");
    public static final EntityType DONKEY = register("donkey");
    public static final EntityType DRAGON_FIREBALL = register("dragon_fireball");
    public static final EntityType DROWNED = register("drowned");
    public static final EntityType EGG = register("egg");
    public static final EntityType ELDER_GUARDIAN = register("elder_guardian");
    public static final EntityType END_CRYSTAL = register("end_crystal");
    public static final EntityType ENDER_DRAGON = register("ender_dragon");
    public static final EntityType ENDER_PEARL = register("ender_pearl");
    public static final EntityType ENDERMAN = register("enderman");
    public static final EntityType ENDERMITE = register("endermite");
    public static final EntityType EVOKER = register("evoker");
    public static final EntityType EVOKER_FANGS = register("evoker_fangs");
    public static final EntityType EXPERIENCE_BOTTLE = register("experience_bottle");
    public static final EntityType EXPERIENCE_ORB = register("experience_orb");
    public static final EntityType EYE_OF_ENDER = register("eye_of_ender");
    public static final EntityType FALLING_BLOCK = register("falling_block");
    public static final EntityType FIREWORK_ROCKET = register("firework_rocket");
    public static final EntityType FOX = register("fox");
    public static final EntityType FROG = register("frog");
    public static final EntityType FURNACE_MINECART = register("furnace_minecart");
    public static final EntityType GHAST = register("ghast");
    public static final EntityType GIANT = register("giant");
    public static final EntityType GLOW_ITEM_FRAME = register("glow_item_frame");
    public static final EntityType GLOW_SQUID = register("glow_squid");
    public static final EntityType GOAT = register("goat");
    public static final EntityType GUARDIAN = register("guardian");
    public static final EntityType HOGLIN = register("hoglin");
    public static final EntityType HOPPER_MINECART = register("hopper_minecart");
    public static final EntityType HORSE = register("horse");
    public static final EntityType HUSK = register("husk");
    public static final EntityType ILLUSIONER = register("illusioner");
    public static final EntityType INTERACTION = register("interaction");
    public static final EntityType IRON_GOLEM = register("iron_golem");
    public static final EntityType ITEM = register("item");
    public static final EntityType ITEM_DISPLAY = register("item_display");
    public static final EntityType ITEM_FRAME = register("item_frame");
    public static final EntityType FIREBALL = register("fireball");
    public static final EntityType LEASH_KNOT = register("leash_knot");
    public static final EntityType LIGHTNING_BOLT = register("lightning_bolt");
    public static final EntityType LLAMA = register("llama");
    public static final EntityType LLAMA_SPIT = register("llama_spit");
    public static final EntityType MAGMA_CUBE = register("magma_cube");
    public static final EntityType MARKER = register("marker");
    public static final EntityType MINECART = register("minecart");
    public static final EntityType MOOSHROOM = register("mooshroom");
    public static final EntityType MULE = register("mule");
    public static final EntityType OCELOT = register("ocelot");
    public static final EntityType PAINTING = register("painting");
    public static final EntityType PANDA = register("panda");
    public static final EntityType PARROT = register("parrot");
    public static final EntityType PHANTOM = register("phantom");
    public static final EntityType PIG = register("pig");
    public static final EntityType PIGLIN = register("piglin");
    public static final EntityType PIGLIN_BRUTE = register("piglin_brute");
    public static final EntityType PILLAGER = register("pillager");
    public static final EntityType POLAR_BEAR = register("polar_bear");
    public static final EntityType POTION = register("potion");
    public static final EntityType PUFFERFISH = register("pufferfish");
    public static final EntityType RABBIT = register("rabbit");
    public static final EntityType RAVAGER = register("ravager");
    public static final EntityType SALMON = register("salmon");
    public static final EntityType SHEEP = register("sheep");
    public static final EntityType SHULKER = register("shulker");
    public static final EntityType SHULKER_BULLET = register("shulker_bullet");
    public static final EntityType SILVERFISH = register("silverfish");
    public static final EntityType SKELETON = register("skeleton");
    public static final EntityType SKELETON_HORSE = register("skeleton_horse");
    public static final EntityType SLIME = register("slime");
    public static final EntityType SMALL_FIREBALL = register("small_fireball");
    public static final EntityType SNIFFER = register("sniffer");
    public static final EntityType SNOW_GOLEM = register("snow_golem");
    public static final EntityType SNOWBALL = register("snowball");
    public static final EntityType SPAWNER_MINECART = register("spawner_minecart");
    public static final EntityType SPECTRAL_ARROW = register("spectral_arrow");
    public static final EntityType SPIDER = register("spider");
    public static final EntityType SQUID = register("squid");
    public static final EntityType STRAY = register("stray");
    public static final EntityType STRIDER = register("strider");
    public static final EntityType TADPOLE = register("tadpole");
    public static final EntityType TEXT_DISPLAY = register("text_display");
    public static final EntityType TNT = register("tnt");
    public static final EntityType TNT_MINECART = register("tnt_minecart");
    public static final EntityType TRADER_LLAMA = register("trader_llama");
    public static final EntityType TRIDENT = register("trident");
    public static final EntityType TROPICAL_FISH = register("tropical_fish");
    public static final EntityType TURTLE = register("turtle");
    public static final EntityType VEX = register("vex");
    public static final EntityType VILLAGER = register("villager");
    public static final EntityType VINDICATOR = register("vindicator");
    public static final EntityType WANDERING_TRADER = register("wandering_trader");
    public static final EntityType WARDEN = register("warden");
    public static final EntityType WIND_CHARGE = register("wind_charge");
    public static final EntityType WITCH = register("witch");
    public static final EntityType WITHER = register("wither");
    public static final EntityType WITHER_SKELETON = register("wither_skeleton");
    public static final EntityType WITHER_SKULL = register("wither_skull");
    public static final EntityType WOLF = register("wolf");
    public static final EntityType ZOGLIN = register("zoglin");
    public static final EntityType ZOMBIE = register("zombie");
    public static final EntityType ZOMBIE_HORSE = register("zombie_horse");
    public static final EntityType ZOMBIE_VILLAGER = register("zombie_villager");
    public static final EntityType ZOMBIFIED_PIGLIN = register("zombified_piglin");
    public static final EntityType PLAYER = register("player");
    public static final EntityType FISHING_BOBBER = register("fishing_bobber");

    public static EntityType register(String name) {
        var entityType = GsonDataHelper.fromJson("/minecraft/entities.json", name, EntityType.class);
        FROM_ID.put(entityType.id(), entityType);
        return entityType;
    }

    public static EntityType getById(int id) {
        return FROM_ID.get(id);
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
