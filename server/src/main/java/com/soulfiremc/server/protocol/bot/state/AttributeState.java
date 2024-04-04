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

import com.soulfiremc.server.data.Attribute;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.ModifierOperation;
import com.soulfiremc.server.util.MathHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class AttributeState {
  private final AttributeType type;
  private final Map<UUID, Attribute.Modifier> modifiers = new Object2ObjectOpenHashMap<>();
  private double baseValue;

  public double calculateValue() {
    var value = baseValue;

    for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.ADDITION)) {
      value += attributeModifier.amount();
    }

    var finalValue = value;

    for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.MULTIPLY_BASE)) {
      finalValue += value * attributeModifier.amount();
    }

    for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.MULTIPLY_TOTAL)) {
      finalValue *= 1.0 + attributeModifier.amount();
    }

    return MathHelper.doubleClamp(finalValue, type.min(), type.max());
  }

  private Iterable<Attribute.Modifier> getModifiersOrEmpty(ModifierOperation operation) {
    return modifiers.values().stream().filter(modifier -> modifier.operation() == operation)
      ::iterator;
  }
}
