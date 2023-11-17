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

public interface Property {
    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, boolean defaultValue) {
        return new BooleanProperty(name, uiDescription, cliDescription, fullDescription, cliNames, defaultValue);
    }

    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, int defaultValue) {
        return new IntegerProperty(name, uiDescription, cliDescription, fullDescription, cliNames, defaultValue);
    }

    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, String defaultValue) {
        return new StringProperty(name, uiDescription, cliDescription, fullDescription, cliNames, defaultValue);
    }

    static Property of(String name, String uiDescription, String cliDescription, String fullDescription, String[] cliNames, ComboOption[] values, int defaultValue) {
        return new ComboProperty(name, uiDescription, cliDescription, fullDescription, cliNames, values, defaultValue);
    }

    record BooleanProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            boolean defaultValue
    ) implements Property {
    }

    record IntegerProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            int defaultValue
    ) implements Property {
    }

    record StringProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            String defaultValue
    ) implements Property {
    }

    record ComboProperty(
            String name,
            String uiDescription,
            String cliDescription,
            String fullDescription,
            String[] cliNames,
            ComboOption[] options,
            int defaultValue
    ) implements Property {
    }

    record ComboOption(
            String id,
            String displayName
    ) {
    }
}
