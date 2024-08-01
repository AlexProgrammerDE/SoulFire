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
package com.soulfiremc.server.settings.lib;

import com.soulfiremc.grpc.generated.BoolSetting;
import com.soulfiremc.grpc.generated.ComboOption;
import com.soulfiremc.grpc.generated.ComboSetting;
import com.soulfiremc.grpc.generated.DoubleSetting;
import com.soulfiremc.grpc.generated.IntSetting;
import com.soulfiremc.grpc.generated.SettingEntry;
import com.soulfiremc.grpc.generated.SettingEntryMinMaxPair;
import com.soulfiremc.grpc.generated.SettingEntryMinMaxPairSingle;
import com.soulfiremc.grpc.generated.SettingEntrySingle;
import com.soulfiremc.grpc.generated.SettingType;
import com.soulfiremc.grpc.generated.SettingsPage;
import com.soulfiremc.grpc.generated.StringListSetting;
import com.soulfiremc.grpc.generated.StringSetting;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.DoubleProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.SingleProperty;
import com.soulfiremc.server.settings.property.StringListProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class ServerSettingsRegistry {
  private final SettingsPage.Type type;
  private final Map<String, NamespaceRegistry> namespaceMap = new LinkedHashMap<>();

  private static IntSetting createIntSetting(IntProperty property) {
    var builder =
      IntSetting.newBuilder()
        .setDef(property.defaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue());

    if (property.format() != null) {
      builder = builder.setFormat(property.format());
    }

    return builder.build();
  }

  private static DoubleSetting createDoubleSetting(DoubleProperty property) {
    var builder =
      DoubleSetting.newBuilder()
        .setDef(property.defaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue());

    if (property.format() != null) {
      builder = builder.setFormat(property.format());
    }

    return builder.build();
  }

  public ServerSettingsRegistry addClass(Class<? extends SettingsObject> clazz, String pageName) {
    return addClass(clazz, pageName, null);
  }

  public ServerSettingsRegistry addClass(
    Class<? extends SettingsObject> clazz, String pageName, @Nullable PluginInfo owningPlugin) {
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
            registry = new NamespaceRegistry(owningPlugin, pageName, new ArrayList<>());
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

  public List<SettingsPage> exportSettingsMeta() {
    var list = new ArrayList<SettingsPage>();

    for (var namespaceEntry : namespaceMap.entrySet()) {
      var namespaceRegistry = namespaceEntry.getValue();
      var entries = new ArrayList<SettingEntry>();
      for (var property : namespaceRegistry.properties) {
        switch (property) {
          case BooleanProperty booleanProperty -> entries.add(
            SettingEntry.newBuilder()
              .setSingle(
                fillSingleProperties(booleanProperty)
                  .setType(
                    SettingType.newBuilder()
                      .setBool(
                        BoolSetting.newBuilder()
                          .setDef(booleanProperty.defaultValue())
                          .build())
                      .build())
                  .build())
              .build());
          case IntProperty intProperty -> entries.add(
            SettingEntry.newBuilder()
              .setSingle(
                fillSingleProperties(intProperty)
                  .setType(
                    SettingType.newBuilder()
                      .setInt(createIntSetting(intProperty))
                      .build())
                  .build())
              .build());
          case DoubleProperty doubleProperty -> entries.add(
            SettingEntry.newBuilder()
              .setSingle(
                fillSingleProperties(doubleProperty)
                  .setType(
                    SettingType.newBuilder()
                      .setDouble(createDoubleSetting(doubleProperty))
                      .build())
                  .build())
              .build());
          case MinMaxPropertyLink minMaxPropertyLink -> {
            var minProperty = minMaxPropertyLink.min();
            var maxProperty = minMaxPropertyLink.max();
            entries.add(
              SettingEntry.newBuilder()
                .setMinMaxPair(
                  SettingEntryMinMaxPair.newBuilder()
                    .setMin(
                      fillMultiProperties(minProperty)
                        .setIntSetting(createIntSetting(minProperty))
                        .build())
                    .setMax(
                      fillMultiProperties(maxProperty)
                        .setIntSetting(createIntSetting(maxProperty))
                        .build())
                    .build())
                .build());
          }
          case StringProperty stringProperty -> entries.add(
            SettingEntry.newBuilder()
              .setSingle(
                fillSingleProperties(stringProperty)
                  .setType(
                    SettingType.newBuilder()
                      .setString(
                        StringSetting.newBuilder()
                          .setDef(stringProperty.defaultValue())
                          .setSecret(stringProperty.secret())
                          .build())
                      .build())
                  .build())
              .build());
          case ComboProperty comboProperty -> {
            var options = new ArrayList<ComboOption>();
            for (var option : comboProperty.options()) {
              options.add(
                ComboOption.newBuilder()
                  .setId(option.id())
                  .setDisplayName(option.displayName())
                  .build());
            }
            entries.add(
              SettingEntry.newBuilder()
                .setSingle(
                  fillSingleProperties(comboProperty)
                    .setType(
                      SettingType.newBuilder()
                        .setCombo(
                          ComboSetting.newBuilder()
                            .setDef(comboProperty.defaultValue())
                            .addAllOptions(options)
                            .build())
                        .build())
                    .build())
                .build());
          }
          case StringListProperty stringListProperty -> entries.add(
            SettingEntry.newBuilder()
              .setSingle(
                fillSingleProperties(stringListProperty)
                  .setType(
                    SettingType.newBuilder()
                      .setStringList(
                        StringListSetting.newBuilder()
                          .addAllDef(stringListProperty.defaultValue())
                          .build())
                      .build())
                  .build())
              .build());
        }
      }

      var settingsPageBuilder = SettingsPage.newBuilder()
        .setType(type)
        .setPageName(namespaceRegistry.pageName)
        .setNamespace(namespaceEntry.getKey())
        .addAllEntries(entries);

      if (namespaceRegistry.owningPlugin != null) {
        settingsPageBuilder.setOwningPlugin(namespaceRegistry.owningPlugin.id());
      }

      list.add(settingsPageBuilder.build());
    }

    return list;
  }

  private SettingEntrySingle.Builder fillSingleProperties(SingleProperty property) {
    return SettingEntrySingle.newBuilder()
      .setKey(property.key())
      .setUiName(property.uiName())
      .addAllCliFlags(Arrays.asList(property.cliFlags()))
      .setDescription(property.description());
  }

  private SettingEntryMinMaxPairSingle.Builder fillMultiProperties(
    SingleProperty property) {
    return SettingEntryMinMaxPairSingle.newBuilder()
      .setKey(property.key())
      .setUiName(property.uiName())
      .addAllCliFlags(Arrays.asList(property.cliFlags()))
      .setDescription(property.description());
  }

  private record NamespaceRegistry(@Nullable PluginInfo owningPlugin, String pageName, List<Property> properties) {}
}
