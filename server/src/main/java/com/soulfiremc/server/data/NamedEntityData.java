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
public record NamedEntityData(String key, int networkId, String entityClass) {
  public static final List<NamedEntityData> VALUES = new ArrayList<>();

  //@formatter:off
  /** Type: BYTE **/
  public static final NamedEntityData ABSTRACT_ARROW__ID_FLAGS = register("id_flags", 8, "abstract_arrow");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ABSTRACT_ARROW__IN_GROUND = register("in_ground", 10, "abstract_arrow");
  /** Type: BYTE **/
  public static final NamedEntityData ABSTRACT_ARROW__PIERCE_LEVEL = register("pierce_level", 9, "abstract_arrow");
  /** Type: INT **/
  public static final NamedEntityData ABSTRACT_BOAT__ID_BUBBLE_TIME = register("id_bubble_time", 13, "abstract_boat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ABSTRACT_BOAT__ID_PADDLE_LEFT = register("id_paddle_left", 11, "abstract_boat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ABSTRACT_BOAT__ID_PADDLE_RIGHT = register("id_paddle_right", 12, "abstract_boat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ABSTRACT_CHESTED_HORSE__ID_CHEST = register("id_chest", 18, "abstract_chested_horse");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ABSTRACT_FISH__FROM_BUCKET = register("from_bucket", 16, "abstract_fish");
  /** Type: BYTE **/
  public static final NamedEntityData ABSTRACT_HORSE__ID_FLAGS = register("id_flags", 17, "abstract_horse");
  /** Type: OPTIONAL_BLOCK_STATE **/
  public static final NamedEntityData ABSTRACT_MINECART__ID_CUSTOM_DISPLAY_BLOCK = register("id_custom_display_block", 11, "abstract_minecart");
  /** Type: INT **/
  public static final NamedEntityData ABSTRACT_MINECART__ID_DISPLAY_OFFSET = register("id_display_offset", 12, "abstract_minecart");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ABSTRACT_PIGLIN__IMMUNE_TO_ZOMBIFICATION = register("immune_to_zombification", 16, "abstract_piglin");
  /** Type: INT **/
  public static final NamedEntityData ABSTRACT_VILLAGER__UNHAPPY_COUNTER = register("unhappy_counter", 17, "abstract_villager");
  /** Type: BOOLEAN **/
  public static final NamedEntityData AGEABLE_MOB__BABY = register("baby", 16, "ageable_mob");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ALLAY__CAN_DUPLICATE = register("can_duplicate", 17, "allay");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ALLAY__DANCING = register("dancing", 16, "allay");
  /** Type: PARTICLE **/
  public static final NamedEntityData AREA_EFFECT_CLOUD__PARTICLE = register("particle", 10, "area_effect_cloud");
  /** Type: FLOAT **/
  public static final NamedEntityData AREA_EFFECT_CLOUD__RADIUS = register("radius", 8, "area_effect_cloud");
  /** Type: BOOLEAN **/
  public static final NamedEntityData AREA_EFFECT_CLOUD__WAITING = register("waiting", 9, "area_effect_cloud");
  /** Type: ARMADILLO_STATE **/
  public static final NamedEntityData ARMADILLO__ARMADILLO_STATE = register("armadillo_state", 17, "armadillo");
  /** Type: ROTATIONS **/
  public static final NamedEntityData ARMOR_STAND__BODY_POSE = register("body_pose", 17, "armor_stand");
  /** Type: BYTE **/
  public static final NamedEntityData ARMOR_STAND__CLIENT_FLAGS = register("client_flags", 15, "armor_stand");
  /** Type: ROTATIONS **/
  public static final NamedEntityData ARMOR_STAND__HEAD_POSE = register("head_pose", 16, "armor_stand");
  /** Type: ROTATIONS **/
  public static final NamedEntityData ARMOR_STAND__LEFT_ARM_POSE = register("left_arm_pose", 18, "armor_stand");
  /** Type: ROTATIONS **/
  public static final NamedEntityData ARMOR_STAND__LEFT_LEG_POSE = register("left_leg_pose", 20, "armor_stand");
  /** Type: ROTATIONS **/
  public static final NamedEntityData ARMOR_STAND__RIGHT_ARM_POSE = register("right_arm_pose", 19, "armor_stand");
  /** Type: ROTATIONS **/
  public static final NamedEntityData ARMOR_STAND__RIGHT_LEG_POSE = register("right_leg_pose", 21, "armor_stand");
  /** Type: INT **/
  public static final NamedEntityData ARROW__ID_EFFECT_COLOR = register("id_effect_color", 11, "arrow");
  /** Type: BOOLEAN **/
  public static final NamedEntityData AXOLOTL__FROM_BUCKET = register("from_bucket", 19, "axolotl");
  /** Type: BOOLEAN **/
  public static final NamedEntityData AXOLOTL__PLAYING_DEAD = register("playing_dead", 18, "axolotl");
  /** Type: INT **/
  public static final NamedEntityData AXOLOTL__VARIANT = register("variant", 17, "axolotl");
  /** Type: BYTE **/
  public static final NamedEntityData BAT__ID_FLAGS = register("id_flags", 16, "bat");
  /** Type: BYTE **/
  public static final NamedEntityData BEE__FLAGS = register("flags", 17, "bee");
  /** Type: INT **/
  public static final NamedEntityData BEE__REMAINING_ANGER_TIME = register("remaining_anger_time", 18, "bee");
  /** Type: BYTE **/
  public static final NamedEntityData BLAZE__FLAGS = register("flags", 16, "blaze");
  /** Type: BLOCK_STATE **/
  public static final NamedEntityData BLOCK_DISPLAY__BLOCK_STATE = register("block_state", 23, "block_display");
  /** Type: BOOLEAN **/
  public static final NamedEntityData BOGGED__SHEARED = register("sheared", 16, "bogged");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CAMEL__DASH = register("dash", 18, "camel");
  /** Type: LONG **/
  public static final NamedEntityData CAMEL__LAST_POSE_CHANGE_TICK = register("last_pose_change_tick", 19, "camel");
  /** Type: INT **/
  public static final NamedEntityData CAT__COLLAR_COLOR = register("collar_color", 22, "cat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CAT__IS_LYING = register("is_lying", 20, "cat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CAT__RELAX_STATE_ONE = register("relax_state_one", 21, "cat");
  /** Type: CAT_VARIANT **/
  public static final NamedEntityData CAT__VARIANT = register("variant", 19, "cat");
  /** Type: CHICKEN_VARIANT **/
  public static final NamedEntityData CHICKEN__VARIANT = register("variant", 17, "chicken");
  /** Type: COW_VARIANT **/
  public static final NamedEntityData COW__VARIANT = register("variant", 17, "cow");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CREAKING__CAN_MOVE = register("can_move", 16, "creaking");
  /** Type: OPTIONAL_BLOCK_POS **/
  public static final NamedEntityData CREAKING__HOME_POS = register("home_pos", 19, "creaking");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CREAKING__IS_ACTIVE = register("is_active", 17, "creaking");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CREAKING__IS_TEARING_DOWN = register("is_tearing_down", 18, "creaking");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CREEPER__IS_IGNITED = register("is_ignited", 18, "creeper");
  /** Type: BOOLEAN **/
  public static final NamedEntityData CREEPER__IS_POWERED = register("is_powered", 17, "creeper");
  /** Type: INT **/
  public static final NamedEntityData CREEPER__SWELL_DIR = register("swell_dir", 16, "creeper");
  /** Type: BYTE **/
  public static final NamedEntityData DISPLAY__BILLBOARD_RENDER_CONSTRAINTS = register("billboard_render_constraints", 15, "display");
  /** Type: INT **/
  public static final NamedEntityData DISPLAY__BRIGHTNESS_OVERRIDE = register("brightness_override", 16, "display");
  /** Type: INT **/
  public static final NamedEntityData DISPLAY__GLOW_COLOR_OVERRIDE = register("glow_color_override", 22, "display");
  /** Type: FLOAT **/
  public static final NamedEntityData DISPLAY__HEIGHT = register("height", 21, "display");
  /** Type: QUATERNION **/
  public static final NamedEntityData DISPLAY__LEFT_ROTATION = register("left_rotation", 13, "display");
  /** Type: INT **/
  public static final NamedEntityData DISPLAY__POS_ROT_INTERPOLATION_DURATION = register("pos_rot_interpolation_duration", 10, "display");
  /** Type: QUATERNION **/
  public static final NamedEntityData DISPLAY__RIGHT_ROTATION = register("right_rotation", 14, "display");
  /** Type: VECTOR3 **/
  public static final NamedEntityData DISPLAY__SCALE = register("scale", 12, "display");
  /** Type: FLOAT **/
  public static final NamedEntityData DISPLAY__SHADOW_RADIUS = register("shadow_radius", 18, "display");
  /** Type: FLOAT **/
  public static final NamedEntityData DISPLAY__SHADOW_STRENGTH = register("shadow_strength", 19, "display");
  /** Type: INT **/
  public static final NamedEntityData DISPLAY__TRANSFORMATION_INTERPOLATION_DURATION = register("transformation_interpolation_duration", 9, "display");
  /** Type: INT **/
  public static final NamedEntityData DISPLAY__TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS = register("transformation_interpolation_start_delta_ticks", 8, "display");
  /** Type: VECTOR3 **/
  public static final NamedEntityData DISPLAY__TRANSLATION = register("translation", 11, "display");
  /** Type: FLOAT **/
  public static final NamedEntityData DISPLAY__VIEW_RANGE = register("view_range", 17, "display");
  /** Type: FLOAT **/
  public static final NamedEntityData DISPLAY__WIDTH = register("width", 20, "display");
  /** Type: BOOLEAN **/
  public static final NamedEntityData DOLPHIN__GOT_FISH = register("got_fish", 17, "dolphin");
  /** Type: INT **/
  public static final NamedEntityData DOLPHIN__MOISTNESS_LEVEL = register("moistness_level", 18, "dolphin");
  /** Type: INT **/
  public static final NamedEntityData ENDER_DRAGON__PHASE = register("phase", 16, "ender_dragon");
  /** Type: OPTIONAL_BLOCK_STATE **/
  public static final NamedEntityData ENDER_MAN__CARRY_STATE = register("carry_state", 16, "ender_man");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ENDER_MAN__CREEPY = register("creepy", 17, "ender_man");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ENDER_MAN__STARED_AT = register("stared_at", 18, "ender_man");
  /** Type: OPTIONAL_BLOCK_POS **/
  public static final NamedEntityData END_CRYSTAL__BEAM_TARGET = register("beam_target", 8, "end_crystal");
  /** Type: BOOLEAN **/
  public static final NamedEntityData END_CRYSTAL__SHOW_BOTTOM = register("show_bottom", 9, "end_crystal");
  /** Type: INT **/
  public static final NamedEntityData ENTITY__AIR_SUPPLY = register("air_supply", 1, "entity");
  /** Type: OPTIONAL_COMPONENT **/
  public static final NamedEntityData ENTITY__CUSTOM_NAME = register("custom_name", 2, "entity");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ENTITY__CUSTOM_NAME_VISIBLE = register("custom_name_visible", 3, "entity");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ENTITY__NO_GRAVITY = register("no_gravity", 5, "entity");
  /** Type: POSE **/
  public static final NamedEntityData ENTITY__POSE = register("pose", 6, "entity");
  /** Type: BYTE **/
  public static final NamedEntityData ENTITY__SHARED_FLAGS = register("shared_flags", 0, "entity");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ENTITY__SILENT = register("silent", 4, "entity");
  /** Type: INT **/
  public static final NamedEntityData ENTITY__TICKS_FROZEN = register("ticks_frozen", 7, "entity");
  /** Type: INT **/
  public static final NamedEntityData EXPERIENCE_ORB__VALUE = register("value", 8, "experience_orb");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData EYE_OF_ENDER__ITEM_STACK = register("item_stack", 8, "eye_of_ender");
  /** Type: BLOCK_POS **/
  public static final NamedEntityData FALLING_BLOCK_ENTITY__START_POS = register("start_pos", 8, "falling_block_entity");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData FIREBALL__ITEM_STACK = register("item_stack", 8, "fireball");
  /** Type: OPTIONAL_UNSIGNED_INT **/
  public static final NamedEntityData FIREWORK_ROCKET_ENTITY__ATTACHED_TO_TARGET = register("attached_to_target", 9, "firework_rocket_entity");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData FIREWORK_ROCKET_ENTITY__ID_FIREWORKS_ITEM = register("id_fireworks_item", 8, "firework_rocket_entity");
  /** Type: BOOLEAN **/
  public static final NamedEntityData FIREWORK_ROCKET_ENTITY__SHOT_AT_ANGLE = register("shot_at_angle", 10, "firework_rocket_entity");
  /** Type: BOOLEAN **/
  public static final NamedEntityData FISHING_HOOK__BITING = register("biting", 9, "fishing_hook");
  /** Type: INT **/
  public static final NamedEntityData FISHING_HOOK__HOOKED_ENTITY = register("hooked_entity", 8, "fishing_hook");
  /** Type: BYTE **/
  public static final NamedEntityData FOX__FLAGS = register("flags", 18, "fox");
  /** Type: OPTIONAL_LIVING_ENTITY_REFERENCE **/
  public static final NamedEntityData FOX__TRUSTED_ID_0 = register("trusted_id_0", 19, "fox");
  /** Type: OPTIONAL_LIVING_ENTITY_REFERENCE **/
  public static final NamedEntityData FOX__TRUSTED_ID_1 = register("trusted_id_1", 20, "fox");
  /** Type: INT **/
  public static final NamedEntityData FOX__TYPE = register("type", 17, "fox");
  /** Type: OPTIONAL_UNSIGNED_INT **/
  public static final NamedEntityData FROG__TONGUE_TARGET = register("tongue_target", 18, "frog");
  /** Type: FROG_VARIANT **/
  public static final NamedEntityData FROG__VARIANT = register("variant", 17, "frog");
  /** Type: BOOLEAN **/
  public static final NamedEntityData GHAST__IS_CHARGING = register("is_charging", 16, "ghast");
  /** Type: INT **/
  public static final NamedEntityData GLOW_SQUID__DARK_TICKS_REMAINING = register("dark_ticks_remaining", 17, "glow_squid");
  /** Type: BOOLEAN **/
  public static final NamedEntityData GOAT__HAS_LEFT_HORN = register("has_left_horn", 18, "goat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData GOAT__HAS_RIGHT_HORN = register("has_right_horn", 19, "goat");
  /** Type: BOOLEAN **/
  public static final NamedEntityData GOAT__IS_SCREAMING_GOAT = register("is_screaming_goat", 17, "goat");
  /** Type: INT **/
  public static final NamedEntityData GUARDIAN__ID_ATTACK_TARGET = register("id_attack_target", 17, "guardian");
  /** Type: BOOLEAN **/
  public static final NamedEntityData GUARDIAN__ID_MOVING = register("id_moving", 16, "guardian");
  /** Type: BOOLEAN **/
  public static final NamedEntityData HOGLIN__IMMUNE_TO_ZOMBIFICATION = register("immune_to_zombification", 17, "hoglin");
  /** Type: INT **/
  public static final NamedEntityData HORSE__ID_TYPE_VARIANT = register("id_type_variant", 18, "horse");
  /** Type: FLOAT **/
  public static final NamedEntityData INTERACTION__HEIGHT = register("height", 9, "interaction");
  /** Type: BOOLEAN **/
  public static final NamedEntityData INTERACTION__RESPONSE = register("response", 10, "interaction");
  /** Type: FLOAT **/
  public static final NamedEntityData INTERACTION__WIDTH = register("width", 8, "interaction");
  /** Type: BYTE **/
  public static final NamedEntityData IRON_GOLEM__FLAGS = register("flags", 16, "iron_golem");
  /** Type: BYTE **/
  public static final NamedEntityData ITEM_DISPLAY__ITEM_DISPLAY = register("item_display", 24, "item_display");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData ITEM_DISPLAY__ITEM_STACK = register("item_stack", 23, "item_display");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData ITEM_ENTITY__ITEM = register("item", 8, "item_entity");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData ITEM_FRAME__ITEM = register("item", 8, "item_frame");
  /** Type: INT **/
  public static final NamedEntityData ITEM_FRAME__ROTATION = register("rotation", 9, "item_frame");
  /** Type: INT **/
  public static final NamedEntityData LIVING_ENTITY__ARROW_COUNT = register("arrow_count", 12, "living_entity");
  /** Type: BOOLEAN **/
  public static final NamedEntityData LIVING_ENTITY__EFFECT_AMBIENCE = register("effect_ambience", 11, "living_entity");
  /** Type: PARTICLES **/
  public static final NamedEntityData LIVING_ENTITY__EFFECT_PARTICLES = register("effect_particles", 10, "living_entity");
  /** Type: FLOAT **/
  public static final NamedEntityData LIVING_ENTITY__HEALTH = register("health", 9, "living_entity");
  /** Type: BYTE **/
  public static final NamedEntityData LIVING_ENTITY__LIVING_ENTITY_FLAGS = register("living_entity_flags", 8, "living_entity");
  /** Type: OPTIONAL_BLOCK_POS **/
  public static final NamedEntityData LIVING_ENTITY__SLEEPING_POS = register("sleeping_pos", 14, "living_entity");
  /** Type: INT **/
  public static final NamedEntityData LIVING_ENTITY__STINGER_COUNT = register("stinger_count", 13, "living_entity");
  /** Type: INT **/
  public static final NamedEntityData LLAMA__STRENGTH = register("strength", 19, "llama");
  /** Type: INT **/
  public static final NamedEntityData LLAMA__VARIANT = register("variant", 20, "llama");
  /** Type: STRING **/
  public static final NamedEntityData MINECART_COMMAND_BLOCK__ID_COMMAND_NAME = register("id_command_name", 13, "minecart_command_block");
  /** Type: COMPONENT **/
  public static final NamedEntityData MINECART_COMMAND_BLOCK__ID_LAST_OUTPUT = register("id_last_output", 14, "minecart_command_block");
  /** Type: BOOLEAN **/
  public static final NamedEntityData MINECART_FURNACE__ID_FUEL = register("id_fuel", 13, "minecart_furnace");
  /** Type: BYTE **/
  public static final NamedEntityData MOB__MOB_FLAGS = register("mob_flags", 15, "mob");
  /** Type: INT **/
  public static final NamedEntityData MUSHROOM_COW__TYPE = register("type", 17, "mushroom_cow");
  /** Type: BOOLEAN **/
  public static final NamedEntityData OCELOT__TRUSTING = register("trusting", 17, "ocelot");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData OMINOUS_ITEM_SPAWNER__ITEM = register("item", 8, "ominous_item_spawner");
  /** Type: PAINTING_VARIANT **/
  public static final NamedEntityData PAINTING__PAINTING_VARIANT = register("painting_variant", 8, "painting");
  /** Type: INT **/
  public static final NamedEntityData PANDA__EAT_COUNTER = register("eat_counter", 19, "panda");
  /** Type: BYTE **/
  public static final NamedEntityData PANDA__HIDDEN_GENE = register("hidden_gene", 21, "panda");
  /** Type: BYTE **/
  public static final NamedEntityData PANDA__ID_FLAGS = register("id_flags", 22, "panda");
  /** Type: BYTE **/
  public static final NamedEntityData PANDA__MAIN_GENE = register("main_gene", 20, "panda");
  /** Type: INT **/
  public static final NamedEntityData PANDA__SNEEZE_COUNTER = register("sneeze_counter", 18, "panda");
  /** Type: INT **/
  public static final NamedEntityData PANDA__UNHAPPY_COUNTER = register("unhappy_counter", 17, "panda");
  /** Type: INT **/
  public static final NamedEntityData PARROT__VARIANT = register("variant", 19, "parrot");
  /** Type: INT **/
  public static final NamedEntityData PHANTOM__ID_SIZE = register("id_size", 16, "phantom");
  /** Type: BOOLEAN **/
  public static final NamedEntityData PIGLIN__BABY = register("baby", 17, "piglin");
  /** Type: BOOLEAN **/
  public static final NamedEntityData PIGLIN__IS_CHARGING_CROSSBOW = register("is_charging_crossbow", 18, "piglin");
  /** Type: BOOLEAN **/
  public static final NamedEntityData PIGLIN__IS_DANCING = register("is_dancing", 19, "piglin");
  /** Type: INT **/
  public static final NamedEntityData PIG__BOOST_TIME = register("boost_time", 17, "pig");
  /** Type: PIG_VARIANT **/
  public static final NamedEntityData PIG__VARIANT = register("variant", 18, "pig");
  /** Type: BOOLEAN **/
  public static final NamedEntityData PILLAGER__IS_CHARGING_CROSSBOW = register("is_charging_crossbow", 17, "pillager");
  /** Type: FLOAT **/
  public static final NamedEntityData PLAYER__PLAYER_ABSORPTION = register("player_absorption", 15, "player");
  /** Type: BYTE **/
  public static final NamedEntityData PLAYER__PLAYER_MAIN_HAND = register("player_main_hand", 18, "player");
  /** Type: BYTE **/
  public static final NamedEntityData PLAYER__PLAYER_MODE_CUSTOMISATION = register("player_mode_customisation", 17, "player");
  /** Type: INT **/
  public static final NamedEntityData PLAYER__SCORE = register("score", 16, "player");
  /** Type: COMPOUND_TAG **/
  public static final NamedEntityData PLAYER__SHOULDER_LEFT = register("shoulder_left", 19, "player");
  /** Type: COMPOUND_TAG **/
  public static final NamedEntityData PLAYER__SHOULDER_RIGHT = register("shoulder_right", 20, "player");
  /** Type: BOOLEAN **/
  public static final NamedEntityData POLAR_BEAR__STANDING = register("standing", 17, "polar_bear");
  /** Type: BLOCK_STATE **/
  public static final NamedEntityData PRIMED_TNT__BLOCK_STATE = register("block_state", 9, "primed_tnt");
  /** Type: INT **/
  public static final NamedEntityData PRIMED_TNT__FUSE = register("fuse", 8, "primed_tnt");
  /** Type: INT **/
  public static final NamedEntityData PUFFERFISH__PUFF_STATE = register("puff_state", 17, "pufferfish");
  /** Type: INT **/
  public static final NamedEntityData RABBIT__TYPE = register("type", 17, "rabbit");
  /** Type: BOOLEAN **/
  public static final NamedEntityData RAIDER__IS_CELEBRATING = register("is_celebrating", 16, "raider");
  /** Type: INT **/
  public static final NamedEntityData SALMON__TYPE = register("type", 17, "salmon");
  /** Type: BYTE **/
  public static final NamedEntityData SHEEP__WOOL = register("wool", 17, "sheep");
  /** Type: DIRECTION **/
  public static final NamedEntityData SHULKER__ATTACH_FACE = register("attach_face", 16, "shulker");
  /** Type: BYTE **/
  public static final NamedEntityData SHULKER__COLOR = register("color", 18, "shulker");
  /** Type: BYTE **/
  public static final NamedEntityData SHULKER__PEEK = register("peek", 17, "shulker");
  /** Type: BOOLEAN **/
  public static final NamedEntityData SKELETON__STRAY_CONVERSION = register("stray_conversion", 16, "skeleton");
  /** Type: INT **/
  public static final NamedEntityData SLIME__ID_SIZE = register("id_size", 16, "slime");
  /** Type: INT **/
  public static final NamedEntityData SNIFFER__DROP_SEED_AT_TICK = register("drop_seed_at_tick", 18, "sniffer");
  /** Type: SNIFFER_STATE **/
  public static final NamedEntityData SNIFFER__STATE = register("state", 17, "sniffer");
  /** Type: BYTE **/
  public static final NamedEntityData SNOW_GOLEM__PUMPKIN = register("pumpkin", 16, "snow_golem");
  /** Type: BYTE **/
  public static final NamedEntityData SPELLCASTER_ILLAGER__SPELL_CASTING = register("spell_casting", 17, "spellcaster_illager");
  /** Type: BYTE **/
  public static final NamedEntityData SPIDER__FLAGS = register("flags", 16, "spider");
  /** Type: INT **/
  public static final NamedEntityData STRIDER__BOOST_TIME = register("boost_time", 17, "strider");
  /** Type: BOOLEAN **/
  public static final NamedEntityData STRIDER__SUFFOCATING = register("suffocating", 18, "strider");
  /** Type: BYTE **/
  public static final NamedEntityData TAMABLE_ANIMAL__FLAGS = register("flags", 17, "tamable_animal");
  /** Type: OPTIONAL_LIVING_ENTITY_REFERENCE **/
  public static final NamedEntityData TAMABLE_ANIMAL__OWNERUUID = register("owneruuid", 18, "tamable_animal");
  /** Type: INT **/
  public static final NamedEntityData TEXT_DISPLAY__BACKGROUND_COLOR = register("background_color", 25, "text_display");
  /** Type: INT **/
  public static final NamedEntityData TEXT_DISPLAY__LINE_WIDTH = register("line_width", 24, "text_display");
  /** Type: BYTE **/
  public static final NamedEntityData TEXT_DISPLAY__STYLE_FLAGS = register("style_flags", 27, "text_display");
  /** Type: COMPONENT **/
  public static final NamedEntityData TEXT_DISPLAY__TEXT = register("text", 23, "text_display");
  /** Type: BYTE **/
  public static final NamedEntityData TEXT_DISPLAY__TEXT_OPACITY = register("text_opacity", 26, "text_display");
  /** Type: ITEM_STACK **/
  public static final NamedEntityData THROWABLE_ITEM_PROJECTILE__ITEM_STACK = register("item_stack", 8, "throwable_item_projectile");
  /** Type: BOOLEAN **/
  public static final NamedEntityData THROWN_TRIDENT__ID_FOIL = register("id_foil", 12, "thrown_trident");
  /** Type: BYTE **/
  public static final NamedEntityData THROWN_TRIDENT__ID_LOYALTY = register("id_loyalty", 11, "thrown_trident");
  /** Type: INT **/
  public static final NamedEntityData TROPICAL_FISH__ID_TYPE_VARIANT = register("id_type_variant", 17, "tropical_fish");
  /** Type: BOOLEAN **/
  public static final NamedEntityData TURTLE__HAS_EGG = register("has_egg", 17, "turtle");
  /** Type: BOOLEAN **/
  public static final NamedEntityData TURTLE__LAYING_EGG = register("laying_egg", 18, "turtle");
  /** Type: FLOAT **/
  public static final NamedEntityData VEHICLE_ENTITY__ID_DAMAGE = register("id_damage", 10, "vehicle_entity");
  /** Type: INT **/
  public static final NamedEntityData VEHICLE_ENTITY__ID_HURT = register("id_hurt", 8, "vehicle_entity");
  /** Type: INT **/
  public static final NamedEntityData VEHICLE_ENTITY__ID_HURTDIR = register("id_hurtdir", 9, "vehicle_entity");
  /** Type: BYTE **/
  public static final NamedEntityData VEX__FLAGS = register("flags", 16, "vex");
  /** Type: VILLAGER_DATA **/
  public static final NamedEntityData VILLAGER__VILLAGER_DATA = register("villager_data", 18, "villager");
  /** Type: INT **/
  public static final NamedEntityData WARDEN__CLIENT_ANGER_LEVEL = register("client_anger_level", 16, "warden");
  /** Type: BOOLEAN **/
  public static final NamedEntityData WITCH__USING_ITEM = register("using_item", 17, "witch");
  /** Type: INT **/
  public static final NamedEntityData WITHER_BOSS__ID_INV = register("id_inv", 19, "wither_boss");
  /** Type: INT **/
  public static final NamedEntityData WITHER_BOSS__TARGET_A = register("target_a", 16, "wither_boss");
  /** Type: INT **/
  public static final NamedEntityData WITHER_BOSS__TARGET_B = register("target_b", 17, "wither_boss");
  /** Type: INT **/
  public static final NamedEntityData WITHER_BOSS__TARGET_C = register("target_c", 18, "wither_boss");
  /** Type: BOOLEAN **/
  public static final NamedEntityData WITHER_SKULL__DANGEROUS = register("dangerous", 8, "wither_skull");
  /** Type: INT **/
  public static final NamedEntityData WOLF__COLLAR_COLOR = register("collar_color", 20, "wolf");
  /** Type: BOOLEAN **/
  public static final NamedEntityData WOLF__INTERESTED = register("interested", 19, "wolf");
  /** Type: INT **/
  public static final NamedEntityData WOLF__REMAINING_ANGER_TIME = register("remaining_anger_time", 21, "wolf");
  /** Type: WOLF_SOUND_VARIANT **/
  public static final NamedEntityData WOLF__SOUND_VARIANT = register("sound_variant", 23, "wolf");
  /** Type: WOLF_VARIANT **/
  public static final NamedEntityData WOLF__VARIANT = register("variant", 22, "wolf");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ZOGLIN__BABY = register("baby", 16, "zoglin");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ZOMBIE_VILLAGER__CONVERTING = register("converting", 19, "zombie_villager");
  /** Type: VILLAGER_DATA **/
  public static final NamedEntityData ZOMBIE_VILLAGER__VILLAGER_DATA = register("villager_data", 20, "zombie_villager");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ZOMBIE__BABY = register("baby", 16, "zombie");
  /** Type: BOOLEAN **/
  public static final NamedEntityData ZOMBIE__DROWNED_CONVERSION = register("drowned_conversion", 18, "zombie");
  /** Type: INT **/
  public static final NamedEntityData ZOMBIE__SPECIAL_TYPE = register("special_type", 17, "zombie");
  //@formatter:on

  public static NamedEntityData register(String key, int networkId, String entityClass) {
    var instance = new NamedEntityData(key, networkId, entityClass);
    VALUES.add(instance);
    return instance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NamedEntityData other)) {
      return false;
    }
    return key.equals(other.key) && entityClass.equals(other.entityClass);
  }

  @Override
  public int hashCode() {
    return key.hashCode() + entityClass.hashCode();
  }
}
