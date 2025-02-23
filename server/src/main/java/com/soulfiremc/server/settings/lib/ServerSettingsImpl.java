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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.ServerConfig;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.GsonInstance;
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

  public static ServerSettingsImpl fromProto(ServerConfig request) {
    return new ServerSettingsImpl(
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
      )
    );
  }

  public JsonObject serializeToTree() {
    return PROFILE_GSON.toJsonTree(this).getAsJsonObject();
  }

  public ServerConfig toProto() {
    return ServerConfig.newBuilder()
      .addAllSettings(this.settings.entrySet().stream()
        .map(entry -> SettingsNamespace.newBuilder()
          .setNamespace(entry.getKey())
          .addAllEntries(entry.getValue().entrySet()
            .stream()
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
      .build();
  }

  @Override
  public Optional<JsonElement> get(Property property) {
    return Optional.ofNullable(settings.get(property.namespace()))
      .flatMap(map -> Optional.ofNullable(map.get(property.key())));
  }
}
