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
import com.soulfiremc.server.data.NamedEntityData;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.Level;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class Mob extends LivingEntity {
  private static final int MOB_FLAG_NO_AI = 1;
  private static final int MOB_FLAG_LEFTHANDED = 2;
  private static final int MOB_FLAG_AGGRESSIVE = 4;
  private final Map<EquipmentSlot, SFItemStack> slots = new EnumMap<>(EquipmentSlot.class);

  public Mob(EntityType entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public Optional<SFItemStack> getItemBySlot(EquipmentSlot slot) {
    return Optional.ofNullable(slots.get(slot));
  }

  @Override
  public void setItemSlot(EquipmentSlot slot, @Nullable SFItemStack item) {
    slots.put(slot, item);
  }

  @Override
  public boolean isEffectiveAi() {
    return super.isEffectiveAi() && !this.isNoAi();
  }

  public boolean isNoAi() {
    return (this.metadataState.getMetadata(NamedEntityData.MOB__MOB_FLAGS, MetadataTypes.BYTE) & MOB_FLAG_NO_AI) != 0;
  }

  public boolean isLeftHanded() {
    return (this.metadataState.getMetadata(NamedEntityData.MOB__MOB_FLAGS, MetadataTypes.BYTE) & MOB_FLAG_LEFTHANDED) != 0;
  }

  public boolean isAggressive() {
    return (this.metadataState.getMetadata(NamedEntityData.MOB__MOB_FLAGS, MetadataTypes.BYTE) & MOB_FLAG_AGGRESSIVE) != 0;
  }
}
