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
package net.pistonmaster.serverwrecker.settings.lib.property;

public sealed interface Property permits BooleanProperty, ComboProperty, IntProperty, StringProperty {
    static Builder builder(String namespace) {
        return new Builder(namespace);
    }

    String namespace();

    String name();

    default PropertyKey propertyKey() {
        return new PropertyKey(namespace(), name());
    }

    record Builder(String namespace) {
        public BooleanProperty ofBoolean(String name, String uiDescription, String cliDescription, String[] cliNames, boolean defaultValue) {
            return new BooleanProperty(namespace, name, uiDescription, cliDescription, cliNames, defaultValue);
        }

        public IntProperty ofInt(String name, String uiDescription, String cliDescription, String[] cliNames, int defaultValue) {
            return new IntProperty(namespace, name, uiDescription, cliDescription, cliNames, defaultValue);
        }

        public StringProperty ofString(String name, String uiDescription, String cliDescription, String[] cliNames, String defaultValue) {
            return new StringProperty(namespace, name, uiDescription, cliDescription, cliNames, defaultValue);
        }

        public ComboProperty ofCombo(String name, String uiDescription, String cliDescription, String[] cliNames, ComboProperty.ComboOption[] values, int defaultValue) {
            return new ComboProperty(namespace, name, uiDescription, cliDescription, cliNames, values, defaultValue);
        }

        public <T extends Enum<?>> ComboProperty ofEnum(String name, String uiDescription, String cliDescription, String[] cliNames, T[] values, T defaultValue) {
            return new ComboProperty(namespace, name, uiDescription, cliDescription, cliNames, ComboProperty.ComboOption.fromEnum(values), defaultValue.ordinal());
        }
    }
}
