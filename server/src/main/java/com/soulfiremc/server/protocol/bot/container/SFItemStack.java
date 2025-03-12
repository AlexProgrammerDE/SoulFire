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
package com.soulfiremc.server.protocol.bot.container;

import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.util.MathHelper;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;

@Getter
public final class SFItemStack extends ItemStack {
  public static final SFItemStack EMPTY = new SFItemStack(ItemType.AIR, 0);
  private final ItemType type;
  @Nullable
  private Entity entityRepresentation;

  private SFItemStack(ItemStack itemStack) {
    super(itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponentsPatch());
    this.type = ItemType.REGISTRY.getById(itemStack.getId());
  }

  private SFItemStack(ItemType itemType, int amount) {
    super(itemType.id(), amount, null);
    this.type = itemType;
  }

  public static @NonNull SFItemStack from(@Nullable ItemStack itemStack) {
    if (itemStack == null) {
      return SFItemStack.EMPTY;
    }

    return new SFItemStack(itemStack);
  }

  @Nullable
  public Entity getEntityRepresentation() {
    return !this.isEmpty() ? this.entityRepresentation : null;
  }

  public void setEntityRepresentation(@Nullable Entity entity) {
    if (!this.isEmpty()) {
      this.entityRepresentation = entity;
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isEmpty() {
    return this == EMPTY || this.type == ItemType.AIR || this.getAmount() <= 0;
  }

  @VisibleForTesting
  public static SFItemStack forTypeSingle(ItemType itemType) {
    return forTypeWithAmount(itemType, 1);
  }

  @VisibleForTesting
  public static SFItemStack forTypeWithAmount(ItemType itemType, int amount) {
    return new SFItemStack(itemType, amount);
  }

  public SFDataComponents getDataComponents() {
    var internalMap = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
    var newComponents = new SFDataComponents(internalMap);
    internalMap.putAll(type.components().components());

    var overrideComponents = super.getDataComponentsPatch();
    if (overrideComponents != null) {
      internalMap.putAll(overrideComponents.getDataComponents());
    }

    return newComponents;
  }

  public boolean canStackWith(SFItemStack other) {
    if (other == null) {
      return false;
    }

    return this.type == other.type;
  }

  public boolean has(DataComponentType<?> component) {
    return getDataComponents().getOptional(component).isPresent();
  }

  public <T> T get(DataComponentType<T> component) {
    return getDataComponents().get(component);
  }

  public <T> T getOrDefault(DataComponentType<T> component, T defaultValue) {
    return getDataComponents().getOptional(component).orElse(defaultValue);
  }

  public int getMaxStackSize() {
    return this.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
  }

  public boolean isStackable() {
    return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
  }

  public boolean isDamageableItem() {
    return this.has(DataComponentTypes.MAX_DAMAGE) && !this.has(DataComponentTypes.UNBREAKABLE) && this.has(DataComponentTypes.DAMAGE);
  }

  public boolean isDamaged() {
    return this.isDamageableItem() && this.getDamageValue() > 0;
  }

  public int getDamageValue() {
    return MathHelper.clamp(this.getOrDefault(DataComponentTypes.DAMAGE, 0), 0, this.getMaxDamage());
  }

  public int getMaxDamage() {
    return this.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
  }

  public boolean isBroken() {
    return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
  }

  public boolean nextDamageWillBreak() {
    return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
  }
}
