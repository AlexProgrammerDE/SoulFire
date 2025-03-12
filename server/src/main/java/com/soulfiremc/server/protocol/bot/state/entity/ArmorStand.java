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

import com.soulfiremc.server.data.EntityDimensions;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.EquipmentSlot;
import com.soulfiremc.server.data.NamedEntityData;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class ArmorStand extends LivingEntity {
  public static final int CLIENT_FLAG_SMALL = 1;
  public static final int CLIENT_FLAG_SHOW_ARMS = 4;
  public static final int CLIENT_FLAG_NO_BASEPLATE = 8;
  public static final int CLIENT_FLAG_MARKER = 16;
  private static final EntityDimensions MARKER_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);
  private static final EntityDimensions BABY_DIMENSIONS = EntityType.ARMOR_STAND.dimensions().scale(0.5F).withEyeHeight(0.9875F);
  private final Map<EquipmentSlot, SFItemStack> handItems = new EnumMap<>(EquipmentSlot.class);
  private final Map<EquipmentSlot, SFItemStack> armorItems = new EnumMap<>(EquipmentSlot.class);
  private int disabledSlots;

  public ArmorStand(Level level) {
    super(EntityType.ARMOR_STAND, level);
  }

  @Override
  public void refreshDimensions() {
    var x = this.x();
    var y = this.y();
    var z = this.z();
    super.refreshDimensions();
    this.setPos(x, y, z);
  }

  private boolean hasPhysics() {
    return !this.isMarker() && !this.isNoGravity();
  }

  @Override
  public boolean isEffectiveAi() {
    return super.isEffectiveAi() && this.hasPhysics();
  }

  @Override
  public Optional<SFItemStack> getItemBySlot(EquipmentSlot slot) {
    return switch (slot.type()) {
      case HAND -> Optional.ofNullable(this.handItems.get(slot));
      case HUMANOID_ARMOR -> Optional.ofNullable(this.armorItems.get(slot));
      case ANIMAL_ARMOR -> Optional.empty();
    };
  }

  @Override
  public void setItemSlot(EquipmentSlot slot, @Nullable SFItemStack item) {
    switch (slot.type()) {
      case HAND -> this.handItems.put(slot, item);
      case HUMANOID_ARMOR -> this.armorItems.put(slot, item);
    }
  }

  @Override
  public HandPreference getMainArm() {
    return HandPreference.RIGHT_HAND;
  }

  @Override
  public boolean canUseSlot(EquipmentSlot slot) {
    return slot != EquipmentSlot.BODY && !this.isDisabled(slot);
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  protected void doPush(Entity entity) {
  }

  @Override
  protected void pushEntities() {
    for (var entity : this.level().getEntities(this.getBoundingBox())
      .stream()
      .filter(entity -> entity instanceof AbstractMinecart minecart && minecart.isRideable())
      .toList()) {
      if (this.distanceToSqr(entity) <= 0.2) {
        entity.push(this);
      }
    }
  }

  @Override
  public void travel(Vector3d travelVector) {
    if (this.hasPhysics()) {
      super.travel(travelVector);
    }
  }

  @Override
  public boolean isBaby() {
    return this.isSmall();
  }

  @Override
  public void onSyncedDataUpdated(NamedEntityData entityData) {
    if (NamedEntityData.ARMOR_STAND__CLIENT_FLAGS.equals(entityData)) {
      this.refreshDimensions();
      this.blocksBuilding = !this.isMarker();
    }

    super.onSyncedDataUpdated(entityData);
  }

  @Override
  public EntityDimensions getDefaultDimensions(Pose pose) {
    return this.getDimensionsMarker(this.isMarker());
  }

  private EntityDimensions getDimensionsMarker(boolean isMarker) {
    if (isMarker) {
      return MARKER_DIMENSIONS;
    } else {
      return this.isBaby() ? BABY_DIMENSIONS : this.entityType().dimensions();
    }
  }

  private boolean isDisabled(EquipmentSlot slot) {
    return (this.disabledSlots & 1 << slot.getFilterBit(0)) != 0 || slot.type() == EquipmentSlot.Type.HAND && !this.showArms();
  }

  public boolean isSmall() {
    return (this.entityData.get(NamedEntityData.ARMOR_STAND__CLIENT_FLAGS, MetadataTypes.BYTE) & CLIENT_FLAG_SMALL) != 0;
  }

  public boolean showArms() {
    return (this.entityData.get(NamedEntityData.ARMOR_STAND__CLIENT_FLAGS, MetadataTypes.BYTE) & CLIENT_FLAG_SHOW_ARMS) != 0;
  }

  public boolean showBasePlate() {
    return (this.entityData.get(NamedEntityData.ARMOR_STAND__CLIENT_FLAGS, MetadataTypes.BYTE) & CLIENT_FLAG_NO_BASEPLATE) == 0;
  }

  public boolean isMarker() {
    return (this.entityData.get(NamedEntityData.ARMOR_STAND__CLIENT_FLAGS, MetadataTypes.BYTE) & CLIENT_FLAG_MARKER) != 0;
  }
}
