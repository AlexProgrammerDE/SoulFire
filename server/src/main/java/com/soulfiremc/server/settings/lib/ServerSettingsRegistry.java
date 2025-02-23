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
    return IntSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .setMin(property.minValue())
      .setMax(property.maxValue())
      .setStep(property.stepValue())
      .setPlaceholder(property.placeholder())
      .setThousandSeparator(property.thousandSeparator())
      .build();
  }

  private static DoubleSetting createDoubleSetting(DoubleProperty property) {
    return DoubleSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .setMin(property.minValue())
      .setMax(property.maxValue())
      .setStep(property.stepValue())
      .setPlaceholder(property.placeholder())
      .setThousandSeparator(property.thousandSeparator())
      .setDecimalScale(property.decimalScale())
      .setFixedDecimalScale(property.fixedDecimalScale())
      .build();
  }

  private static MinMaxSettingEntry createMinMaxSettingEntry(MinMaxPropertyEntry entry) {
    return MinMaxSettingEntry.newBuilder()
      .setUiName(entry.uiName())
      .setDescription(entry.description())
      .setDef(entry.defaultValue())
      .setPlaceholder(entry.placeholder())
      .build();
  }

  private static MinMaxSetting createMinMaxSetting(MinMaxProperty property) {
    return MinMaxSetting.newBuilder()
      .setMin(property.minValue())
      .setMax(property.maxValue())
      .setStep(property.stepValue())
      .setThousandSeparator(property.thousandSeparator())
      .setMinEntry(createMinMaxSettingEntry(property.minEntry()))
      .setMaxEntry(createMinMaxSettingEntry(property.maxEntry()))
      .build();
  }

  private static StringSetting createStringSetting(StringProperty property) {
    return StringSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .setSecret(property.secret())
      .setTextarea(property.textarea())
      .setPlaceholder(property.placeholder())
      .build();
  }

  private static ComboSetting createComboSetting(ComboProperty property) {
    return ComboSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .addAllOptions(property.options()
        .stream()
        .map(option -> ComboOption.newBuilder()
          .setId(option.id())
          .setDisplayName(option.displayName())
          .build())
        .toList())
      .build();
  }

  private static StringListSetting createStringListSetting(StringListProperty property) {
    return StringListSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .addAllDef(property.defaultValue())
      .build();
  }

  private static BoolSetting createBoolSetting(BooleanProperty property) {
    return BoolSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .build();
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
  public ServerSettingsRegistry addInternalPage(Class<? extends SettingsObject> clazz, String pageName, String iconId) {
    return addPage(clazz, pageName, null, iconId, null);
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
  public ServerSettingsRegistry addPluginPage(
    Class<? extends SettingsObject> clazz, String pageName, Plugin owningPlugin, String iconId, BooleanProperty enabledProperty) {
    return addPage(clazz, pageName, owningPlugin, iconId, enabledProperty.key());
  }

  @This
  private ServerSettingsRegistry addPage(
    Class<? extends SettingsObject> clazz, String pageName, @Nullable Plugin owningPlugin, String iconId, @Nullable String enabledProperty) {
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
            return new NamespaceRegistry(pluginInfo, pageName, new ArrayList<>(), iconId, enabledProperty);
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
        var entryBuilder = SettingEntry.newBuilder()
          .setKey(property.key());
        entries.add(switch (property) {
          case BooleanProperty booleanProperty -> entryBuilder.setBool(createBoolSetting(booleanProperty)).build();
          case IntProperty intProperty -> entryBuilder.setInt(createIntSetting(intProperty)).build();
          case DoubleProperty doubleProperty -> entryBuilder.setDouble(createDoubleSetting(doubleProperty)).build();
          case StringProperty stringProperty -> entryBuilder.setString(createStringSetting(stringProperty)).build();
          case ComboProperty comboProperty -> entryBuilder.setCombo(createComboSetting(comboProperty)).build();
          case StringListProperty stringListProperty -> entryBuilder.setStringList(createStringListSetting(stringListProperty)).build();
          case MinMaxProperty minMaxProperty -> entryBuilder.setMinMax(createMinMaxSetting(minMaxProperty)).build();
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

      if (namespaceRegistry.enabledProperty != null) {
        settingsPageBuilder.setEnabledKey(namespaceRegistry.enabledProperty);
      }

      list.add(settingsPageBuilder.build());
    }

    return list;
  }

  private record NamespaceRegistry(@Nullable PluginInfo owningPlugin, String pageName, List<Property> properties, String iconId, @Nullable String enabledProperty) {}
}
