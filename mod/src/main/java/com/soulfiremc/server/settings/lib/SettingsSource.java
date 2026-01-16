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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public sealed interface SettingsSource<S extends SettingsSource<S>> permits BotSettingsSource, InstanceSettingsSource, ServerSettingsSource {
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

  interface Stem<S extends SettingsSource<S>> {
    Map<String, Map<String, JsonElement>> settings();

    default Optional<JsonElement> get(Property<S> property) {
      return Optional.ofNullable(this.settings().get(property.namespace()))
        .flatMap(map -> Optional.ofNullable(map.get(property.key())));
    }

    static Map<String, Map<String, JsonElement>> settingsFromProto(List<SettingsNamespace> settingsList) {
      return settingsList.stream().collect(
          HashMap::new,
          (map, namespace) -> map.put(namespace.getNamespace(), namespace.getEntriesList().stream().collect(
            HashMap::new,
            (innerMap, entry) -> {
              try {
                innerMap.put(entry.getKey(), GsonInstance.GSON.fromJson(JsonFormat.printer().print(entry.getValue()), JsonElement.class));
              } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
              }
            },
            HashMap::putAll
          )),
          HashMap::putAll
        );
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
}
