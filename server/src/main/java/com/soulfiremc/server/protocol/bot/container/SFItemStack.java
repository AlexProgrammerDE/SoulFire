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
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import java.util.Objects;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

@Getter
public class SFItemStack extends ItemStack {
  private final ItemType type;
  private final Int2IntMap enchantments;
  private final int precalculatedHash;

  private SFItemStack(SFItemStack clone, int amount) {
    super(clone.getId(), amount, clone.getDataComponents());
    this.type = clone.type;
    this.enchantments = clone.enchantments;
    this.precalculatedHash = clone.precalculatedHash;
  }

  private SFItemStack(ItemStack itemStack) {
    super(itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponents());
    this.type = ItemType.getById(itemStack.getId());
    var dataComponents = itemStack.getDataComponents();
    if (dataComponents == null || dataComponents.get(DataComponentType.ENCHANTMENTS) == null) {
      this.enchantments = Int2IntMaps.EMPTY_MAP;
    } else {
      var enchantmentsList = Objects.requireNonNull(dataComponents.get(DataComponentType.ENCHANTMENTS)).getEnchantments();
      if (enchantmentsList != null) {
        this.enchantments = new Int2IntArrayMap(enchantmentsList.size());
        this.enchantments.putAll(enchantmentsList);
      } else {
        this.enchantments = Int2IntMaps.EMPTY_MAP;
      }
    }

    this.precalculatedHash = Objects.hash(this.type, this.enchantments);
  }

  private SFItemStack(ItemType itemType, int amount) {
    super(itemType.id(), amount, null);
    this.type = itemType;
    this.enchantments = Int2IntMaps.EMPTY_MAP;
    this.precalculatedHash = Objects.hash(this.type, this.enchantments);
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

  public static SFItemStack forTypeStack(ItemType itemType) {
    return new SFItemStack(itemType, itemType.stackSize());
  }

  public int getEnchantmentLevel(EnchantmentType enchantment) {
    return this.enchantments.get(enchantment.key());
  }

  public SFItemStack withAmount(int amount) {
    return new SFItemStack(this, amount);
  }

  public boolean equalsShape(SFItemStack other) {
    if (other == null) {
      return false;
    }

    return this.type == other.type && this.enchantments.equals(other.enchantments);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof SFItemStack other) {
      return this.equalsShape(other) && this.getAmount() == other.getAmount();
    }

    return false;
  }

  @Override
  public int hashCode() {
    return this.precalculatedHash;
  }
}
