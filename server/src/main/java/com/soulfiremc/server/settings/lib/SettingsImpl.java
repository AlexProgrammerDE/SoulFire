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
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.InstanceConfig;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.AccountData;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.SocketAddressHelper;
import lombok.SneakyThrows;

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

public record SettingsImpl(
  Map<String, Map<String, JsonElement>> settings,
  List<MinecraftAccount> accounts,
  List<SFProxy> proxies) implements SettingsSource {
  public static final SettingsImpl EMPTY = new SettingsImpl(Map.of(), List.of(), List.of());
  private static final Gson PROFILE_GSON =
    new GsonBuilder()
      .registerTypeHierarchyAdapter(ECPublicKey.class, new ECPublicKeyAdapter())
      .registerTypeHierarchyAdapter(ECPrivateKey.class, new ECPrivateKeyAdapter())
      .registerTypeAdapter(MinecraftAccount.class, new MinecraftAccountAdapter())
      .registerTypeAdapter(SocketAddress.class, SocketAddressHelper.TYPE_ADAPTER)
      .setPrettyPrinting()
      .create();

  public static SettingsImpl deserialize(JsonElement json) {
    return PROFILE_GSON.fromJson(json, SettingsImpl.class);
  }

  public JsonObject serializeToTree() {
    return PROFILE_GSON.toJsonTree(this).getAsJsonObject();
  }

  @SneakyThrows
  public static SettingsImpl fromProto(InstanceConfig request) {
    var settingsProperties = new HashMap<String, Map<String, JsonElement>>();

    for (var namespace : request.getSettingsList()) {
      Map<String, JsonElement> namespaceProperties = new HashMap<>();

      for (var entry : namespace.getEntriesList()) {
        namespaceProperties.put(entry.getKey(), GsonInstance.GSON.fromJson(JsonFormat.printer().print(entry.getValue()), JsonElement.class));
      }

      settingsProperties.put(namespace.getNamespace(), namespaceProperties);
    }

    var accounts = new ArrayList<MinecraftAccount>();
    for (var account : request.getAccountsList()) {
      accounts.add(MinecraftAccount.fromProto(account));
    }

    var proxies = new ArrayList<SFProxy>();
    for (var proxy : request.getProxiesList()) {
      proxies.add(SFProxy.fromProto(proxy));
    }

    return new SettingsImpl(settingsProperties, accounts, proxies);
  }

  @SneakyThrows
  public InstanceConfig toProto() {
    var settingsProperties = new HashMap<String, Map<String, Value>>();
    for (var entry : this.settings.entrySet()) {
      var namespace = entry.getKey();
      var innerMap = new HashMap<String, Value>();

      for (var innerEntry : entry.getValue().entrySet()) {
        var key = innerEntry.getKey();
        var value = innerEntry.getValue();

        var valueProto = Value.newBuilder();
        JsonFormat.parser().merge(GsonInstance.GSON.toJson(value), valueProto);

        innerMap.put(key, valueProto.build());
      }

      settingsProperties.put(namespace, innerMap);
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

  @Override
  public Optional<JsonElement> get(PropertyKey key) {
    return Optional.ofNullable(settings.get(key.namespace()))
      .flatMap(map -> Optional.ofNullable(map.get(key.key())));
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
