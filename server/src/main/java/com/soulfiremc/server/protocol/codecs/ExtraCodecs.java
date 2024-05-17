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
package com.soulfiremc.server.protocol.codecs;

import com.google.common.primitives.UnsignedBytes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.Function;

@SuppressWarnings("SameParameterValue")
public class ExtraCodecs {
  public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
    .flatComapMap(
      UnsignedBytes::toInt,
      integer -> integer > 255 ? DataResult.error(() -> "Unsigned byte was too large: " + integer + " > 255") : DataResult.success(integer.byteValue())
    );
  public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, integer -> "Value must be non-negative: " + integer);
  public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, floatValue -> "Value must be positive: " + floatValue);

  private static Codec<Integer> intRangeWithMessage(int min, int max, Function<Integer, String> errorMessage) {
    return Codec.INT
      .validate(
        integer -> integer.compareTo(min) >= 0 && integer.compareTo(max) <= 0
          ? DataResult.success(integer)
          : DataResult.error(() -> errorMessage.apply(integer))
      );
  }

  private static Codec<Float> floatRangeMinExclusiveWithMessage(float min, float max, Function<Float, String> errorMessage) {
    return Codec.FLOAT
      .validate(
        floatValue -> floatValue.compareTo(min) > 0 && floatValue.compareTo(max) <= 0 ? DataResult.success(floatValue) : DataResult.error(() -> errorMessage.apply(floatValue))
      );
  }
}
