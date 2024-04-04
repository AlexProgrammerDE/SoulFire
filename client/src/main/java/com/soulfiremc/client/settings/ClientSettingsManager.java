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
package com.soulfiremc.client.settings;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonElement;
import com.soulfiremc.grpc.generated.AttackStartRequest;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.settings.ProfileDataStructure;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.EnabledWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientSettingsManager {
  private final Multimap<PropertyKey, Consumer<JsonElement>> listeners =
    Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
  private final Map<String, Map<String, Provider<JsonElement>>> providers = new LinkedHashMap<>();
  @Getter
  private final AccountRegistry accountRegistry;
  @Getter
  private final ProxyRegistry proxyRegistry = new ProxyRegistry();

  public void registerProvider(PropertyKey property, Provider<JsonElement> provider) {
    providers
      .computeIfAbsent(property.namespace(), k -> new LinkedHashMap<>())
      .put(property.key(), provider);
  }

  public void registerListener(PropertyKey property, Consumer<JsonElement> listener) {
    listeners.put(property, listener);
  }

  public void loadProfile(Path path) throws IOException {
    var profileDataStructure = ProfileDataStructure.deserialize(Files.readString(path));
    profileDataStructure.handleProperties(
      (propertyKey, jsonElement) -> {
        for (var listener : listeners.get(propertyKey)) {
          listener.accept(jsonElement);
        }
      });

    accountRegistry.setAccounts(profileDataStructure.accounts());
    accountRegistry.callLoadHooks();

    proxyRegistry.setProxies(profileDataStructure.proxies());
    proxyRegistry.callLoadHooks();
  }

  public void saveProfile(Path path) throws IOException {
    Files.createDirectories(path.getParent());

    Files.writeString(path, exportSettings());
  }

  public String exportSettings() {
    var settingsData = new LinkedHashMap<String, Map<String, JsonElement>>();
    for (var namespaceEntry : providers.entrySet()) {
      for (var entry : namespaceEntry.getValue().entrySet()) {
        var namespace = namespaceEntry.getKey();
        var key = entry.getKey();
        var value = entry.getValue().get();

        settingsData.computeIfAbsent(namespace, k -> new LinkedHashMap<>()).put(key, value);
      }
    }

    return new ProfileDataStructure(
      settingsData,
      accountRegistry.accounts().stream().toList(),
      proxyRegistry.proxies().stream().toList())
      .serialize();
  }

  public AttackStartRequest exportSettingsProto() {
    var namespaces = new ArrayList<SettingsNamespace>();

    for (var namespaceEntry : providers.entrySet()) {
      var namespace = namespaceEntry.getKey();
      var namespaceSettings = new ArrayList<SettingsEntry>();

      for (var entry : namespaceEntry.getValue().entrySet()) {
        var key = entry.getKey();
        var value = entry.getValue().get();

        if (!value.isJsonPrimitive()) {
          log.warn("Skipping non-primitive setting: {}#{}", namespace, key);
          continue;
        }

        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
          namespaceSettings.add(
            SettingsEntry.newBuilder()
              .setKey(key)
              .setBoolValue(primitive.getAsBoolean())
              .build());
        } else if (primitive.isNumber()) {
          var number = primitive.getAsNumber();
          if (number instanceof Integer) {
            namespaceSettings.add(
              SettingsEntry.newBuilder().setKey(key).setIntValue(number.intValue()).build());
          } else if (number instanceof Double) {
            namespaceSettings.add(
              SettingsEntry.newBuilder()
                .setKey(key)
                .setDoubleValue(number.doubleValue())
                .build());
          } else {
            log.warn("Skipping unsupported number setting: {}#{}", namespace, key);
          }
        } else if (primitive.isString()) {
          namespaceSettings.add(
            SettingsEntry.newBuilder()
              .setKey(key)
              .setStringValue(primitive.getAsString())
              .build());
        } else {
          log.warn("Skipping unsupported primitive setting: {}#{}", namespace, key);
        }
      }

      namespaces.add(
        SettingsNamespace.newBuilder()
          .setNamespace(namespace)
          .addAllEntries(namespaceSettings)
          .build());
    }

    return AttackStartRequest.newBuilder()
      .addAllSettings(namespaces)
      .addAllAccounts(
        accountRegistry.accounts().stream()
          .filter(EnabledWrapper::enabled)
          .map(EnabledWrapper::value)
          .map(MinecraftAccount::toProto)
          .toList())
      .addAllProxies(
        proxyRegistry.proxies().stream()
          .filter(EnabledWrapper::enabled)
          .map(EnabledWrapper::value)
          .map(SFProxy::toProto)
          .toList())
      .build();
  }
}
