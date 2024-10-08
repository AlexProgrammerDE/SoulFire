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

import com.google.gson.JsonElement;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.settings.PropertyKey;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.GsonInstance;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public interface SettingsSource {
  List<MinecraftAccount> accounts();

  List<SFProxy> proxies();

  default int get(IntProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), Integer.class);
  }

  default double get(DoubleProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), Double.class);
  }

  default boolean get(BooleanProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), Boolean.class);
  }

  default String get(StringProperty property) {
    return getAsType(property.propertyKey(), property.defaultValue(), String.class);
  }

  default <T> T get(ComboProperty property, Function<String, T> converter) {
    return converter.apply(getAsType(property.propertyKey(), property.options()[property.defaultValue()].id(), String.class));
  }

  default <T extends Enum<T>> T get(ComboProperty property, Class<T> clazz) {
    return get(property, s -> Enum.valueOf(clazz, s));
  }

  default List<String> get(StringListProperty property) {
    return List.of(getAsType(property.propertyKey(), property.defaultValue().toArray(new String[0]), String[].class));
  }

  default CustomIntSupplier getRandom(MinMaxPropertyLink property) {
    return () -> SFHelpers.getRandomInt(get(property.min()), get(property.max()));
  }

  default <T> T getAsType(PropertyKey key, T defaultValue, Class<T> clazz) {
    return get(key).map(v -> GsonInstance.GSON.fromJson(v, clazz)).orElse(defaultValue);
  }

  Optional<JsonElement> get(PropertyKey key);

  interface CustomIntSupplier extends IntSupplier {
    default LongSupplier asLongSupplier() {
      return this::getAsLong;
    }

    default long getAsLong() {
      return this.getAsInt();
    }
  }
}
