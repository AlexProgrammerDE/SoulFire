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

public sealed interface Property permits BooleanProperty, ComboProperty, IntProperty, StringProperty, MinMaxPropertyLink {
    static Builder builder(String namespace) {
        return new Builder(namespace);
    }

    String namespace();

    String key();

    default PropertyKey propertyKey() {
        return new PropertyKey(namespace(), key());
    }

    record Builder(String namespace) {
        public BooleanProperty ofBoolean(String key, String uiDescription, String cliDescription, String[] cliNames,
                                         boolean defaultValue) {
            return new BooleanProperty(namespace, key, uiDescription, cliDescription, cliNames, defaultValue);
        }

        public IntProperty ofInt(String key, String uiDescription, String cliDescription, String[] cliNames,
                                 int defaultValue, int minValue, int maxValue, int stepValue) {
            return new IntProperty(namespace, key, uiDescription, cliDescription, cliNames, defaultValue, minValue, maxValue, stepValue, null);
        }

        public IntProperty ofInt(String key, String uiDescription, String cliDescription, String[] cliNames,
                                 int defaultValue, int minValue, int maxValue, int stepValue, String format) {
            return new IntProperty(namespace, key, uiDescription, cliDescription, cliNames, defaultValue, minValue, maxValue, stepValue, format);
        }

        public StringProperty ofString(String key, String uiDescription, String cliDescription, String[] cliNames,
                                       String defaultValue) {
            return new StringProperty(namespace, key, uiDescription, cliDescription, cliNames, defaultValue);
        }

        public ComboProperty ofCombo(String key, String uiDescription, String cliDescription, String[] cliNames,
                                     ComboProperty.ComboOption[] values, int defaultValue) {
            return new ComboProperty(namespace, key, uiDescription, cliDescription, cliNames, values, defaultValue);
        }

        public <T extends Enum<T>> ComboProperty ofEnum(String key, String uiDescription, String cliDescription,
                                                        String[] cliNames, T[] values, T defaultValue) {
            return new ComboProperty(namespace, key, uiDescription, cliDescription, cliNames, ComboProperty.ComboOption.fromEnum(values), defaultValue.ordinal());
        }
    }
}
