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
package net.pistonmaster.serverwrecker.settings;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class SettingsHolder {
    private final List<SWProperty<?>> properties = new ArrayList<>();

    protected <T> SWProperty<T> newProperty(String configId, String friendlyName, T defaultValue, Class<T> type, String description, String... cliNames) {
        SWProperty<T> property = new SWProperty<>(configId, friendlyName, defaultValue, type, description, cliNames, defaultValue);
        properties.add(property);
        return property;
    }
}
