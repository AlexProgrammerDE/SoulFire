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
package net.pistonmaster.serverwrecker.protocol.bot.state;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import net.pistonmaster.serverwrecker.protocol.bot.model.AbilitiesData;

import java.util.Map;

@Data
public class EntityAttributesState {
    private final Map<AttributeType, Attribute> attributeStore = new Object2ObjectOpenHashMap<>();

    public static double getAttributeValue(Attribute attribute) {
        var value = attribute.getValue();

        for (var modifier : attribute.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADD -> value += modifier.getAmount();
                case ADD_MULTIPLIED -> value += modifier.getAmount() * value;
                case MULTIPLY -> value *= modifier.getAmount();
            }
        }

        return value;
    }

    public boolean hasAttribute(AttributeType type) {
        return attributeStore.containsKey(type);
    }

    public Attribute getAttribute(AttributeType type) {
        return attributeStore.get(type);
    }

    public double getAttributeValue(AttributeType type) {
        var attribute = attributeStore.get(type);
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute " + type + " not found!");
        }

        return getAttributeValue(attribute);
    }

    public void setAttribute(Attribute attribute) {
        this.attributeStore.put(attribute.getType(), attribute);
    }

    public void setAbilities(AbilitiesData abilitiesData) {
        setAttribute(new Attribute(AttributeType.Builtin.GENERIC_MOVEMENT_SPEED, abilitiesData.walkSpeed()));
        setAttribute(new Attribute(AttributeType.Builtin.GENERIC_FLYING_SPEED, abilitiesData.flySpeed()));
    }
}
