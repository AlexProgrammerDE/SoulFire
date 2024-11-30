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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.util.MathHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;

import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
public class AttributeState {
  private final AttributeType type;
  private final Map<Key, AttributeModifier> modifiers = new Object2ObjectOpenHashMap<>();
  private double baseValue;

  public double calculateValue() {
    var value = baseValue;

    for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.ADD)) {
      value += attributeModifier.getAmount();
    }

    var finalValue = value;

    for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.ADD_MULTIPLIED_BASE)) {
      finalValue += value * attributeModifier.getAmount();
    }

    for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.ADD_MULTIPLIED_TOTAL)) {
      finalValue *= 1.0 + attributeModifier.getAmount();
    }

    return MathHelper.clamp(finalValue, type.min(), type.max());
  }

  private Iterable<AttributeModifier> getModifiersOrEmpty(ModifierOperation operation) {
    return modifiers.values()
      .stream()
      .filter(modifier -> modifier.getOperation() == operation)
      ::iterator;
  }

  public void addModifier(AttributeModifier modifier) {
    modifiers.put(modifier.getId(), modifier);
  }

  public void removeModifier(Key id) {
    modifiers.remove(id);
  }

  public AttributeModifier getModifier(Key id) {
    return modifiers.get(id);
  }
}
