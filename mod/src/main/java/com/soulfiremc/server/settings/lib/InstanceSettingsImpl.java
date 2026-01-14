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

import com.google.gson.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.InstanceConfig;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.AccountData;
import com.soulfiremc.server.bot.BotEntity;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SocketAddressHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.net.SocketAddress;
import java.util.*;

@With
@Slf4j
public record InstanceSettingsImpl(
  Map<String, Map<String, JsonElement>> settings,
  List<MinecraftAccount> accounts,
  List<SFProxy> proxies,
  List<BotEntity> bots) implements InstanceSettingsSource {
  public static final InstanceSettingsImpl EMPTY = new InstanceSettingsImpl(Map.of(), List.of(), List.of(), List.of());
  private static final Gson PROFILE_GSON =
    GsonInstance.GSON.newBuilder()
      .registerTypeAdapter(MinecraftAccount.class, new MinecraftAccountAdapter())
      .registerTypeAdapter(SocketAddress.class, SocketAddressHelper.TYPE_ADAPTER)
      .create();

  public InstanceSettingsImpl {
    // Remove duplicate accounts
    var newAccounts = new LinkedHashMap<UUID, MinecraftAccount>();
    for (var account : accounts) {
      newAccounts.put(account.profileId(), account);
    }
    accounts = List.copyOf(newAccounts.values());

    // Remove duplicate proxies
    var newProxies = new LinkedHashMap<UUID, SFProxy>();
    for (var proxy : proxies) {
      newProxies.put(proxy.id(), proxy);
    }
    proxies = List.copyOf(newProxies.values());

    // Remove duplicate bots
    var newBots = new LinkedHashMap<UUID, BotEntity>();
    for (var bot : bots) {
      newBots.put(bot.id(), bot);
    }
    bots = List.copyOf(newBots.values());
  }

  public static InstanceSettingsImpl deserialize(JsonElement json) {
    return PROFILE_GSON.fromJson(json, InstanceSettingsImpl.class);
  }

  public static InstanceSettingsImpl fromProto(InstanceConfig request) {
    return new InstanceSettingsImpl(
      request.getSettingsList().stream().collect(
        HashMap::new,
        (map, namespace) -> map.put(namespace.getNamespace(), namespace.getEntriesList().stream().collect(
          HashMap::new,
          (innerMap, entry) -> {
            try {
              innerMap.put(entry.getKey(), GsonInstance.GSON.fromJson(JsonFormat.printer().print(entry.getValue()), JsonElement.class));
            } catch (InvalidProtocolBufferException e) {
              log.error("Failed to deserialize settings", e);
            }
          },
          HashMap::putAll
        )),
        HashMap::putAll
      ),
      request.getAccountsList().stream().map(MinecraftAccount::fromProto).toList(),
      request.getProxiesList().stream().map(SFProxy::fromProto).toList(),
      request.getBotsList().stream().map(BotEntity::fromProto).toList()
    );
  }

  public JsonObject serializeToTree() {
    return PROFILE_GSON.toJsonTree(this).getAsJsonObject();
  }

  public InstanceConfig toProto() {
    return InstanceConfig.newBuilder()
      .addAllSettings(this.settings.entrySet().stream()
        .map(entry -> SettingsNamespace.newBuilder()
          .setNamespace(entry.getKey())
          .addAllEntries(entry.getValue().entrySet().stream()
            .map(innerEntry -> SettingsEntry.newBuilder()
              .setKey(innerEntry.getKey())
              .setValue(SFHelpers.make(Value.newBuilder(), valueProto -> {
                try {
                  JsonFormat.parser().merge(GsonInstance.GSON.toJson(innerEntry.getValue()), valueProto);
                } catch (InvalidProtocolBufferException e) {
                  log.error("Failed to serialize settings", e);
                }
              }))
              .build())
            .toList())
          .build())
        .toList())
      .addAllAccounts(this.accounts.stream().map(MinecraftAccount::toProto).toList())
      .addAllProxies(this.proxies.stream().map(SFProxy::toProto).toList())
      .addAllBots(this.bots.stream().map(BotEntity::toProto).toList())
      .build();
  }

  @Override
  public Optional<JsonElement> get(Property property) {
    return Optional.ofNullable(settings.get(property.namespace()))
      .flatMap(map -> Optional.ofNullable(map.get(property.key())));
  }

  public Optional<MinecraftAccount> getAccountById(UUID profileId) {
    return accounts.stream().filter(a -> a.profileId().equals(profileId)).findFirst();
  }

  public Optional<SFProxy> getProxyById(UUID proxyId) {
    return proxies.stream().filter(p -> p.id().equals(proxyId)).findFirst();
  }

  public Optional<BotEntity> getBotById(UUID botId) {
    return bots.stream().filter(b -> b.id().equals(botId)).findFirst();
  }

  public InstanceSettingsImpl withUpdatedBot(BotEntity updatedBot) {
    var newBots = new ArrayList<>(bots);
    newBots.replaceAll(b -> b.id().equals(updatedBot.id()) ? updatedBot : b);
    return this.withBots(newBots);
  }

  public InstanceSettingsImpl withAddedBot(BotEntity newBot) {
    var newBots = new ArrayList<>(bots);
    newBots.add(newBot);
    return this.withBots(newBots);
  }

  public InstanceSettingsImpl withRemovedBot(UUID botId) {
    var newBots = bots.stream().filter(b -> !b.id().equals(botId)).toList();
    return this.withBots(newBots);
  }

  private static class MinecraftAccountAdapter
    implements JsonDeserializer<MinecraftAccount>, JsonSerializer<MinecraftAccount> {
    @Override
    public MinecraftAccount deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
      var authType =
        context.<AuthType>deserialize(json.getAsJsonObject().get("authType"), AuthType.class);

      return createGson(authType).fromJson(json, MinecraftAccount.class);
    }

    @Override
    public JsonElement serialize(
      MinecraftAccount src, Type typeOfSrc, JsonSerializationContext context) {
      return createGson(src.authType()).toJsonTree(src, MinecraftAccount.class);
    }

    private Gson createGson(AuthType authType) {
      return GsonInstance.GSON.newBuilder()
        .registerTypeAdapter(AccountData.class, new AccountDataAdapter(authType))
        .create();
    }

    private record AccountDataAdapter(AuthType authType)
      implements JsonDeserializer<AccountData>, JsonSerializer<AccountData> {
      @Override
      public AccountData deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
        return PROFILE_GSON.fromJson(json, authType.accountDataClass());
      }

      @Override
      public JsonElement serialize(
        AccountData src, Type typeOfSrc, JsonSerializationContext context) {
        return PROFILE_GSON.toJsonTree(src, authType.accountDataClass());
      }
    }
  }
}
