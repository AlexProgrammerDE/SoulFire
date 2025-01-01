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

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.api.Plugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.settings.property.*;
import lombok.RequiredArgsConstructor;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ServerSettingsRegistry {
  private final Map<String, NamespaceRegistry> namespaceMap = new LinkedHashMap<>();

  private static IntSetting createIntSetting(IntProperty property) {
    var builder =
      IntSetting.newBuilder()
        .setUiName(property.uiName())
        .setDescription(property.description())
        .setDef(property.defaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue())
        .setPlaceholder(property.placeholder());

    property.format().ifPresent(builder::setFormat);

    return builder.build();
  }

  private static DoubleSetting createDoubleSetting(DoubleProperty property) {
    var builder =
      DoubleSetting.newBuilder()
        .setUiName(property.uiName())
        .setDescription(property.description())
        .setDef(property.defaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue())
        .setPlaceholder(property.placeholder());

    property.format().ifPresent(builder::setFormat);

    return builder.build();
  }

  private static MinMaxSetting createMinMaxSetting(MinMaxProperty property) {
    var builder =
      MinMaxSetting.newBuilder()
        .setMinUiName(property.minUiName())
        .setMaxUiName(property.maxUiName())
        .setMinDescription(property.minDescription())
        .setMaxDescription(property.maxDescription())
        .setMinDef(property.minDefaultValue())
        .setMaxDef(property.maxDefaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue())
        .setMinPlaceholder(property.minPlaceholder())
        .setMaxPlaceholder(property.maxPlaceholder());

    property.format().ifPresent(builder::setFormat);

    return builder.build();
  }

  /**
   * Registers an internal class with the settings registry.
   * This is for classes associated to internal settings.
   *
   * @param clazz    The class to register
   * @param pageName The name of the page
   * @param iconId   The icon id
   *                 Icons ids are from <a href="https://lucide.dev">lucide.dev</a>
   * @return The registry
   */
  @This
  @ApiStatus.Internal
  public ServerSettingsRegistry addClass(Class<? extends SettingsObject> clazz, String pageName, String iconId) {
    return addClassInternal(clazz, pageName, null, iconId);
  }

  /**
   * Registers an internal class with the settings registry.
   * This is normally used for plugins, provide your plugin info to register the settings to your plugin.
   *
   * @param clazz        The class to register
   * @param pageName     The name of the page
   * @param owningPlugin The owning plugin
   * @param iconId       The icon id
   *                     Icons ids are from <a href="https://lucide.dev">lucide.dev</a>
   * @return The registry
   */
  @This
  public ServerSettingsRegistry addClass(
    Class<? extends SettingsObject> clazz, String pageName, Plugin owningPlugin, String iconId) {
    return addClassInternal(clazz, pageName, owningPlugin, iconId);
  }

  @This
  private ServerSettingsRegistry addClassInternal(
    Class<? extends SettingsObject> clazz, String pageName, @Nullable Plugin owningPlugin, String iconId) {
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

          var registry = namespaceMap.computeIfAbsent(property.namespace(), k -> {
            var pluginInfo = owningPlugin != null ? owningPlugin.pluginInfo() : null;
            return new NamespaceRegistry(pluginInfo, pageName, new ArrayList<>(), iconId);
          });

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
        entries.add(switch (property) {
          case BooleanProperty booleanProperty -> fillProperties(booleanProperty)
            .setType(
              SettingType.newBuilder()
                .setBool(
                  BoolSetting.newBuilder()
                    .setUiName(booleanProperty.uiName())
                    .setDescription(booleanProperty.description())
                    .setDef(booleanProperty.defaultValue())
                    .build())
                .build())
            .build();
          case IntProperty intProperty -> fillProperties(intProperty)
            .setType(
              SettingType.newBuilder()
                .setInt(createIntSetting(intProperty))
                .build())
            .build();
          case DoubleProperty doubleProperty -> fillProperties(doubleProperty)
            .setType(
              SettingType.newBuilder()
                .setDouble(createDoubleSetting(doubleProperty))
                .build())
            .build();
          case StringProperty stringProperty -> fillProperties(stringProperty)
            .setType(
              SettingType.newBuilder()
                .setString(
                  StringSetting.newBuilder()
                    .setUiName(stringProperty.uiName())
                    .setDescription(stringProperty.description())
                    .setDef(stringProperty.defaultValue())
                    .setSecret(stringProperty.secret())
                    .setTextarea(stringProperty.textarea())
                    .setPlaceholder(stringProperty.placeholder())
                    .build())
                .build())
            .build();
          case ComboProperty comboProperty -> {
            var options = new ArrayList<ComboOption>();
            for (var option : comboProperty.options()) {
              options.add(
                ComboOption.newBuilder()
                  .setId(option.id())
                  .setDisplayName(option.displayName())
                  .build());
            }
            yield fillProperties(comboProperty)
              .setType(
                SettingType.newBuilder()
                  .setCombo(
                    ComboSetting.newBuilder()
                      .setUiName(comboProperty.uiName())
                      .setDescription(comboProperty.description())
                      .setDef(comboProperty.defaultValue())
                      .addAllOptions(options)
                      .build())
                  .build())
              .build();
          }
          case StringListProperty stringListProperty -> fillProperties(stringListProperty)
            .setType(
              SettingType.newBuilder()
                .setStringList(
                  StringListSetting.newBuilder()
                    .setUiName(stringListProperty.uiName())
                    .setDescription(stringListProperty.description())
                    .addAllDef(stringListProperty.defaultValue())
                    .build())
                .build())
            .build();
          case MinMaxProperty minMaxProperty -> fillProperties(minMaxProperty)
            .setType(
              SettingType.newBuilder()
                .setMinMax(createMinMaxSetting(minMaxProperty))
                .build())
            .build();
        });
      }

      var settingsPageBuilder = SettingsPage.newBuilder()
        .setPageName(namespaceRegistry.pageName)
        .setNamespace(namespaceEntry.getKey())
        .addAllEntries(entries)
        .setIconId(namespaceRegistry.iconId);

      if (namespaceRegistry.owningPlugin != null) {
        settingsPageBuilder.setOwningPlugin(namespaceRegistry.owningPlugin.id());
      }

      list.add(settingsPageBuilder.build());
    }

    return list;
  }

  private SettingEntry.Builder fillProperties(Property property) {
    return SettingEntry.newBuilder()
      .setKey(property.key());
  }

  private record NamespaceRegistry(@Nullable PluginInfo owningPlugin, String pageName, List<Property> properties, String iconId) {}
}
