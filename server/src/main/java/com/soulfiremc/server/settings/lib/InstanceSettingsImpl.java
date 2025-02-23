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
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SocketAddressHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@With
@Slf4j
public record InstanceSettingsImpl(
  Map<String, Map<String, JsonElement>> settings,
  List<MinecraftAccount> accounts,
  List<SFProxy> proxies) implements InstanceSettingsSource {
  public static final InstanceSettingsImpl EMPTY = new InstanceSettingsImpl(Map.of(), List.of(), List.of());
  private static final Gson PROFILE_GSON =
    new GsonBuilder()
      .registerTypeHierarchyAdapter(ECPublicKey.class, new ECPublicKeyAdapter())
      .registerTypeHierarchyAdapter(ECPrivateKey.class, new ECPrivateKeyAdapter())
      .registerTypeAdapter(MinecraftAccount.class, new MinecraftAccountAdapter())
      .registerTypeAdapter(SocketAddress.class, SocketAddressHelper.TYPE_ADAPTER)
      .setPrettyPrinting()
      .create();

  public InstanceSettingsImpl {
    // Remove duplicate accounts
    var newAccounts = new LinkedHashMap<UUID, MinecraftAccount>();
    for (var account : accounts) {
      newAccounts.put(account.profileId(), account);
    }

    accounts = List.copyOf(newAccounts.values());
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
      request.getProxiesList().stream().map(SFProxy::fromProto).toList()
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
      .build();
  }

  @Override
  public Optional<JsonElement> get(Property property) {
    return Optional.ofNullable(settings.get(property.namespace()))
      .flatMap(map -> Optional.ofNullable(map.get(property.key())));
  }

  private static class ECPublicKeyAdapter extends AbstractKeyAdapter<ECPublicKey> {
    @Override
    protected ECPublicKey createKey(byte[] bytes) throws JsonParseException {
      try {
        var keyFactory = KeyFactory.getInstance("EC");
        return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
      } catch (GeneralSecurityException e) {
        throw new JsonParseException(e);
      }
    }
  }

  private static class ECPrivateKeyAdapter extends AbstractKeyAdapter<ECPrivateKey> {
    @Override
    protected ECPrivateKey createKey(byte[] bytes) throws JsonParseException {
      try {
        var keyFactory = KeyFactory.getInstance("EC");
        return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
      } catch (GeneralSecurityException e) {
        throw new JsonParseException(e);
      }
    }
  }

  private abstract static class AbstractKeyAdapter<T>
    implements JsonSerializer<Key>, JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
      return createKey(Base64.getDecoder().decode(json.getAsString()));
    }

    @Override
    public JsonElement serialize(Key src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
    }

    protected abstract T createKey(byte[] bytes) throws JsonParseException;
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
      return new GsonBuilder()
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
