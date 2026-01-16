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
public record InstanceSettingsImpl(Stem stem, ServerSettingsSource serverSettings) implements InstanceSettingsSource {
  @Override
  public Optional<JsonElement> get(Property<InstanceSettingsSource> property) {
    return this.stem.get(property);
  }

  @Override
  public List<MinecraftAccount> accounts() {
    return this.stem.accounts();
  }

  @Override
  public List<SFProxy> proxies() {
    return this.stem.proxies();
  }

  @With
  public record Stem(Map<String, Map<String, JsonElement>> settings,
                     List<MinecraftAccount> accounts,
                     List<SFProxy> proxies) implements SettingsSource.Stem<InstanceSettingsSource> {
    public static final Stem EMPTY = new Stem(Map.of(), List.of(), List.of());
    private static final Gson PROFILE_GSON =
      GsonInstance.GSON.newBuilder()
        .registerTypeAdapter(MinecraftAccount.class, new MinecraftAccountAdapter())
        .registerTypeAdapter(SocketAddress.class, SocketAddressHelper.TYPE_ADAPTER)
        .create();

    public Stem {
      // Remove duplicate accounts
      var newAccounts = new LinkedHashMap<UUID, MinecraftAccount>();
      for (var account : accounts) {
        newAccounts.put(account.profileId(), account);
      }

      accounts = List.copyOf(newAccounts.values());
    }

    public static Stem deserialize(JsonElement json) {
      return PROFILE_GSON.fromJson(json, Stem.class);
    }

    public static Stem fromProto(InstanceConfig request) {
      return
        new Stem(
          SettingsSource.Stem.settingsFromProto(request.getSettingsList()),
          request.getAccountsList().stream().map(MinecraftAccount::fromProto).toList(),
          request.getProxiesList().stream().map(SFProxy::fromProto).toList()
        );
    }

    public JsonObject serializeToTree() {
      return PROFILE_GSON.toJsonTree(this).getAsJsonObject();
    }

    public InstanceConfig toProto() {
      return InstanceConfig.newBuilder()
        .addAllSettings(this.settingsToProto())
        .addAllAccounts(this.accounts.stream().map(MinecraftAccount::toProto).toList())
        .addAllProxies(this.proxies.stream().map(SFProxy::toProto).toList())
        .build();
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
}
