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

import com.google.gson.JsonElement;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.AttackStartRequest;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.DoubleProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.GsonInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.SneakyThrows;

public record SettingsHolder(
  Object2ObjectMap<PropertyKey, JsonElement> settingsProperties,
  List<MinecraftAccount> accounts,
  List<SFProxy> proxies) {
  @SneakyThrows
  public static SettingsHolder deserialize(AttackStartRequest request) {
    var settingsProperties = new Object2ObjectOpenHashMap<PropertyKey, JsonElement>();

    for (var namespace : request.getSettingsList()) {
      for (var entry : namespace.getEntriesList()) {
        var propertyKey = new PropertyKey(namespace.getNamespace(), entry.getKey());

        settingsProperties.put(propertyKey, GsonInstance.GSON.fromJson(JsonFormat.printer().print(entry.getValue()), JsonElement.class));
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

    return new SettingsHolder(settingsProperties, accounts, proxies);
  }

  public int get(IntProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), Integer.class);
  }

  public double get(DoubleProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), Double.class);
  }

  public boolean get(BooleanProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), Boolean.class);
  }

  public String get(StringProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), String.class);
  }

  public <T> T get(ComboProperty property, Function<String, T> converter) {
    return converter.apply(getAsType(property.propertyKey(), property.options()[property.defaultValue()].id(), String.class));
  }

  public <T extends Enum<T>> T get(ComboProperty property, Class<T> clazz) {
    return get(property, s -> Enum.valueOf(clazz, s));
  }

  public <T> T getAsType(PropertyKey key, T defaultValue, Class<T> clazz) {
    return GsonInstance.GSON.fromJson(settingsProperties.getOrDefault(key, GsonInstance.GSON.toJsonTree(defaultValue)), clazz);
  }
}
