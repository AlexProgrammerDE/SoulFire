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
package com.soulfiremc.server.util;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * This class is responsible for making a map that is both k -> v and v -> k.
 */
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class DualMap<L, R> {
  private final Map<L, R> map;
  private final Map<R, L> reverseMap;

  public DualMap(Map<L, R> map) {
    this.map = map;
    this.reverseMap = new HashMap<>();
    for (var entry : map.entrySet()) {
      reverseMap.put(entry.getValue(), entry.getKey());
    }
  }

  public static <L extends Enum<L>, R> DualMap<L, R> forEnumSwitch(Class<L> clazz, Function<L, R> mapper) {
    return new DualMap<>(
      Arrays.stream(clazz.getEnumConstants())
        .collect(Collectors.toMap(Function.identity(), mapper))
    );
  }

  public static <T> Codec<T> keyCodec(DualMap<T, String> map) {
    return Codec.STRING.xmap(map::getLeft, map::getRight);
  }

  public static <T> Codec<T> valueCodec(DualMap<String, T> map) {
    return Codec.STRING.xmap(map::getRight, map::getLeft);
  }

  public R getRight(L key) {
    return map.get(key);
  }

  public L getLeft(R value) {
    return reverseMap.get(value);
  }
}
