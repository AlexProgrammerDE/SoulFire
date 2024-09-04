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
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.grpc.generated.AuthRequest;
import com.soulfiremc.grpc.generated.InstanceConfig;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.server.settings.lib.SettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.GsonInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientSettingsManager {
  private final Multimap<PropertyKey, Consumer<JsonElement>> listeners =
    Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
  private final Map<String, Map<String, Provider<JsonElement>>> providers = new LinkedHashMap<>();
  private final RPCClient rpcClient;
  private SettingsImpl settingsSource = SettingsImpl.EMPTY;

  public void registerProvider(PropertyKey property, Provider<JsonElement> provider) {
    providers
      .computeIfAbsent(property.namespace(), k -> new LinkedHashMap<>())
      .put(property.key(), provider);
  }

  public void registerListener(PropertyKey property, Consumer<JsonElement> listener) {
    listeners.put(property, listener);
  }

  public void loadProfile(Path path) throws IOException {
    settingsSource = SettingsImpl.deserialize(GsonInstance.GSON.fromJson(Files.readString(path), JsonElement.class));
    handleProperties(
      (propertyKey, jsonElement) -> {
        for (var listener : listeners.get(propertyKey)) {
          listener.accept(jsonElement);
        }
      });
  }

  public void handleProperties(BiConsumer<PropertyKey, JsonElement> consumer) {
    for (var entry : settingsSource.settings().entrySet()) {
      var namespace = entry.getKey();
      for (var setting : entry.getValue().entrySet()) {
        var key = setting.getKey();
        var settingData = setting.getValue();

        var propertyKey = new PropertyKey(namespace, key);

        // Notify all listeners that this setting has been loaded
        consumer.accept(propertyKey, settingData);
      }
    }
  }

  public InstanceConfig exportSettingsProto() {
    return settingsSource.toProto();
  }

  public void loadFromString(String data, ProxyParser proxyParser) {
    try {
      var newProxies =
        data.lines()
          .map(String::strip)
          .filter(Predicate.not(String::isBlank))
          .distinct()
          .map(proxyParser::parse)
          .toList();

      if (newProxies.isEmpty()) {
        log.warn("No proxies found in the provided data!");
        return;
      }

      settingsSource = new SettingsImpl(
        settingsSource.settings(),
        settingsSource.accounts(),
        newProxies
      );

      log.info("Loaded {} proxies!", newProxies.size());
    } catch (Exception e) {
      log.error("Failed to load proxies from string!", e);
    }
  }

  public void loadFromString(String data, AuthType authType, SFProxy proxy) {
    try {
      var newAccounts =
        data.lines()
          .map(String::strip)
          .filter(Predicate.not(String::isBlank))
          .distinct()
          .map(account -> fromStringSingle(account, authType, proxy))
          .toList();

      if (newAccounts.isEmpty()) {
        log.warn("No accounts found in the provided data!");
        return;
      }

      settingsSource = new SettingsImpl(
        settingsSource.settings(),
        newAccounts,
        settingsSource.proxies()
      );

      log.info("Loaded {} accounts!", newAccounts.size());
    } catch (Exception e) {
      log.error("Failed to load accounts from string!", e);
    }
  }

  private MinecraftAccount fromStringSingle(String data, AuthType authType, SFProxy proxy) {
    try {
      var request =
        AuthRequest.newBuilder()
          .setService(MinecraftAccountProto.AccountTypeProto.valueOf(authType.name()))
          .setPayload(data);

      if (proxy != null) {
        request.setProxy(proxy.toProto());
      }

      return MinecraftAccount.fromProto(
        rpcClient.mcAuthServiceBlocking().login(request.build()).getAccount());
    } catch (Exception e) {
      log.error("Failed to load account from string", e);
      throw new RuntimeException(e);
    }
  }
}
