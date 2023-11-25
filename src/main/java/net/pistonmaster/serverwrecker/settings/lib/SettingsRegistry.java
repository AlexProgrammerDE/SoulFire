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
import net.pistonmaster.serverwrecker.settings.lib.property.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsRegistry {
    private final Map<String, NamespaceRegistry> namespaceMap = new HashMap<>();

    private static IntSetting createIntSetting(IntProperty property) {
        var builder = IntSetting.newBuilder()
                .setDef(property.defaultValue())
                .setMin(property.minValue())
                .setMax(property.maxValue())
                .setStep(property.stepValue());

        if (property.format() != null) {
            builder = builder.setFormat(property.format());
        }

        return builder.build();
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
                                    .setUiDescription(booleanProperty.uiDescription())
                                    .setCliDescription(booleanProperty.cliDescription())
                                    .addAllCliNames(List.of(booleanProperty.cliNames()))
                                    .setType(ClientPluginSettingType.newBuilder()
                                            .setBool(BoolSetting.newBuilder()
                                                    .setDef(booleanProperty.defaultValue())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
                } else if (property instanceof IntProperty intProperty) {
                    entries.add(ClientPluginSettingEntry.newBuilder()
                            .setSingle(ClientPluginSettingEntrySingle.newBuilder()
                                    .setKey(property.key())
                                    .setUiDescription(intProperty.uiDescription())
                                    .setCliDescription(intProperty.cliDescription())
                                    .addAllCliNames(List.of(intProperty.cliNames()))
                                    .setType(ClientPluginSettingType.newBuilder()
                                            .setInt(createIntSetting(intProperty))
                                            .build())
                                    .build())
                            .build());
                } else if (property instanceof MinMaxPropertyLink minMaxPropertyLink) {
                    var minProperty = minMaxPropertyLink.min();
                    var maxProperty = minMaxPropertyLink.max();
                    entries.add(ClientPluginSettingEntry.newBuilder()
                            .setMinMaxPair(ClientPluginSettingEntryMinMaxPair.newBuilder()
                                    .setMin(ClientPluginSettingEntryMinMaxPairSingle.newBuilder()
                                            .setKey(minProperty.key())
                                            .setUiDescription(minProperty.uiDescription())
                                            .setCliDescription(minProperty.cliDescription())
                                            .addAllCliNames(List.of(minProperty.cliNames()))
                                            .setIntSetting(createIntSetting(minProperty))
                                            .build())
                                    .setMax(ClientPluginSettingEntryMinMaxPairSingle.newBuilder()
                                            .setKey(maxProperty.key())
                                            .setUiDescription(maxProperty.uiDescription())
                                            .setCliDescription(maxProperty.cliDescription())
                                            .addAllCliNames(List.of(maxProperty.cliNames()))
                                            .setIntSetting(createIntSetting(maxProperty))
                                            .build())
                                    .build())
                            .build());
                } else if (property instanceof StringProperty stringProperty) {
                    entries.add(ClientPluginSettingEntry.newBuilder()
                            .setSingle(ClientPluginSettingEntrySingle.newBuilder()
                                    .setKey(property.key())
                                    .setUiDescription(stringProperty.uiDescription())
                                    .setCliDescription(stringProperty.cliDescription())
                                    .addAllCliNames(List.of(stringProperty.cliNames()))
                                    .setType(ClientPluginSettingType.newBuilder()
                                            .setString(StringSetting.newBuilder()
                                                    .setDef(stringProperty.defaultValue())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
                } else if (property instanceof ComboProperty comboProperty) {
                    var options = new ArrayList<ComboOption>();
                    for (var option : comboProperty.options()) {
                        options.add(ComboOption.newBuilder()
                                .setId(option.id())
                                .setDisplayName(option.displayName())
                                .build());
                    }

                    entries.add(ClientPluginSettingEntry.newBuilder()
                            .setSingle(ClientPluginSettingEntrySingle.newBuilder()
                                    .setKey(property.key())
                                    .setUiDescription(comboProperty.uiDescription())
                                    .setCliDescription(comboProperty.cliDescription())
                                    .addAllCliNames(List.of(comboProperty.cliNames()))
                                    .setType(ClientPluginSettingType.newBuilder()
                                            .setCombo(ComboSetting.newBuilder()
                                                    .setDef(comboProperty.defaultValue())
                                                    .addAllOptions(options)
                                                    .build())
                                            .build())
                                    .build())
                            .build());
                } else {
                    throw new IllegalStateException("Unknown property type!");
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

    private record NamespaceRegistry(String pageName, boolean hidden, List<Property> properties) {
    }
}
