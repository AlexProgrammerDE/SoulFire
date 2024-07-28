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
package com.soulfiremc.server.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.soulfiremc.server.protocol.codecs.ExtraCodecs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;

public record TagKey<T extends RegistryValue<T>>(ResourceKey<? extends Registry<T>> registry, Key key) {
  public static <T extends RegistryValue<T>> TagKey<T> key(@KeyPattern String key, ResourceKey<?> registry) {
    return key(Key.key(key), registry);
  }

  @SuppressWarnings("unchecked")
  public static <T extends RegistryValue<T>> TagKey<T> key(Key key, ResourceKey<?> registry) {
    return new TagKey<>((ResourceKey<? extends Registry<T>>) registry, key);
  }

  public static <T extends RegistryValue<T>> Codec<TagKey<T>> codec(ResourceKey<? extends Registry<T>> registry) {
    return ExtraCodecs.KYORI_KEY_CODEC.xmap(path -> new TagKey<>(registry, path), TagKey::key);
  }

  @SuppressWarnings("PatternValidation")
  public static <T extends RegistryValue<T>> Codec<TagKey<T>> hashedCodec(ResourceKey<? extends Registry<T>> registry) {
    return Codec.STRING
      .comapFlatMap(
        location -> location.startsWith("#") && Key.parseable(location.substring(1))
          ? DataResult.success(new TagKey<>(registry, Key.key(location.substring(1))))
          : DataResult.error(() -> "Not a tag id"),
        tagKey -> "#" + tagKey.key()
      );
  }
}
