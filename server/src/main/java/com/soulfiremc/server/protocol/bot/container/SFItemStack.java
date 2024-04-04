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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.soulfiremc.server.data.EnchantmentType;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.data.ResourceKey;
import it.unimi.dsi.fastutil.objects.Object2ShortArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMaps;
import java.util.Objects;
import lombok.Getter;

@Getter
public class SFItemStack extends ItemStack {
  private final ItemType type;
  private final Object2ShortMap<ResourceKey> enchantments;
  private final int precalculatedHash;

  private SFItemStack(SFItemStack clone, int amount) {
    super(clone.getId(), amount, clone.getNbt());
    this.type = clone.type;
    this.enchantments = clone.enchantments;
    this.precalculatedHash = clone.precalculatedHash;
  }

  private SFItemStack(ItemStack itemStack) {
    super(itemStack.getId(), itemStack.getAmount(), itemStack.getNbt());
    this.type = ItemType.getById(itemStack.getId());
    var compound = itemStack.getNbt();
    if (compound == null) {
      this.enchantments = Object2ShortMaps.emptyMap();
    } else {
      var enchantmentsList = compound.<ListTag>get("Enchantments");
      if (enchantmentsList != null) {
        this.enchantments = new Object2ShortArrayMap<>(enchantmentsList.size());
        for (var enchantment : enchantmentsList) {
          var enchantmentCompound = (CompoundTag) enchantment;

          this.enchantments.put(
            ResourceKey.fromString(enchantmentCompound.<StringTag>get("id").getValue()),
            enchantmentCompound.<ShortTag>get("lvl").getValue().shortValue());
        }
      } else {
        this.enchantments = Object2ShortMaps.emptyMap();
      }
    }

    this.precalculatedHash = Objects.hash(this.type, this.enchantments);
  }

  private SFItemStack(ItemType itemType, int amount) {
    super(itemType.id(), amount, null);
    this.type = itemType;
    this.enchantments = Object2ShortMaps.emptyMap();
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

  public short getEnchantmentLevel(EnchantmentType enchantment) {
    return this.enchantments.getShort(enchantment.key());
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
