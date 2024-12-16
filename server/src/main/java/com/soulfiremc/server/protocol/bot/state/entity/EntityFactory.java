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
import com.soulfiremc.server.data.EquipmentSlot;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityFactory {
  public static Entity createEntity(BotConnection connection, EntityType entityType, Level level, UUID uuid) {
    if (entityType.playerEntity()) {
      return new RemotePlayer(connection, level, connection.getEntityProfile(uuid).orElseThrow().getProfile());
    } else if (entityType.livingEntity()) {
      // TODO: Implement entity inventories
      return new LivingEntity(entityType, level) {
        @Override
        public Optional<SFItemStack> getItemBySlot(EquipmentSlot slot) {
          return Optional.empty();
        }

        @Override
        public void setItemSlot(EquipmentSlot slot, @Nullable SFItemStack item) {
        }
      };
    } else if (entityType.boatEntity()) {
      return new AbstractBoat(entityType, level);
    } else if (entityType.minecartEntity()) {
      return new AbstractMinecart(entityType, level);
    } else if (entityType.windChargeEntity()) {
      return new AbstractWindCharge(entityType, level);
    } else if (entityType.shulkerEntity()) {
      return new Shulker(entityType, level);
    } else {
      return new Entity(entityType, level);
    }
  }
}
