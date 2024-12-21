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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EquipmentSlot {
  MAINHAND(Type.HAND),
  OFFHAND(Type.HAND),
  FEET(Type.HUMANOID_ARMOR),
  LEGS(Type.HUMANOID_ARMOR),
  CHEST(Type.HUMANOID_ARMOR),
  HEAD(Type.HUMANOID_ARMOR),
  BODY(Type.ANIMAL_ARMOR);

  public static final EquipmentSlot[] VALUES = values();

  private final Type type;

  public static EquipmentSlot fromMCPl(org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot slot) {
    return switch (slot) {
      case MAIN_HAND -> MAINHAND;
      case OFF_HAND -> OFFHAND;
      case BOOTS -> FEET;
      case LEGGINGS -> LEGS;
      case CHESTPLATE -> CHEST;
      case HELMET -> HEAD;
      case BODY -> BODY;
    };
  }

  public boolean isHumanoidArmor() {
    return type == Type.HUMANOID_ARMOR;
  }

  public boolean isArmor() {
    return type == Type.HUMANOID_ARMOR || type == Type.ANIMAL_ARMOR;
  }

  public boolean isHand() {
    return type == Type.HAND;
  }

  public org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot toMCPl() {
    return switch (this) {
      case MAINHAND -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.MAIN_HAND;
      case OFFHAND -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.OFF_HAND;
      case FEET -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.BOOTS;
      case LEGS -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.LEGGINGS;
      case CHEST -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.CHESTPLATE;
      case HEAD -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.HELMET;
      case BODY -> org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot.BODY;
    };
  }

  public enum Type {
    HAND,
    HUMANOID_ARMOR,
    ANIMAL_ARMOR
  }
}
