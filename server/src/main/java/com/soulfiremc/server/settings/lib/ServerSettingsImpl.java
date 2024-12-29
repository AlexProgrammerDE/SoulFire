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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.ServerConfig;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.server.settings.PropertyKey;
import com.soulfiremc.server.util.structs.GsonInstance;
import lombok.SneakyThrows;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@With
@Slf4j
public record ServerSettingsImpl(
  Map<String, Map<String, JsonElement>> settings) implements ServerSettingsSource {
  public static final ServerSettingsImpl EMPTY = new ServerSettingsImpl(Map.of());
  private static final Gson PROFILE_GSON =
    new GsonBuilder()
      .setPrettyPrinting()
      .create();

  public static ServerSettingsImpl deserialize(JsonElement json) {
    return PROFILE_GSON.fromJson(json, ServerSettingsImpl.class);
  }

  @SneakyThrows
  public static ServerSettingsImpl fromProto(ServerConfig request) {
    var settingsProperties = new HashMap<String, Map<String, JsonElement>>();

    for (var namespace : request.getSettingsList()) {
      Map<String, JsonElement> namespaceProperties = new HashMap<>();

      for (var entry : namespace.getEntriesList()) {
        namespaceProperties.put(entry.getKey(), GsonInstance.GSON.fromJson(JsonFormat.printer().print(entry.getValue()), JsonElement.class));
      }

      settingsProperties.put(namespace.getNamespace(), namespaceProperties);
    }

    return new ServerSettingsImpl(settingsProperties);
  }

  public JsonObject serializeToTree() {
    return PROFILE_GSON.toJsonTree(this).getAsJsonObject();
  }

  @SneakyThrows
  public ServerConfig toProto() {
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

    return ServerConfig.newBuilder()
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
      .build();
  }

  @Override
  public Optional<JsonElement> get(PropertyKey key) {
    return Optional.ofNullable(settings.get(key.namespace()))
      .flatMap(map -> Optional.ofNullable(map.get(key.key())));
  }
}
