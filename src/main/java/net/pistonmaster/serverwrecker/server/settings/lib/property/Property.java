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
package net.pistonmaster.serverwrecker.server.settings.lib.property;

import java.util.function.Function;

public sealed interface Property permits SingleProperty, MinMaxPropertyLink {
    static Builder builder(String namespace) {
        return new Builder(namespace);
    }

    String namespace();

    String key();

    default PropertyKey propertyKey() {
        return new PropertyKey(namespace(), key());
    }

    record Builder(String namespace) {
        public BooleanProperty ofBoolean(String key, String uiName, String[] cliFlags, String description,
                                         boolean defaultValue) {
            return new BooleanProperty(namespace, key, uiName, cliFlags, description, defaultValue);
        }

        public IntProperty ofInt(String key, String uiName, String[] cliFlags, String description,
                                 int defaultValue, int minValue, int maxValue, int stepValue) {
            return new IntProperty(namespace, key, uiName, cliFlags, description, defaultValue, minValue, maxValue, stepValue, null);
        }

        public IntProperty ofInt(String key, String uiName, String[] cliFlags, String description,
                                 int defaultValue, int minValue, int maxValue, int stepValue, String format) {
            return new IntProperty(namespace, key, uiName, cliFlags, description, defaultValue, minValue, maxValue, stepValue, format);
        }

        public StringProperty ofString(String key, String uiName, String[] cliFlags, String description,
                                       String defaultValue) {
            return new StringProperty(namespace, key, uiName, cliFlags, description, defaultValue);
        }

        public ComboProperty ofCombo(String key, String uiName, String[] cliFlags, String description,
                                     ComboProperty.ComboOption[] values, int defaultValue) {
            return new ComboProperty(namespace, key, uiName, cliFlags, description, values, defaultValue);
        }

        public <T extends Enum<T>> ComboProperty ofEnum(String key, String uiName, String[] cliFlags, String description, T[] values, T defaultValue) {
            return ofEnumMapped(key, uiName, cliFlags, description, values, defaultValue, Object::toString);
        }

        public <T extends Enum<T>> ComboProperty ofEnumMapped(String key, String uiName, String[] cliFlags, String description, T[] values, T defaultValue, Function<T, String> mapper) {
            return new ComboProperty(namespace, key, uiName, cliFlags, description, ComboProperty.ComboOption.fromEnum(values, mapper), defaultValue.ordinal());
        }
    }
}
