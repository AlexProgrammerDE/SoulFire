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

import com.soulfiremc.server.data.EnchantmentType;
import com.soulfiremc.server.data.ItemType;
import java.util.HashMap;
import java.util.Optional;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

@Getter
public class SFItemStack extends ItemStack {
  private final ItemType type;

  private SFItemStack(SFItemStack clone, int amount) {
    super(clone.getId(), amount, clone.getDataComponents());
    this.type = clone.type;
  }

  private SFItemStack(ItemStack itemStack) {
    super(itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponents());
    this.type = ItemType.REGISTRY.getById(itemStack.getId());
  }

  private SFItemStack(ItemType itemType, int amount) {
    super(itemType.id(), amount, null);
    this.type = itemType;
  }

  public static SFItemStack from(ItemStack itemStack) {
    if (itemStack == null) {
      return null;
    }

    return new SFItemStack(itemStack);
  }

  public static SFItemStack forTypeSingle(ItemType itemType) {
    return new SFItemStack(itemType, 1);
  }

  @Deprecated
  @Override
  @SuppressWarnings("DeprecatedIsStillUsed")
  public DataComponents getDataComponents() {
    return super.getDataComponents();
  }

  public SFDataComponents components() {
    var internalMap = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
    var newComponents = new SFDataComponents(internalMap);
    internalMap.putAll(type.components().components());

    var overrideComponents = super.getDataComponents();
    if (overrideComponents != null) {
      internalMap.putAll(overrideComponents.getDataComponents());
    }

    return newComponents;
  }

  public int getEnchantmentLevel(EnchantmentType enchantment) {
    return components().getOptional(DataComponentType.ENCHANTMENTS)
      .flatMap(enchantments -> Optional.ofNullable(enchantments.getEnchantments().get(enchantment.id())))
      .orElse(0);
  }

  public SFItemStack withAmount(int amount) {
    return new SFItemStack(this, amount);
  }

  public boolean canStackWith(SFItemStack other) {
    if (other == null) {
      return false;
    }

    return this.type == other.type;
  }
}
