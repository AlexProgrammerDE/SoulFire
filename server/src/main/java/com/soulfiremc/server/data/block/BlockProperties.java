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
package com.soulfiremc.server.data.block;

@SuppressWarnings("unused")
public class BlockProperties {
  //@formatter:off
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
  public static final BooleanProperty ATTACHED = BooleanProperty.create("attached");
  public static final BooleanProperty BERRIES = BooleanProperty.create("berries");
  public static final BooleanProperty BLOOM = BooleanProperty.create("bloom");
  public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
  public static final BooleanProperty CAN_SUMMON = BooleanProperty.create("can_summon");
  public static final BooleanProperty CONDITIONAL = BooleanProperty.create("conditional");
  public static final BooleanProperty DISARMED = BooleanProperty.create("disarmed");
  public static final BooleanProperty DRAG = BooleanProperty.create("drag");
  public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
  public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");
  public static final BooleanProperty EYE = BooleanProperty.create("eye");
  public static final BooleanProperty FALLING = BooleanProperty.create("falling");
  public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
  public static final BooleanProperty HAS_BOTTLE_0 = BooleanProperty.create("has_bottle_0");
  public static final BooleanProperty HAS_BOTTLE_1 = BooleanProperty.create("has_bottle_1");
  public static final BooleanProperty HAS_BOTTLE_2 = BooleanProperty.create("has_bottle_2");
  public static final BooleanProperty HAS_RECORD = BooleanProperty.create("has_record");
  public static final BooleanProperty HAS_BOOK = BooleanProperty.create("has_book");
  public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
  public static final BooleanProperty IN_WALL = BooleanProperty.create("in_wall");
  public static final BooleanProperty LIT = BooleanProperty.create("lit");
  public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
  public static final BooleanProperty NATURAL = BooleanProperty.create("natural");
  public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
  public static final BooleanProperty OPEN = BooleanProperty.create("open");
  public static final BooleanProperty PERSISTENT = BooleanProperty.create("persistent");
  public static final BooleanProperty POWERED = BooleanProperty.create("powered");
  public static final BooleanProperty SHORT = BooleanProperty.create("short");
  public static final BooleanProperty SHRIEKING = BooleanProperty.create("shrieking");
  public static final BooleanProperty SIGNAL_FIRE = BooleanProperty.create("signal_fire");
  public static final BooleanProperty SNOWY = BooleanProperty.create("snowy");
  public static final BooleanProperty TIP = BooleanProperty.create("tip");
  public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");
  public static final BooleanProperty UNSTABLE = BooleanProperty.create("unstable");
  public static final BooleanProperty WATERLOGGED = BooleanProperty.create("waterlogged");
  public static final EnumProperty HORIZONTAL_AXIS = EnumProperty.create("axis");
  public static final EnumProperty AXIS = EnumProperty.create("axis");
  public static final BooleanProperty UP = BooleanProperty.create("up");
  public static final BooleanProperty DOWN = BooleanProperty.create("down");
  public static final BooleanProperty NORTH = BooleanProperty.create("north");
  public static final BooleanProperty EAST = BooleanProperty.create("east");
  public static final BooleanProperty SOUTH = BooleanProperty.create("south");
  public static final BooleanProperty WEST = BooleanProperty.create("west");
  public static final EnumProperty FACING = EnumProperty.create("facing");
  public static final EnumProperty FACING_HOPPER = EnumProperty.create("facing");
  public static final EnumProperty HORIZONTAL_FACING = EnumProperty.create("facing");
  public static final IntegerProperty FLOWER_AMOUNT = IntegerProperty.create("flower_amount");
  public static final EnumProperty ORIENTATION = EnumProperty.create("orientation");
  public static final EnumProperty ATTACH_FACE = EnumProperty.create("face");
  public static final EnumProperty BELL_ATTACHMENT = EnumProperty.create("attachment");
  public static final EnumProperty EAST_WALL = EnumProperty.create("east");
  public static final EnumProperty NORTH_WALL = EnumProperty.create("north");
  public static final EnumProperty SOUTH_WALL = EnumProperty.create("south");
  public static final EnumProperty WEST_WALL = EnumProperty.create("west");
  public static final EnumProperty EAST_REDSTONE = EnumProperty.create("east");
  public static final EnumProperty NORTH_REDSTONE = EnumProperty.create("north");
  public static final EnumProperty SOUTH_REDSTONE = EnumProperty.create("south");
  public static final EnumProperty WEST_REDSTONE = EnumProperty.create("west");
  public static final EnumProperty DOUBLE_BLOCK_HALF = EnumProperty.create("half");
  public static final EnumProperty HALF = EnumProperty.create("half");
  public static final EnumProperty RAIL_SHAPE = EnumProperty.create("shape");
  public static final EnumProperty RAIL_SHAPE_STRAIGHT = EnumProperty.create("shape");
  public static final IntegerProperty AGE_1 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_2 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_3 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_4 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_5 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_7 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_15 = IntegerProperty.create("age");
  public static final IntegerProperty AGE_25 = IntegerProperty.create("age");
  public static final IntegerProperty BITES = IntegerProperty.create("bites");
  public static final IntegerProperty CANDLES = IntegerProperty.create("candles");
  public static final IntegerProperty DELAY = IntegerProperty.create("delay");
  public static final IntegerProperty DISTANCE = IntegerProperty.create("distance");
  public static final IntegerProperty EGGS = IntegerProperty.create("eggs");
  public static final IntegerProperty HATCH = IntegerProperty.create("hatch");
  public static final IntegerProperty LAYERS = IntegerProperty.create("layers");
  public static final IntegerProperty LEVEL_CAULDRON = IntegerProperty.create("level");
  public static final IntegerProperty LEVEL_COMPOSTER = IntegerProperty.create("level");
  public static final IntegerProperty LEVEL_FLOWING = IntegerProperty.create("level");
  public static final IntegerProperty LEVEL_HONEY = IntegerProperty.create("honey_level");
  public static final IntegerProperty LEVEL = IntegerProperty.create("level");
  public static final IntegerProperty MOISTURE = IntegerProperty.create("moisture");
  public static final IntegerProperty NOTE = IntegerProperty.create("note");
  public static final IntegerProperty PICKLES = IntegerProperty.create("pickles");
  public static final IntegerProperty POWER = IntegerProperty.create("power");
  public static final IntegerProperty STAGE = IntegerProperty.create("stage");
  public static final IntegerProperty STABILITY_DISTANCE = IntegerProperty.create("distance");
  public static final IntegerProperty RESPAWN_ANCHOR_CHARGES = IntegerProperty.create("charges");
  public static final IntegerProperty ROTATION_16 = IntegerProperty.create("rotation");
  public static final EnumProperty BED_PART = EnumProperty.create("part");
  public static final EnumProperty CHEST_TYPE = EnumProperty.create("type");
  public static final EnumProperty MODE_COMPARATOR = EnumProperty.create("mode");
  public static final EnumProperty DOOR_HINGE = EnumProperty.create("hinge");
  public static final EnumProperty NOTEBLOCK_INSTRUMENT = EnumProperty.create("instrument");
  public static final EnumProperty PISTON_TYPE = EnumProperty.create("type");
  public static final EnumProperty SLAB_TYPE = EnumProperty.create("type");
  public static final EnumProperty STAIRS_SHAPE = EnumProperty.create("shape");
  public static final EnumProperty STRUCTUREBLOCK_MODE = EnumProperty.create("mode");
  public static final EnumProperty BAMBOO_LEAVES = EnumProperty.create("leaves");
  public static final EnumProperty TILT = EnumProperty.create("tilt");
  public static final EnumProperty VERTICAL_DIRECTION = EnumProperty.create("vertical_direction");
  public static final EnumProperty DRIPSTONE_THICKNESS = EnumProperty.create("thickness");
  public static final EnumProperty SCULK_SENSOR_PHASE = EnumProperty.create("sculk_sensor_phase");
  public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_0_OCCUPIED = BooleanProperty.create("slot_0_occupied");
  public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_1_OCCUPIED = BooleanProperty.create("slot_1_occupied");
  public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_2_OCCUPIED = BooleanProperty.create("slot_2_occupied");
  public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_3_OCCUPIED = BooleanProperty.create("slot_3_occupied");
  public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_4_OCCUPIED = BooleanProperty.create("slot_4_occupied");
  public static final BooleanProperty CHISELED_BOOKSHELF_SLOT_5_OCCUPIED = BooleanProperty.create("slot_5_occupied");
  public static final IntegerProperty DUSTED = IntegerProperty.create("dusted");
  public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
  public static final BooleanProperty CRAFTING = BooleanProperty.create("crafting");
  public static final EnumProperty TRIAL_SPAWNER_STATE = EnumProperty.create("trial_spawner_state");
  public static final EnumProperty VAULT_STATE = EnumProperty.create("vault_state");
  public static final BooleanProperty OMINOUS = BooleanProperty.create("ominous");
  //@formatter:on

  private BlockProperties() {}
}
