package net.pistonmaster.serverwrecker.server.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record EntityType(int id, String name, String displayName, String type,
                         float width, float height, String category) {
    public static final List<EntityType> VALUES = new ArrayList<>();

    public static final EntityType ALLAY = register(new EntityType(0, "allay", "Allay", "mob", 0.35F, 0.6F, "Passive mobs"));
    public static final EntityType AREA_EFFECT_CLOUD = register(new EntityType(1, "area_effect_cloud", "Area Effect Cloud", "other", 6F, 0.5F, "UNKNOWN"));
    public static final EntityType ARMOR_STAND = register(new EntityType(2, "armor_stand", "Armor Stand", "living", 0.5F, 1.975F, "Immobile"));
    public static final EntityType ARROW = register(new EntityType(3, "arrow", "Arrow", "projectile", 0.5F, 0.5F, "Projectiles"));
    public static final EntityType AXOLOTL = register(new EntityType(4, "axolotl", "Axolotl", "animal", 0.75F, 0.42F, "Passive mobs"));
    public static final EntityType BAT = register(new EntityType(5, "bat", "Bat", "ambient", 0.5F, 0.9F, "Passive mobs"));
    public static final EntityType BEE = register(new EntityType(6, "bee", "Bee", "animal", 0.7F, 0.6F, "Passive mobs"));
    public static final EntityType BLAZE = register(new EntityType(7, "blaze", "Blaze", "hostile", 0.6F, 1.8F, "Hostile mobs"));
    public static final EntityType BLOCK_DISPLAY = register(new EntityType(8, "block_display", "Block Display", "other", 0F, 0F, "Immobile"));
    public static final EntityType BOAT = register(new EntityType(9, "boat", "Boat", "other", 1.375F, 0.5625F, "Vehicles"));
    public static final EntityType BREEZE = register(new EntityType(10, "breeze", "Breeze", "player", 0.6F, 1.7F, "Hostile mobs"));
    public static final EntityType CAMEL = register(new EntityType(11, "camel", "Camel", "animal", 1.7F, 2.375F, "Passive mobs"));
    public static final EntityType CAT = register(new EntityType(12, "cat", "Cat", "animal", 0.6F, 0.7F, "Passive mobs"));
    public static final EntityType CAVE_SPIDER = register(new EntityType(13, "cave_spider", "Cave Spider", "hostile", 0.7F, 0.5F, "Hostile mobs"));
    public static final EntityType CHEST_BOAT = register(new EntityType(14, "chest_boat", "Boat with Chest", "other", 1.375F, 0.5625F, "Vehicles"));
    public static final EntityType CHEST_MINECART = register(new EntityType(15, "chest_minecart", "Minecart with Chest", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType CHICKEN = register(new EntityType(16, "chicken", "Chicken", "animal", 0.4F, 0.7F, "Passive mobs"));
    public static final EntityType COD = register(new EntityType(17, "cod", "Cod", "water_creature", 0.5F, 0.3F, "Passive mobs"));
    public static final EntityType COMMAND_BLOCK_MINECART = register(new EntityType(18, "command_block_minecart", "Minecart with Command Block", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType COW = register(new EntityType(19, "cow", "Cow", "animal", 0.9F, 1.4F, "Passive mobs"));
    public static final EntityType CREEPER = register(new EntityType(20, "creeper", "Creeper", "hostile", 0.6F, 1.7F, "Hostile mobs"));
    public static final EntityType DOLPHIN = register(new EntityType(21, "dolphin", "Dolphin", "water_creature", 0.9F, 0.6F, "Passive mobs"));
    public static final EntityType DONKEY = register(new EntityType(22, "donkey", "Donkey", "animal", 1.3964844F, 1.5F, "Passive mobs"));
    public static final EntityType DRAGON_FIREBALL = register(new EntityType(23, "dragon_fireball", "Dragon Fireball", "projectile", 1F, 1F, "Projectiles"));
    public static final EntityType DROWNED = register(new EntityType(24, "drowned", "Drowned", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType EGG = register(new EntityType(25, "egg", "Thrown Egg", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType ELDER_GUARDIAN = register(new EntityType(26, "elder_guardian", "Elder Guardian", "hostile", 1.9975F, 1.9975F, "Hostile mobs"));
    public static final EntityType END_CRYSTAL = register(new EntityType(27, "end_crystal", "End Crystal", "other", 2F, 2F, "Immobile"));
    public static final EntityType ENDER_DRAGON = register(new EntityType(28, "ender_dragon", "Ender Dragon", "mob", 16F, 8F, "Hostile mobs"));
    public static final EntityType ENDER_PEARL = register(new EntityType(29, "ender_pearl", "Thrown Ender Pearl", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType ENDERMAN = register(new EntityType(30, "enderman", "Enderman", "hostile", 0.6F, 2.9F, "Hostile mobs"));
    public static final EntityType ENDERMITE = register(new EntityType(31, "endermite", "Endermite", "hostile", 0.4F, 0.3F, "Hostile mobs"));
    public static final EntityType EVOKER = register(new EntityType(32, "evoker", "Evoker", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType EVOKER_FANGS = register(new EntityType(33, "evoker_fangs", "Evoker Fangs", "other", 0.5F, 0.8F, "Hostile mobs"));
    public static final EntityType EXPERIENCE_BOTTLE = register(new EntityType(34, "experience_bottle", "Thrown Bottle o' Enchanting", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType EXPERIENCE_ORB = register(new EntityType(35, "experience_orb", "Experience Orb", "other", 0.5F, 0.5F, "UNKNOWN"));
    public static final EntityType EYE_OF_ENDER = register(new EntityType(36, "eye_of_ender", "Eye of Ender", "other", 0.25F, 0.25F, "UNKNOWN"));
    public static final EntityType FALLING_BLOCK = register(new EntityType(37, "falling_block", "Falling Block", "other", 0.98F, 0.98F, "UNKNOWN"));
    public static final EntityType FIREWORK_ROCKET = register(new EntityType(38, "firework_rocket", "Firework Rocket", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType FOX = register(new EntityType(39, "fox", "Fox", "animal", 0.6F, 0.7F, "Passive mobs"));
    public static final EntityType FROG = register(new EntityType(40, "frog", "Frog", "animal", 0.5F, 0.5F, "Passive mobs"));
    public static final EntityType FURNACE_MINECART = register(new EntityType(41, "furnace_minecart", "Minecart with Furnace", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType GHAST = register(new EntityType(42, "ghast", "Ghast", "mob", 4F, 4F, "Hostile mobs"));
    public static final EntityType GIANT = register(new EntityType(43, "giant", "Giant", "hostile", 3.6F, 12F, "Hostile mobs"));
    public static final EntityType GLOW_ITEM_FRAME = register(new EntityType(44, "glow_item_frame", "Glow Item Frame", "other", 0.5F, 0.5F, "Immobile"));
    public static final EntityType GLOW_SQUID = register(new EntityType(45, "glow_squid", "Glow Squid", "water_creature", 0.8F, 0.8F, "Passive mobs"));
    public static final EntityType GOAT = register(new EntityType(46, "goat", "Goat", "animal", 0.9F, 1.3F, "Passive mobs"));
    public static final EntityType GUARDIAN = register(new EntityType(47, "guardian", "Guardian", "hostile", 0.85F, 0.85F, "Hostile mobs"));
    public static final EntityType HOGLIN = register(new EntityType(48, "hoglin", "Hoglin", "animal", 1.3964844F, 1.4F, "Hostile mobs"));
    public static final EntityType HOPPER_MINECART = register(new EntityType(49, "hopper_minecart", "Minecart with Hopper", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType HORSE = register(new EntityType(50, "horse", "Horse", "animal", 1.3964844F, 1.6F, "Passive mobs"));
    public static final EntityType HUSK = register(new EntityType(51, "husk", "Husk", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType ILLUSIONER = register(new EntityType(52, "illusioner", "Illusioner", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType INTERACTION = register(new EntityType(53, "interaction", "Interaction", "other", 0F, 0F, "Immobile"));
    public static final EntityType IRON_GOLEM = register(new EntityType(54, "iron_golem", "Iron Golem", "mob", 1.4F, 2.7F, "Passive mobs"));
    public static final EntityType ITEM = register(new EntityType(55, "item", "Item", "other", 0.25F, 0.25F, "UNKNOWN"));
    public static final EntityType ITEM_DISPLAY = register(new EntityType(56, "item_display", "Item Display", "other", 0F, 0F, "Immobile"));
    public static final EntityType ITEM_FRAME = register(new EntityType(57, "item_frame", "Item Frame", "other", 0.5F, 0.5F, "Immobile"));
    public static final EntityType FIREBALL = register(new EntityType(58, "fireball", "Fireball", "projectile", 1F, 1F, "Projectiles"));
    public static final EntityType LEASH_KNOT = register(new EntityType(59, "leash_knot", "Leash Knot", "other", 0.375F, 0.5F, "Immobile"));
    public static final EntityType LIGHTNING_BOLT = register(new EntityType(60, "lightning_bolt", "Lightning Bolt", "other", 0F, 0F, "UNKNOWN"));
    public static final EntityType LLAMA = register(new EntityType(61, "llama", "Llama", "animal", 0.9F, 1.87F, "Passive mobs"));
    public static final EntityType LLAMA_SPIT = register(new EntityType(62, "llama_spit", "Llama Spit", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType MAGMA_CUBE = register(new EntityType(63, "magma_cube", "Magma Cube", "mob", 2.04F, 2.04F, "Hostile mobs"));
    public static final EntityType MARKER = register(new EntityType(64, "marker", "Marker", "other", 0F, 0F, "UNKNOWN"));
    public static final EntityType MINECART = register(new EntityType(65, "minecart", "Minecart", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType MOOSHROOM = register(new EntityType(66, "mooshroom", "Mooshroom", "animal", 0.9F, 1.4F, "Passive mobs"));
    public static final EntityType MULE = register(new EntityType(67, "mule", "Mule", "animal", 1.3964844F, 1.6F, "Passive mobs"));
    public static final EntityType OCELOT = register(new EntityType(68, "ocelot", "Ocelot", "animal", 0.6F, 0.7F, "Passive mobs"));
    public static final EntityType PAINTING = register(new EntityType(69, "painting", "Painting", "other", 0.5F, 0.5F, "Immobile"));
    public static final EntityType PANDA = register(new EntityType(70, "panda", "Panda", "animal", 1.3F, 1.25F, "Passive mobs"));
    public static final EntityType PARROT = register(new EntityType(71, "parrot", "Parrot", "animal", 0.5F, 0.9F, "Passive mobs"));
    public static final EntityType PHANTOM = register(new EntityType(72, "phantom", "Phantom", "mob", 0.9F, 0.5F, "Hostile mobs"));
    public static final EntityType PIG = register(new EntityType(73, "pig", "Pig", "animal", 0.9F, 0.9F, "Passive mobs"));
    public static final EntityType PIGLIN = register(new EntityType(74, "piglin", "Piglin", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType PIGLIN_BRUTE = register(new EntityType(75, "piglin_brute", "Piglin Brute", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType PILLAGER = register(new EntityType(76, "pillager", "Pillager", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType POLAR_BEAR = register(new EntityType(77, "polar_bear", "Polar Bear", "animal", 1.4F, 1.4F, "Passive mobs"));
    public static final EntityType POTION = register(new EntityType(78, "potion", "Potion", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType PUFFERFISH = register(new EntityType(79, "pufferfish", "Pufferfish", "water_creature", 0.7F, 0.7F, "Passive mobs"));
    public static final EntityType RABBIT = register(new EntityType(80, "rabbit", "Rabbit", "animal", 0.4F, 0.5F, "Passive mobs"));
    public static final EntityType RAVAGER = register(new EntityType(81, "ravager", "Ravager", "hostile", 1.95F, 2.2F, "Hostile mobs"));
    public static final EntityType SALMON = register(new EntityType(82, "salmon", "Salmon", "water_creature", 0.7F, 0.4F, "Passive mobs"));
    public static final EntityType SHEEP = register(new EntityType(83, "sheep", "Sheep", "animal", 0.9F, 1.3F, "Passive mobs"));
    public static final EntityType SHULKER = register(new EntityType(84, "shulker", "Shulker", "mob", 1F, 1F, "Hostile mobs"));
    public static final EntityType SHULKER_BULLET = register(new EntityType(85, "shulker_bullet", "Shulker Bullet", "projectile", 0.3125F, 0.3125F, "Projectiles"));
    public static final EntityType SILVERFISH = register(new EntityType(86, "silverfish", "Silverfish", "hostile", 0.4F, 0.3F, "Hostile mobs"));
    public static final EntityType SKELETON = register(new EntityType(87, "skeleton", "Skeleton", "hostile", 0.6F, 1.99F, "Hostile mobs"));
    public static final EntityType SKELETON_HORSE = register(new EntityType(88, "skeleton_horse", "Skeleton Horse", "animal", 1.3964844F, 1.6F, "Hostile mobs"));
    public static final EntityType SLIME = register(new EntityType(89, "slime", "Slime", "mob", 2.04F, 2.04F, "Hostile mobs"));
    public static final EntityType SMALL_FIREBALL = register(new EntityType(90, "small_fireball", "Small Fireball", "projectile", 0.3125F, 0.3125F, "Projectiles"));
    public static final EntityType SNIFFER = register(new EntityType(91, "sniffer", "Sniffer", "animal", 1.9F, 1.75F, "Passive mobs"));
    public static final EntityType SNOW_GOLEM = register(new EntityType(92, "snow_golem", "Snow Golem", "mob", 0.7F, 1.9F, "Passive mobs"));
    public static final EntityType SNOWBALL = register(new EntityType(93, "snowball", "Snowball", "projectile", 0.25F, 0.25F, "Projectiles"));
    public static final EntityType SPAWNER_MINECART = register(new EntityType(94, "spawner_minecart", "Minecart with Monster Spawner", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType SPECTRAL_ARROW = register(new EntityType(95, "spectral_arrow", "Spectral Arrow", "projectile", 0.5F, 0.5F, "Projectiles"));
    public static final EntityType SPIDER = register(new EntityType(96, "spider", "Spider", "hostile", 1.4F, 0.9F, "Hostile mobs"));
    public static final EntityType SQUID = register(new EntityType(97, "squid", "Squid", "water_creature", 0.8F, 0.8F, "Passive mobs"));
    public static final EntityType STRAY = register(new EntityType(98, "stray", "Stray", "hostile", 0.6F, 1.99F, "Hostile mobs"));
    public static final EntityType STRIDER = register(new EntityType(99, "strider", "Strider", "animal", 0.9F, 1.7F, "Passive mobs"));
    public static final EntityType TADPOLE = register(new EntityType(100, "tadpole", "Tadpole", "water_creature", 0.4F, 0.3F, "Passive mobs"));
    public static final EntityType TEXT_DISPLAY = register(new EntityType(101, "text_display", "Text Display", "other", 0F, 0F, "Immobile"));
    public static final EntityType TNT = register(new EntityType(102, "tnt", "Primed TNT", "other", 0.98F, 0.98F, "UNKNOWN"));
    public static final EntityType TNT_MINECART = register(new EntityType(103, "tnt_minecart", "Minecart with TNT", "other", 0.98F, 0.7F, "Vehicles"));
    public static final EntityType TRADER_LLAMA = register(new EntityType(104, "trader_llama", "Trader Llama", "animal", 0.9F, 1.87F, "Passive mobs"));
    public static final EntityType TRIDENT = register(new EntityType(105, "trident", "Trident", "projectile", 0.5F, 0.5F, "Projectiles"));
    public static final EntityType TROPICAL_FISH = register(new EntityType(106, "tropical_fish", "Tropical Fish", "water_creature", 0.5F, 0.4F, "Passive mobs"));
    public static final EntityType TURTLE = register(new EntityType(107, "turtle", "Turtle", "animal", 1.2F, 0.4F, "Passive mobs"));
    public static final EntityType VEX = register(new EntityType(108, "vex", "Vex", "hostile", 0.4F, 0.8F, "Hostile mobs"));
    public static final EntityType VILLAGER = register(new EntityType(109, "villager", "Villager", "passive", 0.6F, 1.95F, "Passive mobs"));
    public static final EntityType VINDICATOR = register(new EntityType(110, "vindicator", "Vindicator", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType WANDERING_TRADER = register(new EntityType(111, "wandering_trader", "Wandering Trader", "passive", 0.6F, 1.95F, "Passive mobs"));
    public static final EntityType WARDEN = register(new EntityType(112, "warden", "Warden", "hostile", 0.9F, 2.9F, "Hostile mobs"));
    public static final EntityType WIND_CHARGE = register(new EntityType(113, "wind_charge", "Wind Charge", "player", 0.3125F, 0.3125F, "Projectiles"));
    public static final EntityType WITCH = register(new EntityType(114, "witch", "Witch", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType WITHER = register(new EntityType(115, "wither", "Wither", "hostile", 0.9F, 3.5F, "Hostile mobs"));
    public static final EntityType WITHER_SKELETON = register(new EntityType(116, "wither_skeleton", "Wither Skeleton", "hostile", 0.7F, 2.4F, "Hostile mobs"));
    public static final EntityType WITHER_SKULL = register(new EntityType(117, "wither_skull", "Wither Skull", "projectile", 0.3125F, 0.3125F, "Projectiles"));
    public static final EntityType WOLF = register(new EntityType(118, "wolf", "Wolf", "animal", 0.6F, 0.85F, "Passive mobs"));
    public static final EntityType ZOGLIN = register(new EntityType(119, "zoglin", "Zoglin", "hostile", 1.3964844F, 1.4F, "Hostile mobs"));
    public static final EntityType ZOMBIE = register(new EntityType(120, "zombie", "Zombie", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType ZOMBIE_HORSE = register(new EntityType(121, "zombie_horse", "Zombie Horse", "animal", 1.3964844F, 1.6F, "Hostile mobs"));
    public static final EntityType ZOMBIE_VILLAGER = register(new EntityType(122, "zombie_villager", "Zombie Villager", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType ZOMBIFIED_PIGLIN = register(new EntityType(123, "zombified_piglin", "Zombified Piglin", "hostile", 0.6F, 1.95F, "Hostile mobs"));
    public static final EntityType PLAYER = register(new EntityType(124, "player", "Player", "player", 0.6F, 1.8F, "UNKNOWN"));
    public static final EntityType FISHING_BOBBER = register(new EntityType(125, "fishing_bobber", "Fishing Bobber", "projectile", 0.25F, 0.25F, "Projectiles"));

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
