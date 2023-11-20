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
package net.pistonmaster.serverwrecker.settings.lib;

import it.unimi.dsi.fastutil.objects.*;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.StringProperty;

import java.util.Map;
import java.util.Set;

public record SettingsHolder(
        Object2IntMap<IntProperty> intProperties,
        Object2BooleanMap<BooleanProperty> booleanProperties,
        Object2ObjectMap<StringProperty, String> stringProperties
) {
    public static final SettingsHolder EMPTY = new SettingsHolder(
            Object2IntMaps.emptyMap(),
            Object2BooleanMaps.emptyMap(),
            Object2ObjectMaps.emptyMap()
    );

    public int get(IntProperty property) {
        return intProperties.getOrDefault(property, property.defaultValue());
    }

    public boolean get(BooleanProperty property) {
        return booleanProperties.getOrDefault(property, property.defaultValue());
    }

    public String get(StringProperty property) {
        return stringProperties.getOrDefault(property, property.defaultValue());
    }
}
