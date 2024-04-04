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

import com.soulfiremc.grpc.generated.AttackStartRequest;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.DoubleProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record SettingsHolder(
  Object2ObjectMap<PropertyKey, Number> numberProperties,
  Object2BooleanMap<PropertyKey> booleanProperties,
  Object2ObjectMap<PropertyKey, String> stringProperties,
  List<MinecraftAccount> accounts,
  List<SFProxy> proxies) {
  public static SettingsHolder deserialize(AttackStartRequest request) {
    var numberProperties = new Object2ObjectOpenHashMap<PropertyKey, Number>();
    var booleanProperties = new Object2BooleanOpenHashMap<PropertyKey>();
    var stringProperties = new Object2ObjectOpenHashMap<PropertyKey, String>();

    for (var namespace : request.getSettingsList()) {
      for (var entry : namespace.getEntriesList()) {
        var propertyKey = new PropertyKey(namespace.getNamespace(), entry.getKey());

        switch (entry.getValueCase()) {
          case STRINGVALUE -> stringProperties.put(propertyKey, entry.getStringValue());
          case INTVALUE -> numberProperties.put(propertyKey, entry.getIntValue());
          case BOOLVALUE -> booleanProperties.put(propertyKey, entry.getBoolValue());
          case DOUBLEVALUE -> numberProperties.put(propertyKey, entry.getDoubleValue());
          case VALUE_NOT_SET -> throw new IllegalArgumentException("Value not set");
        }
      }
    }

    var accounts = new ArrayList<MinecraftAccount>();
    for (var account : request.getAccountsList()) {
      accounts.add(MinecraftAccount.fromProto(account));
    }

    var proxies = new ArrayList<SFProxy>();
    for (var proxy : request.getProxiesList()) {
      proxies.add(SFProxy.fromProto(proxy));
    }

    return new SettingsHolder(
      numberProperties, booleanProperties, stringProperties, accounts, proxies);
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
