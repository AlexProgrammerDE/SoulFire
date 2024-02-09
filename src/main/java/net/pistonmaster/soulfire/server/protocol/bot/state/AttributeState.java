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
package net.pistonmaster.soulfire.server.protocol.bot.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.soulfire.server.data.Attribute;
import net.pistonmaster.soulfire.server.data.AttributeType;
import net.pistonmaster.soulfire.server.data.ModifierOperation;
import net.pistonmaster.soulfire.server.util.MathHelper;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class AttributeState {
    private final AttributeType type;
    private double baseValue;
    private List<Attribute.Modifier> modifiers;

    public double calculateValue() {
        double value = baseValue;

        for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.ADDITION)) {
            value += attributeModifier.amount();
        }

        double finalValue = value;

        for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.MULTIPLY_BASE)) {
            finalValue += value * attributeModifier.amount();
        }

        for (var attributeModifier : this.getModifiersOrEmpty(ModifierOperation.MULTIPLY_TOTAL)) {
            finalValue *= 1.0 + attributeModifier.amount();
        }

        return MathHelper.doubleClamp(finalValue, type.min(), type.max());
    }

    private List<Attribute.Modifier> getModifiersOrEmpty(ModifierOperation operation) {
        return modifiers.stream().filter(modifier -> modifier.operation() == operation).toList();
    }
}
