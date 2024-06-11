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
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.soulfiremc.server.data.Registry;
import com.soulfiremc.server.data.RegistryValue;
import com.soulfiremc.server.data.TagKey;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

@SuppressWarnings("SameParameterValue")
public class ExtraCodecs {
  public static final Codec<UUID> UUID_CODEC = Codec.INT_STREAM
    .comapFlatMap(uuids -> fixedSize(uuids, 4).map(ExtraCodecs::uuidFromIntArray), uuid -> Arrays.stream(uuidToIntArray(uuid)));
  @SuppressWarnings("PatternValidation")
  public static final Codec<Key> KYORI_KEY_CODEC = Codec.STRING
    .comapFlatMap(
      s -> Key.parseable(s) ? DataResult.success(Key.key(s)) : DataResult.error(() -> "Not a valid key: " + s),
      Key::asString
    );
  public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
    .flatComapMap(
      UnsignedBytes::toInt,
      integer -> integer > 255 ? DataResult.error(() -> "Unsigned byte was too large: " + integer + " > 255") : DataResult.success(integer.byteValue())
    );
  public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, integer -> "Value must be non-negative: " + integer);
  public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, floatValue -> "Value must be positive: " + floatValue);

  public static <T extends RegistryValue<T>> Codec<HolderSet> holderSetCodec(Registry<T> registry) {
    return Codec.either(
      homogenousList(registry.keyCodec(), false)
        .xmap(l -> l.stream().mapToInt(RegistryValue::id).toArray(), l2 -> Arrays.stream(l2).mapToObj(registry::getById).toList())
        .xmap(HolderSet::new, HolderSet::getHolders),
      TagKey.hashedCodec(registry.registryKey())
        .xmap(
          tagKey -> new HolderSet(tagKey.key()),
          holderSet -> new TagKey<>(registry.registryKey(), Objects.requireNonNull(holderSet.getLocation()))
        )
    ).xmap(
      either -> either.left().orElseGet(() -> either.right().orElseThrow()),
      holderSet -> holderSet.getHolders() == null ? Either.right(holderSet) : Either.left(holderSet)
    );
  }

  public static UUID uuidFromIntArray(int[] bits) {
    return new UUID((long) bits[0] << 32 | (long) bits[1] & 4294967295L, (long) bits[2] << 32 | (long) bits[3] & 4294967295L);
  }

  public static int[] uuidToIntArray(UUID uuid) {
    var l = uuid.getMostSignificantBits();
    var m = uuid.getLeastSignificantBits();
    return leastMostToIntArray(l, m);
  }

  private static int[] leastMostToIntArray(long most, long least) {
    return new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
  }

  public static DataResult<int[]> fixedSize(IntStream stream, int size) {
    var is = stream.limit(size + 1).toArray();
    if (is.length != size) {
      Supplier<String> supplier = () -> "Input is not a list of " + size + " ints";
      return is.length >= size ? DataResult.error(supplier, Arrays.copyOf(is, size)) : DataResult.error(supplier);
    } else {
      return DataResult.success(is);
    }
  }

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

  public static <E> Codec<List<E>> homogenousList(Codec<E> holderCodec, boolean disallowInline) {
    var codec2 = holderCodec.listOf();
    return disallowInline
      ? codec2
      : Codec.either(codec2, holderCodec)
      .xmap(
        either -> either.map(homogenousList -> homogenousList, List::of),
        holderCodecx -> holderCodecx.size() == 1 ? Either.right(holderCodecx.getFirst()) : Either.left(holderCodecx)
      );
  }
}
