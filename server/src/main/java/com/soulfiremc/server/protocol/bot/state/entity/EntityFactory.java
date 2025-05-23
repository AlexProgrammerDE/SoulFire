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
package com.soulfiremc.server.protocol.bot.state.entity;

import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class EntityFactory {
  public static Optional<Entity> createEntity(BotConnection connection, EntityType entityType, Level level, UUID uuid) {
    if (entityType == EntityType.PLAYER) {
      return connection.getEntityProfile(uuid).map(
        playerListEntry -> new RemotePlayer(connection, level, Objects.requireNonNull(playerListEntry.getProfile(), "Player profile is null")));
    } else if (entityType.boatEntity()) {
      return Optional.of(new AbstractBoat(entityType, level));
    } else if (entityType.minecartEntity()) {
      return Optional.of(new AbstractMinecart(entityType, level));
    } else if (entityType.windChargeEntity()) {
      return Optional.of(new AbstractWindCharge(entityType, level));
    } else if (entityType.shulkerEntity()) {
      return Optional.of(new Shulker(entityType, level));
    } else if (entityType == EntityType.BLOCK_DISPLAY) {
      return Optional.of(new Display.BlockDisplay(entityType, level));
    } else if (entityType == EntityType.ITEM_DISPLAY) {
      return Optional.of(new Display.ItemDisplay(entityType, level));
    } else if (entityType == EntityType.TEXT_DISPLAY) {
      return Optional.of(new Display.TextDisplay(entityType, level));
    } else if (entityType == EntityType.ARMOR_STAND) {
      return Optional.of(new ArmorStand(level));
    } else if (entityType.mobEntity()) {
      return Optional.of(new Mob(entityType, level));
    } else if (entityType.livingEntity()) {
      throw new IllegalStateException("Unknown subtype of living entities!");
    } else {
      return Optional.of(new Entity(entityType, level));
    }
  }
}
