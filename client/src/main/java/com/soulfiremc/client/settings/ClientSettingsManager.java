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
import com.soulfiremc.client.cli.SFCommandDefinition;
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.grpc.generated.AccountTypeCredentials;
import com.soulfiremc.grpc.generated.CredentialsAuthRequest;
import com.soulfiremc.grpc.generated.CredentialsAuthResponse;
import com.soulfiremc.grpc.generated.InstanceConfig;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.util.SFHelpers;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClientSettingsManager {
  private final Multimap<PropertyKey, Consumer<JsonElement>> listeners =
    Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
  private final Map<String, Map<String, Provider<JsonElement>>> providers = new LinkedHashMap<>();
  private final RPCClient rpcClient;
  @Setter
  private SFCommandDefinition commandDefinition;
  private InstanceSettingsImpl settingsSource = InstanceSettingsImpl.EMPTY;

  public void registerProvider(PropertyKey property, Provider<JsonElement> provider) {
    providers
      .computeIfAbsent(property.namespace(), k -> new LinkedHashMap<>())
      .put(property.key(), provider);
  }

  public void registerListener(PropertyKey property, Consumer<JsonElement> listener) {
    listeners.put(property, listener);
  }

  public InstanceConfig exportSettingsProto(UUID instanceId) {
    // Load accounts
    if (commandDefinition.accountFile() != null && commandDefinition.authType() != null) {
      try {
        loadFromString(instanceId, Files.readString(commandDefinition.accountFile()), commandDefinition.authType());
      } catch (IOException e) {
        log.error("Failed to load accounts!", e);
        throw new RuntimeException(e);
      }
    }

    // Load proxies
    if (commandDefinition.proxyFile() != null) {
      try {
        loadFromString(
          Files.readString(commandDefinition.proxyFile()),
          commandDefinition.proxyType() == null ? ProxyParser.uriParser() : ProxyParser.typeParser(commandDefinition.proxyType()));
      } catch (IOException e) {
        log.error("Failed to load proxies!", e);
        throw new RuntimeException(e);
      }
    }

    // Load settings
    gatherProviders();
    return settingsSource.toProto();
  }

  private void gatherProviders() {
    var settings = new HashMap<String, Map<String, JsonElement>>();
    providers.forEach((namespace, properties) -> {
      var namespaceMap = new HashMap<String, JsonElement>();
      properties.forEach((key, provider) ->
        namespaceMap.put(key, provider.get()));

      settings.put(namespace, namespaceMap);
    });

    settingsSource = settingsSource.withSettings(settings);
  }

  private void loadFromString(String data, ProxyParser proxyParser) {
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

      settingsSource = settingsSource.withProxies(newProxies);

      log.info("Loaded {} proxies!", newProxies.size());
    } catch (Exception e) {
      log.error("Failed to load proxies from string!", e);
    }
  }

  private void loadFromString(UUID instanceId, String data, AuthType authType) {
    try {
      var newAccounts =
        fromStringList(instanceId, data.lines()
          .map(String::strip)
          .filter(Predicate.not(String::isBlank))
          .distinct()
          .toList(), authType);

      if (newAccounts.isEmpty()) {
        log.warn("No accounts found in the provided data!");
        return;
      }

      settingsSource = settingsSource.withAccounts(newAccounts);

      log.info("Loaded {} accounts!", newAccounts.size());
    } catch (Exception e) {
      log.error("Failed to load accounts from string!", e);
    }
  }

  private List<MinecraftAccount> fromStringList(UUID instanceId, List<String> accounts, AuthType authType) {
    try {
      var request =
        CredentialsAuthRequest.newBuilder()
          .setInstanceId(instanceId.toString())
          .setService(AccountTypeCredentials.valueOf(authType.name()))
          .addAllPayload(accounts);

      return SFHelpers.awaitResultPredicate(
          rpcClient.mcAuthServiceBlocking().loginCredentials(request.build()),
          CredentialsAuthResponse::hasFullList)
        .orElseThrow()
        .getFullList()
        .getAccountList()
        .stream()
        .map(MinecraftAccount::fromProto)
        .toList();
    } catch (Exception e) {
      log.error("Failed to load account from string", e);
      throw new RuntimeException(e);
    }
  }
}
