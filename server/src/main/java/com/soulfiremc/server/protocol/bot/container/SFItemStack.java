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
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Objects;

public final class SFItemStack {
  public static final SFItemStack EMPTY = new SFItemStack(ItemType.AIR, 0);
  @Getter
  private final ItemType type;
  private final @Nullable DataComponents dataComponentsPatch;
  private int count;
  @Nullable
  private Entity entityRepresentation;

  private SFItemStack(ItemStack itemStack) {
    this(ItemType.REGISTRY.getById(itemStack.getId()), itemStack.getAmount(), itemStack.getDataComponentsPatch());
  }

  private SFItemStack(ItemType itemType, int count) {
    this(itemType, count, null);
  }

  private SFItemStack(ItemType itemType, int count, @Nullable DataComponents dataComponentsPatch) {
    this.type = itemType;
    this.count = count;
    this.dataComponentsPatch = dataComponentsPatch;
  }

  public static @NonNull SFItemStack from(@Nullable ItemStack itemStack) {
    if (itemStack == null || itemStack.getId() == ItemType.AIR.id() || itemStack.getAmount() <= 0) {
      return SFItemStack.EMPTY;
    }

    return new SFItemStack(itemStack);
  }

  public static boolean isSameItem(SFItemStack stack, SFItemStack other) {
    return stack.type == other.type;
  }

  public static boolean isSameItemSameComponents(SFItemStack stack, SFItemStack other) {
    if (stack.type != other.type) {
      return false;
    } else {
      return (stack.isEmpty() && other.isEmpty()) || Objects.equals(stack.dataComponentsPatch, other.dataComponentsPatch);
    }
  }

  @VisibleForTesting
  public static SFItemStack forTypeSingle(ItemType itemType) {
    return forTypeWithAmount(itemType, 1);
  }

  @VisibleForTesting
  public static SFItemStack forTypeWithAmount(ItemType itemType, int amount) {
    return new SFItemStack(itemType, amount);
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

  public @Nullable ItemStack toMCPL() {
    if (this.isEmpty()) {
      return null;
    }

    return new ItemStack(type.id(), count, dataComponentsPatch);
  }

  public boolean isEmpty() {
    return this == EMPTY || this.type == ItemType.AIR || this.count <= 0;
  }

  public SFDataComponents getDataComponents() {
    var internalMap = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
    var newComponents = new SFDataComponents(internalMap);
    internalMap.putAll(type.components().components());

    if (dataComponentsPatch != null) {
      internalMap.putAll(dataComponentsPatch.getDataComponents());
    }

    return newComponents;
  }

  public SFItemStack copy() {
    if (this.isEmpty()) {
      return EMPTY;
    } else {
      return new SFItemStack(this.type, this.count, this.dataComponentsPatch);
    }
  }

  public SFItemStack copyAndClear() {
    if (this.isEmpty()) {
      return EMPTY;
    } else {
      var copy = this.copy();
      this.setCount(0);
      return copy;
    }
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

  public int getCount() {
    return this.isEmpty() ? 0 : this.count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  @Override
  public String toString() {
    return "SFIStack{" + type.key() + " x" + count + "}";
  }
}
