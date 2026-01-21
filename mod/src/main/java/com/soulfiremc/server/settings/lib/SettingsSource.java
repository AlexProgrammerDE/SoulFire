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

import com.google.gson.JsonElement;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.SettingsEntry;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.GsonInstance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public sealed interface SettingsSource<S extends SettingsSource.SourceType> permits BotSettingsSource, InstanceSettingsSource, ServerSettingsSource {
  default int get(IntProperty<S> property) {
    return getAsType(property, property.defaultValue(), Integer.class);
  }

  default double get(DoubleProperty<S> property) {
    return getAsType(property, property.defaultValue(), Double.class);
  }

  default boolean get(BooleanProperty<S> property) {
    return getAsType(property, property.defaultValue(), Boolean.class);
  }

  default String get(StringProperty<S> property) {
    return getAsType(property, property.defaultValue(), String.class);
  }

  default <T> T get(ComboProperty<S> property, Function<String, T> converter) {
    return converter.apply(getAsType(property, property.defaultValue(), String.class));
  }

  default <T extends Enum<T>> T get(ComboProperty<S> property, Class<T> clazz) {
    return get(property, s -> Enum.valueOf(clazz, s));
  }

  default List<String> get(StringListProperty<S> property) {
    return List.of(getAsType(property, property.defaultValue().toArray(new String[0]), String[].class));
  }

  default MinMaxProperty.DataLayout get(MinMaxProperty<S> property) {
    return getAsType(property, property.defaultDataLayout(), MinMaxProperty.DataLayout.class);
  }

  default CustomIntSupplier getRandom(MinMaxProperty<S> property) {
    return () -> {
      var layout = get(property);
      return SFHelpers.getRandomInt(layout.min(), layout.max());
    };
  }

  default <T> T getAsType(Property<S> property, T defaultValue, Class<T> clazz) {
    return get(property).map(v -> GsonInstance.GSON.fromJson(v, clazz)).orElse(defaultValue);
  }

  Optional<JsonElement> get(Property<S> property);

  interface CustomIntSupplier extends IntSupplier {
    default LongSupplier asLongSupplier() {
      return this::getAsLong;
    }

    default long getAsLong() {
      return this.getAsInt();
    }
  }

  Stem<S> stem();

  interface Stem<S extends SettingsSource.SourceType> {
    Map<String, Map<String, JsonElement>> settings();

    static Optional<JsonElement> getFromRawSettings(Map<String, Map<String, JsonElement>> settings, Property<?> property) {
      return Optional.ofNullable(settings.get(property.namespace()))
        .flatMap(map -> Optional.ofNullable(map.get(property.key())));
    }

    default Optional<JsonElement> get(Property<S> property) {
      return getFromRawSettings(this.settings(), property);
    }

    static Map<String, Map<String, JsonElement>> settingsFromProto(List<SettingsNamespace> settingsList) {
      return settingsList.stream().collect(
          LinkedHashMap::new,
          (map, namespace) -> map.put(namespace.getNamespace(), namespace.getEntriesList().stream().collect(
            LinkedHashMap::new,
            (innerMap, entry) -> {
              try {
                innerMap.put(entry.getKey(), GsonInstance.GSON.fromJson(JsonFormat.printer().print(entry.getValue()), JsonElement.class));
              } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
              }
            },
            LinkedHashMap::putAll
          )),
          LinkedHashMap::putAll
        );
    }

    static JsonElement valueToJsonElement(Value value) {
      try {
        return GsonInstance.GSON.fromJson(JsonFormat.printer().print(value), JsonElement.class);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
    }

    static Map<String, Map<String, JsonElement>> withUpdatedEntry(
        Map<String, Map<String, JsonElement>> settings,
        String namespace,
        String key,
        JsonElement value) {
      var newSettings = new LinkedHashMap<>(settings);
      var namespaceMap = new LinkedHashMap<>(newSettings.getOrDefault(namespace, Map.of()));
      namespaceMap.put(key, value);
      newSettings.put(namespace, namespaceMap);
      return newSettings;
    }

    default Iterable<? extends SettingsNamespace> settingsToProto() {
      return this.settings().entrySet().stream()
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
                  throw new RuntimeException(e);
                }
              }))
              .build())
            .toList())
          .build())
        .toList();
    }
  }

  sealed interface SourceType {
  }

  final class Bot implements SourceType {
    public static Bot INSTANCE = new Bot();

    private Bot() {
    }
  }

  final class Instance implements SourceType {
    public static Instance INSTANCE = new Instance();

    private Instance() {
    }
  }

  final class Server implements SourceType {
    public static Server INSTANCE = new Server();

    private Server() {
    }
  }
}
