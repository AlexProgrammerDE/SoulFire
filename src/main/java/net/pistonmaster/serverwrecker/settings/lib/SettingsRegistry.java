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

import net.pistonmaster.serverwrecker.grpc.generated.*;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsRegistry {
    private final Map<String, NamespaceRegistry> namespaceMap = new HashMap<>();

    private record NamespaceRegistry(String pageName, boolean hidden, List<Property> properties) {
    }

    public SettingsRegistry addClass(Class<? extends SettingsObject> clazz, String pageName) {
        return addClass(clazz, pageName, false);
    }

    public SettingsRegistry addClass(Class<? extends SettingsObject> clazz, String pageName, boolean hidden) {
        for (var field : clazz.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers())
                    && Property.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);

                try {
                    var property = (Property) field.get(null);
                    if (property == null) {
                        throw new IllegalStateException("Property is null!");
                    }

                    var registry = namespaceMap.get(property.namespace());
                    if (registry == null) {
                        registry = new NamespaceRegistry(pageName, hidden, new ArrayList<>());
                        namespaceMap.put(property.namespace(), registry);
                    }

                    registry.properties.add(property);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to get property!", e);
                }
            }
        }

        return this;
    }

    public List<ClientPluginSettingsPage> exportSettingsMeta() {
        var list = new ArrayList<ClientPluginSettingsPage>();

        for (var entry : namespaceMap.entrySet()) {
            var entries = new ArrayList<ClientPluginSettingEntry>();
            for (var property : entry.getValue().properties) {
                if (property instanceof BooleanProperty booleanProperty) {
                    entries.add(ClientPluginSettingEntry.newBuilder()
                            .setSingle(ClientPluginSettingEntrySingle.newBuilder()
                                    .setKey(property.key())
                                    .setName(booleanProperty.uiDescription())
                                    .setType(ClientPluginSettingType.newBuilder()
                                            .setBool(BoolSetting.newBuilder()
                                                    .setDef(booleanProperty.defaultValue())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
                }
            }

            list.add(ClientPluginSettingsPage.newBuilder()
                    .setPageName(entry.getValue().pageName)
                    .setHidden(entry.getValue().hidden)
                    .setNamespace(entry.getKey())
                    .addAllEntries(entries)
                    .build());
        }

        return list;
    }
}
