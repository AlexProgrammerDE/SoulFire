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
package net.pistonmaster.soulfire.server.settings.lib;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.server.settings.lib.property.*;

import java.util.List;
import java.util.function.Function;

public record SettingsHolder(
        Object2ObjectMap<PropertyKey, Number> numberProperties,
        Object2BooleanMap<PropertyKey> booleanProperties,
        Object2ObjectMap<PropertyKey, String> stringProperties,
        List<MinecraftAccount> accounts,
        List<SWProxy> proxies
) {
    public static final SettingsHolder EMPTY = new SettingsHolder(
            Object2ObjectMaps.emptyMap(),
            Object2BooleanMaps.emptyMap(),
            Object2ObjectMaps.emptyMap(),
            List.of(),
            List.of()
    );

    public int get(IntProperty property) {
        return numberProperties.getOrDefault(property.propertyKey(), property.defaultValue()).intValue();
    }

    public double get(DoubleProperty property) {
        return numberProperties.getOrDefault(property.propertyKey(), property.defaultValue()).doubleValue();
    }

    public boolean get(BooleanProperty property) {
        return booleanProperties.getOrDefault(property.propertyKey(), property.defaultValue());
    }

    public String get(StringProperty property) {
        return stringProperties.getOrDefault(property.propertyKey(), property.defaultValue());
    }

    public <T> T get(ComboProperty property, Function<String, T> converter) {
        return converter.apply(stringProperties.getOrDefault(property.propertyKey(), property.options()[property.defaultValue()].id()));
    }

    public <T extends Enum<T>> T get(ComboProperty property, Class<T> clazz) {
        return get(property, s -> Enum.valueOf(clazz, s));
    }
}
