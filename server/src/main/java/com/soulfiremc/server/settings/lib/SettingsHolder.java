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
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.InstanceConfig;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.DoubleProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.StringListProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.GsonInstance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.SneakyThrows;

public record SettingsHolder(
  Map<PropertyKey, JsonElement> settingsProperties,
  List<MinecraftAccount> accounts,
  List<SFProxy> proxies) {
  public static final SettingsHolder EMPTY = new SettingsHolder(Map.of(), List.of(), List.of());

  @SneakyThrows
  public static SettingsHolder fromProto(InstanceConfig request) {
    var settingsProperties = new HashMap<PropertyKey, JsonElement>();

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

  @SneakyThrows
  public InstanceConfig toProto() {
    Map<String, Map<String, Value>> settingsProperties = new HashMap<>();
    for (var entry : this.settingsProperties.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();

      var namespace = key.namespace();
      var innerKey = key.key();

      var valueProto = Value.newBuilder();
      JsonFormat.parser().merge(GsonInstance.GSON.toJson(value), valueProto);

      settingsProperties.computeIfAbsent(namespace, k -> new HashMap<>())
        .put(innerKey, valueProto.build());
    }

    return InstanceConfig.newBuilder()
      .addAllSettings(settingsProperties.entrySet().stream().map(entry -> {
        var namespace = entry.getKey();

        return SettingsNamespace.newBuilder()
          .setNamespace(namespace)
          .addAllEntries(entry.getValue().entrySet().stream().map(innerEntry -> {
            var key = innerEntry.getKey();
            var value = innerEntry.getValue();
            return SettingsEntry.newBuilder()
              .setKey(key)
              .setValue(value)
              .build();
          }).toList())
          .build();
      }).toList())
      .addAllAccounts(accounts.stream().map(MinecraftAccount::toProto).toList())
      .addAllProxies(proxies.stream().map(SFProxy::toProto).toList())
      .build();
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

  public List<String> get(StringListProperty property) {
    return List.of(getAsType(property.propertyKey(), property.defaultValue().toArray(new String[0]), String[].class));
  }

  public <T> T getAsType(PropertyKey key, T defaultValue, Class<T> clazz) {
    return GsonInstance.GSON.fromJson(settingsProperties.getOrDefault(key, GsonInstance.GSON.toJsonTree(defaultValue)), clazz);
  }
}
