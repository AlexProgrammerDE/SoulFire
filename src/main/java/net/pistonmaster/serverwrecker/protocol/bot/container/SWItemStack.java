/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot.container;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import it.unimi.dsi.fastutil.objects.Object2ShortArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMaps;
import lombok.Getter;
import net.pistonmaster.serverwrecker.data.ItemType;

import java.util.Objects;

@Getter
public class SWItemStack extends ItemStack {
    private final ItemType type;
    private final Object2ShortMap<String> enchantments;
    private final int precalculatedHash = precalculateHash();

    private SWItemStack(SWItemStack clone, int amount) {
        super(clone.getId(), amount, clone.getNbt());
        this.type = clone.type;
        this.enchantments = clone.enchantments;
    }

    private SWItemStack(ItemStack itemStack) {
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
                            enchantmentCompound.<StringTag>get("id").getValue(),
                            enchantmentCompound.<ShortTag>get("lvl").getValue().shortValue()
                    );
                }
            } else {
                this.enchantments = Object2ShortMaps.emptyMap();
            }
        }
    }

    private SWItemStack(ItemType itemType, int amount) {
        super(itemType.id(), amount, null);
        this.type = itemType;
        this.enchantments = Object2ShortMaps.emptyMap();
    }

    private int precalculateHash() {
        return Objects.hash(this.type, this.enchantments);
    }

    public static SWItemStack from(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }

        return new SWItemStack(itemStack);
    }

    public static SWItemStack forType(ItemType itemType) {
        return new SWItemStack(itemType, 1);
    }

    public SWItemStack withAmount(int amount) {
        return new SWItemStack(this, amount);
    }

    public boolean equalsShape(SWItemStack other) {
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

        if (obj instanceof SWItemStack other) {
            return this.precalculatedHash == other.precalculatedHash;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.precalculatedHash;
    }
}
