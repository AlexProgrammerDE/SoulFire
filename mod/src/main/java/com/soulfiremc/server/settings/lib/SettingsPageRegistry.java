/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.settings.lib;

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.api.Plugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.settings.property.*;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class SettingsPageRegistry {
  private final List<PageDefinition> pageList = new ArrayList<>();

  private static IntSetting createIntSetting(IntProperty<?> property) {
    return IntSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .setMin(property.minValue())
      .setMax(property.maxValue())
      .setStep(property.stepValue())
      .setPlaceholder(property.placeholder())
      .setThousandSeparator(property.thousandSeparator())
      .setDisabled(property.disabled())
      .build();
  }

  private static DoubleSetting createDoubleSetting(DoubleProperty<?> property) {
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
      .setDisabled(property.disabled())
      .build();
  }

  private static MinMaxSetting.Entry createMinMaxSettingEntry(MinMaxPropertyEntry entry) {
    return MinMaxSetting.Entry.newBuilder()
      .setUiName(entry.uiName())
      .setDescription(entry.description())
      .setDef(entry.defaultValue())
      .setPlaceholder(entry.placeholder())
      .build();
  }

  private static MinMaxSetting createMinMaxSetting(MinMaxProperty<?> property) {
    return MinMaxSetting.newBuilder()
      .setMin(property.minValue())
      .setMax(property.maxValue())
      .setStep(property.stepValue())
      .setThousandSeparator(property.thousandSeparator())
      .setMinEntry(createMinMaxSettingEntry(property.minEntry()))
      .setMaxEntry(createMinMaxSettingEntry(property.maxEntry()))
      .setDisabled(property.disabled())
      .build();
  }

  private static StringSetting createStringSetting(StringProperty<?> property) {
    return StringSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .setInputType(property.type())
      .setPlaceholder(property.placeholder())
      .setMinLength(property.minLength())
      .setMaxLength(property.maxLength())
      .setPattern(property.pattern())
      .setDisabled(property.disabled())
      .build();
  }

  private static ComboSetting createComboSetting(ComboProperty<?> property) {
    return ComboSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .addAllOptions(property.options()
        .stream()
        .map(option -> {
          var builder = ComboSetting.Option.newBuilder()
            .setId(option.id())
            .setDisplayName(option.displayName())
            .addAllKeywords(option.keywords());
          if (option.iconId() != null) {
            builder.setIconId(option.iconId());
          }

          return builder.build();
        })
        .toList())
      .setDisabled(property.disabled())
      .build();
  }

  private static StringListSetting createStringListSetting(StringListProperty<?> property) {
    return StringListSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .addAllDef(property.defaultValue())
      .setDisabled(property.disabled())
      .build();
  }

  private static BoolSetting createBoolSetting(BooleanProperty<?> property) {
    return BoolSetting.newBuilder()
      .setUiName(property.uiName())
      .setDescription(property.description())
      .setDef(property.defaultValue())
      .setDisabled(property.disabled())
      .build();
  }

  /// Registers an internal class with the settings registry.
  /// This is for classes associated to internal settings.
  /// They need to be handled explicitly by the client.
  ///
  /// @param clazz The class to register
  /// @param id The unique page identifier (URL-safe)
  /// @param pageName The display name of the page
  /// @return The registry
  @This
  @ApiStatus.Internal
  public SettingsPageRegistry addInternalPage(Class<? extends SettingsObject> clazz, String id, String pageName) {
    return addPage(clazz, id, pageName, null, "triangle-alert", null);
  }

  /// Registers an internal class with the settings registry.
  /// This is normally used for plugins, provide your plugin info to register the settings to your plugin.
  ///
  /// @param clazz        The class to register
  /// @param id           The unique page identifier (URL-safe)
  /// @param pageName     The name of the page
  /// @param owningPlugin The owning plugin
  /// @param iconId       The icon id
  ///                                         Icons ids are from <a href="https://lucide.dev">lucide.dev</a>
  /// @return The registry
  @This
  public SettingsPageRegistry addPluginPage(
    Class<? extends SettingsObject> clazz, String id, String pageName, Plugin owningPlugin, String iconId, BooleanProperty<?> enabledProperty) {
    return addPage(clazz, id, pageName, owningPlugin, iconId, enabledProperty);
  }

  @This
  private SettingsPageRegistry addPage(
    Class<? extends SettingsObject> clazz, String id, String pageName, @Nullable Plugin owningPlugin, String iconId, @Nullable BooleanProperty<?> enabledProperty) {
    var properties = new ArrayList<Property<?>>();
    for (var field : clazz.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers())
        && Modifier.isFinal(field.getModifiers())
        && Modifier.isStatic(field.getModifiers())
        && Property.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);

        try {
          var property = (Property<?>) field.get(null);
          if (property == null) {
            throw new IllegalStateException("Property is null!");
          }

          properties.add(property);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException("Failed to get property!", e);
        }
      }
    }

    pageList.add(new PageDefinition(
      id,
      owningPlugin != null ? owningPlugin.pluginInfo() : null,
      pageName,
      properties,
      iconId,
      enabledProperty
    ));

    return this;
  }

  public List<SettingsPage> exportSettingsMeta() {
    var list = new ArrayList<SettingsPage>();

    for (var pageDefinition : pageList) {
      var entries = new ArrayList<SettingsPageEntry>();
      for (var property : pageDefinition.properties) {
        var entryBuilder = SettingsPageEntry.newBuilder()
          .setId(property.toProtoIdentifier())
          .setScope(switch (property.sourceType()) {
            case SettingsSource.Server _ -> SettingsPageEntryScopeType.SERVER;
            case SettingsSource.Instance _ -> SettingsPageEntryScopeType.INSTANCE;
            case SettingsSource.Bot _ -> SettingsPageEntryScopeType.BOT;
          });
        entries.add(switch (property) {
          case BooleanProperty<?> booleanProperty -> entryBuilder.setBool(createBoolSetting(booleanProperty)).build();
          case IntProperty<?> intProperty -> entryBuilder.setInt(createIntSetting(intProperty)).build();
          case DoubleProperty<?> doubleProperty -> entryBuilder.setDouble(createDoubleSetting(doubleProperty)).build();
          case StringProperty<?> stringProperty -> entryBuilder.setString(createStringSetting(stringProperty)).build();
          case ComboProperty<?> comboProperty -> entryBuilder.setCombo(createComboSetting(comboProperty)).build();
          case StringListProperty<?> stringListProperty -> entryBuilder.setStringList(createStringListSetting(stringListProperty)).build();
          case MinMaxProperty<?> minMaxProperty -> entryBuilder.setMinMax(createMinMaxSetting(minMaxProperty)).build();
        });
      }

      var settingsPageBuilder = SettingsPage.newBuilder()
        .setId(pageDefinition.id)
        .setPageName(pageDefinition.pageName)
        .addAllEntries(entries)
        .setIconId(pageDefinition.iconId);

      if (pageDefinition.owningPlugin != null) {
        settingsPageBuilder.setOwningPluginId(pageDefinition.owningPlugin.id());
      }

      if (pageDefinition.enabledProperty != null) {
        settingsPageBuilder.setEnabledIdentifier(pageDefinition.enabledProperty.toProtoIdentifier());
      }

      list.add(settingsPageBuilder.build());
    }

    return list;
  }

  private record PageDefinition(String id, @Nullable PluginInfo owningPlugin, String pageName, List<Property<?>> properties, String iconId, @Nullable Property<?> enabledProperty) {}
}
