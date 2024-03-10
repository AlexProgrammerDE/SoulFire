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

import com.soulfiremc.account.MinecraftAccount;
import com.soulfiremc.proxy.SFProxy;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.DoubleProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.settings.ProfileDataStructure;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.util.EnabledWrapper;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.function.Function;

public record SettingsHolder(
    Object2ObjectMap<PropertyKey, Number> numberProperties,
    Object2BooleanMap<PropertyKey> booleanProperties,
    Object2ObjectMap<PropertyKey, String> stringProperties,
    List<EnabledWrapper<MinecraftAccount>> accounts,
    List<EnabledWrapper<SFProxy>> proxies) {
  public static final SettingsHolder EMPTY =
      new SettingsHolder(
          Object2ObjectMaps.emptyMap(),
          Object2BooleanMaps.emptyMap(),
          Object2ObjectMaps.emptyMap(),
          List.of(),
          List.of());

  public static SettingsHolder createSettingsHolder(ProfileDataStructure settingsSerialized) {
    var numberProperties = new Object2ObjectOpenHashMap<PropertyKey, Number>();
    var booleanProperties = new Object2BooleanOpenHashMap<PropertyKey>();
    var stringProperties = new Object2ObjectOpenHashMap<PropertyKey, String>();

    settingsSerialized.handleProperties(
        (propertyKey, settingData) -> {
          if (settingData.isJsonPrimitive()) {
            var primitive = settingData.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
              booleanProperties.put(propertyKey, primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
              numberProperties.put(propertyKey, primitive.getAsNumber());
            } else if (primitive.isString()) {
              stringProperties.put(propertyKey, primitive.getAsString());
            } else {
              throw new IllegalArgumentException("Unknown primitive type: " + primitive);
            }
          } else {
            throw new IllegalArgumentException("Unknown type: " + settingData);
          }
        });

    return new SettingsHolder(
        numberProperties,
        booleanProperties,
        stringProperties,
        settingsSerialized.accounts(),
        settingsSerialized.proxies());
  }

  public int get(IntProperty property) {
    return numberProperties
        .getOrDefault(property.propertyKey(), property.defaultValue())
        .intValue();
  }

  public double get(DoubleProperty property) {
    return numberProperties
        .getOrDefault(property.propertyKey(), property.defaultValue())
        .doubleValue();
  }

  public boolean get(BooleanProperty property) {
    return booleanProperties.getOrDefault(property.propertyKey(), property.defaultValue());
  }

  public String get(StringProperty property) {
    return stringProperties.getOrDefault(property.propertyKey(), property.defaultValue());
  }

  public <T> T get(ComboProperty property, Function<String, T> converter) {
    return converter.apply(
        stringProperties.getOrDefault(
            property.propertyKey(), property.options()[property.defaultValue()].id()));
  }

  public <T extends Enum<T>> T get(ComboProperty property, Class<T> clazz) {
    return get(property, s -> Enum.valueOf(clazz, s));
  }
}
